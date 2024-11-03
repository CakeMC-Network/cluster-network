package net.cakemc.library.cluster.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.Future
import net.cakemc.library.cluster.*
import net.cakemc.library.cluster.address.ClusterAddress
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.config.ClusterIdentificationContext
import net.cakemc.library.cluster.fallback.endpoint.EndpointType
import net.cakemc.library.cluster.handler.*
import java.io.IOException
import java.net.BindException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Represents a server that handles network communication for cluster synchronization.
 *
 *
 * This class sets up the server using Netty and manages the lifecycle of the server,
 * including starting and stopping the server, handling incoming connections, and
 * synchronizing data across cluster members.
 *
 */
class NetworkServer(networkHandler: SyncNetworkHandler, syncBindings: ClusterIdentificationContext) :
  AbstractNetworkServer() {
  private val syncBindings: ClusterIdentificationContext =
    Objects.requireNonNull(syncBindings, "syncBindings cannot be null")
  private val networkHandler: SyncNetworkHandler =
    Objects.requireNonNull(networkHandler, "networkHandler cannot be null")
  private val syncContext: Context
  private var serverChannel: Channel? = null

  private var bossEventLoopGroup: EventLoopGroup? = null
  private var workerEventLoopGroup: EventLoopGroup? = null
  private var serverChannelClass: Class<out ServerChannel?>? = null

  /**
   * Constructs a FallbackFallbackNetworkServer instance with the specified handler and synchronization bindings.
   *
   * @param networkHandler The handler for processing network events.
   * @param syncBindings   The context for synchronization bindings.
   *
   * @throws NullPointerException  if syncBindings or networkHandler is null.
   * @throws IllegalStateException if the provided SyncNetworkHandler is of type client.
   */
  init {
    check(networkHandler.endpointType != EndpointType.CLIENT) { "SyncNetworkHandler should be of type server" }

    this.syncContext = Objects.requireNonNull(networkHandler.syncContext, "syncContext cannot be null")
  }

  /**
   * Prepares the server for starting by initializing event loop groups and setting
   * the server channel class based on the availability of epoll or kqueue.
   */
  override fun prepare() {
    val ioHandlerFactory = if (EPOLL_AVAILABLE)
      (if (KQUEUE_AVAILABLE) KQueueIoHandler.newFactory() else EpollIoHandler.newFactory())
    else
      NioIoHandler.newFactory()

    this.bossEventLoopGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
    this.workerEventLoopGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
    this.serverChannelClass = if (EPOLL_AVAILABLE)
      (if (KQUEUE_AVAILABLE) KQueueServerSocketChannel::class.java else EpollServerSocketChannel::class.java)
    else
      NioServerSocketChannel::class.java
  }

  /**
   * Starts the network server, binding to the configured socket addresses.
   *
   * @throws IOException           if an I/O error occurs while starting the server.
   * @throws IllegalStateException if the server is already activated.
   */
  @kotlin.jvm.Synchronized
  override fun start() {
    check(serverChannel == null) { "Server is already activated" }

    this.prepare()

    try {
      val serverBootstrap = ServerBootstrap()
        .channel(serverChannelClass)

        .childOption(ChannelOption.IP_TOS, 24)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .childOption(ChannelOption.WRITE_SPIN_COUNT, 1)
        .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)

        .option(ChannelOption.SO_REUSEADDR, true)

        .group(bossEventLoopGroup, workerEventLoopGroup)

        .childHandler(object : ChannelInitializer<SocketChannel>() {
          override fun initChannel(channel: SocketChannel) {
            channel.pipeline()
              .addLast(SyncNettyEncoder(), SyncNettyDecoder(), networkHandler)
          }
        })

      val clusterAddresses: MutableSet<ClusterAddress> = HashSet()
      for (socketAddress in syncBindings.socketConfigs) {
        clusterAddresses.add(
          ClusterAddress(
            (socketAddress).host,
            (socketAddress).port
          )
        )
      }

      if (syncBindings.socketConfigs.isEmpty()) {
        return  // No addresses to bind to
      }

      val clusterMember = syncContext.ownInfo
      if (!clusterMember!!.isValid) {
        return  // Invalid cluster member
      }

      val addressesChanged = updateSyncAddressesIfNeeded(clusterAddresses, clusterMember)

      if (addressesChanged) {
        syncContext.updateMember(clusterMember)
        syncContext.setVirtualLastModified(System.currentTimeMillis())
      }

      // Bind to the socket addresses
      for (address in clusterAddresses) {
        serverChannel = serverBootstrap
          .bind("0.0.0.0", address.port)
          .addListener { future: Future<in Void?> ->
            if (future.isSuccess) return@addListener
            throw BindException("cannot bind server to address ${address}")
          }
          .sync().channel()
      }

      startClusterSyncing(syncContext)
    } catch (interruptedException: InterruptedException) {
      Thread.currentThread().interrupt()
      throw IllegalStateException("Server start interrupted", interruptedException)
    } finally {
      // Shut down the event loop to terminate all threads.
      Runtime.getRuntime().addShutdownHook(Thread {
        workerEventLoopGroup!!.shutdownGracefully()
        bossEventLoopGroup!!.shutdownGracefully()
      })
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
  private fun updateSyncAddressesIfNeeded(
    clusterAddresses: MutableSet<ClusterAddress>,
    clusterMember: ClusterMember
  ): Boolean {
    var addressesChanged = false
    val lastModified = System.currentTimeMillis()

    if (clusterMember.syncAddresses == null) {
      addressesChanged = prepareSyncAddresses(clusterAddresses, clusterMember, lastModified)
    } else if (clusterMember.syncAddresses != clusterAddresses) {
      addressesChanged = prepareSyncAddresses(clusterAddresses, clusterMember, lastModified)
    }
    return addressesChanged
  }

  /**
   * Initiates the cluster synchronization process.
   *
   * @param context The context to use for synchronization.
   */
  override fun startClusterSyncing(context: Context?) {
    var publication = ClusterPublication()

    var syncNetworkHandler = SyncNetworkHandler(syncContext, SynchronisationType.UNI_CAST_ONE_OF)
      .withCallBack(ClusterPublicationHandler(syncContext))
      .withPublicationType(null)
      .withoutCluster(syncContext.ownInfo!!.id)

    syncNetworkHandler.mode = DefaultSyncPublication.SyncMode.SYNC_CLUSTER

    val syncResultMap = syncNetworkHandler.sync(publication).get()
    if (syncResultMap != null && syncResultMap["-1"]!!.isSuccessful) {
      syncContext.isInStartup = false
    }

    val clusterSnapshot = syncContext.snapshot
    val publications: MutableList<Publication> = ArrayList()

    for (node in clusterSnapshot!!.cluster!!) {
      if (node == null)
        continue

      publication = ClusterPublication(
        node.id,
        node.isAuthByKey, node.key,
        node.lastModified,
        node.syncAddresses,
        if (node.isValid) DefaultSyncPublication.Command.COMMAND_TAKE_THIS else DefaultSyncPublication.Command.COMMAND_DEL_THIS
      )
      publications.add(publication)
    }

    syncNetworkHandler = SyncNetworkHandler(syncContext, SynchronisationType.UNI_CAST_ONE_OF)
      .withCallBack(ClusterPublicationHandler(syncContext))
      .withPublicationType(null)
      .withoutCluster(syncContext.ownInfo!!.id)

    syncNetworkHandler.mode = DefaultSyncPublication.SyncMode.SYNC_CLUSTER
    syncNetworkHandler.sync(publications).get()
  }


  /**
   * Prepares the synchronization addresses for the specified cluster member.
   *
   *
   * This method updates the synchronization addresses, last modified timestamp,
   * and aware IDs for the provided cluster member instance. It ensures that
   * the addresses are not null before updating the member's state.
   *
   * @param addresses    A set of cluster addresses to be assigned to the cluster member.
   * @param ownInstance  The cluster member instance that needs to be updated.
   * @param lastModified The timestamp indicating the last modification time.
   *
   * @return `true` if the synchronization addresses were updated successfully.
   *
   * @throws NullPointerException if the provided addresses are null.
   */
  override fun prepareSyncAddresses(
    addresses: MutableSet<ClusterAddress>,
    ownInstance: ClusterMember,
    lastModified: Long
  ): Boolean {
    ownInstance.syncAddresses = Objects.requireNonNull(
      addresses,
      "addresses cannot be null"
    )
    ownInstance.lastModified = lastModified
    ownInstance.setAwareIds(shortArrayOf(syncContext.ownInfo!!.id))
    return true
  }

  /**
   * Stops the network server, closing the server channel and shutting down the event loop groups.
   *
   *
   * This method checks if the server is currently running before attempting to close
   * the server channel and gracefully shut down the boss and worker event loop groups.
   * If the server is not running, no action is taken.
   *
   * @throws RuntimeException if an error occurs during the server shutdown process.
   */
  @kotlin.jvm.Synchronized
  override fun stop() {
    if (serverChannel == null) {
      return  // Server is not running, no action needed.
    }

    try {
      serverChannel!!.close()
      bossEventLoopGroup!!.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).sync()
      workerEventLoopGroup!!.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS).sync()
    } catch (throwable: Throwable) {
      throw RuntimeException("Failed to stop the server", throwable)
    }
  }

  companion object {
    val EPOLL_AVAILABLE: Boolean = Epoll.isAvailable()
    val KQUEUE_AVAILABLE: Boolean = KQueue.isAvailable()
  }
}
