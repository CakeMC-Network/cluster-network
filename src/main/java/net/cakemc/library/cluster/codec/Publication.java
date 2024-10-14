package net.cakemc.library.cluster.codec;

/**
 * Interface representing a publication in the cluster framework, combining
 * encoding and decoding functionalities for publication data.
 *
 * <p>A publication encapsulates information necessary for cluster communication,
 * including serialization to and deserialization from byte arrays.</p>
 *
 * <p>This interface extends both {@link PublicationEncoder} and {@link PublicationDecoder},
 * providing a contract for classes that implement publication functionality.</p>
 */
public interface Publication extends PublicationEncoder, PublicationDecoder {

	/**
	 * Retrieves the unique key associated with this publication.
	 *
	 * <p>The key is typically used to identify the publication within the cluster.</p>
	 *
	 * @return the unique key for this publication
	 */
	String getKey();

	/**
	 * Retrieves the version of this publication.
	 *
	 * <p>The version indicates the current state or iteration of the publication.
	 * It is useful for ensuring that the most up-to-date information is being used
	 * when communicating between cluster members.</p>
	 *
	 * @return the version of the publication
	 */
	long getVersion();
}
