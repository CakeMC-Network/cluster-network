package net.cakemc.library.cluster.network

import net.cakemc.library.cluster.codec.DefaultSyncPublication

/**
 * Abstract base class for network clients in the cluster communication framework.
 *
 *
 * The AbstractNetworkClient class defines the fundamental operations for setting up
 * the next client, publishing synchronization messages, and connecting to a remote host.
 *
 *
 * Concrete subclasses should provide implementations for these methods to define
 * specific behaviors for different types of network clients.
 */
abstract class AbstractNetworkClient {
    /**
     * Sets up the next client in the network communication flow.
     *
     * @throws Exception if an error occurs during the setup process
     */
    @Throws(Exception::class)
    abstract fun setupNextClient()

    /**
     * Publishes a synchronization publication to the network.
     *
     * @param publication the DefaultSyncPublication object to be published
     */
    abstract fun publish(publication: DefaultSyncPublication?)

    abstract fun prepare()

    /**
     * Connects to a specified host and port, and publishes an initial synchronization
     * publication.
     *
     * @param host the hostname or IP address of the remote host
     * @param port the port number to connect to
     * @param publication the DefaultSyncPublication to be sent upon connection
     */
    abstract fun connect(host: String?, port: Int, publication: DefaultSyncPublication?)

    /**
     * Checks if all members have been tried for connection attempts.
     *
     * @return true if all members have been tried; false otherwise
     */
    abstract val isAllTried: Boolean

    /**
     * Retrieves the clusterMember ID associated with this network client.
     *
     * @return the clusterMember ID of this client
     */
    abstract val memberId: Short
}
