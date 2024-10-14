package net.cakemc.library.cluster.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The {@code SoftPublication} class represents a publication within a ring-back packet communication system.
 * This class extends the {@link RingBackPacket} and implements the {@link Publication} interface,
 * enabling it to represent and handle a publish-subscribe style message with a channel, data, and version information.
 */
public class SoftPublication extends RingBackPacket implements Publication {

	private String channel;
	private byte[] data;
	private int version;

	/**
	 * Default constructor for creating an empty {@code SoftPublication} instance.
	 */
	public SoftPublication() {
	}

	/**
	 * Constructs a {@code SoftPublication} instance with the specified packet details and publication data.
	 *
	 * @param packetInstanceAddress the address of the packet instance.
	 * @param nodeWriteAddress      the write address of the node.
	 * @param targetNode            the target node identifier.
	 * @param channel               the publication channel name.
	 * @param data                  the data being published as a byte array.
	 * @param version               the version of the publication.
	 */
	public SoftPublication(
		 long packetInstanceAddress,
		 long nodeWriteAddress,
		 int targetNode,
		 String channel, byte[] data,
		 int version
	) {
		super(packetInstanceAddress, nodeWriteAddress, targetNode);
		this.channel = channel;
		this.data = data;
		this.version = version;
	}

	/**
	 * Retrieves the key associated with this publication, which is the channel name.
	 *
	 * @return the channel name as the key.
	 */
	@Override
	public String getKey() {
		return channel;
	}

	/**
	 * Retrieves the version of the publication.
	 *
	 * @return the publication version.
	 */
	@Override
	public long getVersion() {
		return version;
	}

	/**
	 * Closes the publication by clearing the data and channel references.
	 */
	@Override
	public void close() {
		this.data = null;
		this.channel = null;
	}

	/**
	 * Configures the publication using the provided configuration map.
	 * This method does not currently perform any operations.
	 *
	 * @param config the configuration map.
	 */
	@Override
	public void configure(Map<String, ?> config) {}

	/**
	 * Deserializes the publication data from the provided byte array.
	 *
	 * @param in the input byte array containing serialized publication data.
	 */
	@Override
	public void deserialize(byte[] in) {
		ByteBuf byteBuf = Unpooled.copiedBuffer(in);

		this.version = byteBuf.readInt();

		byte[] channel = new byte[byteBuf.readInt()];
		byteBuf.readBytes(channel);
		this.channel = new String(channel);

		byte[] data = new byte[byteBuf.readInt()];
		byteBuf.readBytes(data);
		this.data = data;

		byteBuf.release();
	}

	/**
	 * Serializes the publication data into a byte array.
	 *
	 * @return the serialized byte array containing publication data.
	 */
	@Override
	public byte[] serialize() {
		ByteBuf byteBuf = Unpooled.buffer();

		byteBuf.writeInt(version);

		byteBuf.writeInt(this.channel.length());
		byteBuf.writeBytes(this.channel.getBytes(StandardCharsets.UTF_8));

		byteBuf.writeInt(this.data.length);
		byteBuf.writeBytes(this.data);

		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);

		byteBuf.release();
		return data;
	}

	/**
	 * Writes the publication data into the provided {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} to write the publication data into.
	 */
	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeInt(version);

		byteBuf.writeInt(this.channel.length());
		byteBuf.writeBytes(this.channel.getBytes(StandardCharsets.UTF_8));

		byteBuf.writeInt(this.data.length);
		byteBuf.writeBytes(this.data);
	}

	/**
	 * Reads the publication data from the provided {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} containing the serialized publication data.
	 */
	@Override
	public void readPacket(ByteBuf byteBuf) {
		this.version = byteBuf.readInt();

		byte[] channel = new byte[byteBuf.readInt()];
		byteBuf.readBytes(channel);
		this.channel = new String(channel);

		byte[] data = new byte[byteBuf.readInt()];
		byteBuf.readBytes(data);
		this.data = data;
	}

	/**
	 * Returns the unique identifier for the packet.
	 *
	 * @return the packet ID as an integer (300).
	 */
	@Override
	public int id() {
		return 300;
	}

	/**
	 * Retrieves the publication channel name.
	 *
	 * @return the channel name.
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * Retrieves the publication data.
	 *
	 * @return the data as a byte array.
	 */
	public byte[] getData() {
		return data;
	}
}
