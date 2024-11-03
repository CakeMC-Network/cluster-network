package net.cakemc.library.cluster.fallback.endpoint.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;

/**
 * The {@code BossHandler} class is a Netty channel handler that manages incoming backPackets
 * and handles connection events for a cluster node.
 *
 * <p>This handler processes {@link Publication} instances,
 * and delegates connection management to the associated cluster node.</p>
 *
 * @see SimpleChannelInboundHandler
 * @see Publication
 * @see AbstractBackUpEndpoint
 * @see EndpointType
 */
public class BossHandler extends SimpleChannelInboundHandler<Publication> {

	private final AbstractBackUpEndpoint clusterNode;
	private final EndpointType endpointType;

	/**
	 * Constructs a new {@code BossHandler} with the specified cluster node and endpoint type.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 * @param endpointType the {@link EndpointType} indicating whether the handler is for a server or client
	 */
	public BossHandler(AbstractBackUpEndpoint clusterNode, EndpointType endpointType) {
		this.clusterNode = clusterNode;
		this.endpointType = endpointType;
	}

	/**
	 * Called when a backPacket is received. This method checks if the backPacket is an instance of {@link Publication}
	 * and delegates the handling to the connection manager.
	 *
	 * @param ctx the {@link ChannelHandlerContext} for this handler
	 * @param backPacket the incoming {@link Publication}
	 * @throws Exception if an error occurs while processing the backPacket
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Publication backPacket) throws Exception {
		clusterNode.getConnectionManager().handleInboundPacket(ctx.channel(), backPacket);
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
