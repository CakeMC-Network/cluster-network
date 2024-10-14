package net.cakemc.library.cluster.fallback.endpoint.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.fallback.endpoint.packet.BackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

/**
 * The {@code FallbackBossHandler} class is a Netty channel handler that manages incoming backPackets
 * and handles connection events for a cluster node.
 *
 * <p>This handler processes {@link BackPacket} instances, specifically handling {@link RingBackPacket} objects,
 * and delegates connection management to the associated cluster node.</p>
 *
 * @see SimpleChannelInboundHandler
 * @see BackPacket
 * @see RingBackPacket
 * @see AbstractBackUpEndpoint
 * @see EndpointType
 */
public class FallbackBossHandler extends SimpleChannelInboundHandler<BackPacket> {

	private final AbstractBackUpEndpoint clusterNode;
	private final EndpointType endpointType;

	/**
	 * Constructs a new {@code FallbackBossHandler} with the specified cluster node and endpoint type.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 * @param endpointType the {@link EndpointType} indicating whether the handler is for a server or client
	 */
	public FallbackBossHandler(AbstractBackUpEndpoint clusterNode, EndpointType endpointType) {
		this.clusterNode = clusterNode;
		this.endpointType = endpointType;
	}

	/**
	 * Called when a backPacket is received. This method checks if the backPacket is an instance of {@link RingBackPacket}
	 * and delegates the handling to the connection manager.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @param backPacket the incoming {@link BackPacket}
	 * @throws Exception if an error occurs while processing the backPacket
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, BackPacket backPacket) throws Exception {
		if (!(backPacket instanceof RingBackPacket ringPacket))
			return;

		clusterNode.getConnectionManager().handleInboundPacket(ctx.channel(), ringPacket);
	}

	/**
	 * Called when the channel becomes active. This method notifies the connection manager
	 * of a new connection.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @throws Exception if an error occurs during the connection notification
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);

		clusterNode.getConnectionManager().handleConnect(ctx.channel(), endpointType);
	}

	/**
	 * Called when the channel becomes inactive. This method notifies the connection manager
	 * of a disconnection.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @throws Exception if an error occurs during the disconnection notification
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);

		clusterNode.getConnectionManager().handleDisconnect(ctx.channel(), endpointType);
	}

	/**
	 * Called when an exception is caught in the channel. This method logs the exception
	 * and closes the channel.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @param cause the {@link Throwable} that was caught
	 * @throws Exception if an error occurs while handling the exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
