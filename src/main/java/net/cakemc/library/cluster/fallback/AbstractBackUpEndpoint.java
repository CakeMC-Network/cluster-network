package net.cakemc.library.cluster.fallback;

import net.cakemc.library.cluster.Session;
import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.api.handler.APIPublicationHandler;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkClient;
import net.cakemc.library.cluster.fallback.endpoint.connection.AbstractConnectionManager;
import net.cakemc.library.cluster.fallback.endpoint.packet.AbstractPacketRegistry;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.ResponseBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RequestBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;
import net.cakemc.library.cluster.config.Snowflake;
import net.cakemc.library.cluster.handler.PublicationHandler;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents an abstract node in the cluster system.
 * This class provides the core functionalities for node operations,
 * packet dispatching, connection management, and unique identifier generation.
 * <p>
 * Subclasses are expected to implement the specific behavior of nodes
 * in the cluster, handling communication and processing of backPackets
 * sent to and from other nodes.
 */
public abstract class AbstractBackUpEndpoint {

	/**
	 * Starts the node and initializes necessary resources for operation.
	 * This method should be called to begin the node's lifecycle,
	 * including establishing connections and preparing to send/receive backPackets.
	 */
	public abstract void start();

	/**
	 * Dispatches a packet to the ring for processing.
	 *
	 * @param packet The {@link RingBackPacket} to be dispatched.
	 *               This represents a message or command intended for
	 *               the cluster's ring topology.
	 */
	public abstract void dispatchPacketToRing(RingBackPacket packet);

	/**
	 * Dispatches a request packet to the ring and handles the reply.
	 *
	 * @param requestPacket The {@link RequestBackPacket} to be sent to the ring.
	 * @param replyPacket   A consumer that processes the {@link ResponseBackPacket}
	 *                      received in response to the request. This allows
	 *                      for asynchronous handling of replies.
	 */
	public abstract void dispatchRequestToRing(RequestBackPacket requestPacket, Consumer<ResponseBackPacket> replyPacket);

	protected abstract void notifyPublicationHandlers(Session session, Publication publication);

	/**
	 * Gets the node's own address information.
	 *
	 * @return The {@link MemberIdentifier} representing this node's unique identifier,
	 *         hostname, and port.
	 */
	public abstract MemberIdentifier getOwnNode();

	/**
	 * Retrieves a map of other nodes in the cluster along with their associated network clients.
	 *
	 * @return A map where the key is {@link MemberIdentifier} for each node and
	 *         the value is the corresponding {@link FallbackFallbackNetworkClient} used for communication.
	 */
	public abstract Map<MemberIdentifier, FallbackFallbackNetworkClient> getOtherNodes();

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
	 *         backPackets by their ID and vice versa.
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

	/**
	 * Registers a new {@link APIPublicationHandler} to handle publications.
	 *
	 * <p>This method ensures that the same {@code APIPublicationHandler} cannot be registered more than once.</p>
	 *
	 * @param publicationHandler the publication handler to register
	 * @throws IllegalArgumentException if the publicationHandler is null
	 */
	public abstract void registerPublicationHandler(APIPublicationHandler publicationHandler);
}
