package net.cakemc.library.cluster;

/**
 * Represents a node in the cluster with an identifier and versioning.
 *
 * <p>This interface defines the basic operations for a cluster node,
 * including retrieving its identifier and version, as well as setting
 * the version.</p>
 */
public interface Node {

	/**
	 * Retrieves the unique identifier of the node.
	 *
	 * @return the ID of the node as a short value
	 */
	short getId();

	/**
	 * Retrieves the version of the node.
	 *
	 * @return the version of the node as a byte value
	 */
	byte getVersion();

	/**
	 * Sets the version of the node.
	 *
	 * @param version the new version to set, as a byte value
	 */
	void setVersion(byte version);
}
