package net.cakemc.library.cluster

import net.cakemc.library.cluster.ClusterMember.MemberState
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.config.SyncConfig
import net.cakemc.library.cluster.handler.SyncNetworkHandler

/**
 * Abstract base class representing the context of the cluster framework.
 *
 *
 * The Context class defines the essential operations required to manage cluster synchronization,
 * including creating network handlers, retrieving clusterMember information, and handling node awareness.
 *
 *
 * Concrete subclasses should provide implementations for these methods to define
 * specific behaviors for different types of clusters.
 */
abstract class Context {
  /**
   * Creates a default synchronization network handler.
   *
   * @return a SyncNetworkHandler instance for handling network synchronization
   */
  abstract fun make(): SyncNetworkHandler

  /**
   * Creates a synchronization network handler for the specified SynchronisationType.
   *
   * @param type the SynchronisationType to be used in the network handler
   * @return a SyncNetworkHandler instance configured for the given SynchronisationType
   */
  abstract fun make(type: SynchronisationType?): SyncNetworkHandler

  /**
   * Retrieves the nodes aware of the specified key and version.
   *
   * @param key the unique key for the data or resource
   * @param version the version number of the resource
   * @return an array of node IDs aware of the key and version
   */
  abstract fun getAwareNodesRaw(key: String?, version: Long): ShortArray?

  /**
   * Retrieves the nodes aware of the specified key and version.
   *
   * @param key the unique key for the data or resource
   * @param version the version number of the resource
   * @return an array of node IDs aware of the key and version
   */
  abstract fun getAwareNodes(key: String?, version: Long): ClusterIdRegistry

  /**
   * Adds the specified nodes as aware of the given key and version.
   *
   * @param key the unique key for the data or resource
   * @param version the version number of the resource
   * @param awareNodes an array of node IDs to be marked as aware of the key and version
   */
  abstract fun addAwareNodesRaw(key: String?, version: Long, awareNodes: ShortArray)

  /**
   * Adds the specified nodes as aware of the given key and version.
   *
   * @param key the unique key for the data or resource
   * @param version the version number of the resource
   * @param awareNodes an array of node IDs to be marked as aware of the key and version
   */
  abstract fun addAwareNodes(key: String?, version: Long, awareNodes: ClusterIdRegistry)

  /**
   * Retrieves the clusterMember information based on the node ID.
   *
   * @param id the unique ID of the clusterMember
   * @return the ClusterMember instance corresponding to the given ID, or null if not found
   */
  abstract fun getMemberById(id: Short): ClusterMember?

  /**
   * Updates the information of the specified clusterMember in the cluster.
   *
   * @param clusterMember the ClusterMember instance to be updated
   */
  abstract fun updateMember(clusterMember: ClusterMember?)

  /**
   * Synchronizes the specified node with the cluster.
   *
   * @param node the ClusterMember instance representing the node to be synchronized
   * @return true if the synchronization was successful; false otherwise
   */
  abstract fun syncCluster(node: ClusterMember?): Boolean

  /**
   * Synchronizes the specified node with the cluster, using the given SynchronisationType.
   *
   * @param node the ClusterMember instance representing the node to be synchronized
   * @param withType the SynchronisationType to be used for synchronization
   * @return true if the synchronization was successful; false otherwise
   */
  abstract fun syncCluster(node: ClusterMember?, withType: SynchronisationType?): Boolean

  /**
   * Checks whether the cluster is fully synchronized for the specified key.
   *
   * @param key the unique key representing the resource
   * @return true if the cluster is fully synchronized for the key; false otherwise
   */
  abstract fun isFullySynced(key: String?): Boolean

  /**
   * Checks whether the cluster has reached quorum synchronization for the specified key.
   *
   * @param key the unique key representing the resource
   * @return true if the quorum synchronization is achieved for the key; false otherwise
   */
  abstract fun isQuorumSynced(key: String?): Boolean

  /**
   * Retrieves a snapshot of the current cluster state.
   *
   * @return a ClusterSnapshot object representing the cluster's current state
   */
  abstract val snapshot: ClusterSnapshot?

  /**
   * Changes the state of the specified node in a synchronized manner.
   *
   * @param node the ClusterMember instance whose state is to be changed
   * @param state the new state to be set for the clusterMember
   */
  abstract fun synchronizedStateChange(node: ClusterMember, state: MemberState?)

  /**
   * Retrieves the information about the current node.
   *
   * @return a ClusterMember instance representing the current node's information
   */
  abstract val ownInfo: ClusterMember?

  /**
   * Resets the key of the node with the given ID.
   *
   * @param id the unique ID of the node
   * @param key the new key to be set for the node
   * @return true if the key was successfully reset; false otherwise
   */
  abstract fun resetNodeKeyById(id: Short, key: String?): Boolean

  /**
   * Checks whether the cluster is in the startup phase.
   *
   * @return true if the cluster is in startup; false otherwise
   */
  /**
   * Sets the startup state of the cluster.
   *
   * @param inStartup true to set the cluster in startup mode; false to mark it as out of startup
   */
  abstract var isInStartup: Boolean

  /**
   * Retrieves the timestamp of the last modification in the cluster.
   *
   * @return the last modified timestamp of the cluster
   */
  abstract val clusterLastModified: Long

  /**
   * Sets a virtual last modified timestamp for the cluster.
   *
   * @param virtualLastModified the virtual last modified timestamp to be set
   */
  abstract fun setVirtualLastModified(virtualLastModified: Long)

  /**
   * Retrieves the synchronization configuration for the cluster.
   *
   * @return a SyncConfig object containing synchronization settings
   */
  abstract val config: SyncConfig

  /**
   * @return a ClusterSnapshot that gets created if the current value is null
   */
  abstract fun getOrCreateSnapshot(): ClusterSnapshot
}
