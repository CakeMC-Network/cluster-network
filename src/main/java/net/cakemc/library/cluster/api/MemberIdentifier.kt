package net.cakemc.library.cluster.api

import net.cakemc.library.cluster.address.ClusterAddress

/**
 * The `MemberIdentifier` class represents a member in a cluster, identified by a unique ID and associated
 * with a [ClusterAddress]. This class provides utility methods for creating member identifiers and accessing
 * their respective IDs and addresses.
 */
class MemberIdentifier {
    /**
     * Returns the unique ID of the member.
     *
     * @return the unique ID of the member.
     */
    val id: Short

    /**
     * Returns the [ClusterAddress] of the member.
     *
     * @return the [ClusterAddress] of the member.
     */
    val address: ClusterAddress

    /**
     * Constructs a `MemberIdentifier` with the specified ID and [ClusterAddress].
     *
     * @param id      the unique ID of the member.
     * @param address the [ClusterAddress] of the member.
     */
    constructor(id: Int, address: ClusterAddress) {
        this.id = id.toShort()
        this.address = address
    }

    /**
     * Constructs a `MemberIdentifier` with the specified ID, host, and port.
     *
     * @param id    the unique ID of the member.
     * @param host  the host address of the member.
     * @param port  the port number of the member.
     */
    internal constructor(id: Int, host: String, port: Int) {
        this.id = id.toShort()
        this.address = ClusterAddress(host, port)
    }

    companion object {
        /**
         * Creates a new `MemberIdentifier` with the specified ID, host, and port.
         *
         * @param id    the unique ID of the member.
         * @param host  the host address of the member.
         * @param port  the port number of the member.
         * @return a new `MemberIdentifier` with the given ID, host, and port.
         */
        @kotlin.jvm.JvmStatic
        fun of(id: Int, host: String, port: Int): MemberIdentifier {
            return MemberIdentifier(id, host, port)
        }

        /**
         * Creates a new `MemberIdentifier` with the specified ID and [ClusterAddress].
         *
         * @param id      the unique ID of the member.
         * @param address the [ClusterAddress] of the member.
         * @return a new `MemberIdentifier` with the given ID and address.
         */
        fun of(id: Int, address: ClusterAddress): MemberIdentifier {
            return MemberIdentifier(id, address)
        }
    }
}
