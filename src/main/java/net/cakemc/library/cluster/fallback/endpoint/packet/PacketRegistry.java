package net.cakemc.library.cluster.fallback.endpoint.packet;

import net.cakemc.library.cluster.api.SoftPublication;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.ClusterHelloBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.PingBackPacket;
import net.cakemc.library.cluster.fallback.endpoint.packet.impl.PongBackPacket;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The {@code PacketRegistry} class is responsible for managing the registration
 * and retrieval of packet instances within the network cluster framework.
 * It provides functionality to obtain backPackets by their unique identifier and
 * to retrieve the list of all registered backPackets.
 */
public class PacketRegistry extends AbstractPacketRegistry {

	/**
	 * A list of all registered packet instances in the registry.
	 */
	public final List<BackPacket> backPackets = List.of(
		 new ClusterHelloBackPacket(),
		 // Keepalive backPackets
		 new PingBackPacket(),
		 new PongBackPacket(),

		 // other
		 new SoftPublication()
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
	 * @return the {@link BackPacket} instance corresponding to the specified id, or
	 *         {@code null} if no packet with the given id exists
	 * @throws RuntimeException if there is an error instantiating the packet
	 */
	@Override
	public BackPacket getPacketById(int id) {
		for (BackPacket backPacket : this.backPackets) {
			if (backPacket.id() != id) continue;

			Class<? extends BackPacket> packetClass = backPacket.getClass();
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
	 * Retrieves the unique identifier for the specified backPacket.
	 *
	 * @param backPacket the {@link BackPacket} instance for which to retrieve the id
	 * @return the unique identifier of the specified backPacket
	 */
	@Override
	public int getIdByPacket(BackPacket backPacket) {
		return backPacket.id();
	}

	/**
	 * Returns the list of all registered backPackets.
	 *
	 * @return a list of {@link BackPacket} instances
	 */
	public List<BackPacket> getPackets() {
		return backPackets;
	}
}
