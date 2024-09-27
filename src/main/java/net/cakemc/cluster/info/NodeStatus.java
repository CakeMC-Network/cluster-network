package net.cakemc.cluster.info;

/**
 * The {@code NodeStatus} enum represents the various states a node in the
 * cluster can have.
 *
 * <p>This enum is used to track the operational status of nodes within
 * the cluster, indicating whether a node is active, inactive, or unresponsive.</p>
 */
public enum NodeStatus {

	/** Indicates that the node is currently active and operational. */
	ACTIVE,

	/** Indicates that the node is currently inactive and not participating in the cluster. */
	INACTIVE,

	/** Indicates that the node is unresponsive, likely due to an error or network issue. */
	UNRESPONSIVE,
	;
}
