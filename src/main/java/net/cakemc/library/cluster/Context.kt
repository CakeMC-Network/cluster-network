package net.cakemc.library.cluster;

import net.cakemc.library.cluster.ClusterMember.MemberState;
import net.cakemc.library.cluster.config.SyncConfig;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

/**
 * Abstract base class representing the context of the cluster framework.
 *
 * <p>The Context class defines the essential operations required to manage cluster synchronization,
 * including creating network handlers, retrieving clusterMember information, and handling node awareness.</p>
 *
 * <p>Concrete subclasses should provide implementations for these methods to define
 * specific behaviors for different types of clusters.</p>
 */
public abstract class Context {

	/**
	 * Creates a default synchronization network handler.
	 *
	 * @return a SyncNetworkHandler instance for handling network synchronization
	 */
	public abstract SyncNetworkHandler make();

	/**
	 * Creates a synchronization network handler for the specified SynchronisationType.
	 *
	 * @param type the SynchronisationType to be used in the network handler
	 * @return a SyncNetworkHandler instance configured for the given SynchronisationType
	 */
	public abstract SyncNetworkHandler make(SynchronisationType type);

	/**
	 * Retrieves the nodes aware of the specified key and version.
	 *
	 * @param key the unique key for the data or resource
	 * @param version the version number of the resource
	 * @return an array of node IDs aware of the key and version
	 */
	public abstract short[] getAwareNodesRaw(String key, long version);

	/**
	 * Retrieves the nodes aware of the specified key and version.
	 *
	 * @param key the unique key for the data or resource
	 * @param version the version number of the resource
	 * @return an array of node IDs aware of the key and version
	 */
	public abstract ClusterIdRegistry getAwareNodes(String key, long version);

	/**
	 * Adds the specified nodes as aware of the given key and version.
	 *
	 * @param key the unique key for the data or resource
	 * @param version the version number of the resource
	 * @param awareNodes an array of node IDs to be marked as aware of the key and version
	 */
	public abstract void addAwareNodesRaw(String key, long version, short[] awareNodes);

	/**
	 * Adds the specified nodes as aware of the given key and version.
	 *
	 * @param key the unique key for the data or resource
	 * @param version the version number of the resource
	 * @param awareNodes an array of node IDs to be marked as aware of the key and version
	 */
	public abstract void addAwareNodes(String key, long version, ClusterIdRegistry awareNodes);

	/**
	 * Retrieves the clusterMember information based on the node ID.
	 *
	 * @param id the unique ID of the clusterMember
	 * @return the ClusterMember instance corresponding to the given ID, or null if not found
	 */
	public abstract ClusterMember getMemberById(short id);

	/**
	 * Updates the information of the specified clusterMember in the cluster.
	 *
	 * @param clusterMember the ClusterMember instance to be updated
	 */
	public abstract void updateMember(ClusterMember clusterMember);

	/**
	 * Synchronizes the specified node with the cluster.
	 *
	 * @param node the ClusterMember instance representing the node to be synchronized
	 * @return true if the synchronization was successful; false otherwise
	 */
	public abstract boolean syncCluster(ClusterMember node);

	/**
	 * Synchronizes the specified node with the cluster, using the given SynchronisationType.
	 *
	 * @param node the ClusterMember instance representing the node to be synchronized
	 * @param withType the SynchronisationType to be used for synchronization
	 * @return true if the synchronization was successful; false otherwise
	 */
	public abstract boolean syncCluster(ClusterMember node, SynchronisationType withType);

	/**
	 * Checks whether the cluster is fully synchronized for the specified key.
	 *
	 * @param key the unique key representing the resource
	 * @return true if the cluster is fully synchronized for the key; false otherwise
	 */
	public abstract boolean isFullySynced(String key);

	/**
	 * Checks whether the cluster has reached quorum synchronization for the specified key.
	 *
	 * @param key the unique key representing the resource
	 * @return true if the quorum synchronization is achieved for the key; false otherwise
	 */
	public abstract boolean isQuorumSynced(String key);

	/**
	 * Retrieves a snapshot of the current cluster state.
	 *
	 * @return a ClusterSnapshot object representing the cluster's current state
	 */
	public abstract ClusterSnapshot getSnapshot();

	/**
	 * Changes the state of the specified node in a synchronized manner.
	 *
	 * @param node the ClusterMember instance whose state is to be changed
	 * @param state the new state to be set for the clusterMember
	 */
	public abstract void synchronizedStateChange(ClusterMember node, MemberState state);

	/**
	 * Retrieves the information about the current node.
	 *
	 * @return a ClusterMember instance representing the current node's information
	 */
	public abstract ClusterMember getOwnInfo();

	/**
	 * Resets the key of the node with the given ID.
	 *
	 * @param id the unique ID of the node
	 * @param key the new key to be set for the node
	 * @return true if the key was successfully reset; false otherwise
	 */
	public abstract boolean resetNodeKeyById(short id, String key);

	/**
	 * Checks whether the cluster is in the startup phase.
	 *
	 * @return true if the cluster is in startup; false otherwise
	 */
	public abstract boolean isInStartup();

	/**
	 * Retrieves the timestamp of the last modification in the cluster.
	 *
	 * @return the last modified timestamp of the cluster
	 */
	public abstract long getClusterLastModified();

	/**
	 * Sets a virtual last modified timestamp for the cluster.
	 *
	 * @param virtualLastModified the virtual last modified timestamp to be set
	 */
	public abstract void setVirtualLastModified(long virtualLastModified);

	/**
	 * Retrieves the synchronization configuration for the cluster.
	 *
	 * @return a SyncConfig object containing synchronization settings
	 */
	public abstract SyncConfig getConfig();

	/**
	 * Sets the startup state of the cluster.
	 *
	 * @param inStartup true to set the cluster in startup mode; false to mark it as out of startup
	 */
	public abstract void setInStartup(boolean inStartup);
}