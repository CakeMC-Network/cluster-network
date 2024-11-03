package net.cakemc.library.cluster.fallback.endpoint.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.fallback.endpoint.EndpointType

/**
 * The `BossHandler` class is a Netty channel handler that manages incoming backPackets
 * and handles connection events for a cluster node.
 *
 *
 * This handler processes [Publication] instances,
 * and delegates connection management to the associated cluster node.
 *
 * @see SimpleChannelInboundHandler
 *
 * @see Publication
 *
 * @see AbstractBackUpEndpoint
 *
 * @see EndpointType
 */
class BossHandler(
    private val clusterNode: AbstractBackUpEndpoint,
    private val endpointType: EndpointType
) : SimpleChannelInboundHandler<Publication>() {
    /**
     * Called when a backPacket is received. This method checks if the backPacket is an instance of [Publication]
     * and delegates the handling to the connection manager.
     *
     * @param ctx the [ChannelHandlerContext] for this handler
     * @param backPacket the incoming [Publication]
     * @throws Exception if an error occurs while processing the backPacket
     */
    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, backPacket: Publication) {
        clusterNode.connectionManager.handleInboundPacket(ctx.channel(), backPacket)
    }

    /**
     * Called when the channel becomes active. This method notifies the connection manager
     * of a new connection.
     *
     * @param ctx the [ChannelHandlerContext] for this handler
     * @throws Exception if an error occurs during the connection notification
     */
    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        clusterNode.connectionManager.handleConnect(ctx.channel(), endpointType)
    }

    /**
     * Called when the channel becomes inactive. This method notifies the connection manager
     * of a disconnection.
     *
     * @param ctx the [ChannelHandlerContext] for this handler
     * @throws Exception if an error occurs during the disconnection notification
     */
    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)

        clusterNode.connectionManager.handleDisconnect(ctx.channel(), endpointType)
    }

    /**
     * Called when an exception is caught in the channel. This method logs the exception
     * and closes the channel.
     *
     * @param ctx the [ChannelHandlerContext] for this handler
     * @param cause the [Throwable] that was caught
     * @throws Exception if an error occurs while handling the exception
     */
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
