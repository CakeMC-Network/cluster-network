package net.cakemc.library.cluster.codec

import io.netty.buffer.ByteBuf
import net.cakemc.library.cluster.SynchronisationType
import net.cakemc.library.cluster.address.ClusterIdRegistry
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Represents a synchronization message within the cluster framework.
 *
 *
 * The `DefaultSyncPublication` class encapsulates various synchronization details,
 * including the message type, command, synchronization mode, and the content to be synchronized.
 */
class DefaultSyncPublication : SyncPublication {
    /**
     * Enum representing the different message types that can be sent in a synchronization message.
     */
    enum class MessageType(value: Int) {
        TYPE_BAD_KEY(0),
        TYPE_BAD_SEQ(1),
        TYPE_BAD_ID(2),
        TYPE_OK(3),
        TYPE_FULL_CHECK(4),
        TYPE_CHECK(5),
        TYPE_NOT_VALID_EDGE(6),
        TYPE_BOTH_STARTUP(7),
        TYPE_FAILED_RING(8),
        TYPE_STARTUP_CHECK(9);

        /**
         * Retrieves the byte value of this message type.
         *
         * @return the byte value of the message type
         */
        val value: Byte = value.toByte()

        companion object {
            /**
             * Gets the MessageType corresponding to the provided byte value.
             *
             * @param value the byte value to match
             * @return the corresponding MessageType, or TYPE_OK if no match is found
             */
            fun getByValue(value: Byte): MessageType {
                for (type in entries) {
                    if (type.value == value) {
                        return type
                    }
                }
                return TYPE_OK // Default fallback
            }
        }
    }

    /**
     * Enum representing various commands associated with synchronization.
     */
    enum class Command(value: Int) {
        COMMAND_TAKE_THIS(0),
        COMMAND_GIVE_THIS(1),
        COMMAND_DEL_THIS(2),
        COMMAND_OK(3),
        COMMAND_RCPT_THIS(4);

        /**
         * Retrieves the byte value of this command.
         *
         * @return the byte value of the command
         */
        val value: Byte = value.toByte()
    }

    /**
     * Enum representing synchronization modes.
     */
    enum class SyncMode(
        /**
         * Retrieves the byte mode of this synchronization mode.
         *
         * @return the byte mode of the synchronization mode
         */
        val mode: Byte
    ) {
        SYNC_CLUSTER(1.toByte()),
        SYNC_MESSAGE(0.toByte());
    }

    // Instance variables for the DefaultSyncPublication
    private var keyChain: MutableList<String?>? = null
    /**
     * Retrieves the unique identifier of this synchronization message.
     *
     * @return the short ID of the synchronization message
     */
    /**
     * Sets the unique identifier for this synchronization message.
     *
     * @param id the short ID to set
     */
    var id: Short = 0
    /**
     * Retrieves the type of this synchronization message.
     *
     * @return the byte type of the synchronization message
     */
    /**
     * Sets the type of this synchronization message.
     *
     * @param type the byte type to set
     */
    var type: MessageType = MessageType.TYPE_OK
    /**
     * Retrieves the sequence number of this synchronization message.
     *
     * @return the byte sequence number
     */
    /**
     * Sets the sequence number for this synchronization message.
     *
     * @param sequence the byte sequence number to set
     */
    var sequence: Byte = 0
    /**
     * Retrieves the synchronization mode for this message.
     *
     * @return the `SyncMode` of this message
     */
    /**
     * Sets the synchronization mode for this message.
     *
     * @param mode the `SyncMode` to set
     */
    var syncMode: SyncMode = SyncMode.SYNC_CLUSTER
    /**
     * Checks if the synchronization message is in startup mode.
     *
     * @return `true` if in startup mode, otherwise `false`
     */
    /**
     * Sets the startup status for this synchronization message.
     *
     * @param inStartup `true` to mark as in startup, otherwise `false`
     */
    var isInStartup: Boolean = false
    private var contents: MutableList<SyncContent>
    /**
     * Retrieves the synchronization type for this message.
     *
     * @return the `SynchronisationType` of this message
     */
    /**
     * Sets the synchronization type for this message.
     *
     * @param synchronisationType the `SynchronisationType` to set
     */
    var syncType: SynchronisationType? = SynchronisationType.UNI_CAST

    /**
     * Retrieves the expected node identifiers associated with this synchronization message.
     *
     * @return the array of expected node IDs
     */
    var expectedIds: ClusterIdRegistry? = null
        private set

    /**
     * Constructs a `DefaultSyncPublication` instance.
     */
    init {
        contents = ArrayList()
    }

    /**
     * Retrieves the key chain associated with this synchronization message.
     *
     * @return the list of keys in the key chain
     */
    fun getKeyChain(): List<String?>? {
        return keyChain
    }

    /**
     * Sets the key chain for this synchronization message.
     *
     * @param keyChain the list of keys to set in the key chain
     */
    fun setKeyChain(keyChain: MutableList<String?>?) {
        this.keyChain = keyChain
    }

    /**
     * Retrieves the contents of this synchronization message.
     *
     * @return the list of `SyncContent` in this message
     */
    fun getContents(): List<SyncContent> {
        return this.contents
    }

    /**
     * Sets the contents for this synchronization message.
     *
     * @param contents the list of `SyncContent` to set
     */
    fun setContents(contents: MutableList<SyncContent>) {
        this.contents = contents
    }

    /**
     * Sets the contents for this synchronization message using a collection of `SyncContent`.
     *
     * @param contents the collection of `SyncContent` to set
     */
    fun setContents(contents: Collection<SyncContent>) {
        this.contents = ArrayList(contents)
    }

