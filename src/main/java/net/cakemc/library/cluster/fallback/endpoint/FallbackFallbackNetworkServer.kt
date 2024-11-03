package net.cakemc.library.cluster.fallback.endpoint

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.fallback.endpoint.codec.CompressionCodec
import net.cakemc.library.cluster.fallback.endpoint.codec.PublicationCodec
import net.cakemc.library.cluster.fallback.endpoint.handler.BossHandler

/**
 * Represents a network server in the cluster that listens for incoming connections from clients.
 *
 *
 * The `FallbackFallbackNetworkServer` class extends `FallbackNetworkPoint` to implement server-specific
 * behavior for initializing the server, handling incoming connections, and shutting down.
 *
 * @see FallbackNetworkPoint
 *
 * @see BossHandler
 */
class FallbackFallbackNetworkServer
/**
 * Constructs a new `FallbackFallbackNetworkServer` with the specified cluster node.
 *
 * @param clusterNode the [AbstractBackUpEndpoint] representing the cluster node
 */
    (clusterNode: AbstractBackUpEndpoint) : FallbackNetworkPoint(clusterNode) {
    /**
     * Returns the boss event loop group for the server.
     *
     * @return the [EventLoopGroup] used for accepting connections
     */
    // Configuration
    var bossGroup: EventLoopGroup? = null
        private set

    /**
     * Returns the worker event loop group for the server.
     *
     * @return the [EventLoopGroup] used for processing connections
     */
    var workerGroup: EventLoopGroup? = null
        private set

    /**
     * Returns the class of the server channel being used.
     *
     * @return the [Class] of the server channel
     */
    var channel: Class<out ServerChannel?>? = null
        private set

    /**
     * Returns the [ServerBootstrap] instance used to configure the server.
     *
     * @return the [ServerBootstrap] instance
     */
    // Server
    var serverBootstrap: ServerBootstrap? = null
        private set

    /**
     * Returns the [ChannelFuture] representing the future of the server's channel.
     *
     * @return the [ChannelFuture] for the server channel
     */
    var channelFuture: ChannelFuture? = null
        private set

    /**
     * Initializes the network server by setting up the boss and worker event loop groups
     * and determining the channel type based on the available I/O model.
     */
    override fun initialize() {
        val ioHandlerFactory =
            if (FallbackNetworkPoint.Companion.EPOLL) (if (FallbackNetworkPoint.Companion.KQUEUE) KQueueIoHandler.newFactory() else EpollIoHandler.newFactory()) else NioIoHandler.newFactory()

        this.bossGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)
        this.workerGroup = MultiThreadIoEventLoopGroup(2, ioHandlerFactory)

        this.channel =
            if (FallbackNetworkPoint.Companion.EPOLL) (if (FallbackNetworkPoint.Companion.KQUEUE) KQueueServerSocketChannel::class.java else EpollServerSocketChannel::class.java) else NioServerSocketChannel::class.java
    }

    /**
     * Starts the server and binds it to the specified host and port.
     *
     *
     * The server is configured with various options, including compression and packet codecs.
     * It listens for incoming connections and initializes the channel pipeline for handling those connections.
     *
     * @throws RuntimeException if the server initialization is interrupted
     */
    override fun connect() {
        try {
            ((ServerBootstrap()
                .channel(channel) // Child settings

                .childOption<Int>(ChannelOption.IP_TOS, 24)
                .childOption<Boolean>(ChannelOption.TCP_NODELAY, true)
                .childOption<ByteBufAllocator>(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) // Data settings

                .childOption<Int>(ChannelOption.WRITE_SPIN_COUNT, 1)

                .option<Boolean>(ChannelOption.SO_REUSEADDR, true)

                .group(bossGroup, workerGroup)
                .childHandler(object : ChannelInitializer<Channel>() {
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
                            BossHandler(clusterNode, EndpointType.SERVER)
                        )
                    }
                })

                .localAddress(host, port).also { this.serverBootstrap = it })
                .bind()

                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .syncUninterruptibly().also { this.channelFuture = it })

                .channel()
                .closeFuture()
                .sync()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Executes periodic tasks for the server.
     *
     *
     * This method can be overridden to add server-specific tick behavior if needed.
     */
    override fun tick() {
        // Server-specific periodic tasks can be implemented here
    }

    /**
     * Shuts down the network server, releasing any resources and connections.
     */
    override fun shutdown() {
        channelFuture!!.channel().close().syncUninterruptibly()
        bossGroup!!.shutdownGracefully().syncUninterruptibly()
        workerGroup!!.shutdownGracefully().syncUninterruptibly()
    }
}
