package net.cakemc.cluster;

import net.cakemc.cluster.algorithm.AbstractNodeAlgorithm;
import net.cakemc.cluster.algorithm.RingTopologicalAlgorithm;
import net.cakemc.cluster.endpoint.NetworkClient;
import net.cakemc.cluster.endpoint.NetworkServer;
import net.cakemc.cluster.endpoint.check.AbstractNodeManager;
import net.cakemc.cluster.endpoint.check.NodeManager;
import net.cakemc.cluster.endpoint.connection.AbstractConnectionManager;
import net.cakemc.cluster.endpoint.connection.ConnectionManager;
import net.cakemc.cluster.handler.DefaultConnectionHandler;
import net.cakemc.cluster.info.NodeInformation;
import net.cakemc.cluster.packet.AbstractPacketRegistry;
import net.cakemc.cluster.packet.PacketRegistry;
import net.cakemc.cluster.packet.ring.RequestPacket;
import net.cakemc.cluster.packet.ring.ResponsePacket;
import net.cakemc.cluster.packet.ring.RingPacket;
import net.cakemc.cluster.tick.TickThread;
import net.cakemc.cluster.tick.TickAble;
import net.cakemc.cluster.units.Snowflake;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Represents a cluster node in a distributed system.
 * This class handles the initialization and management of the node's connections,
 * packet handling, and periodic tasks.
 *
 * <p>The ClusterNode serves as the main entry point for the node's functionalities
 * including connecting to other nodes, managing network interactions,
 * and processing incoming and outgoing packets.</p>
 */
public class ClusterNode extends AbstractNode implements TickAble {

	/** Thread pool for executing tasks asynchronously. */
	private final ExecutorService executorService;

	/** Unique identifier generator for the node. */
	private final Snowflake snowflake;

	/** The cluster key used for identification within the cluster. */
	private final String clusterKey;

	/** The address of the current node. */
	private final NodeAddress ownNode;

	/** A map storing other nodes and their corresponding network clients. */
	private final Map<NodeInformation, NetworkClient> otherNodes;

	/** The server responsible for handling incoming network connections. */
	private final NetworkServer networkServer;

	/** Manager responsible for handling node status checks. */
	private final AbstractNodeManager nodeManager;

	/** Algorithm used for determining node interactions in the cluster. */
	private final AbstractNodeAlgorithm nodeAlgorithm;

	/** Manager responsible for handling network connections. */
	private final AbstractConnectionManager connectionManager;

	/** Registry for managing packet types and their associated handlers. */
	private final AbstractPacketRegistry packetRegistry;

	/** Handler for managing connections and packets. */
	private final DefaultConnectionHandler connectionHandler;

	/** Thread responsible for periodic ticking tasks. */
	private final TickThread tickThread;

	/** Unique network identifier for the node. */
	private final long networkId;

	/**
	 * Constructs a new ClusterNode with the specified parameters.
	 *
	 * @param ownNode     The address of the current node.
	 * @param otherNodes  A list of other nodes to connect to.
	 * @param clusterKey  The key for the cluster.
	 */
	public ClusterNode(NodeAddress ownNode, List<NodeInformation> otherNodes, String clusterKey) {
		this.ownNode = ownNode;
		this.clusterKey = clusterKey;
		this.otherNodes = new HashMap<>();

		this.executorService = Executors.newCachedThreadPool();
		this.snowflake = new Snowflake();

		this.networkId = snowflake.nextId();

		this.packetRegistry = new PacketRegistry();

		this.connectionManager = new ConnectionManager();
		this.connectionHandler = new DefaultConnectionHandler(this);
		this.connectionManager.registerPacketHandler(connectionHandler);

		this.nodeManager = new NodeManager(3, () -> {
			System.out.println("checked targets");
		});

		// Initialize connections to other nodes
		for (NodeInformation otherNode : otherNodes) {
			if (otherNode.getAddress().id() == ownNode.id())
				continue;

			NetworkClient networkClient = new NetworkClient(this, otherNode);
			networkClient.initialize();

			this.nodeManager.addNode(otherNode);
			this.otherNodes.put(otherNode, networkClient);
		}

		this.tickThread = new TickThread(this);

		this.networkServer = new NetworkServer(this);
		this.networkServer.initialize();

		this.nodeAlgorithm = new RingTopologicalAlgorithm(nodeManager, ownNode.id());
	}

