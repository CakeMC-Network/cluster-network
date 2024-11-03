package net.cakemc.library.cluster.fallback.endpoint.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import java.io.*
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * A codec that handles the compression and decompression of byte data using the Deflate algorithm.
 *
 *
 * The `CompressionCodec` class extends [ByteToMessageCodec] to provide automatic
 * compression of outbound byte buffers and decompression of inbound byte buffers. If the data size
 * exceeds a predefined threshold, the data is compressed before being sent. Otherwise, it is sent
 * as is. Incoming messages are also decompressed if they were compressed during transmission.
 *
 * <h2>Compression Threshold</h2>
 *
 * The codec uses a threshold of [.COMPRESSION_THRESHOLD] bytes. If the size of the data
 * being sent is less than this threshold, the data is transmitted without compression. Otherwise,
 * it is compressed using the Deflate algorithm.
 *
 * <h2>Example Usage</h2>
 * <pre>
 * `ChannelPipeline pipeline = channel.pipeline();
 * pipeline.addLast(new CompressionCodec());
 * // Other handlers...
` *
</pre> *
 *
 * @see ByteToMessageCodec
 */
class CompressionCodec : ByteToMessageCodec<ByteBuf>() {
    /**
     * Encodes the provided [ByteBuf] into another [ByteBuf],
     * compressing the data if it exceeds the compression threshold.
     *
     * @param ctx the [ChannelHandlerContext] which this `CompressionCodec` belongs to
     * @param byteBuf the [ByteBuf] containing the data to encode
     * @param byteBuf2 the [ByteBuf] where the encoded data will be written
     * @throws Exception if an error occurs during encoding
     */
    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, byteBuf2: ByteBuf) {
        try {
            val dataLength = byteBuf.readableBytes()

            if (COMPRESSION_THRESHOLD > dataLength) {
                byteBuf2.writeInt(0)
                byteBuf2.writeBytes(byteBuf)
            } else {
                byteBuf2.writeInt(dataLength)
                byteBuf2.writeBytes(compress(readRemainingBytes(byteBuf)))
            }
        } catch (throwable: Throwable) {
            ctx.channel().close().sync()
            throwable.printStackTrace()
        }
    }

    /**
     * Decodes the inbound [ByteBuf] into a list of outbound objects, decompressing
     * the data if it was compressed during transmission.
     *
     * @param ctx the [ChannelHandlerContext] which this `CompressionCodec` belongs to
     * @param in the incoming [ByteBuf] to decode
     * @param out the list where decoded objects will be added
     */
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        if (!ctx.channel().isActive) return
        val dataLength = `in`.readInt()

        if (dataLength == 0) {
            out.add(`in`.retainedSlice())
            `in`.skipBytes(`in`.readableBytes())
            return
        }

        val compressed = readRemainingBytes(`in`)
        val uncompressed = Unpooled.wrappedBuffer(decompress(compressed))
        out.add(uncompressed)
    }

    /**
     * Handles any exceptions that occur during the processing of inbound or outbound messages.
     *
     * @param ctx the [ChannelHandlerContext] which this `CompressionCodec` belongs to
     * @param cause the [Throwable] that was raised
     * @throws Exception if an error occurs during exception handling
     */
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        super.exceptionCaught(ctx, cause)
        ctx.channel().close().sync()
    }

    /**
     * Reads the remaining bytes from the specified [ByteBuf].
     *
     * @param byteBuf the [ByteBuf] from which to read the bytes
     * @return an array of bytes containing the remaining data
     */
    fun readRemainingBytes(byteBuf: ByteBuf): ByteArray {
        val data = ByteArray(byteBuf.readableBytes())
        byteBuf.readBytes(data)
        return data
    }

    /**
     * Compresses the given input byte array using the Deflate algorithm.
     *
     * @param input the byte array to compress
     * @return a compressed byte array
     */
    fun compress(input: ByteArray?): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()

        val outputStream = ByteArrayOutputStream()
        val output = ByteArray(1024)

        while (!deflater.finished()) {
            val compressedSize = deflater.deflate(output)
            outputStream.write(output, 0, compressedSize)
        }

        return outputStream.toByteArray()
    }

    /**
     * Decompresses the given input byte array using the Deflate algorithm.
     *
     * @param input the byte array to decompress
     * @return a decompressed byte array
     * @throws IllegalArgumentException if the input data is not valid for decompression
     */
    fun decompress(input: ByteArray?): ByteArray {
        val inflater = Inflater()
        inflater.setInput(input)

        val outputStream = ByteArrayOutputStream()
        val output = ByteArray(1024)

        try {
            while (!inflater.finished()) {
                val decompressedSize = inflater.inflate(output)
                outputStream.write(output, 0, decompressedSize)
            }
        } catch (exception: DataFormatException) {
            throw IllegalArgumentException("Invalid compression header!", exception)
        }

        return outputStream.toByteArray()
    }

    companion object {
        /**
         * The threshold size (in bytes) for compressing data.
         * Data larger than this value will be compressed.
         */
        private const val COMPRESSION_THRESHOLD = 256
    }
}
