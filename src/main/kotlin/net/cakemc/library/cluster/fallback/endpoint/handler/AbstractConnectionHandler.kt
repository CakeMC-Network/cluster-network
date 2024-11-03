package net.cakemc.library.cluster.fallback.endpoint.handler

import io.netty.channel.Channel
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.fallback.endpoint.EndpointType

/**
 * The `AbstractConnectionHandler` class serves as an abstract base class for handling
 * connection events and packet processing within a cluster node.
 *
 *
 * This class defines methods for handling incoming backPackets, as well as
 * connection establishment and disconnection events. Subclasses should implement
 * the [.handlePacket] method to provide specific
 * packet processing logic.
 *
 * @see Publication
 *
 * @see EndpointType
 */
abstract class AbstractConnectionHandler {
    /**
     * Handles an incoming [Publication] from the specified sender channel.
     *
     * @param sender the [Channel] that sent the packet
     * @param ringPacket the incoming [Publication] to be processed
     */
    abstract fun handlePacket(sender: Channel, ringPacket: Publication)

    /**
     * Handles a connection event when a channel becomes active.
     *
     *
     * This method can be overridden in subclasses to provide specific
     * logic for handling new connections.
     *
     * @param channel the [Channel] that is connecting
     * @param endpointType the [EndpointType] indicating the type of endpoint
     */
    open fun handleConnect(channel: Channel?, endpointType: EndpointType?) {
        // Optional: Implement specific connection handling logic in subclasses.
    }

    /**
     * Handles a disconnection event when a channel becomes inactive.
     *
     *
     * This method can be overridden in subclasses to provide specific
     * logic for handling disconnections.
     *
     * @param channel the [Channel] that is disconnecting
     * @param endpointType the [EndpointType] indicating the type of endpoint
     */
    open fun handleDisconnect(channel: Channel?, endpointType: EndpointType?) {
        // Optional: Implement specific disconnection handling logic in subclasses.
    }
}
