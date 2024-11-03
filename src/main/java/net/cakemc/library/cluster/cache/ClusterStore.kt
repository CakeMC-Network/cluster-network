package net.cakemc.library.cluster.cache

import net.cakemc.library.cluster.ClusterMember

/**
 * This interface defines a storage mechanism for managing cluster members within a distributed system.
 * The `ClusterStore` acts as a central repository where cluster members can be updated, retrieved,
 * or iterated over. It provides methods for storing and accessing [ClusterMember] objects, facilitating
 * cluster-wide operations such as load balancing, failover, and node management.
 *
 *
 * This interface is expected to be implemented by classes that handle the persistence or in-memory
 * storage of cluster members, enabling efficient and scalable cluster management.
 *
 *
 * Key operations include:
 *
 *  * Updating a cluster clusterMember's information
 *  * Retrieving a cluster clusterMember by its unique identifier
 *  * Executing a callback on all members in the store
 *  * Gracefully closing the store and releasing resources
 *
 *
 * @see ClusterMember
 *
 * @see ClusterMemberCallback
 */
interface ClusterStore {
    /**
     * Updates the information of an existing cluster clusterMember or adds a new clusterMember to the cluster store.
     * If the `ClusterMember` already exists, its information is updated; otherwise, it is added to the store.
     *
     * @param node the [ClusterMember] object representing the cluster clusterMember to be updated. This parameter
     * cannot be null and must contain valid clusterMember data.
     *
     *
     * Usage example:
     * <pre>
     * clusterStore.updateClusterMember(existingNode);
    </pre> *
     */
    fun updateClusterMember(node: ClusterMember)

    /**
     * Retrieves a [ClusterMember] from the cluster store based on its unique identifier.
     *
     * @param id the unique identifier (as a `short`) of the cluster clusterMember to be retrieved.
     * The ID corresponds to the clusterMember's position or role in the cluster.
     * @return the [ClusterMember] associated with the given ID, or `null` if no such clusterMember exists.
     *
     *
     * Usage example:
     * <pre>
     * ClusterMember clusterMember = clusterStore.getClusterMember(memberId);
    </pre> *
     */
    fun getClusterMember(id: Short): ClusterMember?

    /**
     * Closes the cluster store and releases any associated resources. This method is typically called
     * when the cluster store is no longer needed, or the system is shutting down. Implementations should
     * ensure that all resources (e.g., connections, threads, caches) are properly cleaned up.
     *
     *
     * Usage example:
     * <pre>
     * clusterStore.close();
    </pre> *
     */
    fun close()

    /**
     * Iterates over all cluster members stored in the cluster store and applies the specified callback
     * to each clusterMember. The callback mechanism enables custom actions to be performed on each node,
     * such as updating state, gathering statistics, or performing health checks.
     *
     * @param callback the [ClusterMemberCallback] to be executed on each [ClusterMember].
     * The callback is called sequentially for each cluster clusterMember in the store.
     *
     *
     * Usage example:
     * <pre>
     * clusterStore.forAll(clusterMember -> {
     * // Custom action for each clusterMember
     * System.out.println(clusterMember);
     * });
    </pre> *
     */
    fun forAll(callback: ClusterMemberCallback)
}
