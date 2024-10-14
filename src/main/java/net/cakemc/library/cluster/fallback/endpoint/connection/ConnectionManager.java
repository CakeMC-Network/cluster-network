package net.cakemc.library.cluster.fallback.endpoint.connection;

import io.netty.channel.Channel;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.BackUpClusterNode;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractFallbackConnectionHandler;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;
import net.cakemc.library.cluster.network.NetworkSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages network connections and packet handling in the cluster.
 *
 * <p>The {@code ConnectionManager} class extends {@link AbstractConnectionManager}
 * and implements the methods for handling connections and packet processing. It maintains
 * a list of {@link AbstractFallbackConnectionHandler} instances that are responsible for managing
 * specific packet types and connection states.</p>
 *
 * <h2>BackPacket Handling</h2>
 * <p>This class handles incoming backPackets by delegating the processing to registered
 * connection handlers. Each handler processes the packet in its own context.</p>
 *
 * <h2>Connection Management</h2>
 * <p>When a connection is established or disconnected, the manager notifies all
 * registered connection handlers, allowing them to take appropriate actions.</p>
 *
 * @see AbstractConnectionManager
 * @see AbstractFallbackConnectionHandler
 * @see RingBackPacket
 */
public class ConnectionManager extends AbstractConnectionManager {

	/**
	 * The list of registered connection handlers for processing backPackets and managing connections.
	 */
	private final List<AbstractFallbackConnectionHandler> abstractFallbackConnectionHandlers = new ArrayList<>();

	private final BackUpClusterNode backUpClusterNode;

	public ConnectionManager(BackUpClusterNode backUpClusterNode) {
		this.backUpClusterNode = backUpClusterNode;
	}

	/**
	 * Handles an inbound packet received from a channel.
	 *
	 * <p>This method iterates through all registered connection handlers and delegates the
	 * processing of the received {@link RingBackPacket} to each handler.</p>
	 *
	 * @param sender the {@link Channel} from which the packet was received
	 * @param ringPacket the {@link RingBackPacket} that was received
	 */
	@Override
	public void handleInboundPacket(Channel sender, RingBackPacket ringPacket) {
		for (AbstractFallbackConnectionHandler abstractFallbackConnectionHandler : abstractFallbackConnectionHandlers) {
			abstractFallbackConnectionHandler.handlePacket(sender, ringPacket);
		}

		if (ringPacket instanceof Publication publication) {
			this.backUpClusterNode.notifyPublicationHandlers(
				 new NetworkSession(sender), publication
			);
		}
	}

	/**
	 * Registers a new packet handler to manage incoming backPackets.
	 *
	 * <p>The registered {@link AbstractFallbackConnectionHandler} will be notified of all backPackets
	 * received through the {@code ConnectionManager}.</p>
	 *
	 * @param abstractFallbackConnectionHandler the {@link AbstractFallbackConnectionHandler} responsible for processing backPackets
	 */
	@Override
	public void registerPacketHandler(AbstractFallbackConnectionHandler abstractFallbackConnectionHandler) {
		abstractFallbackConnectionHandlers.add(abstractFallbackConnectionHandler);
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
		for (AbstractFallbackConnectionHandler abstractFallbackConnectionHandler : abstractFallbackConnectionHandlers) {
			abstractFallbackConnectionHandler.handleDisconnect(channel, endpointType);
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
		for (AbstractFallbackConnectionHandler abstractFallbackConnectionHandler : abstractFallbackConnectionHandlers) {
			abstractFallbackConnectionHandler.handleConnect(channel, endpointType);
		}
	}

	/**
	 * Returns the list of registered packet handlers.
	 *
	 * @return a list of {@link AbstractFallbackConnectionHandler} instances
	 */
	public List<AbstractFallbackConnectionHandler> getPacketHandlers() {
		return abstractFallbackConnectionHandlers;
	}
}
