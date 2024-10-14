package net.cakemc.library.cluster.fallback.endpoint.packet.impl;

import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

/**
 * The {@code PongBackPacket} class represents a response packet sent in
 * response to a {@link PingBackPacket}. This packet contains the timestamp
 * that indicates how long it took to receive the ping.
 *
 * <p>This packet is used for measuring latency and confirming the
 * reachability of nodes in a cluster.</p>
 */
public class PongBackPacket extends RingBackPacket {

	/** The time at which the pong was received, in milliseconds. */
	private long time;

	/**
	 * Constructs a new {@code PongBackPacket} with the specified parameters.
	 *
	 * @param packetInstanceAddress the address of the packet instance
	 * @param nodeWriteAddress the write address of the node
	 * @param targetNode the ID of the target node for this packet
	 * @param time the time at which this pong was sent
	 */
	public PongBackPacket(long packetInstanceAddress, long nodeWriteAddress, int targetNode, long time) {
		super(packetInstanceAddress, nodeWriteAddress, targetNode);
		this.time = time;
	}

	/**
	 * Default constructor for deserialization.
	 */
	public PongBackPacket() {
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
		return 3; // Note: This ID conflicts with PingBackPacket, consider updating.
	}

	/**
	 * Returns the time at which the pong was sent.
	 *
	 * @return the timestamp in milliseconds
	 */
	public long getTime() {
		return time;
	}
}
