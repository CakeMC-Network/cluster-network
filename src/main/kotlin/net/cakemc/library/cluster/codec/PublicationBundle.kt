package net.cakemc.library.cluster.codec

/**
 * An interface for handling the output of publications in the cluster.
 *
 *
 * This interface defines methods for writing single and multiple publication
 * messages to an output destination.
 */
interface PublicationBundle {
    /**
     * Writes a single publication message to the output.
     *
     * @param message the publication message to write
     */
    fun write(message: Publication?)

    /**
     * Writes a list of publication messages to the output.
     *
     * @param message the list of publication messages to write
     */
    fun write(message: MutableList<Publication>?)
}
