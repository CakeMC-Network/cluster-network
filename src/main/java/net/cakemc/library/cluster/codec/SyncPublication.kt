package net.cakemc.library.cluster.codec;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Interface representing a synchronization publication in the cluster communication framework.
 *
 * <p>The SyncPublication interface defines the methods required to serialize and deserialize
 * synchronization data. Implementations of this interface will handle the transmission of
 * synchronization messages between cluster nodes.</p>
 */
public interface SyncPublication {

	/**
	 * Serializes the synchronization publication to the provided ByteBuf.
	 *
	 * @param byteBuf the ByteBuf where the synchronization data should be serialized
	 * @throws IOException if an I/O error occurs during serialization
	 */
	void serialize(ByteBuf byteBuf) throws IOException;

	/**
	 * Deserializes the synchronization publication from the provided ByteBuf.
	 *
	 * @param byteBuf the ByteBuf containing the serialized synchronization data
	 * @throws IOException if an I/O error occurs during deserialization
	 */
	void deserialize(ByteBuf byteBuf) throws IOException;
}
