package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 * Interface for encoding publication data within the cluster framework.
 *
 *
 * This interface defines a method for serializing publication information
 * into a byte array for transmission or storage.
 */
interface PublicationEncoder {
    /**
     * Serializes the publication's internal state into a byteBuf.
     *
     *
     * This method is responsible for converting the publication's data into
     * a format suitable for transmission over the network or for storage.
     * The resulting byte array should encapsulate all necessary information
     * to reconstruct the publication later.
     *
     * @param byteBuf a byteBuf representing the serialized publication data
     */
    fun serialize(byteBuf: ByteBuf)

    fun serialize(): ByteArray {
        val byteBuf = Unpooled.buffer(MAX_PUBLICATION_SIZE)
        this.serialize(byteBuf)
        val bytes = byteBuf.array()

        byteBuf.release()
        return bytes
    }

    companion object {
        const val MAX_PUBLICATION_SIZE: Int = 256
    }
}
