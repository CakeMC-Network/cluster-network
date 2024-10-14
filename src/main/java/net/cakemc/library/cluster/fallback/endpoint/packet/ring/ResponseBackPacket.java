package net.cakemc.library.cluster.fallback.endpoint.packet.ring;

import io.netty.buffer.ByteBuf;

/**
 * The {@code ResponseBackPacket} class is an abstract representation of a packet
 * that responds to a request within the cluster. It extends the {@link RingBackPacket}
 * class and provides mechanisms to read and write response-specific data to and from
 * a {@code ByteBuf}.
 *
 * <p>This class includes a request ID that correlates the response to its original request.</p>
 */
public abstract class ResponseBackPacket extends RingBackPacket {

	/** The unique identifier for the original request this response corresponds to. */
	private int requestId;

	/**
	 * Constructs a new {@code ResponseBackPacket} with no associated request ID.
	 */
	public ResponseBackPacket() {}

	/**
	 * Constructs a new {@code ResponseBackPacket} with the specified request ID.
	 *
	 * @param requestId the unique identifier for the original request
	 */
	public ResponseBackPacket(int requestId) {
		this.requestId = requestId;
	}

	/**
	 * Reads the contents of this response packet from the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to read packet data from
	 */
	@Override
	public void readPacket(ByteBuf byteBuf) {
		this.requestId = byteBuf.readInt();
		readReply(byteBuf);
	}

	/**
	 * Writes the contents of this response packet to the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to write packet data to
	 */
	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeInt(requestId);
		writeReply(byteBuf);
	}

	/**
	 * Reads additional response-specific data from the provided {@code ByteBuf}.
	 *
	 * <p>This method must be implemented by subclasses to define how to read
	 * specific response data.</p>
	 *
	 * @param byteBuf the buffer to read response-specific data from
	 */
	protected abstract void readReply(ByteBuf byteBuf);

	/**
	 * Writes additional response-specific data to the provided {@code ByteBuf}.
	 *
	 * <p>This method must be implemented by subclasses to define how to write
	 * specific response data.</p>
	 *
	 * @param byteBuf the buffer to write response-specific data to
	 */
	protected abstract void writeReply(ByteBuf byteBuf);

	/**
	 * Returns the unique request identifier for the original request that this
	 * response corresponds to.
	 *
	 * @return the request ID
	 */
	public int getRequestId() {
		return requestId;
	}
}
