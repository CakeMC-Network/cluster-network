package net.cakemc.library.cluster.fallback.endpoint.packet;

/**
 * The {@code AbstractPacketRegistry} class serves as a base for managing the
 * registration and retrieval of backPackets within a network cluster. This class
 * defines the contract for packet registry implementations, allowing backPackets to
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
	public abstract BackPacket getPacketById(int id);

	/**
	 * Retrieves the unique identifier associated with the specified backPacket.
	 *
	 * @param backPacket the backPacket instance for which to find the identifier
	 * @return the unique identifier of the specified backPacket, or {@code -1}
	 *         if the backPacket is not registered
	 */
	public abstract int getIdByPacket(BackPacket backPacket);
}
