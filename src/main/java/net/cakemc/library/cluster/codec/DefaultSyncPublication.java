package net.cakemc.library.cluster.codec;

import net.cakemc.library.cluster.SynchronisationType;
import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.io.IOException;
import java.util.*;

/**
 * Represents a synchronization message within the cluster framework.
 *
 * <p>The {@code DefaultSyncPublication} class encapsulates various synchronization details,
 * including the message type, command, synchronization mode, and the content to be synchronized.</p>
 */
public class DefaultSyncPublication implements SyncPublication {

	/**
	 * Enum representing the different message types that can be sent in a synchronization message.
	 */
	public enum MessageType {
		TYPE_BAD_KEY(0),
		TYPE_BAD_SEQ(1),
		TYPE_BAD_ID(2),
		TYPE_OK(3),
		TYPE_FULL_CHECK(4),
		TYPE_CHECK(5),
		TYPE_NOT_VALID_EDGE(6),
		TYPE_BOTH_STARTUP(7),
		TYPE_FAILED_RING(8),
		TYPE_STARTUP_CHECK(9);

		private final byte value;

		MessageType(int value) {
			this.value = (byte) value;
		}

		/**
		 * Retrieves the byte value of this message type.
		 *
		 * @return the byte value of the message type
		 */
		public byte getValue() {
			return value;
		}

		/**
		 * Gets the MessageType corresponding to the provided byte value.
		 *
		 * @param value the byte value to match
		 * @return the corresponding MessageType, or TYPE_OK if no match is found
		 */
		public static MessageType getByValue(byte value) {
			for (MessageType type : values()) {
				if (type.getValue() == value) {
					return type;
				}
			}
			return TYPE_OK; // Default fallback
		}
	}

	/**
	 * Enum representing various commands associated with synchronization.
	 */
	public enum Command {
		COMMAND_TAKE_THIS(0),
		COMMAND_GIVE_THIS(1),
		COMMAND_DEL_THIS(2),
		COMMAND_OK(3),
		COMMAND_RCPT_THIS(4);

		private final byte value;

		Command(int value) {
			this.value = (byte) value;
		}

		/**
		 * Retrieves the byte value of this command.
		 *
		 * @return the byte value of the command
		 */
		public byte getValue() {
			return value;
		}
	}

	/**
	 * Enum representing synchronization modes.
	 */
	public enum SyncMode {
		SYNC_CLUSTER((byte) 1),
		SYNC_MESSAGE((byte) 0);

		private final byte mode;

		SyncMode(byte mode) {
			this.mode = mode;
		}

		/**
		 * Retrieves the byte mode of this synchronization mode.
		 *
		 * @return the byte mode of the synchronization mode
		 */
		public byte getMode() {
			return this.mode;
		}
	}

	// Constants for scheduling and startup status
	public final static byte SCHEDULED = 1;
	public final static byte NOT_SCHEDULED = 0;
	public final static byte IN_STARTUP = 1;
	public final static byte NOT_IN_STARTUP = 0;
	public final static byte SEQ_MAX = 4;

	// Instance variables for the DefaultSyncPublication
	private List<String> keyChain = null;
	private short id = 0;
	private MessageType type = MessageType.TYPE_OK;
	private byte sequence = 0;
	private SyncMode syncMode = SyncMode.SYNC_CLUSTER;
	private boolean inStartup = false;
	private List<SyncContent> contents;
	private SynchronisationType synchronisationType = SynchronisationType.UNI_CAST;
	private ClusterIdRegistry expectedIds;

	/**
	 * Constructs a {@code DefaultSyncPublication} instance.
	 */
	public DefaultSyncPublication() {
		contents = new ArrayList<>();
	}

	/**
	 * Retrieves the type of this synchronization message.
	 *
	 * @return the byte type of the synchronization message
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * Sets the type of this synchronization message.
	 *
	 * @param type the byte type to set
	 */
	public void setType(MessageType type) {
		this.type = type;
	}

	/**
	 * Retrieves the key chain associated with this synchronization message.
	 *
	 * @return the list of keys in the key chain
	 */
	public List<String> getKeyChain() {
		return keyChain;
	}

