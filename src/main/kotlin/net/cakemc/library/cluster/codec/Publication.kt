package net.cakemc.library.cluster.codec

/**
 * Interface representing a publication in the cluster framework, combining
 * encoding and decoding functionalities for publication data.
 *
 *
 * A publication encapsulates information necessary for cluster communication,
 * including serialization to and deserialization from byte arrays.
 *
 *
 * This interface extends both [PublicationEncoder] and [PublicationDecoder],
 * providing a contract for classes that implement publication functionality.
 */
interface Publication : PublicationEncoder, PublicationDecoder {
    /**
     * Retrieves the unique key associated with this publication.
     *
     *
     * The key is typically used to identify the publication within the cluster.
     *
     * @return the unique key for this publication
     */
    val key: String


    /**
     * Retrieves the communication channel associated with this publication.
     *
     *
     * The channel defines the specific path or topic within the cluster
     * where this publication is communicated. It is used to route the publication
     * to the intended destination in the cluster.
     *
     * @return the channel associated with this publication
     */
    /**
     * Sets the communication channel for this publication.
     *
     *
     * This method allows the publication's channel to be configured, defining
     * the specific path or topic for routing within the cluster. It is crucial
     * for setting up the correct destination for the publication.
     *
     * @param channel the channel to be set for this publication
     */
    var channel: String

    /**
     * Retrieves the version of this publication.
     *
     *
     * The version indicates the current state or iteration of the publication.
     * It is useful for ensuring that the most up-to-date information is being used
     * when communicating between cluster members.
     *
     * @return the version of the publication
     */
    val version: Long
}