	/**
	 * Dispatches a packet to the ring for processing.
	 *
	 * @param packet The packet to be dispatched.
	 */
	@Override
	public void dispatchPacketToRing(RingPacket packet) {
		connectionHandler.dispatchPacketToRing(packet);
	}

	/**
	 * Dispatches a request packet to the ring and registers a callback for the reply.
	 *
	 * @param requestPacket The request packet to be sent.
	 * @param replyPacket   A callback to handle the response packet.
	 */
	@Override
	public void dispatchRequestToRing(RequestPacket requestPacket, Consumer<ResponsePacket> replyPacket) {
		connectionHandler.dispatchRequestToRing(requestPacket, replyPacket);
	}

	/**
	 * Executes the tick method for each network client and the network server.
	 * This method is called periodically by the TickThread.
	 */
	@Override
	public void tick() {
		this.otherNodes.forEach((nodeInformation, networkClient) -> networkClient.tick());
		this.networkServer.tick();
		this.connectionHandler.tick();
	}

	/**
	 * Starts the ClusterNode, initiating the server connection and connecting to other nodes.
	 */
	@Override
	public void start() {
		Thread serverThread = new Thread(
			 this.networkServer::connect,
			 "network-server-%s".formatted(this.getOwnNode().id())
		);
		serverThread.start();

		// Connect to other nodes using the executor service
		this.otherNodes.forEach((nodeInformation, networkClient) -> {
			executorService.submit(networkClient::connect);
		});
	}

	/**
	 * Returns the address of the current node.
	 *
	 * @return The own node's address.
	 */
	@Override
	public NodeAddress getOwnNode() {
		return ownNode;
	}

	/**
	 * Returns a map of other nodes and their corresponding network clients.
	 *
	 * @return A map of other nodes.
	 */
	@Override
	public Map<NodeInformation, NetworkClient> getOtherNodes() {
		return otherNodes;
	}

	/**
	 * Returns the TickThread responsible for managing periodic tasks.
	 *
	 * @return The tick thread instance.
	 */
	public TickThread getTaskScheduler() {
		return tickThread;
	}

	/**
	 * Returns the network server instance managing incoming connections.
	 *
	 * @return The network server.
	 */
	public NetworkServer getNetworkServer() {
		return networkServer;
	}

	/**
	 * Returns the connection manager responsible for handling network connections.
	 *
	 * @return The connection manager.
	 */
	@Override
	public AbstractConnectionManager getConnectionManager() {
		return connectionManager;
	}

	/**
	 * Returns the algorithm used for managing node interactions in the cluster.
	 *
	 * @return The node algorithm.
	 */
	public AbstractNodeAlgorithm getNodeAlgorithm() {
		return nodeAlgorithm;
	}

	/**
	 * Returns the manager responsible for checking node statuses.
	 *
	 * @return The node manager.
	 */
	public AbstractNodeManager getNodeManager() {
		return nodeManager;
	}

	/**
	 * Returns the packet registry managing the different packet types.
	 *
	 * @return The packet registry.
	 */
	@Override
	public AbstractPacketRegistry getPacketRegistry() {
		return packetRegistry;
	}

	/**
	 * Returns the unique identifier generator for the node.
	 *
	 * @return The snowflake instance.
	 */
	@Override
	public Snowflake getSnowflake() {
		return snowflake;
	}

	/**
	 * Returns the unique network identifier for the node.
	 *
	 * @return The network identifier.
	 */
	@Override
	public long getNetworkId() {
		return networkId;
	}

	/**
	 * Returns the cluster key used for identification within the cluster.
	 *
	 * @return The cluster key.
	 */
	public String getClusterKey() {
		return clusterKey;
	}
}
