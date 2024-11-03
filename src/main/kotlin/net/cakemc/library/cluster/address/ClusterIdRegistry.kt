package net.cakemc.library.cluster.address

import net.cakemc.library.cluster.SynchronisationType

/**
 * A utility class that manages a registry of cluster IDs.
 *
 *
 * The `ClusterIdRegistry` class provides methods to add, remove, and check for
 * the presence of cluster IDs (represented as shorts). It ensures that IDs are stored
 * without duplicates and provides functionality for bulk operations.
 */
class ClusterIdRegistry {
  var ids: ShortArray

  /**
   * Constructs a new `ClusterIdRegistry` with a single ID.
   *
   * @param id the initial cluster ID to register
   */
  constructor(id: Short) {
    this.ids = shortArrayOf(id)
  }

  /**
   * Constructs a new `ClusterIdRegistry` with an array of IDs.
   *
   * @param ids the initial cluster IDs to register
   */
  constructor(vararg ids: Short) {
    this.ids = ids
  }

  /**
   * Adds a new ID to the registry if it doesn't already exist.
   *
   * @param id the ID to add
   * @return true if the ID was added, false if it already exists
   */
  fun add(id: Short): Boolean {
    if (!contains(id)) {
      ids = ids.copyOf(ids.size + 1)
      ids[ids.size - 1] = id
      return true
    }
    return false
  }

  /**
   * Adds all the IDs from the provided array to the registry.
   * Only IDs that are not already present will be added.
   *
   * @param idsToAdd an array of IDs to add
   * @return true if any ID was added, false if all IDs already existed
   */
  fun addAll(vararg idsToAdd: Short): Boolean {
    var modified = false
    for (id in idsToAdd) {
      modified = modified or add(id) // Add each and check if added
    }
    return modified
  }

  /**
   * Checks if the registry contains a specific ID.
   *
   * @param id the ID to check
   * @return true if the ID exists in the registry, false otherwise
   */
  fun contains(id: Short): Boolean {
    for (currentId in ids) {
      if (currentId == id) {
        return true
      }
    }
    return false
  }

  /**
   * Checks if the registry contains all the IDs from the provided array.
   *
   * @param idsToCheck an array of IDs to check
   * @return true if all IDs exist in the registry, false otherwise
   */
  fun containsAll(vararg idsToCheck: Short): Boolean {
    for (id in idsToCheck) {
      if (!contains(id)) {
        return false
      }
    }
    return true
  }

  /**
   * Removes a specific ID from the registry.
   *
   * @param id the ID to remove
   * @return true if the ID was removed, false if it was not present
   */
  fun remove(id: Short): Boolean {
    if (contains(id)) {
      val newIds = ShortArray(ids.size - 1)
      var index = 0
      for (currentId in ids) {
        if (currentId != id) {
          newIds[index++] = currentId
        }
      }
      ids = newIds
      return true
    }
    return false
  }

  fun indexOf(value: Short): Int {
    for (i in ids) {
      if (ids[i.toInt()] == value) {
        return i.toInt()
      }
    }

    return -1
  }

  fun size(): Int {
    return ids.size
  }


  /**
   * Removes all the IDs from the provided array.
   *
   * @param idsToRemove an array of IDs to remove
   * @return true if any ID was removed, false if none were present
   */
  fun removeAll(vararg idsToRemove: Short): Boolean {
    var modified = false
    for (id in idsToRemove) {
      modified = modified or remove(id) // Remove each and check if removed
    }
    return modified
  }

  val isEmpty: Boolean get() = ids.size == 0

}
