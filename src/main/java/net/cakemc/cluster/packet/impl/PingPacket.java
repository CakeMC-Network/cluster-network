package net.cakemc.cluster.packet.impl;

import io.netty.buffer.ByteBuf;
import net.cakemc.cluster.packet.ring.RingPacket;

/**
 * The {@code PingPacket} class represents a packet used for measuring
 * latency between nodes in a cluster.
 *
 * <p>This packet includes a timestamp that can be used to calculate
 * the round-trip time (RTT) for network communication.</p>
 */
public class PingPacket extends RingPacket {

	/** The time at which the ping was sent, in milliseconds. */
	private long time;

	/**
	 * Constructs a new {@code PingPacket} with the specified parameters.
	 *
	 * @param packetInstanceAddress the address of the packet instance
	 * @param nodeWriteAddress the write address of the node
	 * @param targetNode the ID of the target node for this packet
	 * @param time the time at which this ping was sent
	 */
	public PingPacket(long packetInstanceAddress, long nodeWriteAddress, int targetNode, long time) {
		super(packetInstanceAddress, nodeWriteAddress, targetNode);
		this.time = time;
	}

	/**
	 * Default constructor for deserialization.
	 */
	public PingPacket() {
	}

	/**
	 * Writes the contents of this packet to the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to write packet data to
	 */
	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeLong(time);
	}

	/**
	 * Reads the contents of this packet from the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to read packet data from
	 */
	@Override
	public void readPacket(ByteBuf byteBuf) {
		time = byteBuf.readLong();
	}

	/**
	 * Returns the unique identifier for this packet type.
	 *
	 * @return the packet ID
	 */
	@Override
	public int id() {
		return 3;
	}

	/**
	 * Returns the time at which the ping was sent.
	 *
	 * @return the timestamp in milliseconds
	 */
	public long getTime() {
		return time;
	}
}
