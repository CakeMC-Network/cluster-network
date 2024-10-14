package net.cakemc.library.cluster.network;

import net.cakemc.library.cluster.Context;
import net.cakemc.library.cluster.ClusterMember;
import net.cakemc.library.cluster.address.ClusterAddress;

import java.io.IOException;
import java.util.Set;

/**
 * Abstract base class for network servers in the cluster communication framework.
 *
 * <p>The AbstractNetworkServer class defines the fundamental operations for starting
 * the server and handling cluster synchronization.</p>
 *
 * <p>Concrete subclasses should provide implementations for these methods to define
 * specific behaviors for different types of network servers.</p>
 */
public abstract class AbstractNetworkServer {

	public abstract void prepare();

	/**
	 * Starts the network server, allowing it to accept incoming connections.
	 *
	 * @throws IOException if an error occurs during the startup process
	 */
	public abstract void start() throws IOException;

	/**
	 * Initiates the cluster synchronization process using the provided context.
	 *
	 * @param context the Context object containing the necessary information for synchronization
	 */
	public abstract void startClusterSyncing(Context context);

	/**
	 * Prepares the synchronization addresses for the cluster clusterMember.
	 *
	 * @param myAddrzForSynch a set of ClusterAddress instances representing addresses for synchronization
	 * @param me the ClusterMember instance representing this server's clusterMember information
	 * @param lastModified the last modified timestamp of this clusterMember
	 * @return true if the synchronization addresses were successfully prepared; false otherwise
	 */
	public abstract boolean prepareSyncAddresses(Set<ClusterAddress> myAddrzForSynch, ClusterMember me, long lastModified);

	public abstract void stop();
}
