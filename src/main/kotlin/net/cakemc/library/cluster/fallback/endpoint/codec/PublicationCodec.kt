package net.cakemc.library.cluster.fallback.endpoint.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import net.cakemc.library.cluster.codec.*
import java.lang.invoke.MethodHandles

/**
 * A codec that encodes and decodes [Publication] objects for network transmission.
 *
 * <h2>Encoding</h2>
 *
 * When encoding a packet, this codec writes the packet's ID followed by its data to
 * the provided [ByteBuf].
 *
 * <h2>Decoding</h2>
 *
 * During decoding, the codec reads the packet ID from the byte buffer, retrieves the
 * corresponding packet class from the packet registry, and reads the data into a packet object.
 *
 * <h2>Example Usage</h2>
 * <pre>
 * `ChannelPipeline pipeline = channel.pipeline();
 * pipeline.addLast(new PublicationCodec(packetRegistry));
 * // Other handlers...
` *
</pre> *
 *
 * @see ByteToMessageCodec
 *
 * @see Publication
 */
class PublicationCodec : ByteToMessageCodec<Publication>() {
    init {
        publicationType = ClusterPublication::class.java
    }

    /**
     * Encodes the specified [Publication] into the given [ByteBuf].
     *
     *
     * This method writes the backPacket's ID followed by its serialized data to the byte buffer.
     *
     * @param channelHandlerContext the [ChannelHandlerContext] which this `PublicationCodec` belongs to
     * @param backPacket            the [Publication] to encode
     * @param byteBuf               the [ByteBuf] where the encoded backPacket data will be written
     */
    override fun encode(channelHandlerContext: ChannelHandlerContext, backPacket: Publication, byteBuf: ByteBuf) {
        backPacket.serialize(byteBuf)
    }

    /**
     * Decodes the incoming data from the specified [ByteBuf] into a [Publication].
     *
     *
     * This method reads the packet ID from the byte buffer, retrieves the corresponding
     * packet class from the packet registry, and populates the packet with data read from
     * the byte buffer.
     *
     * @param channelHandlerContext the [ChannelHandlerContext] which this `PublicationCodec` belongs to
     * @param byteBuf               the incoming [ByteBuf] containing the encoded packet data
     * @param list                  the list where the decoded packet will be added
     *
     * @throws Exception if an error occurs during decoding
     */
    @Throws(Exception::class)
    override fun decode(channelHandlerContext: ChannelHandlerContext, byteBuf: ByteBuf, list: MutableList<Any>) {
        checkNotNull(publicationType) { "unable to construct incoming publication clusterPublicationClass is null!" }

        try {
            val publication = MethodHandles
                .privateLookupIn(publicationType, MethodHandles.publicLookup())
                .unreflectConstructor(publicationType!!.getDeclaredConstructor())
                .invokeExact() as Publication

            publication.deserialize(byteBuf)

            list.add(publication)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    companion object {
        var publicationType: Class<out Publication>? = null
    }
}
