package net.cakemc.library.cluster.cache

import java.io.Externalizable
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectOutput

/**
 * Represents a selection of aware nodes in the cluster with a version identifier.
 *
 *
 * This class is designed to store a version number and an array of aware node identifiers.
 * It implements [Externalizable] for custom serialization and deserialization.
 */
class SelectedAwareNodes : Externalizable {
    /**
     * Retrieves the version of the selected aware nodes.
     *
     * @return the version number
     */
    /**
     * Sets the version of the selected aware nodes.
     *
     * @param version the new version number
     */
    var version: Long = 0 // Version of the selected aware nodes
    /**
     * Retrieves the array of aware node identifiers.
     *
     * @return an array of aware node identifiers
     */
    /**
     * Sets the array of aware node identifiers.
     *
     * @param awareNodes the new array of aware node identifiers
     */
    var awareNodes: ShortArray // Array of aware node identifiers

    /**
     * Default constructor for deserialization.
     */
    constructor() {
      this.awareNodes = ShortArray(0);
    }

    /**
     * Creates a new instance with the specified version and aware nodes.
     *
     * @param version the version of the selected aware nodes
     * @param awareNodes the array of aware node identifiers
     */
    constructor(version: Long, awareNodes: ShortArray) {
        this.version = version
        this.awareNodes = awareNodes
    }

    /**
     * Serializes the selected aware nodes to an output stream.
     *
     * @param out the output stream to serialize to
     * @throws IOException if an I/O error occurs during serialization
     */
    @Throws(IOException::class)
    override fun writeExternal(out: ObjectOutput) {
        out.writeLong(version)
        out.writeObject(awareNodes)
    }

    /**
     * Deserializes the selected aware nodes from an input stream.
     *
     * @param in the input stream to deserialize from
     * @throws IOException if an I/O error occurs during deserialization
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    override fun readExternal(`in`: ObjectInput) {
        version = `in`.readLong()
        awareNodes = `in`.readObject() as ShortArray
    }
}
