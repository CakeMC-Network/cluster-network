package net.cakemc.library.cluster.fallback;

import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkClient;
import net.cakemc.library.cluster.fallback.endpoint.handler.DummyConnectionHandler;
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkServer;
import net.cakemc.library.cluster.fallback.endpoint.connection.AbstractConnectionManager;
import net.cakemc.library.cluster.fallback.endpoint.connection.ConnectionManager;
import net.cakemc.library.cluster.tick.TickThread;
import net.cakemc.library.cluster.tick.TickAble;
import net.cakemc.library.cluster.config.Snowflake;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a cluster node in a distributed system.
 * This class handles the initialization and management of the node's connections,
 * packet handling, and periodic tasks.
 *
 * <p>The BackUpClusterNodeBackUp serves as the main entry point for the node's functionalities
 * including connecting to other nodes, managing network interactions,
 * and processing incoming and outgoing backPackets.</p>
 */
public class BackUpClusterNode extends AbstractBackUpEndpoint implements TickAble {

	/** Thread pool for executing tasks asynchronously. */
	private final ExecutorService executorService;

	/** Unique identifier generator for the node. */
	private final Snowflake snowflake;

	/** The cluster key used for identification within the cluster. */
	private final String clusterKey;

	/** The address of the current node. */
	private final MemberIdentifier ownNode;

	/** A map storing other nodes and their corresponding network clients. */
	private final Map<MemberIdentifier, FallbackFallbackNetworkClient> otherNodes;

	/** The server responsible for handling incoming network connections. */
	private final FallbackFallbackNetworkServer fallbackNetworkServer;

	/** Manager responsible for handling network connections. */
	private final AbstractConnectionManager connectionManager;

	/** Handler for managing connections and backPackets. */
	private final DummyConnectionHandler connectionHandler;

	/** Thread responsible for periodic ticking tasks. */
	private final TickThread tickThread;

	/** Unique network identifier for the node. */
	private final long networkId;

	/**
	 * Constructs a new BackUpClusterNodeBackUp with the specified parameters.
	 *
	 * @param ownNode     The address of the current node.
	 * @param otherNodes  A list of other nodes to connect to.
	 * @param clusterKey  The key for the cluster.
	 */
	public BackUpClusterNode(MemberIdentifier ownNode, List<MemberIdentifier> otherNodes, String clusterKey) {
		this.ownNode = ownNode;
		this.clusterKey = clusterKey;
		this.otherNodes = new HashMap<>();

		this.executorService = Executors.newCachedThreadPool();
		this.snowflake = new Snowflake();

		this.networkId = snowflake.nextId();

		this.connectionManager = new ConnectionManager(this);
		this.connectionHandler = new DummyConnectionHandler(this);
		this.connectionManager.registerPacketHandler(connectionHandler);

		// Initialize connections to other nodes
		for (MemberIdentifier otherNode : otherNodes) {
			if (otherNode.getId() == ownNode.getId())
				continue;

			FallbackFallbackNetworkClient fallbackNetworkClient = new FallbackFallbackNetworkClient(this, otherNode);
			fallbackNetworkClient.initialize();

			this.otherNodes.put(otherNode, fallbackNetworkClient);
		}

		this.tickThread = new TickThread(this);

		this.fallbackNetworkServer = new FallbackFallbackNetworkServer(this);
		this.fallbackNetworkServer.initialize();
	}

	/**
	 * Dispatches a packet to the ring for processing.
	 *
	 * @param packet The packet to be dispatched.
	 */
	@Override
	public void dispatchPacketToRing(Publication packet) {
		connectionHandler.dispatchPacketToRing(packet);
	}

	/**
	 * Executes the tick method for each network client and the network server.
	 * This method is called periodically by the TickThread.
	 */
	@Override
	public void tick() {
		this.otherNodes.forEach((nodeInformation, fallbackNetworkClient) -> fallbackNetworkClient.tick());
		this.fallbackNetworkServer.tick();
		this.connectionHandler.tick();
	}

	/**
	 * Starts the BackUpClusterNodeBackUp, initiating the server connection and connecting to other nodes.
	 */
	@Override
	public void start() {
		Thread serverThread = new Thread(
			 this.fallbackNetworkServer::connect,
			 "network-server-%s".formatted(this.getOwnNode().getId())
		);
		serverThread.start();

		// Connect to other nodes using the executor service
		this.otherNodes.forEach((nodeInformation, fallbackNetworkClient) -> {
			executorService.submit(fallbackNetworkClient::connect);
		});
	}

	/**
	 * Returns the address of the current node.
	 *
	 * @return The own node's address.
	 */
	@Override
	public MemberIdentifier getOwnNode() {
		return ownNode;
	}

	/**
	 * Returns a map of other nodes and their corresponding network clients.
	 *
	 * @return A map of other nodes.
	 */
	@Override
	public Map<MemberIdentifier, FallbackFallbackNetworkClient> getOtherNodes() {
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
	public FallbackFallbackNetworkServer getNetworkServer() {
		return fallbackNetworkServer;
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