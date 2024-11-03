package net.cakemc.library.cluster.handler

import net.cakemc.library.cluster.address.ClusterIdRegistry

/**
 * Represents the result of a synchronization operation between cluster nodes.
 *
 *
 * The SyncResult class tracks the success or failure of a synchronization message,
 * along with the IDs of members involved in the operation. It allows for the addition
 * and removal of synced and failed members.
 */
class SyncResult {
    /**
     * Checks if the synchronization was successful.
     *
     * @return true if the message was successfully synced, false otherwise
     */
    /**
     * Sets the success status of the synchronization operation.
     *
     * @param successful true if the message was successfully synced, false otherwise
     */
    var isSuccessful: Boolean = false

    /**
     * Retrieves the IDs of nodes that the message failed to sync with.
     *
     * @return an array of IDs of nodes that failed to sync
     */
    var failedMembers: ClusterIdRegistry
        private set

    /**
     * Retrieves the IDs of nodes that the message was successfully synced with.
     *
     * @return an array of IDs of nodes that successfully synced
     */
    var syncedMembers: ClusterIdRegistry
        private set

    /**
     * Constructs a SyncResult with no synced or failed members.
     */
    init {
        this.failedMembers = ClusterIdRegistry()
        this.syncedMembers = ClusterIdRegistry()
    }

    val failedMembersRaw: ShortArray?
        /**
         * Retrieves the IDs of nodes that the message failed to sync with.
         *
         * @return an array of IDs of nodes that failed to sync
         */
        get() = failedMembers.ids

    /**
     * Sets the IDs of nodes that the message failed to sync with.
     *
     * @param failedMembers an array of IDs of nodes that failed to sync
     */
    fun setFailedMembers(failedMembers: ShortArray) {
        this.failedMembers = ClusterIdRegistry(*failedMembers)
    }

    val syncedMembersRaw: ShortArray?
        /**
         * Retrieves the IDs of nodes that the message was successfully synced with.
         *
         * @return an array of IDs of nodes that successfully synced
         */
        get() = syncedMembers.ids

    /**
     * Sets the IDs of nodes that the message was successfully synced with.
     *
     * @param syncedMembers an array of IDs of nodes that successfully synced
     */
    fun setSyncedMembers(syncedMembers: ShortArray) {
        this.syncedMembers = ClusterIdRegistry(*syncedMembers)
    }

    /**
     * Adds a synced clusterMember's ID to the list of successfully synced members.
     *
     * @param id the ID of the synced clusterMember to add
     */
    fun addSyncedMember(id: Short) {
        syncedMembers.add(id)
    }

    /**
     * Adds multiple synced members' IDs to the list of successfully synced members.
     *
     * @param ids an array of IDs of synced members to add
     */
    fun addSyncedMember(ids: ShortArray?) {
        if (ids == null) {
            return
        }
        syncedMembers.addAll(*ids)
    }

    /**
     * Adds multiple synced members' IDs to the list of successfully synced members.
     *
     * @param ids an array of IDs of synced members to add
     */
    fun addSyncedMember(ids: ClusterIdRegistry?) {
        if (ids == null) {
            return
        }
        syncedMembers.addAll(*ids.ids)
    }

    /**
     * Removes synced members' IDs from the list of successfully synced members.
     *
     * @param ids an array of IDs of synced members to remove
     */
    fun removeSyncedMember(ids: ShortArray?) {
        if (ids == null) {
            return
        }
        for (id in ids) {
            syncedMembers.remove(id)
        }
    }

    /**
     * Removes a synced clusterMember's ID from the list of successfully synced members.
     *
     * @param id the ID of the synced clusterMember to remove
     */
    fun removeSyncedMember(id: Short?) {
        if (id == null) {
            return
        }
        syncedMembers.remove(id)
    }

    /**
     * Adds a failed clusterMember's ID to the list of failed sync members.
     *
     * @param id the ID of the failed clusterMember to add
     */
    fun addFailedMember(id: Short?) {
        if (id == null) {
            return
        }
        failedMembers.add(id)
    }

    /**
     * Adds multiple failed members' IDs to the list of failed sync members.
     *
     * @param ids an array of IDs of failed members to add
     */
    fun addFailedMember(ids: ShortArray?) {
        if (ids == null) {
            return
        }
        failedMembers.addAll(*ids)
    }

    /**
     * Removes failed members' IDs from the list of failed sync members.
     *
     * @param ids an array of IDs of failed members to remove
     */
    fun removeFailedMember(ids: ShortArray?) {
        if (ids == null) {
            return
        }
        for (id in ids) {
            failedMembers.remove(id)
        }
    }

    /**
     * Removes a failed clusterMember's ID from the list of failed sync members.
     *
     * @param id the ID of the failed clusterMember to remove
     */
    fun removeFailedMember(id: Short?) {
        if (id == null) {
            return
        }
        failedMembers.remove(id)
    }
}
