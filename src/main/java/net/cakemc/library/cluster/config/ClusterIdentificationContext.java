package net.cakemc.library.cluster.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of the {@link IdentificationContext} that manages
 * the socket configuration for nodes in a cluster.
 *
 * <p>The `ClusterIdentificationContext` class provides a specific implementation
 * of the {@link IdentificationContext} for storing and retrieving a list of
 * {@link NodeIdentifier} objects, which define the socket configuration for each node.</p>
 *
 * <p>This class uses an internal {@link ArrayList} to manage the node identifiers
 * and offers methods to access or update them.</p>
 */
public class ClusterIdentificationContext extends IdentificationContext {

	// List to hold the NodeIdentifier objects representing the node socket configurations
	private List<NodeIdentifier> nodeIdentifiers;

	/**
	 * Constructs a new `ClusterIdentificationContext` instance with an empty list of node identifiers.
	 */
	public ClusterIdentificationContext() {
		nodeIdentifiers = new ArrayList<>();
	}

	/**
	 * Retrieves the list of {@link NodeIdentifier} objects representing the socket configurations
	 * of the nodes in the cluster.
	 *
	 * @return a {@link List} of {@link NodeIdentifier} objects
	 */
	@Override
	public List<NodeIdentifier> getSocketConfigs() {
		return nodeIdentifiers;
	}

	/**
	 * Sets the list of {@link NodeIdentifier} objects representing the socket configurations
	 * for the nodes in the cluster.
	 *
	 * @param nodeIdentifiers a {@link List} of {@link NodeIdentifier} objects to be set
	 */
	@Override
	public void setSocketConfigs(List<NodeIdentifier> nodeIdentifiers) {
		this.nodeIdentifiers = nodeIdentifiers;
	}
}
