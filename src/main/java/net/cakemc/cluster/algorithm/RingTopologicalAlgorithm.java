package net.cakemc.cluster.algorithm;

import net.cakemc.cluster.endpoint.check.AbstractNodeManager;
import net.cakemc.cluster.info.NodeInformation;
import net.cakemc.cluster.info.NodeStatus;

/**
 * An implementation of a ring-based topological node selection algorithm.
 *
 * <p>This class extends {@link AbstractNodeAlgorithm} to implement a specific node selection strategy
 * based on a ring topology. In a ring topology, nodes are arranged in a circular manner, and the algorithm
 * picks the next available node by checking the nodes that come after the current one in the ring order.</p>
 *
 * <h2>Algorithm Overview</h2>
 * <p>The algorithm works by iterating over the nodes managed by the {@link AbstractNodeManager}. It finds
 * the position of the current node (denoted by {@code ownNodeId}) in the ring and then checks the subsequent
 * nodes for availability. If no available node is found, it defaults to selecting the first node in the array.</p>
 *
 * <h2>Constructor</h2>
 * <p>The constructor requires the {@link AbstractNodeManager} to manage the node data and the {@code ownNodeId}
 * to identify the current node in the ring.</p>
 *
 * <h2>Example</h2>
 * <pre>
 * {@code
 * AbstractNodeManager nodeManager = ...;
 * int ownNodeId = 1;
 * RingTopologicalAlgorithm algorithm = new RingTopologicalAlgorithm(nodeManager, ownNodeId);
 * NodeInformation nextNode = algorithm.pickNextNode();
 * }
 * </pre>
 *
 * @see AbstractNodeAlgorithm
 * @see AbstractNodeManager
 * @see NodeInformation
 * @see NodeStatus
 */
public class RingTopologicalAlgorithm extends AbstractNodeAlgorithm {

	/**
	 * Constructs a new {@code RingTopologicalAlgorithm} instance.
	 *
	 * @param nodeManager the node manager responsible for handling the cluster nodes
	 * @param ownNodeId the unique identifier of the current node
	 */
	public RingTopologicalAlgorithm(AbstractNodeManager nodeManager, int ownNodeId) {
		super(nodeManager, ownNodeId);
	}

	/**
	 * Selects the next available node in the ring topology.
	 *
	 * <p>This method first retrieves the list of nodes from the {@link AbstractNodeManager} and locates
	 * the position of the current node (based on {@code ownNodeId}). It then iterates over the nodes that
	 * follow the current node in the ring order, returning the first available node. If no such node is
	 * available, it defaults to the first node in the list.</p>
	 *
	 * <p>The method makes use of {@link #isNodeAvailable(NodeInformation)} to check each node's availability.</p>
	 *
	 * @return the next available {@link NodeInformation} in the ring
	 */
	@Override
	public NodeInformation pickNextNode() {
		nodeManager.checkNodes(); // Ensure the nodes' statuses are up-to-date.
		NodeInformation[] nodes = nodeManager.getNodes().toArray(new NodeInformation[0]);

		// Find the index of the current node (ownNodeId)
		int ownIndex = -1;
		for (int i = 0; i < nodes.length; i++) {
			NodeInformation information = nodes[i];
			if (information.getAddress().id() == this.ownNodeId) {
				ownIndex = i;
			}
		}

		// Create an array of nodes after the current node in the ring
		int nextSize = nodes.length - (ownIndex + 1);
		NodeInformation[] nextNodes = new NodeInformation[nextSize];
		int subIndex = 0;
		for (int i = ownIndex + 1; i < nodes.length; i++) {
			nextNodes[subIndex++] = nodes[i];
		}

		// Select the first available node
		for (NodeInformation information : nextNodes) {
			if (!isNodeAvailable(information)) {
				continue;
			}
			return information;
		}

		// If no available nodes are found, return the first node in the list
		return nodes[0];
	}

	/**
	 * Checks if a node is available by verifying its status.
	 *
	 * <p>This method retrieves the nodes from the {@link AbstractNodeManager} and checks
	 * whether the provided {@link NodeInformation} corresponds to an active node. A node is
	 * considered available if its status is {@link NodeStatus#ACTIVE}.</p>
	 *
	 * @param nodeInformation the node to check for availability
	 * @return {@code true} if the node is available, otherwise {@code false}
	 */
	public boolean isNodeAvailable(NodeInformation nodeInformation) {
		for (NodeInformation node : this.nodeManager.getNodes()) {
			if (node.getAddress().id() != nodeInformation.getAddress().id()) {
				continue;
			}
			if (node.getStatus() == NodeStatus.ACTIVE) {
				return true;
			}
		}
		return false;
	}

}
