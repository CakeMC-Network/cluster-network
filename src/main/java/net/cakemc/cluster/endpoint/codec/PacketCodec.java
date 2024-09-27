package net.cakemc.cluster.endpoint.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import net.cakemc.cluster.packet.AbstractPacketRegistry;
import net.cakemc.cluster.packet.Packet;

import java.util.List;

/**
 * A codec that encodes and decodes {@link Packet} objects for network transmission.
 *
 * <p>The {@code PacketCodec} class extends {@link ByteToMessageCodec} and is responsible
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
 * pipeline.addLast(new PacketCodec(packetRegistry));
 * // Other handlers...
 * }
 * </pre>
 *
 * @see ByteToMessageCodec
 * @see Packet
 * @see AbstractPacketRegistry
 */
public class PacketCodec extends ByteToMessageCodec<Packet> {

	/**
	 * The packet registry used to map packet IDs to packet classes.
	 */
	private final AbstractPacketRegistry packetRegistry;

	/**
	 * Constructs a new {@code PacketCodec} with the specified packet registry.
	 *
	 * @param packetRegistry the {@link AbstractPacketRegistry} used to retrieve packets by ID
	 */
	public PacketCodec(AbstractPacketRegistry packetRegistry) {
		this.packetRegistry = packetRegistry;
	}

	/**
	 * Encodes the specified {@link Packet} into the given {@link ByteBuf}.
	 *
	 * <p>This method writes the packet's ID followed by its serialized data to the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code PacketCodec} belongs to
	 * @param packet the {@link Packet} to encode
	 * @param byteBuf the {@link ByteBuf} where the encoded packet data will be written
	 * @throws Exception if an error occurs during encoding
	 */
	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {
		byteBuf.writeInt(packet.id());
		packet.write(byteBuf);
	}

	/**
	 * Decodes the incoming data from the specified {@link ByteBuf} into a {@link Packet}.
	 *
	 * <p>This method reads the packet ID from the byte buffer, retrieves the corresponding
	 * packet class from the packet registry, and populates the packet with data read from
	 * the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code PacketCodec} belongs to
	 * @param byteBuf the incoming {@link ByteBuf} containing the encoded packet data
	 * @param list the list where the decoded packet will be added
	 * @throws Exception if an error occurs during decoding
	 */
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
		Packet packet = packetRegistry.getPacketById(byteBuf.readInt());
		packet.read(byteBuf);

		list.add(packet);
	}
}
