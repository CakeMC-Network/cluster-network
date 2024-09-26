package net.cakemc.cluster.info;

import net.cakemc.cluster.NodeAddress;

public class ClusterInformation {

	private final NodeAddress address;
	private final NodeStatus status;

	public ClusterInformation(NodeAddress address, NodeStatus status) {
		this.address = address;
		this.status = status;
	}

	public NodeAddress getAddress() {
		return address;
	}

	public NodeStatus getStatus() {
		return status;
	}
}
