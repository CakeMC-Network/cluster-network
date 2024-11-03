package net.cakemc.library.cluster.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.cakemc.library.cluster.ClusterMember;
import net.cakemc.library.cluster.ClusterSnapshot;
import net.cakemc.library.cluster.Context;
import net.cakemc.library.cluster.SynchronisationType;
import net.cakemc.library.cluster.address.ClusterAddress;
import net.cakemc.library.cluster.codec.ClusterPublication;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.Command;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.SyncMode;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.SyncNettyDecoder;
import net.cakemc.library.cluster.codec.SyncNettyEncoder;
import net.cakemc.library.cluster.config.ClusterIdentificationContext;
import net.cakemc.library.cluster.config.NodeIdentifier;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.handler.ClusterPublicationHandler;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;
import net.cakemc.library.cluster.handler.SyncResult;

import java.io.IOException;
import java.net.BindException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents a server that handles network communication for cluster synchronization.
 * <p>
 * This class sets up the server using Netty and manages the lifecycle of the server,
 * including starting and stopping the server, handling incoming connections, and
 * synchronizing data across cluster members.
 * </p>
 */
public class NetworkServer extends AbstractNetworkServer {

	public static final boolean EPOLL_AVAILABLE = Epoll.isAvailable();
	public static final boolean KQUEUE_AVAILABLE = KQueue.isAvailable();

	private final ClusterIdentificationContext syncBindings;
	private final SyncNetworkHandler networkHandler;
	private final Context syncContext;
	private Channel serverChannel;

	private EventLoopGroup bossEventLoopGroup;
	private EventLoopGroup workerEventLoopGroup;
	private Class<? extends ServerChannel> serverChannelClass;

	/**
	 * Constructs a FallbackFallbackNetworkServer instance with the specified handler and synchronization bindings.
	 *
	 * @param networkHandler The handler for processing network events.
	 * @param syncBindings   The context for synchronization bindings.
	 *
	 * @throws NullPointerException  if syncBindings or networkHandler is null.
	 * @throws IllegalStateException if the provided SyncNetworkHandler is of type client.
	 */
	public NetworkServer(SyncNetworkHandler networkHandler, ClusterIdentificationContext syncBindings) {
		this.syncBindings = Objects.requireNonNull(syncBindings, "syncBindings cannot be null");
		this.networkHandler = Objects.requireNonNull(networkHandler, "networkHandler cannot be null");

		if (networkHandler.endpointType == EndpointType.CLIENT) {
			throw new IllegalStateException("SyncNetworkHandler should be of type server");
		}

		this.syncContext = Objects.requireNonNull(networkHandler.syncContext, "syncContext cannot be null");
	}

