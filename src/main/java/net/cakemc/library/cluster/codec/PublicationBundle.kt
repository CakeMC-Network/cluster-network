package net.cakemc.library.cluster.codec;

import java.util.List;

/**
 * An interface for handling the output of publications in the cluster.
 *
 * <p>This interface defines methods for writing single and multiple publication
 * messages to an output destination.</p>
 */
public interface PublicationBundle {

	/**
	 * Writes a single publication message to the output.
	 *
	 * @param message the publication message to write
	 */
	void write(Publication message);

	/**
	 * Writes a list of publication messages to the output.
	 *
	 * @param message the list of publication messages to write
	 */
	void write(List<Publication> message);
}
