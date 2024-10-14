package net.cakemc.library.cluster.fallback.endpoint;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.codec.CompressionCodec;
import net.cakemc.library.cluster.fallback.endpoint.codec.FallbackPacketCodec;
import net.cakemc.library.cluster.fallback.endpoint.handler.FallbackBossHandler;

/**
 * Represents a network server in the cluster that listens for incoming connections from clients.
 *
 * <p>The {@code FallbackFallbackNetworkServer} class extends {@code FallbackNetworkPoint} to implement server-specific
 * behavior for initializing the server, handling incoming connections, and shutting down.</p>
 *
 * @see FallbackNetworkPoint
 * @see FallbackBossHandler
 */
public class FallbackFallbackNetworkServer extends FallbackNetworkPoint {

	// Configuration
	private EventLoopGroup bossGroup, workerGroup;
	private Class<? extends ServerChannel> channel;

	// Server
	private ServerBootstrap serverBootstrap;
	private ChannelFuture channelFuture;

	/**
	 * Constructs a new {@code FallbackFallbackNetworkServer} with the specified cluster node.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 */
	public FallbackFallbackNetworkServer(AbstractBackUpEndpoint clusterNode) {
		super(clusterNode);
	}

	/**
	 * Initializes the network server by setting up the boss and worker event loop groups
	 * and determining the channel type based on the available I/O model.
	 */
	@Override
	public void initialize() {
		IoHandlerFactory ioHandlerFactory = EPOLL ? (KQUEUE ? KQueueIoHandler.newFactory() : EpollIoHandler.newFactory()) : NioIoHandler.newFactory();

		this.bossGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);
		this.workerGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);

		this.channel = EPOLL ? (KQUEUE ? KQueueServerSocketChannel.class : EpollServerSocketChannel.class) : NioServerSocketChannel.class;
	}

	/**
	 * Starts the server and binds it to the specified host and port.
	 *
	 * <p>The server is configured with various options, including compression and packet codecs.
	 * It listens for incoming connections and initializes the channel pipeline for handling those connections.</p>
	 *
	 * @throws RuntimeException if the server initialization is interrupted
	 */
	@Override
	public void connect() {
		try {
			(this.channelFuture = (this.serverBootstrap = new ServerBootstrap()
				 .channel(channel)

				 // Child settings
				 .childOption(ChannelOption.IP_TOS, 24)
				 .childOption(ChannelOption.TCP_NODELAY, true)
				 .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)

				 // Data settings
				 .childOption(ChannelOption.WRITE_SPIN_COUNT, 1)

				 .option(ChannelOption.SO_REUSEADDR, true)

				 .group(bossGroup, workerGroup)
				 .childHandler(new ChannelInitializer<>() {
					 @Override
					 protected void initChannel(Channel channel) throws Exception {
						 ChannelPipeline channelPipeline = channel.pipeline();
						 channelPipeline.addFirst(COMPRESSION_CODEC, new CompressionCodec());
						 channelPipeline.addAfter(COMPRESSION_CODEC, PACKET_CODEC, new FallbackPacketCodec(clusterNode.getPacketRegistry()));
						 channelPipeline.addAfter(PACKET_CODEC, BOSS_HANDLER, new FallbackBossHandler(clusterNode, EndpointType.SERVER));
					 }
				 })

				 .localAddress(host, port))
				 .bind()

				 .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
				 .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				 .syncUninterruptibly())

				 .channel()
				 .closeFuture()
				 .sync();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes periodic tasks for the server.
	 *
	 * <p>This method can be overridden to add server-specific tick behavior if needed.</p>
	 */
	@Override
	public void tick() {
		// Server-specific periodic tasks can be implemented here
	}

	/**
	 * Shuts down the network server, releasing any resources and connections.
	 */
	@Override
	public void shutdown() {
		this.channelFuture.channel().close().syncUninterruptibly();
		this.bossGroup.shutdownGracefully().syncUninterruptibly();
		this.workerGroup.shutdownGracefully().syncUninterruptibly();
	}

	/**
	 * Returns the boss event loop group for the server.
	 *
	 * @return the {@link EventLoopGroup} used for accepting connections
	 */
	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	/**
	 * Returns the worker event loop group for the server.
	 *
	 * @return the {@link EventLoopGroup} used for processing connections
	 */
	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	/**
	 * Returns the class of the server channel being used.
	 *
	 * @return the {@link Class} of the server channel
	 */
	public Class<? extends ServerChannel> getChannel() {
		return channel;
	}

	/**
	 * Returns the {@link ServerBootstrap} instance used to configure the server.
	 *
	 * @return the {@link ServerBootstrap} instance
	 */
	public ServerBootstrap getServerBootstrap() {
		return serverBootstrap;
	}

	/**
	 * Returns the {@link ChannelFuture} representing the future of the server's channel.
	 *
	 * @return the {@link ChannelFuture} for the server channel
	 */
	public ChannelFuture getChannelFuture() {
		return channelFuture;
	}
}
