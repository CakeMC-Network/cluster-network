package net.cakemc.library.cluster.fallback.endpoint.handler;

import io.netty.channel.Channel;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.ClusterHelloBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.PingBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.PongBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RequestBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.ResponseBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;
import net.cakemc.library.cluster.tick.TickAble;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The {@code FallbackConnectionHandler} class provides a default implementation of the
 * {@link AbstractFallbackConnectionHandler} for managing connections and handling backPackets in a cluster.
 *
 * <p>This class processes incoming backPackets, dispatches requests to other nodes,
 * and manages the lifecycle of connection events. It utilizes a map to keep track of
 * pending requests for asynchronous responses.</p>
 *
 * @see AbstractFallbackConnectionHandler
 * @see RingBackPacket
 * @see RequestBackPacket
 * @see ResponseBackPacket
 */
public class FallbackConnectionHandler extends AbstractFallbackConnectionHandler implements TickAble {

	private final Map<Integer, Consumer<ResponseBackPacket>> pendingRequests;
	private final AbstractBackUpEndpoint clusterNode;

	/**
	 * Constructs a {@code FallbackConnectionHandler} for the specified cluster node.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} instance representing the cluster node
	 */
	public FallbackConnectionHandler(AbstractBackUpEndpoint clusterNode) {
		this.clusterNode = clusterNode;
		this.pendingRequests = new HashMap<>();
	}

	/**
	 * Handles incoming {@link RingBackPacket} instances and processes them based on their type.
	 *
	 * <p>Handles {@link ClusterHelloBackPacket}, {@link PingBackPacket}, and {@link ResponseBackPacket} types specifically.</p>
	 *
	 * @param sender the {@link Channel} that sent the packet
	 * @param ringPacket the incoming {@link RingBackPacket} to be processed
	 */
	@Override
	public void handlePacket(Channel sender, RingBackPacket ringPacket) {
		if (ringPacket instanceof ClusterHelloBackPacket clusterHelloPacket) {
			System.out.println("%s -> %s".formatted(clusterNode.getOwnNode().getId(), clusterHelloPacket.getHelloFrom()));
		}
		if (ringPacket instanceof PingBackPacket packet) {
			sender.writeAndFlush(new PongBackPacket(
				 clusterNode.getSnowflake().nextId(), clusterNode.getNetworkId(),
				 -1, System.currentTimeMillis() - packet.getTime()
			));
		}
		if (ringPacket instanceof ResponseBackPacket responsePacket) {
			Consumer<ResponseBackPacket> packetConsumer = pendingRequests
				 .remove(responsePacket.getRequestId());

			if (packetConsumer == null)
				return;

			packetConsumer.accept(responsePacket);
		}
	}

	/**
	 * Handles the connection event when a channel becomes active.
	 * Sends a {@link ClusterHelloBackPacket} if the endpoint type is CLIENT.
	 *
	 * @param channel the {@link Channel} that is connecting
	 * @param endpointType the {@link EndpointType} indicating the type of endpoint
	 */
	@Override
	public void handleConnect(Channel channel, EndpointType endpointType) {
		super.handleConnect(channel, endpointType);

		if (endpointType == EndpointType.CLIENT) {
			channel.writeAndFlush(new ClusterHelloBackPacket(
				 clusterNode.getSnowflake().nextId(), clusterNode.getNetworkId(),
				 -1, clusterNode.getOwnNode().getId()
			));
		}
	}

	/**
	 * Handles the disconnection event when a channel becomes inactive.
	 *
	 * @param channel the {@link Channel} that is disconnecting
	 * @param endpointType the {@link EndpointType} indicating the type of endpoint
	 */
	@Override
	public void handleDisconnect(Channel channel, EndpointType endpointType) {
		super.handleDisconnect(channel, endpointType);
	}

	/**
	 * Dispatches a {@link RingBackPacket} to all other nodes in the cluster.
	 *
	 * @param packet the {@link RingBackPacket} to be dispatched
	 */
	public void dispatchPacketToRing(RingBackPacket packet) {
		this.clusterNode.getOtherNodes().forEach((nodeInformation, networkClient) -> networkClient.dispatchPacket(packet));
	}

	/**
	 * Dispatches a {@link RequestBackPacket} to the ring and registers a consumer to handle
	 * the response. Throws an exception if the request packet has no target.
	 *
	 * @param requestPacket the {@link RequestBackPacket} to be sent
	 * @param replyPacket the {@link Consumer} to handle the corresponding {@link ResponseBackPacket}
	 * @throws IllegalArgumentException if the request packet does not specify a target
	 */
	public void dispatchRequestToRing(RequestBackPacket requestPacket, Consumer<ResponseBackPacket> replyPacket) {
		if (requestPacket.getRequestId() == -1)
			throw new IllegalArgumentException("can't send request packet with no node target!");

		this.pendingRequests.put(requestPacket.getRequestId(), replyPacket);
		this.dispatchPacketToRing(requestPacket);
	}

	/**
	 * Performs any necessary updates or checks during the tick cycle.
	 */
	@Override
	public void tick() {
		// No specific tick actions defined.
	}
}
