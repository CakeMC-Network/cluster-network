package net.cakemc.library.cluster.handler

import net.cakemc.library.cluster.Session
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.codec.PublicationBundle

/**
 * A callback interface for handling synchronization events in the cluster.
 *
 *
 * This interface defines methods for processing synchronization messages
 * and handling the result of synchronization features.
 */
fun interface PublicationHandler {
    /**
     * Processes a synchronization callback with the given parameters.
     *
     * @param session the current session associated with the synchronization
     * @param message the synchronization message received
     * @param withNodes an array of node IDs involved in the synchronization
     * @param out the output object for writing responses
     * @return `true` if the callback was processed successfully;
     * `false` otherwise
     */
    fun callBack(
        session: Session?,
        message: Publication,
        withNodes: ClusterIdRegistry,
        out: PublicationBundle
    ): Boolean

    /**
     * Handles the result of a synchronization feature.
     *
     * @param syncFeature the synchronization feature result to process
     */
    fun result(syncFeature: Map<String?, SyncResult>?) {}
}
