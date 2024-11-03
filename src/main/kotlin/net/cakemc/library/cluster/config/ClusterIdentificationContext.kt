package net.cakemc.library.cluster.config

import java.util.*

/**
 * Concrete implementation of the [IdentificationContext] that manages
 * the socket configuration for nodes in a cluster.
 *
 *
 * The `ClusterIdentificationContext` class provides a specific implementation
 * of the [IdentificationContext] for storing and retrieving a list of
 * [NodeIdentifier] objects, which define the socket configuration for each node.
 *
 *
 * This class uses an internal [ArrayList] to manage the node identifiers
 * and offers methods to access or update them.
 */
class ClusterIdentificationContext : IdentificationContext() {
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
    // List to hold the NodeIdentifier objects representing the node socket configurations
    override var socketConfigs: List<NodeIdentifier> = ArrayList()
}
