package net.cakemc.library.cluster.address

import java.io.Externalizable
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectOutput
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Represents a storable socket address that can be serialized and deserialized.
 *
 *
 * This class implements the [Externalizable] interface, allowing instances
 * of this class to be serialized to and deserialized from a byte stream.
 */
abstract class StorableSocketAddress : Externalizable {
    var address: InetSocketAddress? = null

    /**
     * Default constructor for Externalizable.
     *
     * This constructor is required for the [Externalizable] interface.
     */
    constructor()

    /**
     * Constructs a `StorableSocketAddress` with the specified InetAddress and port.
     *
     * @param address the InetAddress of the socket
     * @param port the port number
     */
    constructor(address: InetAddress?, port: Int) {
        this.address = InetSocketAddress(address, port)
    }

    /**
     * Constructs a `StorableSocketAddress` with the specified hostname and port.
     *
     * @param hostname the hostname of the socket
     * @param port the port number
     */
    constructor(hostname: String, port: Int) {
        this.address = InetSocketAddress(hostname, port)
    }

    /**
     * Retrieves the InetAddress of the socket.
     *
     * @return the InetAddress of the socket
     */
    fun getAddress(): InetAddress {
        return address!!.address
    }

    val port: Int
        /**
         * Retrieves the port number of the socket.
         *
         * @return the port number
         */
        get() = address!!.port

    /**
     * Writes the socket address to the specified ObjectOutput stream.
     *
     * @param out the ObjectOutput stream to write the address to
     * @throws IOException if an I/O error occurs during writing
     */
    @Throws(IOException::class)
    override fun writeExternal(out: ObjectOutput) {
        val addressBytes = getAddress().address
        out.writeShort(addressBytes.size)
        out.write(addressBytes)
        out.writeInt(port)
    }

    /**
     * Reads the socket address from the specified ObjectInput stream.
     *
     * @param in the ObjectInput stream to read the address from
     * @throws IOException if an I/O error occurs during reading
     */
    @Throws(IOException::class)
    override fun readExternal(`in`: ObjectInput) {
        val length = `in`.readShort()
        var inetAddress: InetAddress? = null

        if (length > 0) {
            val addressBytes = ByteArray(length.toInt())
            `in`.readFully(addressBytes)
            inetAddress = InetAddress.getByAddress(addressBytes)
        }

        val port = `in`.readInt()
        this.address = InetSocketAddress(inetAddress, port)
    }

    /**
     * Compares this socket address to the specified object for equality.
     *
     * @param obj the object to compare to
     * @return `true` if the specified object is equal to this socket address, `false` otherwise
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj is StorableSocketAddress) {
            return this.address == obj.address
        }
        return false
    }

    /**
     * Returns a hash code value for this socket address.
     *
     * @return a hash code value for this socket address
     */
    override fun hashCode(): Int {
        return if (address != null) address!!.hashCode() else 0
    }

    /**
     * Returns a string representation of this socket address.
     *
     * @return a string representation of this socket address
     */
    override fun toString(): String {
        return address.toString()
    }
}
