package net.cakemc.library.cluster.fallback.endpoint.packet.ring;

import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.fallback.endpoint.packet.BackPacket;

/**
 * The {@code RingBackPacket} class is an abstract representation of a packet that
 * is part of a ring network structure in a cluster. It extends the {@link BackPacket}
 * class and includes additional fields for handling packet metadata such as
 * instance addresses, node write addresses, distribution time, and target node.
 */
public abstract class RingBackPacket extends BackPacket {

	/** The unique address for this packet instance. */
	private long packetInstanceAddress;

	/** The address of the node that wrote this packet. */
	private long nodeWriteAddress;

	/** The timestamp indicating when this packet was distributed. */
	private long distributionTime;

	/** The target node for this packet. */
	private int targetNode;

	/**
	 * Constructs a new {@code RingBackPacket} with specified addresses and target node.
	 *
	 * @param packetInstanceAddress the unique address for this packet instance
	 * @param nodeWriteAddress the address of the node that wrote this packet
	 * @param targetNode the target node for this packet
	 */
	public RingBackPacket(long packetInstanceAddress, long nodeWriteAddress, int targetNode) {
		this.packetInstanceAddress = packetInstanceAddress;
		this.nodeWriteAddress = nodeWriteAddress;
		this.distributionTime = System.currentTimeMillis();
		this.targetNode = targetNode;
	}

	/**
	 * Constructs a new {@code RingBackPacket} with default values.
	 */
	public RingBackPacket() {}

	/**
	 * Reads the contents of this ring packet from the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to read packet data from
	 */
	@Override
	public void read(ByteBuf byteBuf) {
		this.packetInstanceAddress = byteBuf.readLong();
		this.nodeWriteAddress = byteBuf.readLong();
		this.distributionTime = byteBuf.readLong();
		this.targetNode = byteBuf.readInt();

		this.readPacket(byteBuf);
	}

	/**
	 * Writes the contents of this ring packet to the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to write packet data to
	 */
	@Override
	public void write(ByteBuf byteBuf) {
		byteBuf.writeLong(packetInstanceAddress);
		byteBuf.writeLong(nodeWriteAddress);
		byteBuf.writeLong(distributionTime);
		byteBuf.writeInt(targetNode);

		this.writePacket(byteBuf);
	}

	/**
	 * Writes additional data specific to the implementation of this packet.
	 *
	 * <p>This method must be implemented by subclasses to define how to write
	 * packet-specific data.</p>
	 *
	 * @param byteBuf the buffer to write packet-specific data to
	 */
	public abstract void writePacket(ByteBuf byteBuf);

	/**
	 * Reads additional data specific to the implementation of this packet.
	 *
	 * <p>This method must be implemented by subclasses to define how to read
	 * packet-specific data.</p>
	 *
	 * @param byteBuf the buffer to read packet-specific data from
	 */
	public abstract void readPacket(ByteBuf byteBuf);

	/**
	 * Returns the unique address for this packet instance.
	 *
	 * @return the packet instance address
	 */
	public long getPacketInstanceAddress() {
		return packetInstanceAddress;
	}

	/**
	 * Returns the address of the node that wrote this packet.
	 *
	 * @return the node write address
	 */
	public long getNodeWriteAddress() {
		return nodeWriteAddress;
	}

	/**
	 * Returns the timestamp indicating when this packet was distributed.
	 *
	 * @return the distribution time
	 */
	public long getDistributionTime() {
		return distributionTime;
	}

	/**
	 * Returns the target node for this packet.
	 *
	 * @return the target node
	 */
	public int getTargetNode() {
		return targetNode;
	}
}
