package net.cakemc.library.cluster.fallback

import net.cakemc.library.cluster.api.MemberIdentifier
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.config.Snowflake
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkClient
import net.cakemc.library.cluster.fallback.endpoint.connection.AbstractConnectionManager

/**
 * Represents an abstract node in the cluster system.
 * This class provides the core functionalities for node operations,
 * packet dispatching, connection management, and unique identifier generation.
 *
 *
 * Subclasses are expected to implement the specific behavior of nodes
 * in the cluster, handling communication and processing of backPackets
 * sent to and from other nodes.
 */
abstract class AbstractBackUpEndpoint {
    /**
     * Starts the node and initializes necessary resources for operation.
     * This method should be called to begin the node's lifecycle,
     * including establishing connections and preparing to send/receive backPackets.
     */
    abstract fun start()

    /**
     * Dispatches a packet to the ring for processing.
     *
     * @param packet The [Publication] to be dispatched.
     * This represents a message or command intended for
     * the cluster's ring topology.
     */
    abstract fun dispatchPacketToRing(packet: Publication?)

    /**
     * Gets the node's own address information.
     *
     * @return The [MemberIdentifier] representing this node's unique identifier,
     * hostname, and port.
     */
    abstract val ownNode: MemberIdentifier

    /**
     * Retrieves a map of other nodes in the cluster along with their associated network clients.
     *
     * @return A map where the key is [MemberIdentifier] for each node and
     * the value is the corresponding [FallbackFallbackNetworkClient] used for communication.
     */
    abstract val otherNodes: Map<MemberIdentifier?, FallbackFallbackNetworkClient?>

    /**
     * Gets the connection manager responsible for managing connections to other nodes.
     *
     * @return The [AbstractConnectionManager] instance managing the network connections.
     */
    abstract val connectionManager: AbstractConnectionManager

    /**
     * Retrieves the unique identifier generator for this node.
     *
     * @return The [Snowflake] instance used for generating unique IDs for
     * various purposes, such as requests and identifiers.
     */
    abstract val snowflake: Snowflake

    /**
     * Gets the network ID of this node.
     *
     * @return The unique network ID assigned to this node, used for identification
     * within the cluster.
     */
    abstract val networkId: Long
}
