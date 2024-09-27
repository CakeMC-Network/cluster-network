package net.cakemc.cluster.handler;

import io.netty.channel.Channel;
import net.cakemc.cluster.AbstractNode;
import net.cakemc.cluster.endpoint.EndpointType;
import net.cakemc.cluster.packet.impl.ClusterHelloPacket;
import net.cakemc.cluster.packet.impl.PingPacket;
import net.cakemc.cluster.packet.impl.PongPacket;
import net.cakemc.cluster.packet.ring.RequestPacket;
import net.cakemc.cluster.packet.ring.ResponsePacket;
import net.cakemc.cluster.packet.ring.RingPacket;
import net.cakemc.cluster.tick.TickAble;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The {@code DefaultConnectionHandler} class provides a default implementation of the
 * {@link ConnectionHandler} for managing connections and handling packets in a cluster.
 *
 * <p>This class processes incoming packets, dispatches requests to other nodes,
 * and manages the lifecycle of connection events. It utilizes a map to keep track of
 * pending requests for asynchronous responses.</p>
 *
 * @see ConnectionHandler
 * @see RingPacket
 * @see RequestPacket
 * @see ResponsePacket
 */
public class DefaultConnectionHandler extends ConnectionHandler implements TickAble {

	private final Map<Integer, Consumer<ResponsePacket>> pendingRequests;
	private final AbstractNode clusterNode;

	/**
	 * Constructs a {@code DefaultConnectionHandler} for the specified cluster node.
	 *
	 * @param clusterNode the {@link AbstractNode} instance representing the cluster node
	 */
	public DefaultConnectionHandler(AbstractNode clusterNode) {
		this.clusterNode = clusterNode;
		this.pendingRequests = new HashMap<>();
	}

	/**
	 * Handles incoming {@link RingPacket} instances and processes them based on their type.
	 *
	 * <p>Handles {@link ClusterHelloPacket}, {@link PingPacket}, and {@link ResponsePacket} types specifically.</p>
	 *
	 * @param sender the {@link Channel} that sent the packet
	 * @param ringPacket the incoming {@link RingPacket} to be processed
	 */
	@Override
	public void handlePacket(Channel sender, RingPacket ringPacket) {
		if (ringPacket instanceof ClusterHelloPacket clusterHelloPacket) {
			System.out.println("%s -> %s".formatted(clusterNode.getOwnNode().id(), clusterHelloPacket.getHelloFrom()));
		}
		if (ringPacket instanceof PingPacket packet) {
			sender.writeAndFlush(new PongPacket(
				 clusterNode.getSnowflake().nextId(), clusterNode.getNetworkId(),
				 -1, System.currentTimeMillis() - packet.getTime()
			));
		}
		if (ringPacket instanceof ResponsePacket responsePacket) {
			Consumer<ResponsePacket> packetConsumer = pendingRequests
				 .remove(responsePacket.getRequestId());

			if (packetConsumer == null)
				return;

			packetConsumer.accept(responsePacket);
		}
	}

	/**
	 * Handles the connection event when a channel becomes active.
	 * Sends a {@link ClusterHelloPacket} if the endpoint type is CLIENT.
	 *
	 * @param channel the {@link Channel} that is connecting
	 * @param endpointType the {@link EndpointType} indicating the type of endpoint
	 */
	@Override
	public void handleConnect(Channel channel, EndpointType endpointType) {
		super.handleConnect(channel, endpointType);

		if (endpointType == EndpointType.CLIENT) {
			channel.writeAndFlush(new ClusterHelloPacket(
				 clusterNode.getSnowflake().nextId(), clusterNode.getNetworkId(),
				 -1, clusterNode.getOwnNode().id()
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
	 * Dispatches a {@link RingPacket} to all other nodes in the cluster.
	 *
	 * @param packet the {@link RingPacket} to be dispatched
	 */
	public void dispatchPacketToRing(RingPacket packet) {
		this.clusterNode.getOtherNodes().forEach((nodeInformation, networkClient) -> networkClient.dispatchPacket(packet));
	}

	/**
	 * Dispatches a {@link RequestPacket} to the ring and registers a consumer to handle
	 * the response. Throws an exception if the request packet has no target.
	 *
	 * @param requestPacket the {@link RequestPacket} to be sent
	 * @param replyPacket the {@link Consumer} to handle the corresponding {@link ResponsePacket}
	 * @throws IllegalArgumentException if the request packet does not specify a target
	 */
	public void dispatchRequestToRing(RequestPacket requestPacket, Consumer<ResponsePacket> replyPacket) {
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
