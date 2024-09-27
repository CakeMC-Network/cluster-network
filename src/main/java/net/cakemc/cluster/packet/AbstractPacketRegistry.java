package net.cakemc.cluster.packet;

/**
 * The {@code AbstractPacketRegistry} class serves as a base for managing the
 * registration and retrieval of packets within a network cluster. This class
 * defines the contract for packet registry implementations, allowing packets to
 * be retrieved by their unique identifiers and vice versa.
 */
public abstract class AbstractPacketRegistry {

	/**
	 * Retrieves a packet instance corresponding to the specified identifier.
	 *
	 * @param id the unique identifier of the packet
	 * @return the packet associated with the given identifier, or {@code null}
	 *         if no packet is found for the specified identifier
	 */
	public abstract Packet getPacketById(int id);

	/**
	 * Retrieves the unique identifier associated with the specified packet.
	 *
	 * @param packet the packet instance for which to find the identifier
	 * @return the unique identifier of the specified packet, or {@code -1}
	 *         if the packet is not registered
	 */
	public abstract int getIdByPacket(Packet packet);
}