	/**
	 * Sets the key chain for this synchronization message.
	 *
	 * @param keyChain the list of keys to set in the key chain
	 */
	public void setKeyChain(List<String> keyChain) {
		this.keyChain = keyChain;
	}

	/**
	 * Retrieves the unique identifier of this synchronization message.
	 *
	 * @return the short ID of the synchronization message
	 */
	public short getId() {
		return id;
	}

	/**
	 * Sets the unique identifier for this synchronization message.
	 *
	 * @param id the short ID to set
	 */
	public void setId(short id) {
		this.id = id;
	}

	/**
	 * Retrieves the sequence number of this synchronization message.
	 *
	 * @return the byte sequence number
	 */
	public byte getSequence() {
		return sequence;
	}

	/**
	 * Sets the sequence number for this synchronization message.
	 *
	 * @param sequence the byte sequence number to set
	 */
	public void setSequence(byte sequence) {
		this.sequence = sequence;
	}

	/**
	 * Retrieves the synchronization mode for this message.
	 *
	 * @return the {@code SyncMode} of this message
	 */
	public SyncMode getSyncMode() {
		return this.syncMode;
	}

	/**
	 * Sets the synchronization mode for this message.
	 *
	 * @param mode the {@code SyncMode} to set
	 */
	public void setSyncMode(final SyncMode mode) {
		this.syncMode = mode;
	}

	/**
	 * Checks if the synchronization message is in startup mode.
	 *
	 * @return {@code true} if in startup mode, otherwise {@code false}
	 */
	public boolean isInStartup() {
		return inStartup;
	}

	/**
	 * Sets the startup status for this synchronization message.
	 *
	 * @param inStartup {@code true} to mark as in startup, otherwise {@code false}
	 */
	public void setInStartup(boolean inStartup) {
		this.inStartup = inStartup;
	}

	/**
	 * Retrieves the contents of this synchronization message.
	 *
	 * @return the list of {@code SyncContent} in this message
	 */
	public List<SyncContent> getContents() {
		return this.contents;
	}

	/**
	 * Sets the contents for this synchronization message.
	 *
	 * @param contents the list of {@code SyncContent} to set
	 */
	public void setContents(List<SyncContent> contents) {
		this.contents = contents;
	}

	/**
	 * Sets the contents for this synchronization message using a collection of {@code SyncContent}.
	 *
	 * @param contents the collection of {@code SyncContent} to set
	 */
	public void setContents(Collection<SyncContent> contents) {
		this.contents = new ArrayList<SyncContent>(contents);
	}

	/**
	 * Adds a {@code SyncContent} object to the contents of this synchronization message.
	 *
	 * @param content the {@code SyncContent} to add
	 */
	public void addContents(SyncContent content) {
		this.contents.add(content);
	}

	/**
	 * Retrieves the synchronization type for this message.
	 *
	 * @return the {@code SynchronisationType} of this message
	 */
	public SynchronisationType getSyncType() {
		return synchronisationType;
	}

	/**
	 * Sets the synchronization type for this message.
	 *
	 * @param synchronisationType the {@code SynchronisationType} to set
	 */
	public void setSyncType(SynchronisationType synchronisationType) {
		this.synchronisationType = synchronisationType;
	}

	/**
	 * Retrieves the expected node identifiers associated with this synchronization message.
	 *
	 * @return the array of expected node IDs
	 */
	public short[] getExpectedIdsRaw() {
		return expectedIds.getIds();
	}

	/**
	 * Retrieves the expected node identifiers associated with this synchronization message.
	 *
	 * @return the array of expected node IDs
	 */
	public ClusterIdRegistry getExpectedIds() {
		return expectedIds;
	}

	/**
	 * Sets the expected node identifiers for this synchronization message.
	 *
	 * @param expectedIds the array of expected node IDs to set
	 */
	public void setExpectedIds(short[] expectedIds) {
		this.expectedIds = new ClusterIdRegistry(expectedIds);
	}

	/**
	 * Sets the expected node identifiers for this synchronization message.
	 *
	 * @param expectedIds the array of expected node IDs to set
	 */
	public void setExpectedIds(ClusterIdRegistry expectedIds) {
		this.expectedIds = expectedIds;
	}

