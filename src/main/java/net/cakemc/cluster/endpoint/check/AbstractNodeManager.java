package net.cakemc.cluster.endpoint.check;

import net.cakemc.cluster.info.NodeInformation;

import java.util.List;

/**
 * An abstract class that manages node information within a cluster.
 *
 * <p>This class provides a base for implementing node management functionality in a distributed
 * cluster system. The {@code AbstractNodeManager} handles adding nodes, checking their status,
 * and retrieving the list of nodes. Concrete implementations should define how nodes are added
 * and checked, as well as how to maintain the list of nodes in the cluster.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Manage the list of nodes in the cluster by adding and maintaining their state.</li>
 *   <li>Provide a way to check the status of the nodes, ensuring that the cluster is up-to-date.</li>
 *   <li>Allow retrieval of the current list of nodes for use by algorithms or other components.</li>
 * </ul>
 *
 * <p>Classes extending {@code AbstractNodeManager} must implement the abstract methods to define
 * the specific node management logic based on the use case and underlying system architecture.</p>
 *
 * <h2>Example</h2>
 * <pre>
 * {@code
 * public class SimpleNodeManager extends AbstractNodeManager {
 *
 *     private List<NodeInformation> nodes = new ArrayList<>();
 *
 *     @Override
 *     public void addNode(NodeInformation nodeInformation) {
 *         nodes.add(nodeInformation);
 *     }
 *
 *     @Override
 *     public void checkNodes() {
 *         // Implementation for checking nodes' status
 *     }
 *
 *     @Override
 *     public List<NodeInformation> getNodes() {
 *         return nodes;
 *     }
 * }
 * }
 * </pre>
 *
 * @see NodeInformation
 */
public abstract class AbstractNodeManager {

	/**
	 * Adds a node to the cluster.
	 *
	 * <p>This method is responsible for adding a new {@link NodeInformation} to the list of managed nodes.
	 * Concrete implementations should define how the node is integrated and possibly handle scenarios like
	 * duplicate nodes or invalid entries.</p>
	 *
	 * @param nodeInformation the information about the node to be added to the cluster
	 */
	public abstract void addNode(NodeInformation nodeInformation);

	/**
	 * Checks the status of nodes in the cluster.
	 *
	 * <p>This method is responsible for verifying the status of each node in the cluster. Implementations
	 * of this method should ensure that all nodes' states are up-to-date. This may involve network checks,
	 * heartbeat monitoring, or other forms of validation to ensure the cluster's integrity.</p>
	 */
	public abstract void checkNodes();

	/**
	 * Retrieves the list of nodes currently managed in the cluster.
	 *
	 * <p>This method returns a list of {@link NodeInformation} objects representing all the nodes that
	 * are currently being managed by this {@code AbstractNodeManager} instance. The list can be used
	 * by algorithms or other systems interacting with the cluster.</p>
	 *
	 * @return a {@link List} of {@link NodeInformation} representing the current nodes in the cluster
	 */
	public abstract List<NodeInformation> getNodes();

}
