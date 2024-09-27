package test;

import net.cakemc.cluster.AbstractNode;
import net.cakemc.cluster.ClusterNode;
import net.cakemc.cluster.NodeAddress;
import net.cakemc.cluster.info.NodeInformation;
import net.cakemc.cluster.info.NodeStatus;
import net.cakemc.cluster.units.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A test class for simulating the startup of multiple cluster nodes
 * and calculating their potential connections.
 *
 * <p>This class serves as a basic test harness for the cluster node
 * implementation. It initializes several nodes, starts them in separate threads,
 * and calculates the number of unique connections between them.</p>
 */
public class NodeTest {

	/**
	 * A static list of NodeInformation instances representing nodes in the cluster.
	 * Each node is initialized with an address and a status of INACTIVE.
	 */
	private static final List<NodeInformation> INFORMATIONS = List.of(
		 new NodeInformation(new NodeAddress(1, "127.0.0.1", 30001), NodeStatus.INACTIVE),
		 new NodeInformation(new NodeAddress(2, "127.0.0.1", 30002), NodeStatus.INACTIVE),
		 new NodeInformation(new NodeAddress(3, "127.0.0.1", 30003), NodeStatus.INACTIVE),
		 new NodeInformation(new NodeAddress(4, "127.0.0.1", 30004), NodeStatus.INACTIVE)
	);

	/**
	 * The main method that serves as the entry point for the NodeTest class.
	 * It starts multiple cluster nodes based on the information in the INFORMATIONS list
	 * and calculates the total number of connections between them.
	 *
	 * @param args Command-line arguments (not used).
	 */
	public static void main(String[] args) {
		// Start each node defined in the INFORMATIONS list
		for (NodeInformation information : INFORMATIONS) {
			startNode(information.getAddress());
		}

		// Calculate and print the total number of unique connections
		System.out.println(calculateAllConnections(INFORMATIONS.size()).size() * 2);
	}

	/**
	 * Starts a cluster node in a new thread.
	 *
	 * @param ownInformation The address of the node being started.
	 */
	public static void startNode(NodeAddress ownInformation) {
		System.out.println("starting %s".formatted(ownInformation));
		Thread thread = new Thread(() -> {
			AbstractNode node = new ClusterNode(ownInformation, INFORMATIONS, "-");
			node.start();
		});
		thread.setName(ownInformation.toString());
		thread.start();
	}

	/**
	 * Calculates all unique connections between nodes in the cluster.
	 *
	 * @param nodeCount The number of nodes in the cluster.
	 * @return A list of unique connections represented as pairs of node indices.
	 */
	public static List<Pair<Integer, Integer>> calculateAllConnections(int nodeCount) {
		List<Pair<Integer, Integer>> connections = new ArrayList<>();

		// Generate all unique pairs of nodes
		for (int i = 0; i < nodeCount; i++) {
			for (int j = i + 1; j < nodeCount; j++) {
				connections.add(new Pair<>(i, j));
			}
		}

		return connections;
	}
}
