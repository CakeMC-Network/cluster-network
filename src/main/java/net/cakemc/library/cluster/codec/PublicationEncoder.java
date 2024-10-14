package net.cakemc.library.cluster.codec;

/**
 * Interface for encoding publication data within the cluster framework.
 *
 * <p>This interface defines a method for serializing publication information
 * into a byte array for transmission or storage.</p>
 */
public interface PublicationEncoder {

	/**
	 * Serializes the publication's internal state into a byte array.
	 *
	 * <p>This method is responsible for converting the publication's data into
	 * a format suitable for transmission over the network or for storage.
	 * The resulting byte array should encapsulate all necessary information
	 * to reconstruct the publication later.</p>
	 *
	 * @return a byte array representing the serialized publication data
	 */
	byte[] serialize();
}
