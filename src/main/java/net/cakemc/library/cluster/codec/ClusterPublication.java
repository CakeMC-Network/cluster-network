package net.cakemc.library.cluster.codec;

import net.cakemc.library.cluster.address.ClusterAddress;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.Command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a publication in the cluster framework, encapsulating information
 * required for communication between cluster members.
 *
 * <p>The class implements the {@link Publication} interface and provides methods
 * for serialization and deserialization of publication data, including synchronization
 * addresses and authentication information.</p>
 */
public class ClusterPublication implements Publication {

	private short id = -1; // Unique identifier for the publication
	private boolean authByKey = true; // Indicates whether authentication is by key
	private String authKey = ""; // Authentication key, if required
	private long version = 0; // Version of the publication
	private Set<ClusterAddress> syncAddresses = new HashSet<>(); // Set of synchronization addresses
	private Command command = Command.COMMAND_OK; // Command byte for the publication

	/**
	 * Default constructor initializing a new instance of {@link ClusterPublication}.
	 */
	public ClusterPublication() {
	}

	/**
	 * Constructs a new {@link ClusterPublication} with the specified parameters.
	 *
	 * @param id             the unique identifier for this publication
	 * @param authByKey     whether the publication is authenticated by key
	 * @param key            the authentication key
	 * @param version        the version of the publication
	 * @param syncAddresses  a set of synchronization addresses
	 * @param command        the command byte for this publication
	 */
	public ClusterPublication(short id, boolean authByKey, String key, long version,
	                          Set<ClusterAddress> syncAddresses, Command command) {
		this.id = id;
		this.authByKey = authByKey;
		this.authKey = key;
		this.version = version;
		this.syncAddresses = syncAddresses != null ? syncAddresses : new HashSet<>();
		this.command = command;
	}

	/**
	 * Gets the unique identifier of this publication.
	 *
	 * @return the unique identifier of the publication
	 */
	public short getId() {
		return id;
	}

	/**
	 * Sets the unique identifier for this publication.
	 *
	 * @param id the unique identifier to set
	 */
	public void setId(short id) {
		this.id = id;
	}

	/**
	 * Checks if the publication is authenticated by a key.
	 *
	 * @return {@code true} if authenticated by key; {@code false} otherwise
	 */
	public boolean isAuthByKey() {
		return authByKey;
	}

	/**
	 * Sets whether the publication should be authenticated by key.
	 *
	 * @param authByKey {@code true} to authenticate by key; {@code false} otherwise
	 */
	public void setAuthByKey(boolean authByKey) {
		this.authByKey = authByKey;
	}

	/**
	 * Gets the authentication key for this publication.
	 *
	 * @return the authentication key
	 */
	public String getAuthKey() {
		return authKey;
	}

	/**
	 * Sets the authentication key for this publication.
	 *
	 * @param key the authentication key to set
	 */
	public void setAuthKey(String key) {
		this.authKey = key;
	}

	/**
	 * Gets the set of synchronization addresses for this publication.
	 *
	 * @return a set of {@link ClusterAddress} objects representing synchronization addresses
	 */
	public Set<ClusterAddress> getSyncAddresses() {
		return syncAddresses;
	}

	/**
	 * Sets the synchronization addresses for this publication.
	 *
	 * @param syncAddresses the set of synchronization addresses to set
	 */
	public void setSyncAddresses(Set<ClusterAddress> syncAddresses) {
		this.syncAddresses = syncAddresses != null ? syncAddresses : new HashSet<>();
	}

	/**
	 * Gets the command byte for this publication.
	 *
	 * @return the command byte
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Sets the command byte for this publication.
	 *
	 * @param command the command byte to set
	 */
	public void setCommand(Command command) {
		this.command = command;
	}

	/**
	 * Serializes this publication into a byte array for transmission.
	 *
	 * @return a byte array representing the serialized publication, or {@code null} if an error occurs
	 */
	@Override
	public byte[] serialize() {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
		     DataOutputStream out = new DataOutputStream(stream)) {

			out.writeShort(id);
			out.writeBoolean(authByKey);
			out.writeUTF(authKey);
			out.writeLong(version);
			out.writeByte(command.ordinal());

			// Write the size of syncAddresses
			out.writeByte(syncAddresses.size());
			for (ClusterAddress addr : syncAddresses) {
				byte[] ipBytes = addr.getAddress().getAddress();
				out.writeByte(ipBytes.length);
				out.write(ipBytes);
				out.writeInt(addr.getPort());
			}

			return stream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace(); // Consider using a logger instead
			return null; // You might want to throw a specific exception instead
		}
	}

	/**
	 * Closes resources associated with this publication.
	 *
	 * <p>This method can be implemented to release any resources held by this
	 * publication, if necessary.</p>
	 */
	@Override
	public void close() {
		// Implement close logic if needed
	}

	/**
	 * Configures this publication with the given parameters.
	 *
	 * @param config a map of configuration parameters
	 */
	@Override
	public void configure(Map<String, ?> config) {
		// Implement configuration logic if needed
	}

	/**
	 * Deserializes this publication from the given byte array.
	 *
	 * @param data the byte array containing serialized publication data
	 */
	@Override
	public void deserialize(byte[] data) {
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			id = in.readShort();
			authByKey = in.readBoolean();
			authKey = in.readUTF();
			version = in.readLong();
			command = Command.values()[in.readByte()];

			int len = in.readByte();
			syncAddresses = new HashSet<>(); // Initialize the set to avoid null

			for (int i = 0; i < len; i++) {
				byte ipLength = in.readByte();
				byte[] ip = new byte[ipLength];
				in.readFully(ip); // Use readFully to ensure all bytes are read
				int port = in.readInt();
				syncAddresses.add(new ClusterAddress(InetAddress.getByAddress(ip), port));
			}
		} catch (Exception e) {
			e.printStackTrace(); // Consider using a logger instead
		}
	}

	/**
	 * Gets the key for this publication, which is based on its unique identifier.
	 *
	 * @return the key for this publication
	 */
	@Override
	public String getKey() {
		return String.valueOf(id);
	}

	/**
	 * Gets the version of this publication.
	 *
	 * @return the version of the publication
	 */
	@Override
	public long getVersion() {
		return version;
	}

	/**
	 * Sets the version for this publication.
	 *
	 * @param version the version to set
	 */
	public void setVersion(long version) {
		this.version = version;
	}
}
