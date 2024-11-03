package net.cakemc.library.cluster.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Decoder for synchronizing messages over a Netty channel.
 *
 * <p>The {@code SyncNettyDecoder} class extends {@code ByteToMessageDecoder}
 * to handle incoming byte streams and convert them into {@code DefaultSyncPublication} messages.</p>
 */
public class SyncNettyDecoder extends ByteToMessageDecoder {

	/**
	 * Decodes the incoming bytes from the specified {@code ByteBuf} into {@code DefaultSyncPublication} messages.
	 *
	 * <p>This method checks if there are enough readable bytes in the provided {@code ByteBuf}
	 * to deserialize a {@code DefaultSyncPublication} message. If sufficient bytes are available,
	 * it creates a new {@code DefaultSyncPublication} instance, deserializes the message data,
	 * and adds it to the output list.</p>
	 *
	 * @param ctx the {@code ChannelHandlerContext} which provides various operations for the channel
	 * @param in the {@code ByteBuf} containing the incoming message bytes
	 * @param out a {@code List} where the decoded messages will be added
	 * @throws Exception if an error occurs during the decoding process
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// Check if there's enough readable bytes to deserialize a DefaultSyncPublication
		if (in.readableBytes() < Short.BYTES + 1) { // Assuming you need at least the size of id (short) + command (byte)
			return; // Not enough data to decode a message yet
		}

		// Create a new DefaultSyncPublication message
		DefaultSyncPublication message = new DefaultSyncPublication();
		// Deserialize the message
		message.deserialize(in);
		// Add the deserialized message to the output list
		out.add(message);
	}
}