    /**
     * Adds a `SyncContent` object to the contents of this synchronization message.
     *
     * @param content the `SyncContent` to add
     */
    fun addContents(content: SyncContent) {
        contents.add(content)
    }

    val expectedIdsRaw: ShortArray?
        /**
         * Retrieves the expected node identifiers associated with this synchronization message.
         *
         * @return the array of expected node IDs
         */
        get() = expectedIds!!.ids

    /**
     * Sets the expected node identifiers for this synchronization message.
     *
     * @param expectedIds the array of expected node IDs to set
     */
    fun setExpectedIds(expectedIds: ShortArray) {
        this.expectedIds = ClusterIdRegistry(*expectedIds)
    }

    /**
     * Sets the expected node identifiers for this synchronization message.
     *
     * @param expectedIds the array of expected node IDs to set
     */
    fun setExpectedIds(expectedIds: ClusterIdRegistry?) {
        this.expectedIds = expectedIds
    }

    /**
     * Deserializes the synchronization message from a DataInputStream.
     *
     * @param in the DataInputStream to read from
     * @throws IOException if an I/O error occurs during deserialization
     */
    @Throws(IOException::class)
    override fun deserialize(`in`: ByteBuf) {
        id = `in`.readShort()
        type = MessageType.entries[`in`.readByte().toInt()]
        sequence = `in`.readByte()
        isInStartup = `in`.readBoolean()
        var mode = `in`.readByte()
        syncMode = if ((mode == SyncMode.SYNC_CLUSTER.mode)) SyncMode.SYNC_CLUSTER else SyncMode.SYNC_MESSAGE
        mode = `in`.readByte()
        syncType = SynchronisationType.Companion.getByValue(mode)
        var len = `in`.readByte().toInt()
        if (len > 0) {
            keyChain = ArrayList()
            for (i in 0 until len) {
                keyChain?.add(readUTF(`in`))
            }
        }
        len = `in`.readShort().toInt()
        if (len > 0) {
            val expectedIds = ShortArray(len)
            for (i in 0 until len) {
                expectedIds[i] = `in`.readShort()
            }
            this.expectedIds = ClusterIdRegistry(*expectedIds)
        }
        len = `in`.readInt()
        if (len > 0) {
            for (i in 0 until len) {
                var contentLen = `in`.readInt()
                val message = if ((contentLen > 0)) ByteArray(contentLen) else null
                if (message != null) {
                    `in`.readBytes(message)
                }
                val version = `in`.readLong()
                val key = readUTF(`in`)
                contentLen = `in`.readShort().toInt()

                val awareIds = if ((contentLen > 0)) ShortArray(contentLen) else ShortArray(0)

                for (j in 0 until contentLen) {
                    awareIds[j] = `in`.readShort()
                }

                val s = SyncContent(
                    key, version,
                    ClusterIdRegistry(*awareIds), message
                )
                contents.add(s)
            }
        }
    }

    /**
     * Serializes the synchronization message to a DataOutputStream.
     *
     * @param out the DataOutputStream to write to
     * @throws IOException if an I/O error occurs during serialization
     */
    @Throws(IOException::class)
    override fun serialize(out: ByteBuf) {
        out.writeShort(id.toInt())
        out.writeByte(type.ordinal)
        out.writeByte(sequence.toInt())
        out.writeBoolean(isInStartup)
        out.writeByte(syncMode.mode.toInt())
        out.writeByte(syncType!!.value.toInt())
        val keys = getKeyChain()
        if (keys != null) {
            out.writeByte(keys.size)
            for (key in keys) {
                writeUTF(out, key)
            }
        } else {
            out.writeByte(0)
        }
        if (this.expectedIds == null || expectedIds!!.size() == 0) {
            out.writeShort(0)
        } else {
            out.writeShort(expectedIds!!.size())
            for (expectedId in expectedIds!!.ids) {
                out.writeShort(expectedId.toInt())
            }
        }

        out.writeInt(contents.size)
        if (contents.size == 0) {
            return
        }

        for (c in contents) {
            if (c.content == null || c.content.size == 0) {
                out.writeInt(0)
            } else {
                out.writeInt(c.content.size)
                out.writeBytes(c.content)
            }
            out.writeLong(c.version)
            writeUTF(out, c.key)
            if (c.awareIds == null || c.awareIds.size() == 0) {
                out.writeShort(0)
            } else {
                out.writeShort(c.awareIds.size())
                for (aShort in c.awareIds.ids) {
                    out.writeShort(aShort.toInt())
                }
            }
        }
    }

    fun writeUTF(out: ByteBuf, value: String?) {
        if (value == null) {
            out.writeShort(0) // Write length of 0 for null
            return
        }
        val bytes: ByteArray = value.toByteArray(StandardCharsets.UTF_8)
        out.writeShort(bytes.size) // Write the length of the string
        out.writeBytes(bytes) // Write the string bytes
    }

    fun readUTF(`in`: ByteBuf): String? {
        val length = `in`.readShort().toInt() // Read the length of the string
        if (length == 0) {
            return null // Return null for length 0
        }
        val bytes = ByteArray(length)
        `in`.readBytes(bytes) // Read the string bytes
        return String(bytes, StandardCharsets.UTF_8) // Convert bytes to String
    }

    companion object {
        // Constants for scheduling and startup status
        const val SCHEDULED: Byte = 1
        const val NOT_SCHEDULED: Byte = 0
        const val IN_STARTUP: Byte = 1
        const val NOT_IN_STARTUP: Byte = 0
        const val SEQ_MAX: Byte = 4
    }
}
