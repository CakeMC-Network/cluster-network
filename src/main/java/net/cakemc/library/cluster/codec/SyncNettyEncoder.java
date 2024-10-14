package net.cakemc.library.cluster.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encoder for synchronizing publication messages over a Netty channel.
 *
 * <p>The {@code SyncNettyEncoder} class extends {@code MessageToByteEncoder}
 * to convert {@code DefaultSyncPublication} messages into byte streams suitable for
 * transmission over a Netty channel.</p>
 */
public class SyncNettyEncoder extends MessageToByteEncoder<DefaultSyncPublication> {

	/**
	 * Encodes a {@code DefaultSyncPublication} message into the provided {@code ByteBuf}.
	 *
	 * <p>This method checks if the provided message is null, throwing an
	 * {@code IllegalArgumentException} if it is. If the message is valid,
	 * it calls the {@code serialize} method of {@code DefaultSyncPublication} to
	 * write the serialized data to the provided {@code ByteBuf}.</p>
	 *
	 * @param ctx the {@code ChannelHandlerContext} which provides various operations for the channel
	 * @param message the {@code DefaultSyncPublication} message to encode
	 * @param out the {@code ByteBuf} where the encoded bytes will be written
	 * @throws IllegalArgumentException if the message to encode is null
	 * @throws Exception if an error occurs during the encoding process
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, DefaultSyncPublication message, ByteBuf out) throws Exception {
		if (message == null) {
			throw new IllegalArgumentException("Message to encode cannot be null");
		}

		// Serialize the DefaultSyncPublication to the ByteBuf
		message.serialize(out);
	}
}
