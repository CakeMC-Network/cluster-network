package test.units;

import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.BackUpClusterNode;

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
public class FallBackNodeTest {

	private static final List<AbstractBackUpEndpoint> NODES = new ArrayList<>();

	/**
	 * A static list of NodeInformation instances representing nodes in the cluster.
	 * Each node is initialized with an address and a status of INACTIVE.
	 */
	private static final List<MemberIdentifier> INFORMATIONS = List.of(
		  MemberIdentifier.of(1, "127.0.0.1", 30001),
		  MemberIdentifier.of(2, "127.0.0.1", 30002),
		  MemberIdentifier.of(3, "127.0.0.1", 30003),
		  MemberIdentifier.of(4, "127.0.0.1", 30004)
	);

	/**
	 * The main method that serves as the entry point for the FallBackNodeTest class.
	 * It starts multiple cluster nodes based on the information in the INFORMATIONS list
	 * and calculates the total number of connections between them.
	 *
	 * @param args Command-line arguments (not used).
	 */
	public static void main(String[] args) throws InterruptedException {
		// Start each node defined in the INFORMATIONS list
		for (MemberIdentifier information : INFORMATIONS) {
			startNode(information);
		}

		// Calculate and print the total number of unique connections
		System.out.println(calculateAllConnections(INFORMATIONS.size()).size() * 2);
	}

	/**
	 * Starts a cluster node in a new thread.
	 *
	 * @param ownInformation The address of the node being started.
	 */
	public static void startNode(MemberIdentifier ownInformation) {
		System.out.println("starting %s".formatted(ownInformation));
		Thread thread = new Thread(() -> {
			AbstractBackUpEndpoint node = new BackUpClusterNode(ownInformation, INFORMATIONS, "-");
			NODES.add(node);

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
