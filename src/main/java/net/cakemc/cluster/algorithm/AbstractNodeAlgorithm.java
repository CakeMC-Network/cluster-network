package net.cakemc.cluster.algorithm;

import net.cakemc.cluster.endpoint.check.AbstractNodeManager;
import net.cakemc.cluster.info.NodeInformation;

/**
 * An abstract base class for algorithms used in node selection within the cluster.
 *
 * <p>This class serves as the foundation for implementing various node selection algorithms
 * within a distributed cluster system. Each algorithm should define how to pick the next node
 * based on its specific strategy or policy.</p>
 *
 * <h2>Usage</h2>
 * <p>To use this class, extend it and implement the {@link #pickNextNode()} method, which defines
 * the logic for selecting the next node from the cluster.</p>
 *
 * <p>Each concrete implementation of this class is responsible for interacting with the {@link AbstractNodeManager}
 * to manage and select nodes. The node manager provides the required cluster data, and the algorithm utilizes
 * this data to determine the next node.</p>
 *
 * <h2>Constructor</h2>
 * <p>The constructor takes two parameters:</p>
 * <ul>
 *   <li>{@code nodeManager} – An instance of {@link AbstractNodeManager} that is used to interact with
 *   the nodes in the cluster.</li>
 *   <li>{@code ownNodeId} – The unique identifier of the node on which this algorithm is running.</li>
 * </ul>
 *
 * <p>The {@code ownNodeId} is typically used to ensure that a node doesn't pick itself when selecting the next node.</p>
 *
 * @see AbstractNodeManager
 * @see NodeInformation
 */
public abstract class AbstractNodeAlgorithm {

	/**
	 * The node manager responsible for managing node data in the cluster.
	 * This instance provides access to the list of nodes and their status within the cluster.
	 */
	protected final AbstractNodeManager nodeManager;

	/**
	 * The unique identifier for the current node.
	 * This ID represents the node that is running the current instance of the algorithm.
	 */
	protected final int ownNodeId;

	/**
	 * Constructs a new {@code AbstractNodeAlgorithm}.
	 *
	 * @param nodeManager the node manager responsible for managing nodes in the cluster
	 * @param ownNodeId the unique identifier of the current node
	 */
	protected AbstractNodeAlgorithm(AbstractNodeManager nodeManager, int ownNodeId) {
		this.nodeManager = nodeManager;
		this.ownNodeId = ownNodeId;
	}

	/**
	 * Selects the next node in the cluster according to the algorithm's strategy.
	 *
	 * <p>Concrete implementations of this method must define the logic for selecting the next
	 * node to communicate with or interact with within the cluster. The method should return
	 * an instance of {@link NodeInformation} representing the selected node.</p>
	 *
	 * @return the next node's information, as determined by the algorithm
	 */
	public abstract NodeInformation pickNextNode();

}
