package net.cakemc.library.cluster.api

import io.netty.buffer.ByteBuf
import net.cakemc.library.cluster.codec.*
import java.nio.charset.StandardCharsets

/**
 * The `SoftPublication` class represents a publication within a ring-back packet communication system.
 * This class implements the [Publication] interface,
 * enabling it to represent and handle a publish-subscribe style message with a channel, data, and version information.
 */
class SoftPublication : Publication {
    /**
     * Retrieves the publication channel name.
     *
     * @return the channel name.
     */
    override var channel: String = ""

  /**
     * Retrieves the publication data.
     *
     * @return the data as a byte array.
     */
    var data: ByteArray? = null
      private set

  override var version: Long = 0

    /**
     * Default constructor for creating an empty `SoftPublication` instance.
     */
    constructor()

    /**
     * Constructs a `SoftPublication` instance with the specified packet details and publication data.
     *
     * @param channel the publication channel name.
     * @param data    the data being published as a byte array.
     * @param version the version of the publication.
     */
    constructor(
        channel: String, data: ByteArray?,
        version: Long
    ) {
        this.channel = channel
        this.data = data
        this.version = version
    }

    override val key: String
        /**
         * Retrieves the key associated with this publication, which is the channel name.
         *
         * @return the channel name as the key.
         */
        get() = channel!!

    /**
     * Closes the publication by clearing the data and channel references.
     */
    override fun close() {
        this.data = null
        this.channel = "default"
    }

    /**
     * Configures the publication using the provided configuration map.
     * This method does not currently perform any operations.
     *
     * @param config the configuration map.
     */
    override fun configure(config: Map<String?, *>?) {}

    /**
     * Writes the publication data into the provided [ByteBuf].
     *
     * @param byteBuf the [ByteBuf] to write the publication data into.
     */
    override fun serialize(byteBuf: ByteBuf) {
        byteBuf.writeLong(version)

        byteBuf.writeInt(channel!!.length)
        byteBuf.writeBytes(channel!!.toByteArray(StandardCharsets.UTF_8))

        byteBuf.writeInt(data!!.size)
        byteBuf.writeBytes(this.data)
    }

    /**
     * Reads the publication data from the provided [ByteBuf].
     *
     * @param byteBuf the [ByteBuf] containing the serialized publication data.
     */
    override fun deserialize(byteBuf: ByteBuf) {
        this.version = byteBuf.readLong()

        val channel = ByteArray(byteBuf.readInt())
        byteBuf.readBytes(channel)
        this.channel = String(channel)

        val data = ByteArray(byteBuf.readInt())
        byteBuf.readBytes(data)
        this.data = data
    }
}
