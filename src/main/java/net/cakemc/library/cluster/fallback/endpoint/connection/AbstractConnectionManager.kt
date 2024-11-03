package net.cakemc.library.cluster.fallback.endpoint.connection

import io.netty.channel.Channel
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.fallback.endpoint.EndpointType
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler

/**
 * An abstract class that manages network connections and packet handling.
 *
 *
 * The `AbstractConnectionManager` provides the foundational methods for managing
 * connections in the cluster. Subclasses are expected to implement the specific behavior
 * for registering packet handlers, processing inbound backPackets, and managing connection
 * states such as connecting and disconnecting.
 *
 * <h2>BackPacket Handling</h2>
 *
 * BackPacket handlers are responsible for processing incoming backPackets based on their type.
 * The [AbstractConnectionHandler] can be registered for handling specific packet types.
 *
 * <h2>Connection Management</h2>
 *
 * This class defines methods for handling connections and disconnections, allowing
 * subclasses to implement the logic for managing client and server connections.
 *
 * @see AbstractConnectionHandler
 *
 * @see Publication
 */
abstract class AbstractConnectionManager {
    /**
     * Registers a packet handler to manage incoming backPackets.
     *
     * @param abstractConnectionHandler the [AbstractConnectionHandler] responsible for processing backPackets
     */
    abstract fun registerPacketHandler(abstractConnectionHandler: AbstractConnectionHandler)

    /**
     * Handles an inbound packet received from a channel.
     *
     * @param sender the [Channel] from which the packet was received
     * @param ringPacket the [Publication] that was received
     */
    abstract fun handleInboundPacket(sender: Channel, ringPacket: Publication)

    /**
     * Handles the disconnection of a channel.
     *
     * @param channel the [Channel] that has disconnected
     * @param endpointType the [EndpointType] of the disconnected endpoint
     */
    abstract fun handleDisconnect(channel: Channel?, endpointType: EndpointType?)

    /**
     * Handles the connection of a channel.
     *
     * @param channel the [Channel] that has connected
     * @param endpointType the [EndpointType] of the connected endpoint
     */
    abstract fun handleConnect(channel: Channel?, endpointType: EndpointType?)
}
