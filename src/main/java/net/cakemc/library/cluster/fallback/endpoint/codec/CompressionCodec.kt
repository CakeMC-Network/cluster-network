package net.cakemc.library.cluster.fallback.endpoint.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A codec that handles the compression and decompression of byte data using the Deflate algorithm.
 *
 * <p>The {@code CompressionCodec} class extends {@link ByteToMessageCodec} to provide automatic
 * compression of outbound byte buffers and decompression of inbound byte buffers. If the data size
 * exceeds a predefined threshold, the data is compressed before being sent. Otherwise, it is sent
 * as is. Incoming messages are also decompressed if they were compressed during transmission.</p>
 *
 * <h2>Compression Threshold</h2>
 * <p>The codec uses a threshold of {@link #COMPRESSION_THRESHOLD} bytes. If the size of the data
 * being sent is less than this threshold, the data is transmitted without compression. Otherwise,
 * it is compressed using the Deflate algorithm.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * {@code
 * ChannelPipeline pipeline = channel.pipeline();
 * pipeline.addLast(new CompressionCodec());
 * // Other handlers...
 * }
 * </pre>
 *
 * @see ByteToMessageCodec
 */
public class CompressionCodec extends ByteToMessageCodec<ByteBuf> {

	/**
	 * The threshold size (in bytes) for compressing data.
	 * Data larger than this value will be compressed.
	 */
	private static final int COMPRESSION_THRESHOLD = 256;

	/**
	 * Encodes the provided {@link ByteBuf} into another {@link ByteBuf},
	 * compressing the data if it exceeds the compression threshold.
	 *
	 * @param ctx the {@link ChannelHandlerContext} which this {@code CompressionCodec} belongs to
	 * @param byteBuf the {@link ByteBuf} containing the data to encode
	 * @param byteBuf2 the {@link ByteBuf} where the encoded data will be written
	 * @throws Exception if an error occurs during encoding
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf byteBuf, ByteBuf byteBuf2) throws Exception {
		try {
			int dataLength = byteBuf.readableBytes();

			if (COMPRESSION_THRESHOLD > dataLength) {
				byteBuf2.writeInt(0);
				byteBuf2.writeBytes(byteBuf);
			} else {
				byteBuf2.writeInt(dataLength);
				byteBuf2.writeBytes(compress(readRemainingBytes(byteBuf)));
			}
		} catch (Throwable throwable) {
			ctx.channel().close().sync();
			throwable.printStackTrace();
		}
	}

	/**
	 * Decodes the inbound {@link ByteBuf} into a list of outbound objects, decompressing
	 * the data if it was compressed during transmission.
	 *
	 * @param ctx the {@link ChannelHandlerContext} which this {@code CompressionCodec} belongs to
	 * @param in the incoming {@link ByteBuf} to decode
	 * @param out the list where decoded objects will be added
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		if (!ctx.channel().isActive()) return;
		int dataLength = in.readInt();

		if (dataLength == 0) {
			out.add(in.retainedSlice());
			in.skipBytes(in.readableBytes());
			return;
		}

		byte[] compressed = readRemainingBytes(in);
		ByteBuf uncompressed = Unpooled.wrappedBuffer(decompress(compressed));
		out.add(uncompressed);
	}

	/**
	 * Handles any exceptions that occur during the processing of inbound or outbound messages.
	 *
	 * @param ctx the {@link ChannelHandlerContext} which this {@code CompressionCodec} belongs to
	 * @param cause the {@link Throwable} that was raised
	 * @throws Exception if an error occurs during exception handling
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		ctx.channel().close().sync();
	}

	/**
	 * Reads the remaining bytes from the specified {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} from which to read the bytes
	 * @return an array of bytes containing the remaining data
	 */
	byte[] readRemainingBytes(ByteBuf byteBuf) {
		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);
		return data;
	}

	/**
	 * Compresses the given input byte array using the Deflate algorithm.
	 *
	 * @param input the byte array to compress
	 * @return a compressed byte array
	 */
	byte[] compress(byte[] input) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] output = new byte[1024];

		while (!deflater.finished()) {
			int compressedSize = deflater.deflate(output);
			outputStream.write(output, 0, compressedSize);
		}

		return outputStream.toByteArray();
	}

	/**
	 * Decompresses the given input byte array using the Deflate algorithm.
	 *
	 * @param input the byte array to decompress
	 * @return a decompressed byte array
	 * @throws IllegalArgumentException if the input data is not valid for decompression
	 */
	byte[] decompress(byte[] input) {
		Inflater inflater = new Inflater();
		inflater.setInput(input);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] output = new byte[1024];

		try {
			while (!inflater.finished()) {
				int decompressedSize = inflater.inflate(output);
				outputStream.write(output, 0, decompressedSize);
			}
		} catch (DataFormatException exception) {
			throw new IllegalArgumentException("Invalid compression header!", exception);
		}

		return outputStream.toByteArray();
	}
}
