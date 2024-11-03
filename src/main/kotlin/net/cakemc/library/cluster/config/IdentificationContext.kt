package net.cakemc.library.cluster.config

/**
 * Abstract class representing the context used for identifying nodes in the cluster.
 *
 *
 * The `IdentificationContext` class provides methods to manage a list of
 * [NodeIdentifier] objects, which define the socket configuration for the nodes in the cluster.
 *
 *
 * Subclasses of `IdentificationContext` must implement methods for retrieving
 * and setting the list of node socket configurations.
 */
abstract class IdentificationContext {
    /**
     * Retrieves the list of [NodeIdentifier] objects representing the socket configurations
     * of the nodes in the cluster.
     *
     * @return a [List] of [NodeIdentifier] objects
     */
    /**
     * Sets the list of [NodeIdentifier] objects representing the socket configurations
     * for the nodes in the cluster.
     *
     * @param nodeIdentifiers a [List] of [NodeIdentifier] objects to be set
     */
    abstract var socketConfigs: List<NodeIdentifier>
}
