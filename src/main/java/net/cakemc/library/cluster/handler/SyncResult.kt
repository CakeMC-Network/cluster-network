package net.cakemc.library.cluster.handler;

import net.cakemc.library.cluster.address.ClusterIdRegistry;

/**
 * Represents the result of a synchronization operation between cluster nodes.
 *
 * <p>The SyncResult class tracks the success or failure of a synchronization message,
 * along with the IDs of members involved in the operation. It allows for the addition
 * and removal of synced and failed members.</p>
 */
public class SyncResult {

	private boolean successful;
	private ClusterIdRegistry failedMembers;
	private ClusterIdRegistry syncedMembers;

	/**
	 * Constructs a SyncResult with no synced or failed members.
	 */
	public SyncResult() {
		this.failedMembers = new ClusterIdRegistry();
		this.syncedMembers = new ClusterIdRegistry();
	}

	/**
	 * Checks if the synchronization was successful.
	 *
	 * @return true if the message was successfully synced, false otherwise
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Sets the success status of the synchronization operation.
	 *
	 * @param successful true if the message was successfully synced, false otherwise
	 */
	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	/**
	 * Retrieves the IDs of nodes that the message failed to sync with.
	 *
	 * @return an array of IDs of nodes that failed to sync
	 */
	public short[] getFailedMembersRaw() {
		return failedMembers.getIds();
	}

	/**
	 * Retrieves the IDs of nodes that the message failed to sync with.
	 *
	 * @return an array of IDs of nodes that failed to sync
	 */
	public ClusterIdRegistry getFailedMembers() {
		return failedMembers;
	}

	/**
	 * Sets the IDs of nodes that the message failed to sync with.
	 *
	 * @param failedMembers an array of IDs of nodes that failed to sync
	 */
	public void setFailedMembers(short[] failedMembers) {
		this.failedMembers = new ClusterIdRegistry(failedMembers);
	}

	/**
	 * Retrieves the IDs of nodes that the message was successfully synced with.
	 *
	 * @return an array of IDs of nodes that successfully synced
	 */
	public short[] getSyncedMembersRaw() {
		return syncedMembers.getIds();
	}

	/**
	 * Retrieves the IDs of nodes that the message was successfully synced with.
	 *
	 * @return an array of IDs of nodes that successfully synced
	 */
	public ClusterIdRegistry getSyncedMembers() {
		return syncedMembers;
	}

	/**
	 * Sets the IDs of nodes that the message was successfully synced with.
	 *
	 * @param syncedMembers an array of IDs of nodes that successfully synced
	 */
	public void setSyncedMembers(short[] syncedMembers) {
		this.syncedMembers = new ClusterIdRegistry(syncedMembers);
	}

	/**
	 * Adds a synced clusterMember's ID to the list of successfully synced members.
	 *
	 * @param id the ID of the synced clusterMember to add
	 */
	public void addSyncedMember(short id) {
		syncedMembers.add(id);
	}

	/**
	 * Adds multiple synced members' IDs to the list of successfully synced members.
	 *
	 * @param ids an array of IDs of synced members to add
	 */
	public void addSyncedMember(short[] ids) {
		if (ids == null) {
			return;
		}
		syncedMembers.addAll(ids);
	}

	/**
	 * Adds multiple synced members' IDs to the list of successfully synced members.
	 *
	 * @param ids an array of IDs of synced members to add
	 */
	public void addSyncedMember(ClusterIdRegistry ids) {
		if (ids == null) {
			return;
		}
		syncedMembers.addAll(ids.getIds());
	}

	/**
	 * Removes synced members' IDs from the list of successfully synced members.
	 *
	 * @param ids an array of IDs of synced members to remove
	 */
	public void removeSyncedMember(short[] ids) {
		if (ids == null) {
			return;
		}
		for (short id : ids) {
			syncedMembers.remove(id);
		}
	}

	/**
	 * Removes a synced clusterMember's ID from the list of successfully synced members.
	 *
	 * @param id the ID of the synced clusterMember to remove
	 */
	public void removeSyncedMember(Short id) {
		if (id == null) {
			return;
		}
		syncedMembers.remove(id);
	}

	/**
	 * Adds a failed clusterMember's ID to the list of failed sync members.
	 *
	 * @param id the ID of the failed clusterMember to add
	 */
	public void addFailedMember(Short id) {
		if (id == null) {
			return;
		}
		failedMembers.add(id);
	}

	/**
	 * Adds multiple failed members' IDs to the list of failed sync members.
	 *
	 * @param ids an array of IDs of failed members to add
	 */
	public void addFailedMember(short[] ids) {
		if (ids == null) {
			return;
		}
		failedMembers.addAll(ids);
	}

	/**
	 * Removes failed members' IDs from the list of failed sync members.
	 *
	 * @param ids an array of IDs of failed members to remove
	 */
	public void removeFailedMember(short[] ids) {
		if (ids == null) {
			return;
		}
		for (short id : ids) {
			failedMembers.remove(id);

		}
	}

	/**
	 * Removes a failed clusterMember's ID from the list of failed sync members.
	 *
	 * @param id the ID of the failed clusterMember to remove
	 */
	public void removeFailedMember(Short id) {
		if (id == null) {
			return;
		}
		failedMembers.remove(id);
	}
}
