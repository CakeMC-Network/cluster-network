package net.cakemc.library.cluster.fallback.endpoint

import io.netty.channel.epoll.Epoll
import io.netty.channel.kqueue.KQueue
import net.cakemc.library.cluster.api.MemberIdentifier
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.tick.TickAble

/**
 * Represents a network endpoint in the cluster that can establish connections and
 * communicate with other nodes.
 *
 *
 * The `FallbackNetworkPoint` class serves as an abstract base for various network
 * implementations, such as client and server endpoints. It manages connection
 * parameters and provides methods for initializing, connecting, and shutting down
 * the network point.
 *
 *
 * This class also includes support for different channel types, specifically
 * Epoll and KQueue, which are available depending on the operating system.
 *
 * @see AbstractBackUpEndpoint
 *
 * @see MemberIdentifier
 *
 * @see Publication
 *
 * @see TickAble
 */
abstract class FallbackNetworkPoint : TickAble {
    /**
     * The cluster node associated with this network point.
     */
    protected val clusterNode: AbstractBackUpEndpoint

    /**
     * Returns the host address of this network point.
     *
     * @return the host address as a `String`
     */
    /**
     * The host address of this network point.
     */
    val host: String

    /**
     * Returns the port number of this network point.
     *
     * @return the port number as an `int`
     */
    /**
     * The port number of this network point.
     */
    val port: Int

    /**
     * Checks if this network point is currently connected.
     *
     * @return `true` if connected, `false` otherwise
     */
    /**
     * Indicates whether this network point is currently connected.
     */
    var isConnected: Boolean = false
        protected set

    /**
     * Constructs a new `FallbackNetworkPoint` with the specified cluster node.
     *
     * @param clusterNode the [AbstractBackUpEndpoint] representing the cluster node
     */
    constructor(clusterNode: AbstractBackUpEndpoint) {
        this.clusterNode = clusterNode
        this.host = clusterNode.ownNode.address.address!!.hostName
        this.port = clusterNode.ownNode.address.port
    }

    /**
     * Constructs a new `FallbackNetworkPoint` with the specified cluster node, host,
     * and port.
     *
     * @param clusterNode the [AbstractBackUpEndpoint] representing the cluster node
     * @param host the host address of the network point
     * @param port the port number of the network point
     */
    constructor(clusterNode: AbstractBackUpEndpoint, host: String, port: Int) {
        this.clusterNode = clusterNode
        this.host = host
        this.port = port
    }

    /**
     * Constructs a new `FallbackNetworkPoint` using the specified cluster node and
     * node information.
     *
     * @param clusterNode the [AbstractBackUpEndpoint] representing the cluster node
     * @param nodeInformation the [MemberIdentifier] containing address details
     */
    constructor(clusterNode: AbstractBackUpEndpoint, nodeInformation: MemberIdentifier) {
        this.clusterNode = clusterNode
        this.host = nodeInformation.address.address!!.hostName
        this.port = nodeInformation.address.port
    }

    /**
     * Initializes the network point, setting up necessary resources.
     */
    abstract fun initialize()

    /**
     * Establishes a connection to the specified network point.
     */
    abstract fun connect()

    /**
     * Shuts down the network point, releasing any resources and connections.
     */
    abstract fun shutdown()

    /**
     * Dispatches a packet to the appropriate handler.
     *
     *
     * This method can be overridden to implement custom packet dispatching logic.
     *
     * @param packet the [Publication] to be dispatched
     */
    open fun dispatchPacket(packet: Publication?) {}

    companion object {
        /**
         * The name of the packet codec handler.
         */
        val PACKET_CODEC: String = "packet_codec"

        /**
         * The name of the compression codec handler.
         */
        val COMPRESSION_CODEC: String = "compression_codec"

        /**
         * The name of the boss handler for managing acceptor threads.
         */
        val BOSS_HANDLER: String = "boss_handler"

        /**
         * Indicates whether Epoll is available for use.
         */
        val EPOLL: Boolean = Epoll.isAvailable()

        /**
         * Indicates whether KQueue is available for use.
         */
        val KQUEUE: Boolean = KQueue.isAvailable()
    }
}
