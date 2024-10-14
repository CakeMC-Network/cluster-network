package net.cakemc.library.cluster.api.handler;

import net.cakemc.library.cluster.Session;
import net.cakemc.library.cluster.codec.Publication;

/**
 * The {@code APIPublicationHandler} interface defines the contract for handling
 * publications received in a session. Implementers of this interface can define
 * custom behavior for processing publications.
 */
public interface APIPublicationHandler {

	/**
	 * Handles the given publication for the specified session.
	 *
	 * @param session    the {@link Session} in which the publication was received.
	 * @param publication the {@link Publication} to be processed.
	 */
	void handlePublication(Session session, Publication publication);
}