	/**
	 * Deserializes the synchronization message from a DataInputStream.
	 *
	 * @param in the DataInputStream to read from
	 * @throws IOException if an I/O error occurs during deserialization
	 */
	@Override
	public void deserialize(ByteBuf in) throws IOException {
		id = in.readShort();
		type = MessageType.values()[in.readByte()];
		sequence = in.readByte();
		inStartup = in.readBoolean();
		byte mode = in.readByte();
		syncMode = (mode == SyncMode.SYNC_CLUSTER.getMode()) ? SyncMode.SYNC_CLUSTER : SyncMode.SYNC_MESSAGE;
		mode = in.readByte();
		synchronisationType = SynchronisationType.getByValue(mode);
		int len = in.readByte();
		if (len > 0) {
			keyChain = new ArrayList<>();
			for (int i = 0; i < len; i++) {
				keyChain.add(readUTF(in));
			}
		}
		len = in.readShort();
		if (len > 0) {
			short[] expectedIds = new short[len];
			for (int i = 0; i < len; i++) {
				expectedIds[i] = in.readShort();
			}
			this.expectedIds = new ClusterIdRegistry(expectedIds);
		}
		len = in.readInt();
		if (len > 0) {
			for (int i = 0; i < len; i++) {
				int contentLen = in.readInt();
				byte[] message = (contentLen > 0) ? new byte[contentLen] : null;
				if (message != null) {
					in.readBytes(message);
				}
				long version = in.readLong();
				String key = readUTF(in);
				contentLen = in.readShort();

				short[] awareIds = (contentLen > 0) ?
				                   new short[contentLen] : new short[0];

				for (int j = 0; j < contentLen; j++) {
					awareIds[j] = in.readShort();
				}

				SyncContent s = new SyncContent(key, version,
				                                new ClusterIdRegistry(awareIds), message);
				this.contents.add(s);
			}
		}
	}

	/**
	 * Serializes the synchronization message to a DataOutputStream.
	 *
	 * @param out the DataOutputStream to write to
	 * @throws IOException if an I/O error occurs during serialization
	 */
	@Override
	public void serialize(ByteBuf out) throws IOException {
		out.writeShort(id);
		out.writeByte(type.ordinal());
		out.writeByte(sequence);
		out.writeBoolean(inStartup);
		out.writeByte(syncMode.getMode());
		out.writeByte(synchronisationType.getValue());
		List<String> keys = getKeyChain();
		if (keys != null) {
			out.writeByte(keys.size());
			for (String key : keys) {
				writeUTF(out, key);
			}
		} else {
			out.writeByte(0);
		}
		if (this.expectedIds == null || this.expectedIds.size() == 0) {
			out.writeShort(0);
		} else {
			out.writeShort(this.expectedIds.size());
			for (short expectedId : this.expectedIds.getIds()) {
				out.writeShort(expectedId);
			}
		}

		out.writeInt(contents.size());
		if (contents.size() == 0) {
			return;
		}

		for (SyncContent c : contents) {
			if (c.getContent() == null || c.getContent().length == 0) {
				out.writeInt(0);
			} else {
				out.writeInt(c.getContent().length);
				out.writeBytes(c.getContent());
			}
			out.writeLong(c.getVersion());
			writeUTF(out, c.getKey());
			if (c.getAwareIds() == null || c.getAwareIds().size() == 0) {
				out.writeShort(0);
			} else {
				out.writeShort(c.getAwareIds().size());
				for (short aShort : c.getAwareIds().getIds()) {
					out.writeShort(aShort);
				}
			}
		}
	}

	public void writeUTF(ByteBuf out, String value) {
		if (value == null) {
			out.writeShort(0); // Write length of 0 for null
			return;
		}
		byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		out.writeShort(bytes.length); // Write the length of the string
		out.writeBytes(bytes); // Write the string bytes
	}

	public String readUTF(ByteBuf in) {
		int length = in.readShort(); // Read the length of the string
		if (length == 0) {
			return null; // Return null for length 0
		}
		byte[] bytes = new byte[length];
		in.readBytes(bytes); // Read the string bytes
		return new String(bytes, java.nio.charset.StandardCharsets.UTF_8); // Convert bytes to String
	}

}
