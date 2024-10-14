package net.cakemc.library.cluster.fallback.endpoint.packet.impl;

import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

/**
 * The {@code ClusterHelloBackPacket} class represents a packet used for
 * establishing communication between nodes in a cluster.
 *
 * <p>This packet contains information about the sender node, facilitating
 * the identification of nodes within the cluster during the handshake process.</p>
 */
public class ClusterHelloBackPacket extends RingBackPacket {

	/** The ID of the node sending the hello packet. */
	private int helloFrom;

	/**
	 * Default constructor for deserialization.
	 */
	public ClusterHelloBackPacket() {
	}

	/**
	 * Constructs a new {@code ClusterHelloBackPacket} with the specified parameters.
	 *
	 * @param packetInstanceAddress the address of the packet instance
	 * @param nodeWriteAddress the write address of the node
	 * @param targetNode the ID of the target node for this packet
	 * @param helloFrom the ID of the node sending this packet
	 */
	public ClusterHelloBackPacket(long packetInstanceAddress, long nodeWriteAddress, int targetNode, int helloFrom) {
		super(packetInstanceAddress, nodeWriteAddress, targetNode);
		this.helloFrom = helloFrom;
	}

	/**
	 * Writes the contents of this packet to the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to write packet data to
	 */
	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeInt(helloFrom);
	}

	/**
	 * Reads the contents of this packet from the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to read packet data from
	 */
	@Override
	public void readPacket(ByteBuf byteBuf) {
		this.helloFrom = byteBuf.readInt();
	}

	/**
	 * Returns the unique identifier for this packet type.
	 *
	 * @return the packet ID
	 */
	@Override
	public int id() {
		return 1;
	}

	/**
	 * Returns the ID of the node that sent this hello packet.
	 *
	 * @return the sender's node ID
	 */
	public int getHelloFrom() {
		return helloFrom;
	}
}
