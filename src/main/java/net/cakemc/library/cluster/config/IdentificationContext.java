package net.cakemc.library.cluster.config;

import java.util.List;

/**
 * Abstract class representing the context used for identifying nodes in the cluster.
 *
 * <p>The `IdentificationContext` class provides methods to manage a list of
 * {@link NodeIdentifier} objects, which define the socket configuration for the nodes in the cluster.</p>
 *
 * <p>Subclasses of `IdentificationContext` must implement methods for retrieving
 * and setting the list of node socket configurations.</p>
 */
public abstract class IdentificationContext {

	/**
	 * Retrieves the list of {@link NodeIdentifier} objects representing the socket configurations
	 * of the nodes in the cluster.
	 *
	 * @return a {@link List} of {@link NodeIdentifier} objects
	 */
	public abstract List<NodeIdentifier> getSocketConfigs();

	/**
	 * Sets the list of {@link NodeIdentifier} objects representing the socket configurations
	 * for the nodes in the cluster.
	 *
	 * @param nodeIdentifiers a {@link List} of {@link NodeIdentifier} objects to be set
	 */
	public abstract void setSocketConfigs(List<NodeIdentifier> nodeIdentifiers);
}