	/**
	 * Prepares the server for starting by initializing event loop groups and setting
	 * the server channel class based on the availability of epoll or kqueue.
	 */
	@Override
	public void prepare() {
		IoHandlerFactory ioHandlerFactory = EPOLL_AVAILABLE
		                                    ? (KQUEUE_AVAILABLE ? KQueueIoHandler.newFactory() : EpollIoHandler.newFactory())
		                                    : NioIoHandler.newFactory();

		this.bossEventLoopGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);
		this.workerEventLoopGroup = new MultiThreadIoEventLoopGroup(2, ioHandlerFactory);
		this.serverChannelClass = EPOLL_AVAILABLE
		                          ? (KQUEUE_AVAILABLE ? KQueueServerSocketChannel.class : EpollServerSocketChannel.class)
		                          : NioServerSocketChannel.class;
	}

	/**
	 * Starts the network server, binding to the configured socket addresses.
	 *
	 * @throws IOException           if an I/O error occurs while starting the server.
	 * @throws IllegalStateException if the server is already activated.
	 */
	@Override
	public synchronized void start() {
		if (serverChannel != null) {
			throw new IllegalStateException("Server is already activated");
		}

		this.prepare();

		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap()
				 .channel(serverChannelClass)

				 .childOption(ChannelOption.IP_TOS, 24)
				 .childOption(ChannelOption.TCP_NODELAY, true)
				 .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				 .childOption(ChannelOption.WRITE_SPIN_COUNT, 1)
				 .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
				 .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)

				 .option(ChannelOption.SO_REUSEADDR, true)

				 .group(bossEventLoopGroup, workerEventLoopGroup)

				 .childHandler(new ChannelInitializer<SocketChannel>() {
					 @Override
					 protected void initChannel(SocketChannel channel) {
						 channel.pipeline()
						        .addLast(new SyncNettyEncoder(), new SyncNettyDecoder(), networkHandler);
					 }
				 });

			Set<ClusterAddress> clusterAddresses = new HashSet<>();
			for (NodeIdentifier socketAddress : syncBindings.getSocketConfigs()) {
				clusterAddresses.add(new ClusterAddress(
					 (socketAddress).host(),
					 (socketAddress).port()
				));
			}

			if (syncBindings.getSocketConfigs().isEmpty()) {
				return; // No addresses to bind to
			}

			ClusterMember clusterMember = syncContext.getOwnInfo();
			if (!clusterMember.isValid()) {
				return; // Invalid cluster member
			}

			boolean addressesChanged = updateSyncAddressesIfNeeded(clusterAddresses, clusterMember);

			if (addressesChanged) {
				syncContext.updateMember(clusterMember);
				syncContext.setVirtualLastModified(System.currentTimeMillis());
			}

			// Bind to the socket addresses
			for (ClusterAddress address : clusterAddresses) {
				serverChannel = serverBootstrap
					 .bind("0.0.0.0", address.getPort())
					 .addListener(future -> {
						 if (future.isSuccess())
							 return;

						 throw new BindException("cannot bind server to address %s".formatted(address));
					 })
					 .sync().channel();
			}

			startClusterSyncing(syncContext);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Server start interrupted", interruptedException);
		} finally {
			// Shut down the event loop to terminate all threads.
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				workerEventLoopGroup.shutdownGracefully();
				bossEventLoopGroup.shutdownGracefully();
			}));
		}
	}

	/**
	 * Updates the synchronization addresses if needed for the specified cluster member.
	 *
	 * @param clusterAddresses The new set of cluster addresses.
	 * @param clusterMember    The cluster member to update.
	 *
	 * @return true if the addresses were changed, false otherwise.
	 */
	private boolean updateSyncAddressesIfNeeded(Set<ClusterAddress> clusterAddresses, ClusterMember clusterMember) {
		boolean addressesChanged = false;
		long lastModified = System.currentTimeMillis();

		if (clusterMember.getSyncAddresses() == null) {
			addressesChanged = prepareSyncAddresses(clusterAddresses, clusterMember, lastModified);
		} else if (!clusterMember.getSyncAddresses().equals(clusterAddresses)) {
			addressesChanged = prepareSyncAddresses(clusterAddresses, clusterMember, lastModified);
		}
		return addressesChanged;
	}

	/**
	 * Initiates the cluster synchronization process.
	 *
	 * @param context The context to use for synchronization.
	 */
	@Override
	public void startClusterSyncing(Context context) {
		ClusterPublication publication = new ClusterPublication();

		SyncNetworkHandler syncNetworkHandler = new SyncNetworkHandler(syncContext, SynchronisationType.UNI_CAST_ONE_OF)
			 .withCallBack(new ClusterPublicationHandler(syncContext))
			 .withPublicationType(null)
			 .withoutCluster(syncContext.getOwnInfo().getId());

		syncNetworkHandler.mode = SyncMode.SYNC_CLUSTER;

		Map<String, SyncResult> syncResultMap = syncNetworkHandler.sync(publication).get();
		if (syncResultMap != null && syncResultMap.get("-1").isSuccessful()) {
			syncContext.setInStartup(false);
		}

		ClusterSnapshot clusterSnapshot = syncContext.getSnapshot();
		List<Publication> publications = new ArrayList<>();

		for (ClusterMember node : clusterSnapshot.getCluster()) {
			publication = new ClusterPublication(
				 node.getId(),
				 node.isAuthByKey(), node.getKey(),
				 node.getLastModified(),
				 node.getSyncAddresses(),
				 node.isValid() ? Command.COMMAND_TAKE_THIS : Command.COMMAND_DEL_THIS
			);
			publications.add(publication);
		}

		syncNetworkHandler = new SyncNetworkHandler(syncContext, SynchronisationType.UNI_CAST_ONE_OF)
			 .withCallBack(new ClusterPublicationHandler(syncContext))
			 .withPublicationType(null)
			 .withoutCluster(syncContext.getOwnInfo().getId());

		syncNetworkHandler.mode = SyncMode.SYNC_CLUSTER;
		syncNetworkHandler.sync(publications).get();
	}


	/**
	 * Prepares the synchronization addresses for the specified cluster member.
	 * <p>
	 * This method updates the synchronization addresses, last modified timestamp,
	 * and aware IDs for the provided cluster member instance. It ensures that
	 * the addresses are not null before updating the member's state.
	 *
	 * @param addresses    A set of cluster addresses to be assigned to the cluster member.
	 * @param ownInstance  The cluster member instance that needs to be updated.
	 * @param lastModified The timestamp indicating the last modification time.
	 *
	 * @return {@code true} if the synchronization addresses were updated successfully.
	 *
	 * @throws NullPointerException if the provided addresses are null.
	 */
	@Override
	public boolean prepareSyncAddresses(Set<ClusterAddress> addresses, ClusterMember ownInstance, long lastModified) {
		ownInstance.setSyncAddresses(Objects.requireNonNull(addresses, "addresses cannot be null"));
		ownInstance.setLastModified(lastModified);
		ownInstance.setAwareIds(new short[]{ syncContext.getOwnInfo().getId() });
		return true;
	}

	/**
	 * Stops the network server, closing the server channel and shutting down the event loop groups.
	 * <p>
	 * This method checks if the server is currently running before attempting to close
	 * the server channel and gracefully shut down the boss and worker event loop groups.
	 * If the server is not running, no action is taken.
	 *
	 * @throws RuntimeException if an error occurs during the server shutdown process.
	 */
	@Override
	public synchronized void stop() {
		if (serverChannel == null) {
			return; // Server is not running, no action needed.
		}

		try {
			serverChannel.close();
			bossEventLoopGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).sync();
			workerEventLoopGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).sync();
		} catch (Throwable throwable) {
			throw new RuntimeException("Failed to stop the server", throwable);
		}
	}

}
