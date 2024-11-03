package net.cakemc.library.cluster.network

import net.cakemc.library.cluster.*
import net.cakemc.library.cluster.address.ClusterAddress
import java.io.IOException

/**
 * Abstract base class for network servers in the cluster communication framework.
 *
 *
 * The AbstractNetworkServer class defines the fundamental operations for starting
 * the server and handling cluster synchronization.
 *
 *
 * Concrete subclasses should provide implementations for these methods to define
 * specific behaviors for different types of network servers.
 */
abstract class AbstractNetworkServer {
    abstract fun prepare()

    /**
     * Starts the network server, allowing it to accept incoming connections.
     *
     * @throws IOException if an error occurs during the startup process
     */
    @Throws(IOException::class)
    abstract fun start()

    /**
     * Initiates the cluster synchronization process using the provided context.
     *
     * @param context the Context object containing the necessary information for synchronization
     */
    abstract fun startClusterSyncing(context: Context?)

    /**
     * Prepares the synchronization addresses for the cluster clusterMember.
     *
     * @param myAddrzForSynch a set of ClusterAddress instances representing addresses for synchronization
     * @param me the ClusterMember instance representing this server's clusterMember information
     * @param lastModified the last modified timestamp of this clusterMember
     * @return true if the synchronization addresses were successfully prepared; false otherwise
     */
    abstract fun prepareSyncAddresses(
        myAddrzForSynch: MutableSet<ClusterAddress>,
        me: ClusterMember,
        lastModified: Long
    ): Boolean

    abstract fun stop()
}
