package net.cakemc.library.cluster.fallback.endpoint.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import net.cakemc.library.cluster.codec.ClusterPublication;
import net.cakemc.library.cluster.codec.Publication;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * A codec that encodes and decodes {@link Publication} objects for network transmission.
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
 * pipeline.addLast(new PublicationCodec(packetRegistry));
 * // Other handlers...
 * }
 * </pre>
 *
 * @see ByteToMessageCodec
 * @see Publication
 */
public class PublicationCodec extends ByteToMessageCodec<Publication> {

	public static Class<? extends Publication> publicationType;

	public PublicationCodec() {
		publicationType = ClusterPublication.class;
	}

	/**
	 * Encodes the specified {@link Publication} into the given {@link ByteBuf}.
	 *
	 * <p>This method writes the backPacket's ID followed by its serialized data to the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code PublicationCodec} belongs to
	 * @param backPacket            the {@link Publication} to encode
	 * @param byteBuf               the {@link ByteBuf} where the encoded backPacket data will be written
	 *
	 */
	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Publication backPacket, ByteBuf byteBuf) {
		backPacket.serialize(byteBuf);
	}

	/**
	 * Decodes the incoming data from the specified {@link ByteBuf} into a {@link Publication}.
	 *
	 * <p>This method reads the packet ID from the byte buffer, retrieves the corresponding
	 * packet class from the packet registry, and populates the packet with data read from
	 * the byte buffer.</p>
	 *
	 * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@code PublicationCodec} belongs to
	 * @param byteBuf               the incoming {@link ByteBuf} containing the encoded packet data
	 * @param list                  the list where the decoded packet will be added
	 *
	 * @throws Exception if an error occurs during decoding
	 */
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
		if (publicationType == null) {
			throw new IllegalStateException("unable to construct incoming publication clusterPublicationClass is null!");
		}

		try {
			Publication publication = (Publication) MethodHandles
				 .privateLookupIn(publicationType, MethodHandles.publicLookup())
				 .unreflectConstructor(publicationType.getDeclaredConstructor())
				 .invokeExact();

			publication.deserialize(byteBuf);

			list.add(publication);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

}
