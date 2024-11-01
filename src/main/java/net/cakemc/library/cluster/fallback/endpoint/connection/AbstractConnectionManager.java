package net.cakemc.library.cluster.fallback.endpoint.connection;

import io.netty.channel.Channel;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler;

/**
 * An abstract class that manages network connections and packet handling.
 *
 * <p>The {@code AbstractConnectionManager} provides the foundational methods for managing
 * connections in the cluster. Subclasses are expected to implement the specific behavior
 * for registering packet handlers, processing inbound backPackets, and managing connection
 * states such as connecting and disconnecting.</p>
 *
 * <h2>BackPacket Handling</h2>
 * <p>BackPacket handlers are responsible for processing incoming backPackets based on their type.
 * The {@link AbstractConnectionHandler} can be registered for handling specific packet types.</p>
 *
 * <h2>Connection Management</h2>
 * <p>This class defines methods for handling connections and disconnections, allowing
 * subclasses to implement the logic for managing client and server connections.</p>
 *
 * @see AbstractConnectionHandler
 * @see Publication
 */
public abstract class AbstractConnectionManager {

	/**
	 * Registers a packet handler to manage incoming backPackets.
	 *
	 * @param abstractConnectionHandler the {@link AbstractConnectionHandler} responsible for processing backPackets
	 */
	public abstract void registerPacketHandler(AbstractConnectionHandler abstractConnectionHandler);

	/**
	 * Handles an inbound packet received from a channel.
	 *
	 * @param sender the {@link Channel} from which the packet was received
	 * @param ringPacket the {@link Publication} that was received
	 */
	public abstract void handleInboundPacket(Channel sender, Publication ringPacket);

	/**
	 * Handles the disconnection of a channel.
	 *
	 * @param channel the {@link Channel} that has disconnected
	 * @param endpointType the {@link EndpointType} of the disconnected endpoint
	 */
	public abstract void handleDisconnect(Channel channel, EndpointType endpointType);

	/**
	 * Handles the connection of a channel.
	 *
	 * @param channel the {@link Channel} that has connected
	 * @param endpointType the {@link EndpointType} of the connected endpoint
	 */
	public abstract void handleConnect(Channel channel, EndpointType endpointType);
}
