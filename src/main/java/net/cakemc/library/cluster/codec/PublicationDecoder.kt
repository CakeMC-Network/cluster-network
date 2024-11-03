package net.cakemc.library.cluster.codec;

import io.netty.buffer.ByteBuf;

import java.util.Map;

/**
 * Interface for decoding publication data within the cluster framework.
 *
 * <p>This interface defines methods for configuring the decoder, closing it,
 * and deserializing publication data from a byte array.</p>
 */
public interface PublicationDecoder {

	/**
	 * Closes the decoder and releases any resources it may hold.
	 *
	 * <p>This method should be called when the decoder is no longer needed to
	 * ensure proper resource management and avoid memory leaks.</p>
	 */
	void close();

	/**
	 * Configures the decoder with the specified settings.
	 *
	 * <p>The configuration map can contain various parameters necessary for
	 * initializing the decoder's behavior. The specifics of these parameters
	 * should be documented elsewhere, as they can vary by implementation.</p>
	 *
	 * @param config a map of configuration parameters
	 */
	void configure(Map<String, ?> config);

	/**
	 * Deserializes the provided byte array into the publication's internal state.
	 *
	 * <p>This method reads the byte array and populates the fields of the
	 * implementing class accordingly. It is expected to handle any necessary
	 * exceptions or errors that may arise during deserialization.</p>
	 *
	 * @param data the byteBuf representing the serialized publication data
	 */
	void deserialize(ByteBuf data);
}
