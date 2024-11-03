package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import java.io.IOException

/**
 * Interface representing a synchronization publication in the cluster communication framework.
 *
 *
 * The SyncPublication interface defines the methods required to serialize and deserialize
 * synchronization data. Implementations of this interface will handle the transmission of
 * synchronization messages between cluster nodes.
 */
interface SyncPublication {
    /**
     * Serializes the synchronization publication to the provided ByteBuf.
     *
     * @param byteBuf the ByteBuf where the synchronization data should be serialized
     * @throws IOException if an I/O error occurs during serialization
     */
    @Throws(IOException::class)
    fun serialize(byteBuf: ByteBuf)

    /**
     * Deserializes the synchronization publication from the provided ByteBuf.
     *
     * @param byteBuf the ByteBuf containing the serialized synchronization data
     * @throws IOException if an I/O error occurs during deserialization
     */
    @Throws(IOException::class)
    fun deserialize(byteBuf: ByteBuf)
}
