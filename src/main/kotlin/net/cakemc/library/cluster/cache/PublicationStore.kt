package net.cakemc.library.cluster.cache

/**
 * This interface defines a storage mechanism for managing node awareness within a distributed system.
 * The `PublicationStore` tracks which nodes are aware of a specific publication, allowing for
 * efficient synchronization and state management across the cluster. Each publication is identified
 * by a unique `key` and a `version`, and the store maintains a record of nodes that are
 * aware of the publication.
 *
 *
 * This interface provides methods to:
 *
 *  * Update the list of nodes that are aware of a publication
 *  * Retrieve the list of aware nodes for a given publication version
 *  * Gracefully shut down the store and release any resources
 *
 *
 *
 * Implementing classes should provide mechanisms for efficient and scalable management of
 * publication awareness, which is useful in scenarios like cache invalidation, replication,
 * or event-driven systems.
 */
interface PublicationStore {
    /**
     * Updates the list of nodes that are aware of a given publication version. This method is used
     * to record which nodes in the cluster are aware of the specified publication, identified by
     * its `key` and `version`.
     *
     * @param key the unique identifier for the publication. This is typically a string that uniquely
     * represents the resource or event being published across the cluster.
     * @param version the version of the publication. This long value distinguishes different versions
     * of the same publication, enabling version-aware synchronization across nodes.
     * @param awareNodes an array of `short` values representing the nodes that are aware of the
     * publication at the given version. Each element in the array is the ID of a node.
     *
     *
     * Usage example:
     * <pre>
     * short[] nodes = {1, 2, 3};
     * publicationStore.updateAwareNodes("resourceKey", 12345L, nodes);
    </pre> *
     */
    fun updateAwareNodes(key: String?, version: Long, awareNodes: ShortArray)

    /**
     * Retrieves the list of nodes that are aware of a specific version of a publication. This method
     * returns an array of `short` values, each representing a node ID that is aware of the
     * publication identified by the given `key` and `version`.
     *
     * @param key the unique identifier for the publication. This string identifies the resource or event
     * for which node awareness is being tracked.
     * @param version the version of the publication. This long value corresponds to the version of the
     * publication whose aware nodes are being retrieved.
     * @return an array of `short` values representing the IDs of nodes that are aware of the
     * specified publication version, or `null` if no nodes are found.
     *
     *
     * Usage example:
     * <pre>
     * short[] awareNodes = publicationStore.getAwareNodes("resourceKey", 12345L);
    </pre> *
     */
    fun getAwareNodes(key: String?, version: Long): ShortArray?

    /**
     * Shuts down the publication store and releases any associated resources. This method is typically
     * called when the store is no longer needed, or when the system is being gracefully shut down.
     * Implementations should ensure that any in-progress operations are completed and that all resources
     * (e.g., caches, threads, connections) are properly released.
     *
     *
     * Usage example:
     * <pre>
     * publicationStore.shutdown();
    </pre> *
     */
    fun shutdown()
}
