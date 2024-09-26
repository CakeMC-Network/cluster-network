package net.cakemc.cluster.endpoint.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import net.cakemc.cluster.packet.Packet;
import net.cakemc.cluster.packet.PacketRegistry;

import java.util.List;

public class PacketCodec extends ByteToMessageCodec<Packet> {

	private final PacketRegistry packetRegistry;

	public PacketCodec(PacketRegistry packetRegistry) {
		this.packetRegistry = packetRegistry;
	}

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {
		byteBuf.writeInt(packet.id());
		packet.write(byteBuf);
	}

	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
		Packet packet = packetRegistry.getPacketById(byteBuf.readInt());
		packet.read(byteBuf);

		list.add(packet);
	}

}
