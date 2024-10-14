package net.cakemc.library.cluster.fallback.endpoint.packet;

import io.netty.buffer.ByteBuf;

/**
 * The {@code BackPacket} class serves as an abstract base class for all packet
 * implementations within the network cluster framework. It defines the
 * essential methods for reading and writing packet data to and from a
 * {@link ByteBuf}, as well as retrieving the packet's unique identifier.
 */
public abstract class BackPacket {

	/**
	 * Reads packet data from the specified {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} from which to read the packet data
	 */
	public abstract void read(ByteBuf byteBuf);

	/**
	 * Writes packet data to the specified {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} to which the packet data will be written
	 */
	public abstract void write(ByteBuf byteBuf);

	/**
	 * Returns the unique identifier for this packet.
	 *
	 * @return the unique identifier of the packet
	 */
	public abstract int id();
}
