package net.cakemc.cluster.endpoint.check;

import net.cakemc.cluster.info.NodeInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A concrete implementation of {@link AbstractNodeManager} that manages node information
 * within a cluster, including adding nodes and checking their statuses concurrently.
 *
 * <p>The {@code NodeManager} class maintains a list of {@link NodeInformation} objects and provides
 * functionality to check the status of each node in parallel using threads. The checks are coordinated
 * using a {@link CountDownLatch}, which allows for waiting until all nodes have been checked or a
 * timeout has been reached.</p>
 *
 * <h2>Constructor</h2>
 * <p>The constructor initializes the node manager with a specified timeout and a callback to
 * be executed after checking the nodes.</p>
 *
 * @see AbstractNodeManager
 * @see NodeInformation
 */
public class NodeManager extends AbstractNodeManager {

	/**
	 * The list of nodes currently managed by this instance.
	 * This list holds the {@link NodeInformation} objects for all nodes in the cluster.
	 */
	private final List<NodeInformation> nodes;

	/**
	 * The maximum time to wait for node checks to complete.
	 * This value is specified in seconds.
	 */
	private final long timeout;

	/**
	 * The callback to be executed after checking all nodes.
	 * This is typically used for post-check processing or notifications.
	 */
	private final Runnable callback;

	/**
	 * Constructs a new {@code NodeManager} with the specified timeout and callback.
	 *
	 * @param timeout the maximum time to wait for node checks, in seconds
	 * @param callback the callback to execute after node checks are complete
	 */
	public NodeManager(long timeout, Runnable callback) {
		this.timeout = timeout;
		this.callback = callback;
		this.nodes = new ArrayList<>();
	}

	/**
	 * Adds a new node to the cluster.
	 *
	 * <p>This method adds the specified {@link NodeInformation} to the list of managed nodes.</p>
	 *
	 * @param nodeInformation the information about the node to be added to the cluster
	 */
	@Override
	public void addNode(NodeInformation nodeInformation) {
		this.nodes.add(nodeInformation);
	}

	/**
	 * Checks the statuses of all nodes in the cluster.
	 *
	 * <p>This method initiates a check for each node in a separate thread. It uses a {@link CountDownLatch}
	 * to wait until all nodes have been checked or the timeout has been reached. After checking, it
	 * executes the specified callback.</p>
	 *
	 * <p>Each node's check logic should be implemented in the section marked with "todo check here".</p>
	 */
	@Override
	public void checkNodes() {
		CountDownLatch latch = new CountDownLatch(nodes.size());

		for (NodeInformation node : nodes) {
			new Thread(() -> {
				try {
					// TODO: Implement the logic to check the node's status here.
				} finally {
					latch.countDown();
				}
			}).start();
		}

		try {
			if (!latch.await(timeout, TimeUnit.SECONDS)) {
				System.out.println("Timeout reached, stopping checks.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("Checking interrupted.");
		}

		callback.run();
	}

	/**
	 * Retrieves the list of nodes currently managed in the cluster.
	 *
	 * @return a {@link List} of {@link NodeInformation} representing the current nodes in the cluster
	 */
	@Override
	public List<NodeInformation> getNodes() {
		return nodes;
	}

}
