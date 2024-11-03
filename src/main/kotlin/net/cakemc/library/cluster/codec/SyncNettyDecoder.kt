package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * Decoder for synchronizing messages over a Netty channel.
 *
 *
 * The `SyncNettyDecoder` class extends `ByteToMessageDecoder`
 * to handle incoming byte streams and convert them into `DefaultSyncPublication` messages.
 */
class SyncNettyDecoder : ByteToMessageDecoder() {
    /**
     * Decodes the incoming bytes from the specified `ByteBuf` into `DefaultSyncPublication` messages.
     *
     *
     * This method checks if there are enough readable bytes in the provided `ByteBuf`
     * to deserialize a `DefaultSyncPublication` message. If sufficient bytes are available,
     * it creates a new `DefaultSyncPublication` instance, deserializes the message data,
     * and adds it to the output list.
     *
     * @param ctx the `ChannelHandlerContext` which provides various operations for the channel
     * @param in the `ByteBuf` containing the incoming message bytes
     * @param out a `List` where the decoded messages will be added
     * @throws Exception if an error occurs during the decoding process
     */
    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        // Check if there's enough readable bytes to deserialize a DefaultSyncPublication
        if (`in`.readableBytes() < java.lang.Short.BYTES + 1) { // Assuming you need at least the size of id (short) + command (byte)
            return  // Not enough data to decode a message yet
        }

        // Create a new DefaultSyncPublication message
        val message = DefaultSyncPublication()
        // Deserialize the message
        message.deserialize(`in`)
        // Add the deserialized message to the output list
        out.add(message)
    }
}
