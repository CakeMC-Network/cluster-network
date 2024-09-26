package net.cakemc.cluster.packet.impl;

import io.netty.buffer.ByteBuf;
import net.cakemc.cluster.packet.Packet;

public class ClusterCommandPacket extends Packet {
	@Override
	public void read(ByteBuf byteBuf) {

	}

	@Override
	public void write(ByteBuf byteBuf) {

	}

	@Override
	public int id() {
		return 3;
	}
}
