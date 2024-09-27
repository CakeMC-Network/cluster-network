package net.cakemc.cluster.units;

import net.cakemc.cluster.info.NodeStatus;

/**
 * The {@code IdCallBack} interface provides a callback mechanism for handling
 * node identifiers along with their corresponding status in the cluster.
 * Implementations of this interface should define how to process the
 * provided node ID and status.
 */
public interface IdCallBack {

	/**
	 * Accepts a node ID and its status.
	 *
	 * @param id the unique identifier of the node
	 * @param status the status of the node, represented by a {@link NodeStatus}
	 *               enumeration
	 */
	void accept(long id, NodeStatus status);
}
