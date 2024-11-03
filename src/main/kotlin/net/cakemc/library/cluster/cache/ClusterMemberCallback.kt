package net.cakemc.library.cluster.cache

import net.cakemc.library.cluster.ClusterMember

/**
 * This interface defines a callback mechanism for handling cluster members within a distributed system.
 * It is designed to be implemented by classes that need to perform actions when moving to the next clusterMember
 * in a cluster environment. The callback allows for custom logic when iterating or interacting with
 * cluster nodes, particularly useful in ring or round-robin topologies.
 *
 *
 * Typical use cases include load balancing, task distribution, or cluster management where actions
 * need to be executed when moving between cluster members.
 *
 *
 * The implementing class should provide its own logic in the `next` method to specify
 * how to handle the next [ClusterMember] in the cluster sequence.
 *
 * @see ClusterMember
 */
interface ClusterMemberCallback {
    /**
     * This method is called to handle the next clusterMember in the cluster. Implementing classes should
     * define what actions need to be taken with the provided [ClusterMember] instance.
     *
     * @param node the [ClusterMember] representing the next node in the cluster. This parameter cannot
     * be null, and the method should ensure appropriate handling of the node.
     *
     *
     * Usage example:
     * <pre>
     * clusterMemberCallback.next(nextNode);
    </pre> *
     */
    fun next(node: ClusterMember?)
}
