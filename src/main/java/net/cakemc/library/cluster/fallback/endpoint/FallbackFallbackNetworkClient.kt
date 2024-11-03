package net.cakemc.library.cluster.fallback.endpoint

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import net.cakemc.library.cluster.api.MemberIdentifier
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.fallback.endpoint.codec.CompressionCodec
import net.cakemc.library.cluster.fallback.endpoint.codec.PublicationCodec
import net.cakemc.library.cluster.fallback.endpoint.handler.BossHandler
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Represents a network client in the cluster that connects to server nodes.
 *
 *
 * The `FallbackFallbackNetworkClient` class extends `FallbackNetworkPoint` to implement
 * client-specific behavior for establishing connections, sending backPackets, and
 * handling reconnections in case of failures.
 *
 * @see FallbackNetworkPoint
 *
 * @see Publication
 */
class FallbackFallbackNetworkClient
/**
 * Constructs a new `FallbackFallbackNetworkClient` with the specified cluster node and
 * node information.
 *
 * @param clusterNode the [AbstractBackUpEndpoint] representing the cluster node
 * @param nodeInformation the [MemberIdentifier] containing address details
 */
    (clusterNode: AbstractBackUpEndpoint, nodeInformation: MemberIdentifier) :
    FallbackNetworkPoint(clusterNode, nodeInformation) {
    // Configuration
    private var eventLoopGroup: EventLoopGroup? = null
    private var channel: Class<out SocketChannel>? = null
    private var channelFuture: ChannelFuture? = null

    // Reconnection state
    private var reconnecting = false
    private var lastAttemptTime: Long = 0
    private var awaitFirstStart = true

    /**
     * Initializes the network client by setting up the event loop group and channel type
     * based on the available I/O model.
     */
    override fun initialize() {
        val ioHandlerFactory =
            if (FallbackNetworkPoint.Companion.EPOLL) (if (FallbackNetworkPoint.Companion.KQUEUE) KQueueIoHandler.newFactory() else EpollIoHandler.newFactory()) else NioIoHandler.newFactory()

        this.eventLoopGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
        this.channel =
            if (FallbackNetworkPoint.Companion.EPOLL) (if (FallbackNetworkPoint.Companion.KQUEUE) KQueueSocketChannel::class.java else EpollSocketChannel::class.java) else NioSocketChannel::class.java
    }

    /**
     * Establishes a connection to the server using the specified host and port.
     *
     *
     * If the connection attempt fails, it enters a reconnecting state that will
     * periodically attempt to reconnect based on the configured timeout.
     */
    override fun connect() {
        this.awaitFirstStart = false
        if (this.checkAvailable(this.host, this.port)) {
            this.reconnecting = true
            lastAttemptTime = System.currentTimeMillis()
        }
        try {
            // Set up the client bootstrap
            this.channelFuture = Bootstrap()
                .channel(channel)
                .option(ChannelOption.IP_TOS, 24)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .group(eventLoopGroup)
                .handler(object : ChannelInitializer<Channel>() {
                    @Throws(Exception::class)
                    override fun initChannel(channel: Channel) {
                        val channelPipeline = channel.pipeline()
                        channelPipeline.addFirst(COMPRESSION_CODEC, CompressionCodec())
                        channelPipeline.addAfter(
                            COMPRESSION_CODEC,
                            PACKET_CODEC,
                            PublicationCodec()
                        )
                        channelPipeline.addAfter(
                            PACKET_CODEC,
                            BOSS_HANDLER,
                            BossHandler(clusterNode, EndpointType.CLIENT)
                        )
                    }
                })
                .remoteAddress(host, port)
                .connect()
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener { channelFuture: ChannelFuture ->
                    if (channelFuture.isSuccess) {
                        this.isConnected = true
                        this.reconnecting = false
                    } else {
                        this.isConnected = false
                        this.reconnecting = true
                        lastAttemptTime = System.currentTimeMillis()
                    }
                })
                .syncUninterruptibly()

            channelFuture!!.channel().closeFuture().sync()
        } catch (e: InterruptedException) {
            this.reconnecting = true
            lastAttemptTime = System.currentTimeMillis()
        }
    }

    /**
     * Dispatches a packet to the connected server.
     *
     *
     * If the connection is not active, this method does nothing.
     *
     * @param packet the [Publication] to be dispatched
     */
    override fun dispatchPacket(packet: Publication?) {
        super.dispatchPacket(packet)

        if (channelFuture == null || !channelFuture!!.channel().isActive) return

        channelFuture!!.channel().writeAndFlush(packet)
            .addListener(ChannelFutureListener { channelFuture: ChannelFuture ->
                if (!channelFuture.isSuccess && channelFuture.cause() != null) channelFuture.cause().printStackTrace()
            })
    }

    /**
     * Executes periodic tasks, including reconnecting if the client is not connected.
     */
    override fun tick() {
        if (!isConnected && !reconnecting) {
            // Start reconnect attempt
            this.reconnecting = true
            lastAttemptTime = System.currentTimeMillis()
        }

        if (reconnecting && System.currentTimeMillis() - lastAttemptTime > RETRY_TIMEOUT && !awaitFirstStart) {
            connect()
        }
    }

    /**
     * Checks the availability of a host and port by attempting to connect.
     *
     * @param host the host address to check
     * @param port the port number to check
     * @return `true` if the host and port are available, `false` otherwise
     */
    fun checkAvailable(host: String, port: Int): Boolean {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1000)
                return true
            }
        } catch (e: IOException) {
            return false
        }
    }

    /**
     * Shuts down the network client, releasing any resources and connections.
     */
    override fun shutdown() {
        if (this.channelFuture != null) {
            channelFuture!!.channel().close().syncUninterruptibly()
        }
        if (this.eventLoopGroup != null) {
            eventLoopGroup!!.shutdownGracefully().syncUninterruptibly()
        }
        this.isConnected = false
    }

    companion object {
        /**
         * Timeout duration (in milliseconds) for retrying to connect to the server.
         */
        const val RETRY_TIMEOUT: Long = 5000
    }
}
