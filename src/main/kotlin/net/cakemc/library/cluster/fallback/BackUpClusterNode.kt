package net.cakemc.library.cluster.fallback

import net.cakemc.library.cluster.api.MemberIdentifier
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.config.Snowflake
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkClient
import net.cakemc.library.cluster.fallback.endpoint.FallbackFallbackNetworkServer
import net.cakemc.library.cluster.fallback.endpoint.connection.AbstractConnectionManager
import net.cakemc.library.cluster.fallback.endpoint.connection.ConnectionManager
import net.cakemc.library.cluster.fallback.endpoint.handler.DummyConnectionHandler
import net.cakemc.library.cluster.tick.TickAble
import net.cakemc.library.cluster.tick.TickThread
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Represents a cluster node in a distributed system.
 * This class handles the initialization and management of the node's connections,
 * packet handling, and periodic tasks.
 *
 *
 * The BackUpClusterNodeBackUp serves as the main entry point for the node's functionalities
 * including connecting to other nodes, managing network interactions,
 * and processing incoming and outgoing backPackets.
 */
class BackUpClusterNode(
    /** The address of the current node.  */
    override val ownNode: MemberIdentifier, otherNodes: List<MemberIdentifier>,
    /** The cluster key used for identification within the cluster.  */
    val clusterKey: String?
) :
    AbstractBackUpEndpoint(), TickAble {
    /** Thread pool for executing tasks asynchronously.  */

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    /**
     * Returns the unique identifier generator for the node.
     *
     * @return The snowflake instance.
     */
    /** Unique identifier generator for the node.  */
    override val snowflake: Snowflake = Snowflake()

    /**
     * Returns the cluster key used for identification within the cluster.
     *
     * @return The cluster key.
     */

    /**
     * Returns the address of the current node.
     *
     * @return The own node's address.
     */

    /** A map storing other nodes and their corresponding network clients.  */
    override val otherNodes: MutableMap<MemberIdentifier?, FallbackFallbackNetworkClient?> = HashMap()

    /**
     * Returns the network server instance managing incoming connections.
     *
     * @return The network server.
     */
    /** The server responsible for handling incoming network connections.  */
    val networkServer: FallbackFallbackNetworkServer

    /**
     * Returns the connection manager responsible for handling network connections.
     *
     * @return The connection manager.
     */

    /** Manager responsible for handling network connections.  */
    override val connectionManager: AbstractConnectionManager = ConnectionManager(this)

    /** Handler for managing connections and backPackets.  */
    private val connectionHandler = DummyConnectionHandler(this)

    /**
     * Returns the TickThread responsible for managing periodic tasks.
     *
     * @return The tick thread instance.
     */
    /** Thread responsible for periodic ticking tasks.  */
    val taskScheduler: TickThread

    /**
     * Returns the unique network identifier for the node.
     *
     * @return The network identifier.
     */

    /** Unique network identifier for the node.  */
    override val networkId: Long = snowflake.nextId()

    /**
     * Constructs a new BackUpClusterNodeBackUp with the specified parameters.
     *
     * @param ownNode     The address of the current node.
     * @param otherNodes  A list of other nodes to connect to.
     * @param clusterKey  The key for the cluster.
     */
    init {
        connectionManager.registerPacketHandler(connectionHandler)

        // Initialize connections to other nodes
        for (otherNode in otherNodes) {
            if (otherNode.id == ownNode.id) continue

            val fallbackNetworkClient = FallbackFallbackNetworkClient(this, otherNode)
            fallbackNetworkClient.initialize()

            this.otherNodes.put(otherNode, fallbackNetworkClient)
        }

        this.taskScheduler = TickThread(this)

        this.networkServer = FallbackFallbackNetworkServer(this)
        networkServer.initialize()
    }

    /**
     * Dispatches a packet to the ring for processing.
     *
     * @param packet The packet to be dispatched.
     */
    override fun dispatchPacketToRing(packet: Publication?) {
        connectionHandler.dispatchPacketToRing(packet)
    }

    /**
     * Executes the tick method for each network client and the network server.
     * This method is called periodically by the TickThread.
     */
    override fun tick() {
        otherNodes.forEach { (_: MemberIdentifier?,
                               fallbackNetworkClient: FallbackFallbackNetworkClient?) -> fallbackNetworkClient?.tick() }
        networkServer.tick()
        connectionHandler.tick()
    }

    /**
     * Starts the BackUpClusterNodeBackUp, initiating the server connection and connecting to other nodes.
     */
    override fun start() {
        val serverThread = Thread(
            { networkServer.connect() },
            "network-server-${ownNode.id}"
        )
        serverThread.start()

        // Connect to other nodes using the executor service
        otherNodes.forEach { (nodeInformation: MemberIdentifier?, fallbackNetworkClient: FallbackFallbackNetworkClient?) ->
            executorService.submit { fallbackNetworkClient?.connect() }
        }
    }

}
