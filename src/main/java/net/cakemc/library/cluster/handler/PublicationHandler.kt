package net.cakemc.library.cluster.handler;

import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.PublicationBundle;
import net.cakemc.library.cluster.Session;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.util.Map;

/**
 * A callback interface for handling synchronization events in the cluster.
 *
 * <p>This interface defines methods for processing synchronization messages
 * and handling the result of synchronization features.</p>
 */
@FunctionalInterface
public interface PublicationHandler {

	/**
	 * Processes a synchronization callback with the given parameters.
	 *
	 * @param session the current session associated with the synchronization
	 * @param message the synchronization message received
	 * @param withNodes an array of node IDs involved in the synchronization
	 * @param out the output object for writing responses
	 * @return {@code true} if the callback was processed successfully;
	 *         {@code false} otherwise
	 */
	boolean callBack(
		 Session session,
		 Publication message,
		 ClusterIdRegistry withNodes,
		 PublicationBundle out
	);

	/**
	 * Handles the result of a synchronization feature.
	 *
	 * @param syncFeature the synchronization feature result to process
	 */
	default void result(Map<String, SyncResult> syncFeature) {}
}
