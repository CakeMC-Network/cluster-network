package net.cakemc.library.cluster.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.cakemc.library.cluster.ClusterMember;
import net.cakemc.library.cluster.address.ClusterAddress;
import net.cakemc.library.cluster.codec.DefaultSyncPublication;
import net.cakemc.library.cluster.codec.SyncNettyDecoder;
import net.cakemc.library.cluster.codec.SyncNettyEncoder;
import net.cakemc.library.cluster.handler.HandlerState;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles network communication for the cluster, managing socket connections
 * and message publishing.
 */
public class NetworkClient extends AbstractNetworkClient {
	public static final boolean IS_EPOLL_AVAILABLE = Epoll.isAvailable();
	public static final boolean IS_KQUEUE_AVAILABLE = KQueue.isAvailable();

	private final ClusterMember clusterMember;
	private final List<ClusterAddress> socketAddresses;
	private final Object synchronizationLock = new Object();

	private ChannelFuture channelFuture;
	private Class<? extends SocketChannel> socketChannelClass;
	private EventLoopGroup bossEventLoopGroup;

	private Channel activeChannel;

	private int currentSocketIndex;
	private int lastSocketIndex;

	private boolean allSocketsAttempted = false;
	private boolean isClientImproper;

	private SyncNetworkHandler networkHandler;

	/**
	 * Constructs a FallbackFallbackNetworkClient instance with the specified cluster member and handler.
	 *
	 * @param clusterMember The cluster member associated with this client.
	 * @param networkHandler The handler for processing network events.
	 */
	public NetworkClient(ClusterMember clusterMember, SyncNetworkHandler networkHandler) {
		this.socketAddresses = new ArrayList<>(clusterMember.getSyncAddresses());
		this.networkHandler = networkHandler;
		this.clusterMember = clusterMember;

		if (socketAddresses.isEmpty()) {
			clusterMember.setCurrentSocketIndex(currentSocketIndex = -1);
		} else {
			currentSocketIndex = clusterMember.getCurrentSocketIndex();
			lastSocketIndex = currentSocketIndex;
		}
	}

	/**
	 * Prepares the next client socket for connection.
	 *
	 * @throws Exception if there are no socket addresses defined or an error occurs while setting up the next client.
	 */
	@Override
	public void setupNextClient() throws Exception {
		if (currentSocketIndex == -1) {
			throw new Exception("No socket addresses defined");
		}
		lastSocketIndex = currentSocketIndex;
		currentSocketIndex = (currentSocketIndex + 1) % socketAddresses.size();
		clusterMember.setCurrentSocketIndex(currentSocketIndex);
		if (currentSocketIndex == lastSocketIndex) {
			allSocketsAttempted = true;
		}
	}

	/**
	 * Publishes a message to the currently active socket channel, if available.
	 *
	 * @param publication The {@link DefaultSyncPublication} object to publish.
	 */
	@Override
	public void publish(DefaultSyncPublication publication) {
		synchronized (synchronizationLock) {
			if (currentSocketIndex > -1 && networkHandler != null) {
				ClusterAddress currentClusterAddress = socketAddresses.get(currentSocketIndex);

				if (activeChannel == null || !activeChannel.isActive()) {
					Thread connectionThread = new Thread(
						 () -> connect(
							  currentClusterAddress.getAddress().getHostAddress(),
							  currentClusterAddress.getPort(), publication
						 ),
						 "sending-thread-" + Math.random()
					);

					connectionThread.setDaemon(true);
					connectionThread.start();
					return;
				}

				this.activeChannel.writeAndFlush(publication);
			} else if (networkHandler != null) {
				networkHandler.workCallback(this, HandlerState.WORK_FAILED);
			}
		}
	}

