package net.cakemc.library.cluster;

/**
 * This enum represents the different synchronization types used in the cluster framework.
 *
 * <p>Each synchronization type defines a specific method of communication and consensus among cluster nodes.
 * The available types include unicast and ring-based approaches, with variations for quorum requirements
 * and load balancing strategies.</p>
 *
 * <ul>
 *     <li>{@link #UNI_CAST} - Direct communication between nodes.</li>
 *     <li>{@link #RING} - A ring topology for communication among nodes.</li>
 *     <li>{@link #UNI_CAST_QUORUM} - Unicast synchronization requiring a quorum of nodes.</li>
 *     <li>{@link #RING_QUORUM} - Ring-based synchronization requiring a quorum.</li>
 *     <li>{@link #UNI_CAST_BALANCE} - Unicast synchronization with load balancing.</li>
 *     <li>{@link #UNI_CAST_BALANCE_QUORUM} - Unicast synchronization with load balancing and quorum.</li>
 *     <li>{@link #RING_BALANCE} - Ring synchronization with load balancing.</li>
 *     <li>{@link #RING_BALANCE_QUORUM} - Ring synchronization with load balancing and quorum.</li>
 *     <li>{@link #UNI_CAST_ONE_OF} - Unicast synchronization to one of the aware nodes.</li>
 * </ul>
 *
 * <p>This enum provides utility methods to convert synchronization types to byte values,
 * determine types based on certain criteria, and check if a given type belongs to a specific category
 * (like balance, unicast, or ring).</p>
 */
public enum SynchronisationType {

	/** Direct communication between nodes. */
	UNI_CAST,

	/** A ring topology for communication among nodes. */
	RING,

	/** Unicast synchronization requiring a quorum of nodes. */
	UNI_CAST_QUORUM,

	/** Ring-based synchronization requiring a quorum. */
	RING_QUORUM,

	/** Unicast synchronization with load balancing. */
	UNI_CAST_BALANCE,

	/** Unicast synchronization with load balancing and quorum. */
	UNI_CAST_BALANCE_QUORUM,

	/** Ring synchronization with load balancing. */
	RING_BALANCE,

	/** Ring synchronization with load balancing and quorum. */
	RING_BALANCE_QUORUM,

	/** Unicast synchronization to one of the aware nodes. */
	UNI_CAST_ONE_OF;

	/**
	 * Converts the {@link SynchronisationType} to a byte value based on its ordinal position.
	 *
	 * @return the byte value corresponding to the ordinal of this {@link SynchronisationType}.
	 */
	public byte getValue() {
		return (byte) ordinal();
	}

	/**
	 * Retrieves the {@link SynchronisationType} associated with the given byte value.
	 *
	 * @param value the byte value representing a {@link SynchronisationType}.
	 * @return the corresponding {@link SynchronisationType}, or {@code null} if no match is found.
	 *
	 * <p>This method iterates through the defined enum values to find a matching type.</p>
	 */
	public static SynchronisationType getByValue(byte value) {
		for (SynchronisationType type : SynchronisationType.values()) {
			if (type.getValue() == value) {
				return type;
			}
		}
		return null; // Consider using Optional<SynchronisationType> instead for better handling
	}

	/**
	 * Checks if the given {@link SynchronisationType} is a balance type.
	 *
	 * @param synchronisationType the {@link SynchronisationType} to check.
	 * @return {@code true} if the synchronisationType is a balance type; {@code false} otherwise.
	 */
	public static boolean isBalanceType(SynchronisationType synchronisationType) {
		return synchronisationType == RING_BALANCE ||
		       synchronisationType == RING_BALANCE_QUORUM ||
		       synchronisationType == UNI_CAST_BALANCE ||
		       synchronisationType == UNI_CAST_BALANCE_QUORUM;
	}

	/**
	 * Checks if the given {@link SynchronisationType} is a unicast type.
	 *
	 * @param synchronisationType the {@link SynchronisationType} to check.
	 * @return {@code true} if the synchronisationType is a unicast type; {@code false} otherwise.
	 */
	public static boolean isUnicastType(SynchronisationType synchronisationType) {
		return synchronisationType == UNI_CAST ||
		       synchronisationType == UNI_CAST_QUORUM ||
		       synchronisationType == UNI_CAST_BALANCE ||
		       synchronisationType == UNI_CAST_BALANCE_QUORUM ||
		       synchronisationType == UNI_CAST_ONE_OF;
	}

	/**
	 * Checks if the given {@link SynchronisationType} is a ring type.
	 *
	 * @param synchronisationType the {@link SynchronisationType} to check.
	 * @return {@code true} if the synchronisationType is a ring type; {@code false} otherwise.
	 */
	public static boolean isRingType(SynchronisationType synchronisationType) {
		return synchronisationType == RING ||
		       synchronisationType == RING_QUORUM ||
		       synchronisationType == RING_BALANCE ||
		       synchronisationType == RING_BALANCE_QUORUM;
	}
}
