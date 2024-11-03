package net.cakemc.library.cluster

import net.cakemc.library.cluster.address.ClusterIdRegistry

abstract class Snapshot {
    abstract fun getById(id: Short, memberCheck: Int): ClusterMember?

    abstract val validClusterIDs: ClusterIdRegistry?

    abstract val inValidClusterIDs: ClusterIdRegistry?

    abstract val validCluster: List<ClusterMember?>

    abstract val aliveCluster: List<ClusterMember?>

    abstract val cluster: List<ClusterMember?>?
}
