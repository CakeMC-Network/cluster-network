package net.cakemc.library.cluster.fallback.endpoint.packet.ring;

import io.netty.buffer.ByteBuf;

/**
 * The {@code RequestBackPacket} class is an abstract representation of a packet
 * that requests an operation within the cluster. It extends the {@link RingBackPacket}
 * class and provides mechanisms to read and write request-specific data to and from
 * a {@code ByteBuf}.
 *
 * <p>This class includes a unique request ID that helps in identifying and
 * correlating requests and responses in the communication process.</p>
 */
public abstract class RequestBackPacket extends RingBackPacket {

	/** The unique identifier for this request. */
	private int requestId;

	/**
	 * Constructs a new {@code RequestBackPacket} with a generated request ID.
	 */
	public RequestBackPacket() {
		this.requestId = generateRequestId();
	}

	/**
	 * Constructs a new {@code RequestBackPacket} with the specified request ID.
	 *
	 * @param requestId the unique identifier for this request
	 */
	public RequestBackPacket(int requestId) {
		this.requestId = requestId;
	}

	/**
	 * Reads the contents of this request packet from the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to read packet data from
	 */
	@Override
	public void readPacket(ByteBuf byteBuf) {
		this.requestId = byteBuf.readInt();
		readRequest(byteBuf);
	}

	/**
	 * Writes the contents of this request packet to the provided {@code ByteBuf}.
	 *
	 * @param byteBuf the buffer to write packet data to
	 */
	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeInt(requestId);
		writeRequest(byteBuf);
	}

	/**
	 * Returns the unique request identifier for this packet.
	 *
	 * @return the request ID
	 */
	public int getRequestId() {
		return requestId;
	}

	/**
	 * Reads additional request-specific data from the provided {@code ByteBuf}.
	 *
	 * <p>This method must be implemented by subclasses to define how to read
	 * specific request data.</p>
	 *
	 * @param byteBuf the buffer to read request-specific data from
	 */
	protected abstract void readRequest(ByteBuf byteBuf);

	/**
	 * Writes additional request-specific data to the provided {@code ByteBuf}.
	 *
	 * <p>This method must be implemented by subclasses to define how to write
	 * specific request data.</p>
	 *
	 * @param byteBuf the buffer to write request-specific data to
	 */
	protected abstract void writeRequest(ByteBuf byteBuf);

	/**
	 * Generates a unique request ID based on the current system time.
	 *
	 * @return the generated request ID
	 */
	private int generateRequestId() {
		return (int) (System.currentTimeMillis() & 0xFFFF);
	}
}
