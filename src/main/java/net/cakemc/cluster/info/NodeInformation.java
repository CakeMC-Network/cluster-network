package net.cakemc.cluster.info;

import net.cakemc.cluster.NodeAddress;

/**
 * The {@code NodeInformation} class encapsulates the information related to a node
 * in the cluster, including its address and current status.
 *
 * <p>This class is used to manage and represent the state of each node within the
 * cluster network.</p>
 */
public class NodeInformation {

	private final NodeAddress address;
	private NodeStatus status;

	/**
	 * Constructs a {@code NodeInformation} instance with the specified address and status.
	 *
	 * @param address the {@link NodeAddress} representing the node's network address
	 * @param status the initial {@link NodeStatus} of the node
	 */
	public NodeInformation(NodeAddress address, NodeStatus status) {
		this.address = address;
		this.status = status;
	}

	/**
	 * Retrieves the network address of the node.
	 *
	 * @return the {@link NodeAddress} of the node
	 */
	public NodeAddress getAddress() {
		return address;
	}

	/**
	 * Retrieves the current status of the node.
	 *
	 * @return the {@link NodeStatus} of the node
	 */
	public NodeStatus getStatus() {
		return status;
	}

	/**
	 * Sets the current status of the node.
	 *
	 * @param status the new {@link NodeStatus} to set
	 */
	public void setStatus(NodeStatus status) {
		this.status = status;
	}

	/**
	 * Returns a string representation of the node's information, including its
	 * address ID and status.
	 *
	 * @return a string representing the node information
	 */
	@Override
	public String toString() {
		return "NodeInformation{" +
		       "id=" + address.id() +
		       ", status=" + status +
		       '}';
	}
}
