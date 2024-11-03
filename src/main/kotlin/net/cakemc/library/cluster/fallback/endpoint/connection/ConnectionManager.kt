package net.cakemc.library.cluster.fallback.endpoint.connection

import io.netty.channel.Channel
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.fallback.BackUpClusterNode
import net.cakemc.library.cluster.fallback.endpoint.EndpointType
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler

/**
 * Manages network connections and packet handling in the cluster.
 *
 *
 * The `ConnectionManager` class extends [AbstractConnectionManager]
 * and implements the methods for handling connections and packet processing. It maintains
 * a list of [AbstractConnectionHandler] instances that are responsible for managing
 * specific packet types and connection states.
 *
 * <h2>BackPacket Handling</h2>
 *
 * This class handles incoming backPackets by delegating the processing to registered
 * connection handlers. Each handler processes the packet in its own context.
 *
 * <h2>Connection Management</h2>
 *
 * When a connection is established or disconnected, the manager notifies all
 * registered connection handlers, allowing them to take appropriate actions.
 *
 * @see AbstractConnectionManager
 *
 * @see AbstractConnectionHandler
 */
class ConnectionManager(private val backUpClusterNode: BackUpClusterNode) : AbstractConnectionManager() {
    /**
     * The list of registered connection handlers for processing backPackets and managing connections.
     */
    private val abstractConnectionHandlers: MutableList<AbstractConnectionHandler> = ArrayList()

    /**
     * Handles an inbound packet received from a channel.
     *
     *
     * This method iterates through all registered connection handlers and delegates the
     * processing of the received [Publication] to each handler.
     *
     * @param sender the [Channel] from which the packet was received
     * @param ringPacket the [Publication] that was received
     */
    override fun handleInboundPacket(sender: Channel, ringPacket: Publication) {
        for (connectionHandler in abstractConnectionHandlers) {
            connectionHandler.handlePacket(sender, ringPacket)
        }
    }

    /**
     * Registers a new packet handler to manage incoming backPackets.
     *
     *
     * The registered [AbstractConnectionHandler] will be notified of all backPackets
     * received through the `ConnectionManager`.
     *
     * @param abstractConnectionHandler the [AbstractConnectionHandler] responsible for processing backPackets
     */
    override fun registerPacketHandler(abstractConnectionHandler: AbstractConnectionHandler) {
        abstractConnectionHandlers.add(abstractConnectionHandler)
    }

    /**
     * Handles the disconnection of a channel.
     *
     *
     * This method notifies all registered connection handlers about the disconnection
     * event, allowing them to perform necessary cleanup or state updates.
     *
     * @param channel the [Channel] that has disconnected
     * @param endpointType the [EndpointType] of the disconnected endpoint
     */
    override fun handleDisconnect(channel: Channel?, endpointType: EndpointType?) {
        for (abstractConnectionHandler in abstractConnectionHandlers) {
            abstractConnectionHandler.handleDisconnect(channel, endpointType)
        }
    }

    /**
     * Handles the connection of a channel.
     *
     *
     * This method notifies all registered connection handlers about the new connection,
     * allowing them to take appropriate actions for the established connection.
     *
     * @param channel the [Channel] that has connected
     * @param endpointType the [EndpointType] of the connected endpoint
     */
    override fun handleConnect(channel: Channel?, endpointType: EndpointType?) {
        for (abstractConnectionHandler in abstractConnectionHandlers) {
            abstractConnectionHandler.handleConnect(channel, endpointType)
        }
    }

    val packetHandlers: List<AbstractConnectionHandler>
        /**
         * Returns the list of registered packet handlers.
         *
         * @return a list of [AbstractConnectionHandler] instances
         */
        get() = abstractConnectionHandlers
}
