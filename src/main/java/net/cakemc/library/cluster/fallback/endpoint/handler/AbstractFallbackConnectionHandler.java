package net.cakemc.library.cluster.fallback.endpoint.handler;

import io.netty.channel.Channel;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

/**
 * The {@code AbstractFallbackConnectionHandler} class serves as an abstract base class for handling
 * connection events and packet processing within a cluster node.
 *
 * <p>This class defines methods for handling incoming backPackets, as well as
 * connection establishment and disconnection events. Subclasses should implement
 * the {@link #handlePacket(Channel, RingBackPacket)} method to provide specific
 * packet processing logic.</p>
 *
 * @see RingBackPacket
 * @see EndpointType
 */
public abstract class AbstractFallbackConnectionHandler {

	/**
	 * Handles an incoming {@link RingBackPacket} from the specified sender channel.
	 *
	 * @param sender the {@link Channel} that sent the packet
	 * @param ringPacket the incoming {@link RingBackPacket} to be processed
	 */
	public abstract void handlePacket(Channel sender, RingBackPacket ringPacket);

	/**
	 * Handles a connection event when a channel becomes active.
	 *
	 * <p>This method can be overridden in subclasses to provide specific
	 * logic for handling new connections.</p>
	 *
	 * @param channel the {@link Channel} that is connecting
	 * @param endpointType the {@link EndpointType} indicating the type of endpoint
	 */
	public void handleConnect(Channel channel, EndpointType endpointType) {
		// Optional: Implement specific connection handling logic in subclasses.
	}

	/**
	 * Handles a disconnection event when a channel becomes inactive.
	 *
	 * <p>This method can be overridden in subclasses to provide specific
	 * logic for handling disconnections.</p>
	 *
	 * @param channel the {@link Channel} that is disconnecting
	 * @param endpointType the {@link EndpointType} indicating the type of endpoint
	 */
	public void handleDisconnect(Channel channel, EndpointType endpointType) {
		// Optional: Implement specific disconnection handling logic in subclasses.
	}
}
