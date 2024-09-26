package net.cakemc.cluster.packet;

import net.cakemc.cluster.packet.impl.ClusterAuthPacket;
import net.cakemc.cluster.packet.impl.ClusterCommandPacket;
import net.cakemc.cluster.packet.impl.ClusterStatusPacket;

import java.util.List;

public class PacketRegistry {

	public final List<Packet> packets = List.of(
		 new ClusterAuthPacket(),
		 new ClusterStatusPacket(),
		 new ClusterCommandPacket()
	);

	public PacketRegistry() {
	}

	public Packet getPacketById(int id) {
		return packets.stream()
		              .filter(packet -> packet.id() == id)
		              .findFirst()
		              .orElse(null);
	}

	public int getIdByPacket(Packet packet) {
		return packet.id();
	}

	public List<Packet> getPackets() {
		return packets;
	}
}
