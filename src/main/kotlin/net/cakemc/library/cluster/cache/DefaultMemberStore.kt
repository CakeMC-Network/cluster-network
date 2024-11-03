package net.cakemc.library.cluster.cache

import net.cakemc.library.cluster.ClusterMember
import java.io.Serializable

/**
 * This class provides a default implementation of the [ClusterStore] interface, using a
 * [HashMap] to store and manage [ClusterMember] objects in a cluster. The `DefaultMemberStore`
 * acts as an in-memory store where each clusterMember is identified by a unique [Serializable] key, typically
 * representing the clusterMember's ID.
 *
 *
 * This class is suitable for systems where cluster membership information needs to be efficiently
 * updated, retrieved, and iterated over. It provides core functionality for managing cluster nodes,
 * including adding/updating nodes, retrieving a node by its ID, iterating over all nodes, and
 * closing the store by clearing the cache.
 *
 *
 * The store can be easily extended or adapted for more complex storage mechanisms if needed.
 *
 * @see ClusterMember
 *
 * @see ClusterStore
 *
 * @see ClusterMemberCallback
 */
class DefaultMemberStore : ClusterStore {
    /**
     * The internal cache used to store the [ClusterMember] objects. Each clusterMember is keyed by its unique
     * `Serializable` ID, which is retrieved from the clusterMember using [ClusterMember.getId].
     */
    private val cache: MutableMap<Serializable, ClusterMember> =
        HashMap()

    /**
     * Adds or updates a [ClusterMember] in the store. If the clusterMember with the same ID already exists, it will
     * be replaced with the new one. Otherwise, the clusterMember is added to the store.
     *
     * @param node the [ClusterMember] object to be added or updated. The `ClusterMember`'s ID, retrieved by
     * [ClusterMember.getId], is used as the key in the cache. This parameter cannot be null.
     *
     *
     * Usage example:
     * <pre>
     * ClusterMember node = new ClusterMember(...);
     * memberStore.updateClusterMember(node);
    </pre> *
     */
    override fun updateClusterMember(node: ClusterMember) {
        cache.put(node.id, node)
    }

    /**
     * Retrieves a [ClusterMember] from the store using its unique ID.
     *
     * @param id the unique identifier of the [ClusterMember], typically as a `short`. This ID is used
     * to look up the clusterMember in the cache.
     * @return the [ClusterMember] associated with the given ID, or `null` if no clusterMember exists with
     * the specified ID.
     *
     *
     * Usage example:
     * <pre>
     * ClusterMember clusterMember = memberStore.getClusterMember(5);
    </pre> *
     */
    override fun getClusterMember(id: Short): ClusterMember? {
        return cache[id]
    }

    /**
     * Clears the store by removing all [ClusterMember] objects from the cache.
     *
     *
     * This method should be called to release resources and prepare the store for shutdown. Once this
     * method is called, the store is effectively empty, and any subsequent lookups will return `null`.
     *
     *
     * Usage example:
     * <pre>
     * memberStore.close();
    </pre> *
     */
    override fun close() {
        cache.clear()
    }

    /**
     * Iterates over all [ClusterMember] objects in the store and applies the specified callback to each.
     * The `ClusterMemberCallback` is executed sequentially for each clusterMember in the store.
     *
     * @param callback the [ClusterMemberCallback] that is called for each [ClusterMember] in the store.
     * This callback cannot be null.
     *
     *
     * Usage example:
     * <pre>
     * memberStore.forAll(clusterMember -> {
     * // Custom action for each clusterMember
     * System.out.println(clusterMember);
     * });
    </pre> *
     */
    override fun forAll(callback: ClusterMemberCallback) {
        for (clusterMember in cache.values) {
            callback.next(clusterMember)
        }
    }

    val allMembers: Collection<ClusterMember>
        /**
         * Retrieves a [Collection] of all [ClusterMember] objects currently stored in the cache.
         * This method allows direct access to the underlying members without requiring iteration through
         * a callback.
         *
         * @return a [Collection] of [ClusterMember] objects, or an empty collection if no members are present.
         *
         *
         * Usage example:
         * <pre>
         * Collection<ClusterMember> allMembers = memberStore.getAllMembers();
        </ClusterMember></pre> *
         */
        get() = cache.values
}
