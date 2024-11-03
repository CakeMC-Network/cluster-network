package net.cakemc.library.cluster

import net.cakemc.library.cluster.ClusterMember.MemberState
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.cache.*
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.config.SyncConfig
import net.cakemc.library.cluster.handler.*
import java.util.*
import kotlin.concurrent.Volatile

/**
 * Context for managing synchronization within a cluster.
 *
 *
 * This class is responsible for handling the synchronization of members in a cluster,
 * managing clusterMember states, and providing functionality for message storage and retrieval.
 */
class SyncContext @JvmOverloads constructor(// The identifier for this clusterMember
  val ownId: Short,
  /**
   * Retrieves the synchronization configuration.
   *
   * @return the SyncConfig object
   */
  override val config: SyncConfig = SyncConfig // Configuration for synchronization
    ()
) : Context() {
  /**
   * Checks if the context is in the startup phase.
   *
   * @return true if in startup, false otherwise
   */
  override var isInStartup: Boolean = true // Flag indicating if the context is in startup phase

  private val clusterStore: ClusterStore = DefaultMemberStore() // Store for cluster members
  private val messageStore: PublicationStore = DefaultPublicationStore() // Store for publication messages

  /**
   * Retrieves the last modification timestamp of the cluster.
   *
   * @return the last modified timestamp
   */
  @Volatile
  override var clusterLastModified: Long = System.currentTimeMillis()
  // Timestamp for last modification

  @Volatile
  override var snapshot: ClusterSnapshot? = null;
  // Snapshot of the cluster's current state

  /**
   * Constructs a new SyncContext with the given clusterMember ID and configuration.
   *
   * @param ownId the ID of the clusterMember
   */
  init {
    var clusterMember = getMemberById(ownId)
    if (clusterMember == null) {
      val awareIds = ShortArray(0)
      clusterMember = ClusterMember(
        ownId, null, true, "",
        System.currentTimeMillis(),
        awareIds, MemberState.VALID
      )
      updateMember(clusterMember)
    }
  }

  /**
   * Creates a new SyncNetworkHandler for synchronization.
   *
   * @return a new instance of SyncNetworkHandler
   */
  override fun make(): SyncNetworkHandler {
    val sync = SyncNetworkHandler(this)
    sync.selfMember = this.ownInfo
    return sync
  }

  /**
   * Creates a new SyncNetworkHandler with a specified synchronization type.
   *
   * @param type the type of synchronization to perform
   * @return a new instance of SyncNetworkHandler
   */
  override fun make(type: SynchronisationType?): SyncNetworkHandler {
    val s = SyncNetworkHandler(this, type)
    isInStartup = false
    return s
  }

  /**
   * Retrieves aware nodes for a specific message key and version.
   *
   * @param key the message key
   * @param version the version number
   * @return an array of aware node identifiers
   */
  override fun getAwareNodesRaw(key: String?, version: Long): ShortArray? {
    return messageStore.getAwareNodes(key, version)
  }

  /**
   * Retrieves aware nodes for a specific message key and version.
   *
   * @param key the message key
   * @param version the version number
   * @return an array of aware node identifiers
   */
  override fun getAwareNodes(key: String?, version: Long): ClusterIdRegistry {
    return ClusterIdRegistry(*messageStore.getAwareNodes(key, version)!!)
  }

  /**
   * Updates the aware nodes for a specific message key and version.
   *
   * @param key the message key
   * @param version the version number
   * @param awareNodes an array of aware node identifiers
   */
  override fun addAwareNodesRaw(key: String?, version: Long, awareNodes: ShortArray) {
    messageStore.updateAwareNodes(key, version, awareNodes)
  }

  override fun addAwareNodes(key: String?, version: Long, awareNodes: ClusterIdRegistry) {
    messageStore.updateAwareNodes(key, version, awareNodes.ids)
  }

  /**
   * Retrieves a clusterMember by its identifier.
   *
   * @param id the identifier of the clusterMember
   * @return the ClusterMember object, or null if not found
   */
  override fun getMemberById(id: Short): ClusterMember? {
    return clusterStore.getClusterMember(id)
  }

  /**
   * Updates the state of a clusterMember in the cluster.
   *
   * @param clusterMember the ClusterMember object to update
   */
  override fun updateMember(clusterMember: ClusterMember?) {
    if (clusterMember == null) {
      return
    }
    clusterMember.addAwareId(ownId)

    if (!clusterMember.awareIds!!.contains(clusterMember.id)) {
      val mem = clusterStore.getClusterMember(clusterMember.id)
      if (mem != null && mem.key != clusterMember.key) {
        clusterMember.addKeyChain(mem.keyChain)
      }
    }
    clusterLastModified = System.currentTimeMillis()
    clusterStore.updateClusterMember(clusterMember)
    invalidateSnapshot()
  }

  /**
   * Synchronizes the cluster with a specific clusterMember.
   *
   * @param node the clusterMember to synchronize with
   * @return true if synchronization was successful, false otherwise
   */
  override fun syncCluster(node: ClusterMember?): Boolean {
    return syncCluster(node, SynchronisationType.UNI_CAST_BALANCE)
  }

  /**
   * Synchronizes the cluster with a specific clusterMember using a specified synchronization type.
   *
   * @param node the clusterMember to synchronize with
   * @param withType the type of synchronization to use
   * @return true if synchronization was successful, false otherwise
   */
  override fun syncCluster(node: ClusterMember?, withType: SynchronisationType?): Boolean {
    if (node == null) {
      return false
    }

    updateMember(node)
    val handler = make(withType)
      .withoutCluster(ownId)
      .withCallBack(ClusterPublicationHandler(this))

    handler.mode = DefaultSyncPublication.SyncMode.SYNC_CLUSTER
    val messages: MutableList<Publication> = ArrayList()
    val snapshot = getOrCreateSnapshot()

    for (n in snapshot.cluster!!) {
      if (n == null)
        continue

      val msg = ClusterPublication(
        n.id,
        n.isAuthByKey, n.key,
        n.lastModified, n.syncAddresses,
        if (n.isValid) DefaultSyncPublication.Command.COMMAND_TAKE_THIS
        else DefaultSyncPublication.Command.COMMAND_DEL_THIS
      )
      messages.add(msg)
    }

    val feature = handler.sync(messages).get()
    if (feature == null) {
      if (node.id == ownId) {
        isInStartup = false
        return true
      }
      return false
    }
    if (feature[node.id.toString()]!!.isSuccessful) {
      isInStartup = false
      return true
    }
    return false
  }

  /**
   * Checks if the cluster is fully synchronized for a specific key.
   *
   * @param key the key to check synchronization status
   * @return true if fully synchronized, false otherwise
   */
  override fun isFullySynced(key: String?): Boolean {
    return false // Placeholder for actual implementation
  }

  /**
   * Checks if a quorum is synchronized for a specific key.
   *
   * @param key the key to check synchronization status
   * @return true if quorum is synchronized, false otherwise
   */
  override fun isQuorumSynced(key: String?): Boolean {
    return false // Placeholder for actual implementation
  }

  /**
   * Retrieves the current snapshot of the cluster.
   *
   * @return the current ClusterSnapshot
   */
  override fun getOrCreateSnapshot(): ClusterSnapshot {
    if (snapshot != null) {
      return snapshot as ClusterSnapshot
    }

    val tmpMonitor = ClusterSnapshot()
    clusterStore.forAll(object: ClusterMemberCallback {
      override fun next(node: ClusterMember?) {
        if (node == null)
          return

        if (node.isValid) {
          tmpMonitor.validClusterIDs.add(node.id)
          tmpMonitor.validCluster.add(node)
          tmpMonitor.aliveCluster.add(node)
        } else if (!node.isDown) {
          tmpMonitor.aliveCluster.add(node)
          if (node.id != ownId) {
            tmpMonitor.inValidClusterIDs.add(node.id)
          }
        } else if (node.id != ownId) {
          tmpMonitor.inValidClusterIDs.add(node.id)
        }
        tmpMonitor.cluster!!.add(node)
        tmpMonitor.idClusterMap.put(node.id, node)
      }

    })

    return tmpMonitor.also { snapshot = it }
  }

  /**
   * Invalidates the current snapshot of the cluster.
   */
  private fun invalidateSnapshot() {
    snapshot = null
  }

  /**
   * Changes the state of a clusterMember and updates the cluster snapshot.
   *
   * @param node the clusterMember whose state is changing
   * @param state the new state of the clusterMember
   */
  override fun synchronizedStateChange(node: ClusterMember, state: MemberState?) {
    node.state = state
    invalidateSnapshot()
    clusterLastModified = System.currentTimeMillis()
  }

  override val ownInfo: ClusterMember?
    /**
     * Retrieves information about this clusterMember.
     *
     * @return the ClusterMember object representing this clusterMember
     */
    get() = clusterStore.getClusterMember(ownId)

  /**
   * Resets the key for a clusterMember identified by its ID.
   *
   * @param id the ID of the clusterMember
   * @param key the new key to set
   * @return true if the key was successfully reset, false otherwise
   */
  override fun resetNodeKeyById(id: Short, key: String?): Boolean {
    val node = getMemberById(id)
    if (node != null && key != null) {
      check(node.isValid) { "node is invalid state" }

      if (node.key == key) {
        return true
      }
      node.resetKey(key)
      clusterStore.updateClusterMember(node)
    }
    return false
  }

  /**
   * Sets the last modification timestamp for the cluster.
   *
   * @param virtualLastModified the new last modified timestamp
   */
  override fun setVirtualLastModified(virtualLastModified: Long) {
    this.clusterLastModified = virtualLastModified
  }
}
