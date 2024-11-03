package net.cakemc.library.cluster.network

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.Future
import net.cakemc.library.cluster.ClusterMember
import net.cakemc.library.cluster.address.ClusterAddress
import net.cakemc.library.cluster.codec.DefaultSyncPublication
import net.cakemc.library.cluster.codec.SyncNettyDecoder
import net.cakemc.library.cluster.codec.SyncNettyEncoder
import net.cakemc.library.cluster.handler.HandlerState
import net.cakemc.library.cluster.handler.SyncNetworkHandler
import java.net.ConnectException
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Handles network communication for the cluster, managing socket connections
 * and message publishing.
 */
class NetworkClient(clusterMember: ClusterMember, networkHandler: SyncNetworkHandler?) : AbstractNetworkClient() {
    /**
     * Gets the cluster member associated with this client.
     *
     * @return The [ClusterMember] associated with this client.
     */
    val member: ClusterMember = clusterMember

    private val socketAddresses: List<ClusterAddress?> =
        ArrayList(clusterMember.syncAddresses)
    private val synchronizationLock = Any()

    private var channelFuture: ChannelFuture? = null
    private var socketChannelClass: Class<out SocketChannel>? = null
    private var bossEventLoopGroup: EventLoopGroup? = null

    private var activeChannel: Channel? = null

    private var currentSocketIndex = 0
    private var lastSocketIndex = 0

    private var allSocketsAttempted = false
    /**
     * Checks if the client is in an improper state.
     *
     * @return True if the client is improper, false otherwise.
     */
    /**
     * Sets the improper state of the client.
     *
     * @param improper True to set the client as improper, false to set it as proper.
     */
    var isImproper: Boolean = false

    /**
     * Gets the handler associated with this network client.
     *
     * @return The [SyncNetworkHandler] instance handling network events.
     */
    /**
     * Sets a new handler for processing network events.
     *
     * @param newHandler The [SyncNetworkHandler] to set as the new handler.
     */
    var handler: SyncNetworkHandler? = networkHandler

    /**
     * Constructs a FallbackFallbackNetworkClient instance with the specified cluster member and handler.
     *
     * @param clusterMember The cluster member associated with this client.
     * @param networkHandler The handler for processing network events.
     */
    init {
        if (socketAddresses.isEmpty()) {
            clusterMember.currentSocketIndex = -1.also { currentSocketIndex = it }
        } else {
            currentSocketIndex = clusterMember.currentSocketIndex
            lastSocketIndex = currentSocketIndex
        }
    }

    /**
     * Prepares the next client socket for connection.
     *
     * @throws Exception if there are no socket addresses defined or an error occurs while setting up the next client.
     */
    @Throws(Exception::class)
    override fun setupNextClient() {
        if (currentSocketIndex == -1) {
            throw Exception("No socket addresses defined")
        }
        lastSocketIndex = currentSocketIndex
        currentSocketIndex = (currentSocketIndex + 1) % socketAddresses.size
        member.currentSocketIndex = currentSocketIndex
        if (currentSocketIndex == lastSocketIndex) {
            allSocketsAttempted = true
        }
    }

    /**
     * Publishes a message to the currently active socket channel, if available.
     *
     * @param publication The [DefaultSyncPublication] object to publish.
     */
    override fun publish(publication: DefaultSyncPublication?) {
        synchronized(synchronizationLock) {
            if (currentSocketIndex > -1 && handler != null) {
                val currentClusterAddress = socketAddresses[currentSocketIndex]

                if (activeChannel == null || !activeChannel!!.isActive) {
                    val connectionThread = Thread(
                        {
                            connect(
                                currentClusterAddress!!.address?.hostName,
                                currentClusterAddress.port, publication
                            )
                        },
                        "sending-thread-" + ThreadLocalRandom
                            .current().nextInt(9999)
                    )

                    connectionThread.isDaemon = true
                    connectionThread.start()
                    return
                }

                activeChannel!!.writeAndFlush(publication)
            } else if (handler != null) {
                handler!!.workCallback(this, HandlerState.WORK_FAILED)
            } else {

            }
        }
    }

    /**
     * Prepares the necessary components for establishing a network connection,
     * including selecting the appropriate IO handler based on the available transport.
     */
    override fun prepare() {
        val ioHandlerFactory = if (IS_EPOLL_AVAILABLE)
            (if (IS_KQUEUE_AVAILABLE) KQueueIoHandler.newFactory() else EpollIoHandler.newFactory())
        else
            NioIoHandler.newFactory()

        this.bossEventLoopGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
        this.socketChannelClass = if (IS_EPOLL_AVAILABLE)
            (if (IS_KQUEUE_AVAILABLE) KQueueSocketChannel::class.java else EpollSocketChannel::class.java)
        else
            NioSocketChannel::class.java
    }

    /**
     * Connects to the specified host and port, and publishes the given message.
     *
     * @param host        The host address to connect to.
     * @param port        The port to connect to.
     * @param publication The [DefaultSyncPublication] object to publish upon connection.
     */
    @Suppress("deprecation")
    override fun connect(host: String?, port: Int, publication: DefaultSyncPublication?) {
        this.prepare()

        try {
            channelFuture = Bootstrap()
                .group(bossEventLoopGroup)
                .channel(socketChannelClass)
                .option(ChannelOption.IP_TOS, 24)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_SPIN_COUNT, 1)
                .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
                .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        channel.pipeline()
                            .addLast(SyncNettyEncoder(), SyncNettyDecoder())
                    }
                })
                .remoteAddress(host, port)
                .connect()
                .sync()
                .addListener { future: Future<in Void?> ->
                    if (future.isSuccess) {
                        this.activeChannel = channelFuture!!.channel() // Store the active channel
                        activeChannel?.writeAndFlush(publication)
                    } else {
                        handler!!.workCallback(this, HandlerState.WORK_FAILED)
                    }
                }
                .channel()
                .closeFuture()
                .sync()
        } catch (exception: Throwable) {
            if (exception is ConnectException) {
                // todo handle reconnect handler
                return
            }

            handler!!.workCallback(this, HandlerState.WORK_FAILED)
        } finally {
            bossEventLoopGroup!!.shutdownGracefully()
        }
    }

    /**
     * Checks if all socket connections have been attempted.
     *
     * @return True if all connections have been tried, false otherwise.
     */
    fun isAllSocketsAttempted(): Boolean {
        return allSocketsAttempted || currentSocketIndex < 0
    }

    override val memberId: Short
        /**
         * Gets the unique identifier for this cluster member.
         *
         * @return The ID of the cluster member.
         */
        get() = if (member != null) member.id else 0

    /**
     * Returns a string representation of the network client, primarily for debugging purposes.
     *
     * @return A string representation of the network client, showing its member ID.
     */
    override fun toString(): String {
        return "NetworkClient{" +
                "clusterMember=" + member +
                ", socketAddresses=" + socketAddresses +
                ", synchronizationLock=" + synchronizationLock +
                ", currentSocketIndex=" + currentSocketIndex +
                ", lastSocketIndex=" + lastSocketIndex +
                ", allSocketsAttempted=" + allSocketsAttempted +
                ", isClientImproper=" + isImproper +
                '}'
    }


    override val isAllTried: Boolean
        /**
         * Checks if all socket connections have been attempted.
         *
         * @return True if all connections have been tried, false otherwise.
         */
        get() = allSocketsAttempted || currentSocketIndex < 0

    companion object {
        val IS_EPOLL_AVAILABLE: Boolean = Epoll.isAvailable()
        val IS_KQUEUE_AVAILABLE: Boolean = KQueue.isAvailable()
    }
}
