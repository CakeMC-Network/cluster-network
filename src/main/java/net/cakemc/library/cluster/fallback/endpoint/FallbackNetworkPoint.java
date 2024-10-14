package net.cakemc.library.cluster.fallback.endpoint;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;
import net.cakemc.library.cluster.tick.TickAble;

/**
 * Represents a network endpoint in the cluster that can establish connections and
 * communicate with other nodes.
 *
 * <p>The {@code FallbackNetworkPoint} class serves as an abstract base for various network
 * implementations, such as client and server endpoints. It manages connection
 * parameters and provides methods for initializing, connecting, and shutting down
 * the network point.</p>
 *
 * <p>This class also includes support for different channel types, specifically
 * Epoll and KQueue, which are available depending on the operating system.</p>
 *
 * @see AbstractBackUpEndpoint
 * @see MemberIdentifier
 * @see RingBackPacket
 * @see TickAble
 */
public abstract class FallbackNetworkPoint implements TickAble {

	/**
	 * The name of the packet codec handler.
	 */
	protected static final String PACKET_CODEC = "packet_codec";

	/**
	 * The name of the compression codec handler.
	 */
	protected static final String COMPRESSION_CODEC = "compression_codec";

	/**
	 * The name of the boss handler for managing acceptor threads.
	 */
	protected static final String BOSS_HANDLER = "boss_handler";

	/**
	 * Indicates whether Epoll is available for use.
	 */
	public static final boolean EPOLL = Epoll.isAvailable();

	/**
	 * Indicates whether KQueue is available for use.
	 */
	public static final boolean KQUEUE = KQueue.isAvailable();

	/**
	 * The cluster node associated with this network point.
	 */
	protected final AbstractBackUpEndpoint clusterNode;

	/**
	 * The host address of this network point.
	 */
	protected final String host;

	/**
	 * The port number of this network point.
	 */
	protected final int port;

	/**
	 * Indicates whether this network point is currently connected.
	 */
	protected boolean connected;

	/**
	 * Constructs a new {@code FallbackNetworkPoint} with the specified cluster node.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 */
	public FallbackNetworkPoint(AbstractBackUpEndpoint clusterNode) {
		this.clusterNode = clusterNode;
		this.host = clusterNode.getOwnNode().getAddress().getAddress().getHostAddress();
		this.port = clusterNode.getOwnNode().getAddress().getPort();
	}

	/**
	 * Constructs a new {@code FallbackNetworkPoint} with the specified cluster node, host,
	 * and port.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 * @param host the host address of the network point
	 * @param port the port number of the network point
	 */
	public FallbackNetworkPoint(AbstractBackUpEndpoint clusterNode, String host, int port) {
		this.clusterNode = clusterNode;
		this.host = host;
		this.port = port;
	}

	/**
	 * Constructs a new {@code FallbackNetworkPoint} using the specified cluster node and
	 * node information.
	 *
	 * @param clusterNode the {@link AbstractBackUpEndpoint} representing the cluster node
	 * @param nodeInformation the {@link MemberIdentifier} containing address details
	 */
	public FallbackNetworkPoint(AbstractBackUpEndpoint clusterNode, MemberIdentifier nodeInformation) {
		this.clusterNode = clusterNode;
		this.host = nodeInformation.getAddress().getAddress().getHostAddress();
		this.port = nodeInformation.getAddress().getPort();
	}

	/**
	 * Initializes the network point, setting up necessary resources.
	 */
	public abstract void initialize();

	/**
	 * Establishes a connection to the specified network point.
	 */
	public abstract void connect();

	/**
	 * Shuts down the network point, releasing any resources and connections.
	 */
	public abstract void shutdown();

	/**
	 * Returns the host address of this network point.
	 *
	 * @return the host address as a {@code String}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the port number of this network point.
	 *
	 * @return the port number as an {@code int}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Checks if this network point is currently connected.
	 *
	 * @return {@code true} if connected, {@code false} otherwise
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Dispatches a packet to the appropriate handler.
	 *
	 * <p>This method can be overridden to implement custom packet dispatching logic.</p>
	 *
	 * @param packet the {@link RingBackPacket} to be dispatched
	 */
	public void dispatchPacket(RingBackPacket packet) {}
}
