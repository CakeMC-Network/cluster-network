package net.cakemc.cluster.endpoint.connection;

import io.netty.channel.Channel;
import net.cakemc.cluster.endpoint.EndpointType;
import net.cakemc.cluster.handler.ConnectionHandler;
import net.cakemc.cluster.packet.ring.RingPacket;

/**
 * An abstract class that manages network connections and packet handling.
 *
 * <p>The {@code AbstractConnectionManager} provides the foundational methods for managing
 * connections in the cluster. Subclasses are expected to implement the specific behavior
 * for registering packet handlers, processing inbound packets, and managing connection
 * states such as connecting and disconnecting.</p>
 *
 * <h2>Packet Handling</h2>
 * <p>Packet handlers are responsible for processing incoming packets based on their type.
 * The {@link ConnectionHandler} can be registered for handling specific packet types.</p>
 *
 * <h2>Connection Management</h2>
 * <p>This class defines methods for handling connections and disconnections, allowing
 * subclasses to implement the logic for managing client and server connections.</p>
 *
 * @see ConnectionHandler
 * @see RingPacket
 */
public abstract class AbstractConnectionManager {

	/**
	 * Registers a packet handler to manage incoming packets.
	 *
	 * @param connectionHandler the {@link ConnectionHandler} responsible for processing packets
	 */
	public abstract void registerPacketHandler(ConnectionHandler connectionHandler);

	/**
	 * Handles an inbound packet received from a channel.
	 *
	 * @param sender the {@link Channel} from which the packet was received
	 * @param ringPacket the {@link RingPacket} that was received
	 */
	public abstract void handleInboundPacket(Channel sender, RingPacket ringPacket);

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
