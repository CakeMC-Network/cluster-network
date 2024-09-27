package net.cakemc.cluster;

import net.cakemc.cluster.endpoint.NetworkClient;
import net.cakemc.cluster.endpoint.connection.AbstractConnectionManager;
import net.cakemc.cluster.info.NodeInformation;
import net.cakemc.cluster.packet.AbstractPacketRegistry;
import net.cakemc.cluster.packet.Packet;
import net.cakemc.cluster.packet.ring.ResponsePacket;
import net.cakemc.cluster.packet.ring.RequestPacket;
import net.cakemc.cluster.packet.ring.RingPacket;
import net.cakemc.cluster.units.Snowflake;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents an abstract node in the cluster system.
 * This class provides the core functionalities for node operations,
 * packet dispatching, connection management, and unique identifier generation.
 * <p>
 * Subclasses are expected to implement the specific behavior of nodes
 * in the cluster, handling communication and processing of packets
 * sent to and from other nodes.
 */
public abstract class AbstractNode {

	/**
	 * Starts the node and initializes necessary resources for operation.
	 * This method should be called to begin the node's lifecycle,
	 * including establishing connections and preparing to send/receive packets.
	 */
	public abstract void start();

	/**
	 * Dispatches a packet to the ring for processing.
	 *
	 * @param packet The {@link RingPacket} to be dispatched.
	 *               This represents a message or command intended for
	 *               the cluster's ring topology.
	 */
	public abstract void dispatchPacketToRing(RingPacket packet);

	/**
	 * Dispatches a request packet to the ring and handles the reply.
	 *
	 * @param requestPacket The {@link RequestPacket} to be sent to the ring.
	 * @param replyPacket   A consumer that processes the {@link ResponsePacket}
	 *                      received in response to the request. This allows
	 *                      for asynchronous handling of replies.
	 */
	public abstract void dispatchRequestToRing(RequestPacket requestPacket, Consumer<ResponsePacket> replyPacket);

	/**
	 * Gets the node's own address information.
	 *
	 * @return The {@link NodeAddress} representing this node's unique identifier,
	 *         hostname, and port.
	 */
	public abstract NodeAddress getOwnNode();

	/**
	 * Retrieves a map of other nodes in the cluster along with their associated network clients.
	 *
	 * @return A map where the key is {@link NodeInformation} for each node and
	 *         the value is the corresponding {@link NetworkClient} used for communication.
	 */
	public abstract Map<NodeInformation, NetworkClient> getOtherNodes();

	/**
	 * Gets the connection manager responsible for managing connections to other nodes.
	 *
	 * @return The {@link AbstractConnectionManager} instance managing the network connections.
	 */
	public abstract AbstractConnectionManager getConnectionManager();

	/**
	 * Retrieves the packet registry for managing packet types and their serialization/deserialization.
	 *
	 * @return The {@link AbstractPacketRegistry} that provides functionality to retrieve
	 *         packets by their ID and vice versa.
	 */
	public abstract AbstractPacketRegistry getPacketRegistry();

	/**
	 * Retrieves the unique identifier generator for this node.
	 *
	 * @return The {@link Snowflake} instance used for generating unique IDs for
	 *         various purposes, such as requests and identifiers.
	 */
	public abstract Snowflake getSnowflake();

	/**
	 * Gets the network ID of this node.
	 *
	 * @return The unique network ID assigned to this node, used for identification
	 *         within the cluster.
	 */
	public abstract long getNetworkId();
}
