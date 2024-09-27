package net.cakemc.cluster.packet;

import net.cakemc.cluster.packet.impl.ClusterHelloPacket;
import net.cakemc.cluster.packet.impl.PingPacket;
import net.cakemc.cluster.packet.impl.PongPacket;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The {@code PacketRegistry} class is responsible for managing the registration
 * and retrieval of packet instances within the network cluster framework.
 * It provides functionality to obtain packets by their unique identifier and
 * to retrieve the list of all registered packets.
 */
public class PacketRegistry extends AbstractPacketRegistry {

	/**
	 * A list of all registered packet instances in the registry.
	 */
	public final List<Packet> packets = List.of(
		 new ClusterHelloPacket(),
		 // Keepalive packets
		 new PingPacket(),
		 new PongPacket()
	);

	/**
	 * Constructs a new {@code PacketRegistry} instance.
	 */
	public PacketRegistry() {
	}

	/**
	 * Retrieves a packet instance by its unique identifier.
	 *
	 * @param id the unique identifier of the packet
	 * @return the {@link Packet} instance corresponding to the specified id, or
	 *         {@code null} if no packet with the given id exists
	 * @throws RuntimeException if there is an error instantiating the packet
	 */
	@Override
	public Packet getPacketById(int id) {
		for (Packet packet : this.packets) {
			if (packet.id() != id) continue;

			Class<? extends Packet> packetClass = packet.getClass();
			try {
				return packetClass.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException |
			         InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	/**
	 * Retrieves the unique identifier for the specified packet.
	 *
	 * @param packet the {@link Packet} instance for which to retrieve the id
	 * @return the unique identifier of the specified packet
	 */
	@Override
	public int getIdByPacket(Packet packet) {
		return packet.id();
	}

	/**
	 * Returns the list of all registered packets.
	 *
	 * @return a list of {@link Packet} instances
	 */
	public List<Packet> getPackets() {
		return packets;
	}
}
