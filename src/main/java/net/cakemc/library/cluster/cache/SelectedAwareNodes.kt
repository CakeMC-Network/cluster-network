package net.cakemc.library.cluster.cache;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents a selection of aware nodes in the cluster with a version identifier.
 *
 * <p>This class is designed to store a version number and an array of aware node identifiers.
 * It implements {@link Externalizable} for custom serialization and deserialization.</p>
 */
public class SelectedAwareNodes implements Externalizable {

	protected long version;      // Version of the selected aware nodes
	protected short[] awareNodes; // Array of aware node identifiers

	/**
	 * Default constructor for deserialization.
	 */
	public SelectedAwareNodes() {}

	/**
	 * Creates a new instance with the specified version and aware nodes.
	 *
	 * @param version the version of the selected aware nodes
	 * @param awareNodes the array of aware node identifiers
	 */
	public SelectedAwareNodes(long version, short[] awareNodes) {
		this.version = version;
		this.awareNodes = awareNodes;
	}

	/**
	 * Retrieves the version of the selected aware nodes.
	 *
	 * @return the version number
	 */
	public long getVersion() {
		return version;
	}

	/**
	 * Retrieves the array of aware node identifiers.
	 *
	 * @return an array of aware node identifiers
	 */
	public short[] getAwareNodes() {
		return awareNodes;
	}

	/**
	 * Sets the array of aware node identifiers.
	 *
	 * @param awareNodes the new array of aware node identifiers
	 */
	public void setAwareNodes(short[] awareNodes) {
		this.awareNodes = awareNodes;
	}

	/**
	 * Sets the version of the selected aware nodes.
	 *
	 * @param version the new version number
	 */
	public void setVersion(long version) {
		this.version = version;
	}

	/**
	 * Serializes the selected aware nodes to an output stream.
	 *
	 * @param out the output stream to serialize to
	 * @throws IOException if an I/O error occurs during serialization
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(version);
		out.writeObject(awareNodes);
	}

	/**
	 * Deserializes the selected aware nodes from an input stream.
	 *
	 * @param in the input stream to deserialize from
	 * @throws IOException if an I/O error occurs during deserialization
	 * @throws ClassNotFoundException if the class of a serialized object cannot be found
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		version = in.readLong();
		awareNodes = (short[]) in.readObject();
	}
}
