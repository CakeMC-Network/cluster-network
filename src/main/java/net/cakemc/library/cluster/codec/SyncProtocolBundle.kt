package net.cakemc.library.cluster.codec

/**
 * Implementation of the [PublicationBundle] interface for managing
 * the output of publication messages in a synchronization protocol.
 *
 *
 * This class stores publication messages and provides methods for
 * writing single or multiple messages.
 */
class SyncProtocolBundle
/**
 * Default constructor for the [SyncProtocolBundle].
 */
    : PublicationBundle {
    private var messages: MutableList<Publication>? = null

    /**
     * Retrieves the list of messages written to this output.
     *
     * @return a list of publication messages, or null if no messages have been written
     */
    fun getMessages(): List<Publication>? {
        return messages
    }

    /**
     * Writes a single publication message to this output.
     *
     *
     * If the provided message is null, the internal message list is cleared.
     *
     * @param message the publication message to write
     */
    override fun write(message: Publication?) {
        if (message == null) {
            messages = null
            return
        }
        this.messages = ArrayList()
        messages?.add(message)
    }

    /**
     * Writes a list of publication messages to this output.
     *
     * @param messages the list of publication messages to write
     */
    override fun write(messages: MutableList<Publication>?) {
        this.messages = messages
    }
}
