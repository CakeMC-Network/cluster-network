package net.cakemc.cluster.endpoint.connection;

import io.netty.channel.Channel;
import net.cakemc.cluster.endpoint.EndpointType;
import net.cakemc.cluster.handler.ConnectionHandler;
import net.cakemc.cluster.packet.ring.RingPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages network connections and packet handling in the cluster.
 *
 * <p>The {@code ConnectionManager} class extends {@link AbstractConnectionManager}
 * and implements the methods for handling connections and packet processing. It maintains
 * a list of {@link ConnectionHandler} instances that are responsible for managing
 * specific packet types and connection states.</p>
 *
 * <h2>Packet Handling</h2>
 * <p>This class handles incoming packets by delegating the processing to registered
 * connection handlers. Each handler processes the packet in its own context.</p>
 *
 * <h2>Connection Management</h2>
 * <p>When a connection is established or disconnected, the manager notifies all
 * registered connection handlers, allowing them to take appropriate actions.</p>
 *
 * @see AbstractConnectionManager
 * @see ConnectionHandler
 * @see RingPacket
 */
public class ConnectionManager extends AbstractConnectionManager {

	/**
	 * The list of registered connection handlers for processing packets and managing connections.
	 */
	private final List<ConnectionHandler> connectionHandlers = new ArrayList<>();

	/**
	 * Handles an inbound packet received from a channel.
	 *
	 * <p>This method iterates through all registered connection handlers and delegates the
	 * processing of the received {@link RingPacket} to each handler.</p>
	 *
	 * @param sender the {@link Channel} from which the packet was received
	 * @param ringPacket the {@link RingPacket} that was received
	 */
	@Override
	public void handleInboundPacket(Channel sender, RingPacket ringPacket) {
		for (ConnectionHandler connectionHandler : connectionHandlers) {
			connectionHandler.handlePacket(sender, ringPacket);
		}
	}

	/**
	 * Registers a new packet handler to manage incoming packets.
	 *
	 * <p>The registered {@link ConnectionHandler} will be notified of all packets
	 * received through the {@code ConnectionManager}.</p>
	 *
	 * @param connectionHandler the {@link ConnectionHandler} responsible for processing packets
	 */
	@Override
	public void registerPacketHandler(ConnectionHandler connectionHandler) {
		connectionHandlers.add(connectionHandler);
	}

	/**
	 * Handles the disconnection of a channel.
	 *
	 * <p>This method notifies all registered connection handlers about the disconnection
	 * event, allowing them to perform necessary cleanup or state updates.</p>
	 *
	 * @param channel the {@link Channel} that has disconnected
	 * @param endpointType the {@link EndpointType} of the disconnected endpoint
	 */
	@Override
	public void handleDisconnect(Channel channel, EndpointType endpointType) {
		for (ConnectionHandler connectionHandler : connectionHandlers) {
			connectionHandler.handleDisconnect(channel, endpointType);
		}
	}

	/**
	 * Handles the connection of a channel.
	 *
	 * <p>This method notifies all registered connection handlers about the new connection,
	 * allowing them to take appropriate actions for the established connection.</p>
	 *
	 * @param channel the {@link Channel} that has connected
	 * @param endpointType the {@link EndpointType} of the connected endpoint
	 */
	@Override
	public void handleConnect(Channel channel, EndpointType endpointType) {
		for (ConnectionHandler connectionHandler : connectionHandlers) {
			connectionHandler.handleConnect(channel, endpointType);
		}
	}

	/**
	 * Returns the list of registered packet handlers.
	 *
	 * @return a list of {@link ConnectionHandler} instances
	 */
	public List<ConnectionHandler> getPacketHandlers() {
		return connectionHandlers;
	}
}
