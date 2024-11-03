package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf

/**
 * Interface for decoding publication data within the cluster framework.
 *
 *
 * This interface defines methods for configuring the decoder, closing it,
 * and deserializing publication data from a byte array.
 */
interface PublicationDecoder {
    /**
     * Closes the decoder and releases any resources it may hold.
     *
     *
     * This method should be called when the decoder is no longer needed to
     * ensure proper resource management and avoid memory leaks.
     */
    fun close()

    /**
     * Configures the decoder with the specified settings.
     *
     *
     * The configuration map can contain various parameters necessary for
     * initializing the decoder's behavior. The specifics of these parameters
     * should be documented elsewhere, as they can vary by implementation.
     *
     * @param config a map of configuration parameters
     */
    fun configure(config: Map<String?, *>?)

    /**
     * Deserializes the provided byte array into the publication's internal state.
     *
     *
     * This method reads the byte array and populates the fields of the
     * implementing class accordingly. It is expected to handle any necessary
     * exceptions or errors that may arise during deserialization.
     *
     * @param data the byteBuf representing the serialized publication data
     */
    fun deserialize(data: ByteBuf)
}
