package net.cakemc.cluster.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.cakemc.cluster.AbstractNode;
import net.cakemc.cluster.endpoint.EndpointType;
import net.cakemc.cluster.packet.Packet;
import net.cakemc.cluster.packet.ring.RingPacket;

/**
 * The {@code BossHandler} class is a Netty channel handler that manages incoming packets
 * and handles connection events for a cluster node.
 *
 * <p>This handler processes {@link Packet} instances, specifically handling {@link RingPacket} objects,
 * and delegates connection management to the associated cluster node.</p>
 *
 * @see SimpleChannelInboundHandler
 * @see Packet
 * @see RingPacket
 * @see AbstractNode
 * @see EndpointType
 */
public class BossHandler extends SimpleChannelInboundHandler<Packet> {

	private final AbstractNode clusterNode;
	private final EndpointType endpointType;

	/**
	 * Constructs a new {@code BossHandler} with the specified cluster node and endpoint type.
	 *
	 * @param clusterNode the {@link AbstractNode} representing the cluster node
	 * @param endpointType the {@link EndpointType} indicating whether the handler is for a server or client
	 */
	public BossHandler(AbstractNode clusterNode, EndpointType endpointType) {
		this.clusterNode = clusterNode;
		this.endpointType = endpointType;
	}

	/**
	 * Called when a packet is received. This method checks if the packet is an instance of {@link RingPacket}
	 * and delegates the handling to the connection manager.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @param packet the incoming {@link Packet}
	 * @throws Exception if an error occurs while processing the packet
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		if (!(packet instanceof RingPacket ringPacket))
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
