package net.cakemc.library.cluster.api;

import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.codec.Publication;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The {@code SoftPublication} class represents a publication within a ring-back packet communication system.
 * This class implements the {@link Publication} interface,
 * enabling it to represent and handle a publish-subscribe style message with a channel, data, and version information.
 */
public class SoftPublication implements Publication {

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
	 * @param channel the publication channel name.
	 * @param data    the data being published as a byte array.
	 * @param version the version of the publication.
	 */
	public SoftPublication(
		 String channel, byte[] data,
		 int version
	) {
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
	 * Writes the publication data into the provided {@link ByteBuf}.
	 *
	 * @param byteBuf the {@link ByteBuf} to write the publication data into.
	 */
	@Override
	public void serialize(ByteBuf byteBuf) {
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
	public void deserialize(ByteBuf byteBuf) {
		this.version = byteBuf.readInt();

		byte[] channel = new byte[byteBuf.readInt()];
		byteBuf.readBytes(channel);
		this.channel = new String(channel);

		byte[] data = new byte[byteBuf.readInt()];
		byteBuf.readBytes(data);
		this.data = data;
	}


	/**
	 * Retrieves the publication channel name.
	 *
	 * @return the channel name.
	 */
	@Override
	public String getChannel() {
		return channel;
	}

	@Override public void setChannel(String channel) {
		this.channel = channel;
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
