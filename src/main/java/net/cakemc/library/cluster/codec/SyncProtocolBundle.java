package net.cakemc.library.cluster.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link PublicationBundle} interface for managing
 * the output of publication messages in a synchronization protocol.
 *
 * <p>This class stores publication messages and provides methods for
 * writing single or multiple messages.</p>
 */
public class SyncProtocolBundle implements PublicationBundle {
	private List<Publication> messages = null;

	/**
	 * Default constructor for the {@link SyncProtocolBundle}.
	 */
	public SyncProtocolBundle() {
	}

	/**
	 * Retrieves the list of messages written to this output.
	 *
	 * @return a list of publication messages, or null if no messages have been written
	 */
	public List<Publication> getMessages() {
		return messages;
	}

	/**
	 * Writes a single publication message to this output.
	 *
	 * <p>If the provided message is null, the internal message list is cleared.</p>
	 *
	 * @param message the publication message to write
	 */
	@Override
	public void write(Publication message) {
		if (message == null) {
			messages = null;
			return;
		}
		this.messages = new ArrayList<>();
		this.messages.add(message);
	}

	/**
	 * Writes a list of publication messages to this output.
	 *
	 * @param messages the list of publication messages to write
	 */
	@Override
	public void write(List<Publication> messages) {
		this.messages = messages;
	}
}
