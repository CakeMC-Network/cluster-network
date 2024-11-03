package net.cakemc.library.cluster.address;

import java.util.Arrays;

/**
 * A utility class that manages a registry of cluster IDs.
 *
 * <p>The `ClusterIdRegistry` class provides methods to add, remove, and check for
 * the presence of cluster IDs (represented as shorts). It ensures that IDs are stored
 * without duplicates and provides functionality for bulk operations.</p>
 */
public class ClusterIdRegistry {

	private short[] ids = new short[0];

	/**
	 * Constructs a new `ClusterIdRegistry` with a single ID.
	 *
	 * @param id the initial cluster ID to register
	 */
	public ClusterIdRegistry(short id) {
		this.ids = new short[]{id};
	}

	/**
	 * Constructs a new `ClusterIdRegistry` with an array of IDs.
	 *
	 * @param ids the initial cluster IDs to register
	 */
	public ClusterIdRegistry(short... ids) {
		this.ids = ids;
	}

	/**
	 * Adds a new ID to the registry if it doesn't already exist.
	 *
	 * @param id the ID to add
	 * @return true if the ID was added, false if it already exists
	 */
	public boolean add(short id) {
		if (ids == null) {
			this.ids = new short[] { id };
		}
		if (!contains(id)) {
			ids = Arrays.copyOf(ids, ids.length + 1);
			ids[ids.length - 1] = id;
			return true;
		}
		return false;
	}

	/**
	 * Adds all the IDs from the provided array to the registry.
	 * Only IDs that are not already present will be added.
	 *
	 * @param idsToAdd an array of IDs to add
	 * @return true if any ID was added, false if all IDs already existed
	 */
	public boolean addAll(short... idsToAdd) {
		boolean modified = false;
		for (short id : idsToAdd) {
			modified |= add(id); // Add each and check if added
		}
		return modified;
	}

	/**
	 * Checks if the registry contains a specific ID.
	 *
	 * @param id the ID to check
	 * @return true if the ID exists in the registry, false otherwise
	 */
	public boolean contains(short id) {
		if (ids == null)
			return false;

		for (short currentId : ids) {
			if (currentId == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the registry contains all the IDs from the provided array.
	 *
	 * @param idsToCheck an array of IDs to check
	 * @return true if all IDs exist in the registry, false otherwise
	 */
	public boolean containsAll(short... idsToCheck) {
		for (short id : idsToCheck) {
			if (!contains(id)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes a specific ID from the registry.
	 *
	 * @param id the ID to remove
	 * @return true if the ID was removed, false if it was not present
	 */
	public boolean remove(short id) {
		if (contains(id)) {
			short[] newIds = new short[ids.length - 1];
			int index = 0;
			for (short currentId : ids) {
				if (currentId != id) {
					newIds[index++] = currentId;
				}
			}
			ids = newIds;
			return true;
		}
		return false;
	}

	public int indexOf(short value) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == value) {
				return i;
			}
		}
		return -1;
	}
	public int size() {
		return this.ids.length;
	}


	/**
	 * Removes all the IDs from the provided array.
	 *
	 * @param idsToRemove an array of IDs to remove
	 * @return true if any ID was removed, false if none were present
	 */
	public boolean removeAll(short... idsToRemove) {
		boolean modified = false;
		for (short id : idsToRemove) {
			modified |= remove(id); // Remove each and check if removed
		}
		return modified;
	}

	public boolean isEmpty() {
		return this.ids.length == 0;
	}

	/**
	 * Returns a defensive copy of the current array of IDs.
	 *
	 * @return an array of all registered IDs
	 */
	public short[] getIds() {
		return ids.clone();
	}
}
