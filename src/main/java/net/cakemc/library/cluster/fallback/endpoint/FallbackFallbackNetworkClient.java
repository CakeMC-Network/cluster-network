package net.cakemc.library.cluster.fallback.endpoint;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.codec.CompressionCodec;
import net.cakemc.library.cluster.fallback.endpoint.codec.FallbackPacketCodec;
import net.cakemc.library.cluster.fallback.endpoint.handler.FallbackBossHandler;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Represents a network client in the cluster that connects to server nodes.
 *
 * <p>The {@code FallbackFallbackNetworkClient} class extends {@code FallbackNetworkPoint} to implement
 * client-specific behavior for establishing connections, sending backPackets, and
 * handling reconnections in case of failures.</p>
 *
 * @see FallbackNetworkPoint
 * @see RingBackPacket
 */
public class FallbackFallbackNetworkClient extends FallbackNetworkPoint {

	/**
	 * Timeout duration (in milliseconds) for retrying to connect to the server.
	 */
	public static final long RETRY_TIMEOUT = 5000;

	// Configuration
	private EventLoopGroup eventLoopGroup;
	private Class<? extends SocketChannel> channel;
	private ChannelFuture channelFuture;

	// Reconnection state
	private boolean reconnecting;
	private long lastAttemptTime;
	private boolean awaitFirstStart = true;

	/**
	 * Constructs a new {@code FallbackFallbackNetworkClient} with the specified cluster node and
	 * node information.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 * @param nodeInformation the {@link MemberIdentifier} containing address details
	 */
	public FallbackFallbackNetworkClient(AbstractBackUpEndpoint clusterNode, MemberIdentifier nodeInformation) {
		super(clusterNode, nodeInformation);
	}

	/**
	 * Initializes the network client by setting up the event loop group and channel type
	 * based on the available I/O model.
	 */
	@Override
	public void initialize() {
		IoHandlerFactory ioHandlerFactory = EPOLL ? (KQUEUE ? KQueueIoHandler.newFactory() : EpollIoHandler.newFactory()) : NioIoHandler.newFactory();

		this.eventLoopGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);
		this.channel = EPOLL ? (KQUEUE ? KQueueSocketChannel.class : EpollSocketChannel.class) : NioSocketChannel.class;
	}

	/**
	 * Establishes a connection to the server using the specified host and port.
	 *
	 * <p>If the connection attempt fails, it enters a reconnecting state that will
	 * periodically attempt to reconnect based on the configured timeout.</p>
	 */
	@Override
	public void connect() {
		this.awaitFirstStart = false;
		if (this.checkAvailable(this.host, this.port)) {
			this.reconnecting = true;
			lastAttemptTime = System.currentTimeMillis();
		}
		try {
			// Set up the client bootstrap
			this.channelFuture = new Bootstrap()
				 .channel(channel)
				 .option(ChannelOption.IP_TOS, 24)
				 .option(ChannelOption.TCP_NODELAY, true)
				 .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				 .option(ChannelOption.SO_REUSEADDR, true)
				 .group(eventLoopGroup)
				 .handler(new ChannelInitializer<>() {
					 @Override
					 protected void initChannel(Channel channel) throws Exception {
						 ChannelPipeline channelPipeline = channel.pipeline();
						 channelPipeline.addFirst(COMPRESSION_CODEC, new CompressionCodec());
						 channelPipeline.addAfter(COMPRESSION_CODEC, PACKET_CODEC, new FallbackPacketCodec(clusterNode.getPacketRegistry()));
						 channelPipeline.addAfter(PACKET_CODEC, BOSS_HANDLER, new FallbackBossHandler(clusterNode, EndpointType.CLIENT));
					 }
				 })
				 .remoteAddress(host, port)
				 .connect()
				 .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
				 .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				 .addListener((ChannelFutureListener) channelFuture -> {
					 if (channelFuture.isSuccess()) {
						 this.connected = true;
						 this.reconnecting = false;
					 } else {
						 this.connected = false;
						 this.reconnecting = true;
						 lastAttemptTime = System.currentTimeMillis();
					 }
				 })
				 .syncUninterruptibly();

			this.channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			this.reconnecting = true;
			lastAttemptTime = System.currentTimeMillis();
		}
	}

	/**
	 * Dispatches a packet to the connected server.
	 *
	 * <p>If the connection is not active, this method does nothing.</p>
	 *
	 * @param packet the {@link RingBackPacket} to be dispatched
	 */
	@Override
	public void dispatchPacket(RingBackPacket packet) {
		super.dispatchPacket(packet);

		if (channelFuture == null || !channelFuture.channel().isActive())
			return;

		channelFuture.channel().writeAndFlush(packet)
		             .addListener((ChannelFutureListener) channelFuture -> {
			             if (!channelFuture.isSuccess() && channelFuture.cause() != null)
				             channelFuture.cause().printStackTrace();
		             });
	}

	/**
	 * Executes periodic tasks, including reconnecting if the client is not connected.
	 */
	@Override
	public void tick() {
		if (!connected && !reconnecting) {
			// Start reconnect attempt
			this.reconnecting = true;
			lastAttemptTime = System.currentTimeMillis();
		}

		if (reconnecting && System.currentTimeMillis() - lastAttemptTime > RETRY_TIMEOUT && !awaitFirstStart) {
			connect();
		}
	}

	/**
	 * Checks the availability of a host and port by attempting to connect.
	 *
	 * @param host the host address to check
	 * @param port the port number to check
	 * @return {@code true} if the host and port are available, {@code false} otherwise
	 */
	boolean checkAvailable(String host, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 1000);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Shuts down the network client, releasing any resources and connections.
	 */
	@Override
	public void shutdown() {
		if (this.channelFuture != null) {
			this.channelFuture.channel().close().syncUninterruptibly();
		}
		if (this.eventLoopGroup != null) {
			this.eventLoopGroup.shutdownGracefully().syncUninterruptibly();
		}
		this.connected = false;
	}
}
