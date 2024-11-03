package net.cakemc.library.cluster

import net.cakemc.library.cluster.address.ClusterIdRegistry

/**
 * Represents a snapshot of the cluster's state, capturing information about the members
 * and their statuses, including valid, invalid, and alive members.
 *
 *
 * The `ClusterSnapshot` class provides methods to access different views of the cluster,
 * such as the set of valid, invalid, and alive members, as well as a map for quick lookup
 * of members by their ID.
 *
 *
 * Each clusterMember of the cluster is represented by a [ClusterMember] object, and the snapshot
 * allows for filtering members based on different checks, such as whether a clusterMember is valid
 * or marked as down.
 */
class ClusterSnapshot : Snapshot() {
    // Arrays holding the IDs of valid and invalid cluster members
    override var validClusterIDs: ClusterIdRegistry = ClusterIdRegistry()
    override var inValidClusterIDs: ClusterIdRegistry = ClusterIdRegistry()

    // Lists holding the valid, alive, and all members of the cluster
    override var validCluster: MutableList<ClusterMember?> = ArrayList()
    override var aliveCluster: MutableList<ClusterMember?> = ArrayList()
    override var cluster: MutableList<ClusterMember?>? = ArrayList()

    // Map for quick lookup of members by their ID
    var idClusterMap: MutableMap<Short?, ClusterMember?> = HashMap()

    /**
     * Retrieves a [ClusterMember] from the cluster by its ID, applying an optional clusterMember check.
     *
     * @param id the ID of the clusterMember to retrieve
     * @param memberCheck one of the clusterMember check constants to apply (e.g., [.MEMBER_CHECK_VALID])
     * @return the [ClusterMember] object if it exists and passes the check, or null otherwise
     */
    override fun getById(id: Short, memberCheck: Int): ClusterMember? {
        val clusterMember = idClusterMap[id] ?: return null
        if (memberCheck == MEMBER_CHECK_NONE) {
            return clusterMember
        } else if (memberCheck == MEMBER_CHECK_VALID_OR_DOWN) {
            if (clusterMember.isValid || clusterMember.isDown) {
                return clusterMember
            }
        }
        if (clusterMember.isValid) {
            return clusterMember
        }
        return null
    }

    companion object {
        // Constants for different clusterMember validation checks
        const val MEMBER_CHECK_NONE: Int = 0
        const val MEMBER_CHECK_VALID_OR_DOWN: Int = 1
        const val MEMBER_CHECK_VALID: Int = 2
    }
}