	/**
	 * Prepares the necessary components for establishing a network connection,
	 * including selecting the appropriate IO handler based on the available transport.
	 */
	@Override
	public void prepare() {
		IoHandlerFactory ioHandlerFactory = IS_EPOLL_AVAILABLE
		                                    ? (IS_KQUEUE_AVAILABLE ? KQueueIoHandler.newFactory() : EpollIoHandler.newFactory())
		                                    : NioIoHandler.newFactory();

		this.bossEventLoopGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);
		this.socketChannelClass = IS_EPOLL_AVAILABLE
		                          ? (IS_KQUEUE_AVAILABLE ? KQueueSocketChannel.class : EpollSocketChannel.class)
		                          : NioSocketChannel.class;
	}

	/**
	 * Connects to the specified host and port, and publishes the given message.
	 *
	 * @param host        The host address to connect to.
	 * @param port        The port to connect to.
	 * @param publication The {@link DefaultSyncPublication} object to publish upon connection.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void connect(String host, int port, DefaultSyncPublication publication) {
		this.prepare();

		try {
			channelFuture = new Bootstrap()
				 .group(bossEventLoopGroup)
				 .channel(socketChannelClass)
				 .option(ChannelOption.IP_TOS, 24)
				 .option(ChannelOption.TCP_NODELAY, true)
				 .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				 .option(ChannelOption.WRITE_SPIN_COUNT, 1)
				 .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
				 .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
				 .option(ChannelOption.SO_REUSEADDR, true)
				 .handler(new ChannelInitializer<SocketChannel>() {
					 @Override
					 protected void initChannel(SocketChannel channel) {
						 channel.pipeline()
						        .addLast(new SyncNettyEncoder(), new SyncNettyDecoder());
					 }
				 })
				 .remoteAddress(host, port)
				 .connect()
				 .sync()
				 .addListener(future -> {
					 if (future.isSuccess()) {
						 this.activeChannel = channelFuture.channel(); // Store the active channel
						 activeChannel.writeAndFlush(publication);
					 } else {
						 networkHandler.workCallback(this, HandlerState.WORK_FAILED);
					 }
				 })
				 .channel()
				 .closeFuture()
				 .sync();

		} catch (Exception e) {
			networkHandler.workCallback(this, HandlerState.WORK_FAILED);
		} finally {
			bossEventLoopGroup.shutdownGracefully();
		}
	}

	/**
	 * Gets the handler associated with this network client.
	 *
	 * @return The {@link SyncNetworkHandler} instance handling network events.
	 */
	public SyncNetworkHandler getHandler() {
		return networkHandler;
	}

	/**
	 * Sets a new handler for processing network events.
	 *
	 * @param newHandler The {@link SyncNetworkHandler} to set as the new handler.
	 */
	public void setHandler(SyncNetworkHandler newHandler) {
		this.networkHandler = newHandler;
	}

	/**
	 * Checks if all socket connections have been attempted.
	 *
	 * @return True if all connections have been tried, false otherwise.
	 */
	public boolean isAllSocketsAttempted() {
		return allSocketsAttempted || currentSocketIndex < 0;
	}

	/**
	 * Gets the unique identifier for this cluster member.
	 *
	 * @return The ID of the cluster member.
	 */
	public short getMemberId() {
		return clusterMember != null ? clusterMember.getId() : 0;
	}

	/**
	 * Gets the cluster member associated with this client.
	 *
	 * @return The {@link ClusterMember} associated with this client.
	 */
	public ClusterMember getMember() {
		return clusterMember;
	}

	/**
	 * Returns a string representation of the network client, primarily for debugging purposes.
	 *
	 * @return A string representation of the network client, showing its member ID.
	 */
	@Override
	public String toString() {
		return String.valueOf(getMemberId());
	}

	/**
	 * Checks if the client is in an improper state.
	 *
	 * @return True if the client is improper, false otherwise.
	 */
	public boolean isImproper() {
		return isClientImproper;
	}

	/**
	 * Sets the improper state of the client.
	 *
	 * @param improper True to set the client as improper, false to set it as proper.
	 */
	public void setImproper(boolean improper) {
		this.isClientImproper = improper;
	}


	/**
	 * Checks if all socket connections have been attempted.
	 *
	 * @return True if all connections have been tried, false otherwise.
	 */
	@Override
	public boolean isAllTried() {
		return allSocketsAttempted || currentSocketIndex < 0;
	}

}
