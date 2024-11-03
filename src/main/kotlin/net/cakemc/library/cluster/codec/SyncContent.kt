package net.cakemc.library.cluster.codec

import net.cakemc.library.cluster.address.ClusterIdRegistry

/**
 * Represents the synchronization content within the cluster framework.
 *
 *
 * The `SyncContent` class encapsulates data necessary for synchronizing
 * information across cluster nodes, including a unique key, version number,
 * aware node identifiers, and the associated content.
 */
class SyncContent(
    /**
     * Retrieves the unique key associated with this sync content.
     *
     * @return the unique key for this sync content
     */
    val key: String?, // Unique identifier for the sync content
    /**
     * Retrieves the version number of this sync content.
     *
     * @return the version number of this sync content
     */
    var version: Long, // Version number of the sync content
    awareIds: ClusterIdRegistry?, content: ByteArray?
) {
    /**
     * Retrieves the array of aware node identifiers.
     *
     * @return an array of aware node identifiers
     */
    val awareIds: ClusterIdRegistry? // Array of aware node identifiers

    /**
     * Retrieves the associated content as a byte array.
     *
     * @return the associated content in byte array format
     */
    val content: ByteArray? // Associated content in byte array format

    /**
     * Constructs a `SyncContent` object with the specified parameters.
     *
     * @param key      the unique key identifying this sync content
     * @param version  the version number of this sync content
     * @param awareIds array of aware node identifiers
     * @param content  the associated content in byte array format
     */
    init {
        this.version = version
        this.awareIds = awareIds
        this.content = content
    }

    val awareIdsRaw: ShortArray?
        /**
         * Retrieves the array of aware node identifiers.
         *
         * @return an array of aware node identifiers
         */
        get() = awareIds!!.ids

    /**
     * Adds a single aware node identifier to the existing array of aware IDs.
     *
     *
     * If the aware IDs array is null, this method does nothing.
     *
     * @param node the aware node identifier to add
     */
    fun addAwareId(node: Short) {
        if (awareIds == null) {
            return
        }
        awareIds.addAll(node)
    }

    /**
     * Adds multiple aware node identifiers to the existing array of aware IDs.
     *
     *
     * If the aware IDs array is null, this method does nothing.
     *
     * @param nodes an array of aware node identifiers to add
     */
    fun addAwareId(nodes: ShortArray) {
        if (awareIds == null) {
            return
        }

        awareIds.addAll(*nodes)
    }

    /**
     * Adds multiple aware node identifiers to the existing array of aware IDs.
     *
     *
     * If the aware IDs array is null, this method does nothing.
     *
     * @param nodes an array of aware node identifiers to add
     */
    fun addAwareId(nodes: ClusterIdRegistry) {
        if (awareIds == null) {
            return
        }

        awareIds.addAll(*nodes.ids)
    }

    override fun hashCode(): Int {
        return key!!.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is SyncContent) {
            if (obj.key == null || key == null) {
                return false
            }
            return obj.key == key
        }
        return false
    }

    override fun toString(): String {
        return "key=" + key +
                ", version=" + version +
                ", awareNodes=" + this.awareIds
    }
}
