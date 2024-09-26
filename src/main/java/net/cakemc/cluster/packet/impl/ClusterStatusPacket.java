package net.cakemc.cluster.packet.impl;

import io.netty.buffer.ByteBuf;
import net.cakemc.cluster.NodeAddress;
import net.cakemc.cluster.info.ClusterInformation;
import net.cakemc.cluster.info.NodeStatus;
import net.cakemc.cluster.packet.Packet;

import java.nio.charset.StandardCharsets;

public class ClusterStatusPacket extends Packet {

	private ClusterInformation[] informations;

	public ClusterStatusPacket(ClusterInformation[] informations) {
		this.informations = informations;
	}

	public ClusterStatusPacket() {
	}

	@Override
	public void read(ByteBuf byteBuf) {
		this.informations = new ClusterInformation[byteBuf.readInt()];
		for (int i = 0 ; i < informations.length ; i++) {
			NodeAddress nodeAddress = new NodeAddress(
				 byteBuf.readLong(),
				 (String) byteBuf.readCharSequence(0, StandardCharsets.UTF_8),
				 byteBuf.readShort()
			);
			NodeStatus nodeStatus = NodeStatus.values()[byteBuf.readInt()];
			informations[i] = (new ClusterInformation(nodeAddress, nodeStatus));
		}
	}

	@Override
	public void write(ByteBuf byteBuf) {
		byteBuf.writeInt(informations.length);
		for (ClusterInformation information : informations) {
			byteBuf.writeLong(information.getAddress().id());
			byteBuf.writeCharSequence(information.getAddress().host(), StandardCharsets.UTF_8);
			byteBuf.writeShort(information.getAddress().port());
			byteBuf.writeByte(information.getStatus().ordinal());
		}
	}

	@Override
	public int id() {
		return 2;
	}

	public ClusterInformation[] getInformations() {
		return informations;
	}

}
