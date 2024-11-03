package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import net.cakemc.library.cluster.address.ClusterAddress
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

/**
 * Represents a publication in the cluster framework, encapsulating information
 * required for communication between cluster members.
 *
 *
 * The class implements the [Publication] interface and provides methods
 * for serialization and deserialization of publication data, including synchronization
 * addresses and authentication information.
 */
class ClusterPublication : Publication {
    /**
     * Gets the unique identifier of this publication.
     *
     * @return the unique identifier of the publication
     */
    /**
     * Sets the unique identifier for this publication.
     *
     * @param id the unique identifier to set
     */
    var id: Short = -1 // Unique identifier for the publication
    /**
     * Checks if the publication is authenticated by a key.
     *
     * @return `true` if authenticated by key; `false` otherwise
     */
    /**
     * Sets whether the publication should be authenticated by key.
     *
     * @param authByKey `true` to authenticate by key; `false` otherwise
     */
    var isAuthByKey: Boolean = true // Indicates whether authentication is by key
    /**
     * Gets the authentication key for this publication.
     *
     * @return the authentication key
     */
    /**
     * Sets the authentication key for this publication.
     *
     * @param key the authentication key to set
     */
    var authKey: String? = "" // Authentication key, if required
    /**
     * Gets the version of this publication.
     *
     * @return the version of the publication
     */
    /**
     * Sets the version for this publication.
     *
     * @param version the version to set
     */
    override var version: Long = 0 // Version of the publication
    var syncAddresses: MutableSet<ClusterAddress> = HashSet() // Set of synchronization addresses
    /**
     * Gets the command byte for this publication.
     *
     * @return the command byte
     */
    /**
     * Sets the command byte for this publication.
     *
     * @param command the command byte to set
     */
    var command: DefaultSyncPublication.Command =
        DefaultSyncPublication.Command.COMMAND_OK // Command byte for the publication

    override var channel: String = "default"

    /**
     * Default constructor initializing a new instance of [ClusterPublication].
     */
    constructor()

    /**
     * Constructs a new [ClusterPublication] with the specified parameters.
     *
     * @param id            the unique identifier for this publication
     * @param authByKey     whether the publication is authenticated by key
     * @param key           the authentication key
     * @param version       the version of the publication
     * @param syncAddresses a set of synchronization addresses
     * @param command       the command byte for this publication
     */
    constructor(
        id: Short, authByKey: Boolean, key: String?, version: Long,
        syncAddresses: MutableSet<ClusterAddress>?, command: DefaultSyncPublication.Command
    ) {
        this.id = id
        this.isAuthByKey = authByKey
        this.authKey = key
        this.version = version
        this.syncAddresses = syncAddresses ?: HashSet()
        this.command = command
    }

    /**
     * Serializes this publication into a byte array for transmission.
     *
     * @return a byte array representing the serialized publication, or `null` if an error occurs
     */
    override fun serialize(byteBuf: ByteBuf) {
        byteBuf.writeShort(id.toInt())
        byteBuf.writeBoolean(isAuthByKey)

        byteBuf.writeInt(authKey!!.length)
        byteBuf.writeBytes(authKey!!.toByteArray(StandardCharsets.UTF_8))

        byteBuf.writeLong(version)
        byteBuf.writeByte(command.ordinal)

        byteBuf.writeByte(syncAddresses.size)
        for (syncAddress in syncAddresses) {
            byteBuf.writeByte(syncAddress.address!!.address.address.size)
            byteBuf.writeBytes(syncAddress.address!!.address.address)
            byteBuf.writeInt(syncAddress.port)
        }
    }


    /**
     * Deserializes this publication from the given byte array.
     *
     * @param data the byte array containing serialized publication data
     */
    override fun deserialize(data: ByteBuf) {
        id = data.readShort()
        isAuthByKey = data.readBoolean()

        val authKeyBytes = ByteArray(data.readInt())
        data.readBytes(authKeyBytes)
        authKey = String(authKeyBytes)

        version = data.readLong()
        command = DefaultSyncPublication.Command.entries[data.readByte().toInt()]

        val len = data.readByte().toInt()
        syncAddresses = HashSet() // Initialize the set to avoid null
        try {
            for (i in 0 until len) {
                val ipLength = data.readByte()
                val ip = ByteArray(ipLength.toInt())
                data.readBytes(ip)
                val port = data.readInt()


                syncAddresses.add(ClusterAddress(InetAddress.getByAddress(ip), port))
            }
        } catch (e: UnknownHostException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Closes resources associated with this publication.
     *
     *
     * This method can be implemented to release any resources held by this
     * publication, if necessary.
     */
    override fun close() {
        // Implement close logic if needed
    }

    /**
     * Configures this publication with the given parameters.
     *
     * @param config a map of configuration parameters
     */
    override fun configure(config: Map<String?, *>?) {
        // Implement configuration logic if needed
    }

    override val key: String
        /**
         * Gets the key for this publication, which is based on its unique identifier.
         *
         * @return the key for this publication
         */
        get() = id.toString()
}
