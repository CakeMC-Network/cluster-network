package net.cakemc.library.cluster.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Interface for encoding publication data within the cluster framework.
 *
 * <p>This interface defines a method for serializing publication information
 * into a byte array for transmission or storage.</p>
 */
public interface PublicationEncoder {

	int MAX_PUBLICATION_SIZE = 256;

	/**
	 * Serializes the publication's internal state into a byteBuf.
	 *
	 * <p>This method is responsible for converting the publication's data into
	 * a format suitable for transmission over the network or for storage.
	 * The resulting byte array should encapsulate all necessary information
	 * to reconstruct the publication later.</p>
	 *
	 * @param byteBuf a byteBuf representing the serialized publication data
	 */
	void serialize(ByteBuf byteBuf);

	default byte[] serialize() {
		ByteBuf byteBuf = Unpooled.buffer(MAX_PUBLICATION_SIZE);
		this.serialize(byteBuf);
		byte[] bytes = byteBuf.array();

		byteBuf.release();
		return bytes;
	}
}
