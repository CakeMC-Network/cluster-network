package net.cakemc.library.cluster.fallback.endpoint.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import net.cakemc.library.cluster.fallback.endpoint.packet.AbstractPacketRegistry;
import net.cakemc.library.cluster.fallback.endpoint.packet.BackPacket;

import java.util.List;

/**
 * A codec that encodes and decodes {@link BackPacket} objects for network transmission.
 *
 * <p>The {@code FallbackPacketCodec} class extends {@link ByteToMessageCodec} and is responsible
 * for converting packet objects to a byte representation and vice versa. It uses an
 * {@link AbstractPacketRegistry} to manage the association between packet IDs and packet
 * classes.</p>
 *
 * <h2>Encoding</h2>
 * <p>When encoding a packet, this codec writes the packet's ID followed by its data to
 * the provided {@link ByteBuf}.</p>
 *
 * <h2>Decoding</h2>
 * <p>During decoding, the codec reads the packet ID from the byte buffer, retrieves the
 * corresponding packet class from the packet registry, and reads the data into a packet object.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * {@code
 * ChannelPipeline pipeline = channel.pipeline();
 * pipeline.addLast(new FallbackPacketCodec(packetRegistry));
 * // Other handlers...
 * }
 * </pre>
 *
 * @see ByteToMessageCodec
 * @see BackPacket
 * @see AbstractPacketRegistry
 */
public class FallbackPacketCodec extends ByteToMessageCodec<BackPacket> {

	/**
	 * The packet registry used to map packet IDs to packet classes.
	 */
	private final AbstractPacketRegistry packetRegistry;

	/**
	 * Constructs a new {@code FallbackPacketCodec} with the specified packet registry.
	 *
	 * @param packetRegistry the {@link AbstractPacketRegistry} used to retrieve backPackets by ID
	 */
	public FallbackPacketCodec(AbstractPacketRegistry packetRegistry) {
		this.packetRegistry = packetRegistry;
	}

	/**
	 * Encodes the specified {@link BackPacket} into the given {@link ByteBuf}.
	 *
	 * <p>This method writes the backPacket's ID followed by its serialized data to the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code FallbackPacketCodec} belongs to
	 * @param backPacket the {@link BackPacket} to encode
	 * @param byteBuf the {@link ByteBuf} where the encoded backPacket data will be written
	 * @throws Exception if an error occurs during encoding
	 */
	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, BackPacket backPacket, ByteBuf byteBuf) throws Exception {
		byteBuf.writeInt(backPacket.id());
		backPacket.write(byteBuf);
	}

	/**
	 * Decodes the incoming data from the specified {@link ByteBuf} into a {@link BackPacket}.
	 *
	 * <p>This method reads the packet ID from the byte buffer, retrieves the corresponding
	 * packet class from the packet registry, and populates the packet with data read from
	 * the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code FallbackPacketCodec} belongs to
	 * @param byteBuf the incoming {@link ByteBuf} containing the encoded packet data
	 * @param list the list where the decoded packet will be added
	 * @throws Exception if an error occurs during decoding
	 */
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
		BackPacket backPacket = packetRegistry.getPacketById(byteBuf.readInt());
		backPacket.read(byteBuf);

		list.add(backPacket);
	}
}
