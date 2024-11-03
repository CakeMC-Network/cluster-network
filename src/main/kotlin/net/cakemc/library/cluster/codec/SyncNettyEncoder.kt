package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * Encoder for synchronizing publication messages over a Netty channel.
 *
 *
 * The `SyncNettyEncoder` class extends `MessageToByteEncoder`
 * to convert `DefaultSyncPublication` messages into byte streams suitable for
 * transmission over a Netty channel.
 */
class SyncNettyEncoder : MessageToByteEncoder<DefaultSyncPublication>() {
    /**
     * Encodes a `DefaultSyncPublication` message into the provided `ByteBuf`.
     *
     *
     * This method checks if the provided message is null, throwing an
     * `IllegalArgumentException` if it is. If the message is valid,
     * it calls the `serialize` method of `DefaultSyncPublication` to
     * write the serialized data to the provided `ByteBuf`.
     *
     * @param ctx the `ChannelHandlerContext` which provides various operations for the channel
     * @param message the `DefaultSyncPublication` message to encode
     * @param out the `ByteBuf` where the encoded bytes will be written
     * @throws IllegalArgumentException if the message to encode is null
     * @throws Exception if an error occurs during the encoding process
     */
    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, message: DefaultSyncPublication, out: ByteBuf) {
        requireNotNull(message) { "Message to encode cannot be null" }

        // Serialize the DefaultSyncPublication to the ByteBuf
        message.serialize(out)
    }
}
