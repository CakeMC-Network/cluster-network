package net.cakemc.library.cluster

/**
 * This enum represents the different synchronization types used in the cluster framework.
 *
 *
 * Each synchronization type defines a specific method of communication and consensus among cluster nodes.
 * The available types include unicast and ring-based approaches, with variations for quorum requirements
 * and load balancing strategies.
 *
 *
 *  * [.UNI_CAST] - Direct communication between nodes.
 *  * [.RING] - A ring topology for communication among nodes.
 *  * [.UNI_CAST_QUORUM] - Unicast synchronization requiring a quorum of nodes.
 *  * [.RING_QUORUM] - Ring-based synchronization requiring a quorum.
 *  * [.UNI_CAST_BALANCE] - Unicast synchronization with load balancing.
 *  * [.UNI_CAST_BALANCE_QUORUM] - Unicast synchronization with load balancing and quorum.
 *  * [.RING_BALANCE] - Ring synchronization with load balancing.
 *  * [.RING_BALANCE_QUORUM] - Ring synchronization with load balancing and quorum.
 *  * [.UNI_CAST_ONE_OF] - Unicast synchronization to one of the aware nodes.
 *
 *
 *
 * This enum provides utility methods to convert synchronization types to byte values,
 * determine types based on certain criteria, and check if a given type belongs to a specific category
 * (like balance, unicast, or ring).
 */
enum class SynchronisationType {
  /** Direct communication between nodes.  */
  UNI_CAST,

  /** A ring topology for communication among nodes.  */
  RING,

  /** Unicast synchronization requiring a quorum of nodes.  */
  UNI_CAST_QUORUM,

  /** Ring-based synchronization requiring a quorum.  */
  RING_QUORUM,

  /** Unicast synchronization with load balancing.  */
  UNI_CAST_BALANCE,

  /** Unicast synchronization with load balancing and quorum.  */
  UNI_CAST_BALANCE_QUORUM,

  /** Ring synchronization with load balancing.  */
  RING_BALANCE,

  /** Ring synchronization with load balancing and quorum.  */
  RING_BALANCE_QUORUM,

  /** Unicast synchronization to one of the aware nodes.  */
  UNI_CAST_ONE_OF;

  val value: Byte
    /**
     * Converts the [SynchronisationType] to a byte value based on its ordinal position.
     *
     * @return the byte value corresponding to the ordinal of this [SynchronisationType].
     */
    get() = ordinal as Byte

  companion object {
    /**
     * Retrieves the [SynchronisationType] associated with the given byte value.
     *
     * @param value the byte value representing a [SynchronisationType].
     * @return the corresponding [SynchronisationType], or `null` if no match is found.
     *
     *
     * This method iterates through the defined enum values to find a matching type.
     */
    fun getByValue(value: Byte): SynchronisationType? {
      for (type in entries) {
        if (type.value == value) {
          return type
        }
      }
      return null // Consider using Optional<SynchronisationType> instead for better handling
    }

    /**
     * Checks if the given [SynchronisationType] is a balance type.
     *
     * @param synchronisationType the [SynchronisationType] to check.
     * @return `true` if the synchronisationType is a balance type; `false` otherwise.
     */
    fun isBalanceType(synchronisationType: SynchronisationType?): Boolean {
      return synchronisationType == RING_BALANCE || synchronisationType == RING_BALANCE_QUORUM || synchronisationType == UNI_CAST_BALANCE || synchronisationType == UNI_CAST_BALANCE_QUORUM
    }

    /**
     * Checks if the given [SynchronisationType] is a unicast type.
     *
     * @param synchronisationType the [SynchronisationType] to check.
     * @return `true` if the synchronisationType is a unicast type; `false` otherwise.
     */
    fun isUnicastType(synchronisationType: SynchronisationType?): Boolean {
      return synchronisationType == UNI_CAST || synchronisationType == UNI_CAST_QUORUM || synchronisationType == UNI_CAST_BALANCE || synchronisationType == UNI_CAST_BALANCE_QUORUM || synchronisationType == UNI_CAST_ONE_OF
    }

    /**
     * Checks if the given [SynchronisationType] is a ring type.
     *
     * @param synchronisationType the [SynchronisationType] to check.
     * @return `true` if the synchronisationType is a ring type; `false` otherwise.
     */
    fun isRingType(synchronisationType: SynchronisationType?): Boolean {
      return synchronisationType == RING || synchronisationType == RING_QUORUM || synchronisationType == RING_BALANCE || synchronisationType == RING_BALANCE_QUORUM
    }
  }
}

