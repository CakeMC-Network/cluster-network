package net.cakemc.library.cluster.address

import java.net.InetAddress

/**
 * Represents a network address for a cluster node, including the IP address and port.
 *
 *
 * The `ClusterAddress` class extends [StorableSocketAddress] to provide
 * a convenient way to store and manage addresses in a cluster environment.
 */
class ClusterAddress : StorableSocketAddress {
    /**
     * Default constructor for `ClusterAddress`.
     *
     *
     * This constructor initializes the address with default values.
     */
    constructor() : super() // Explicitly call the superclass constructor


    /**
     * Constructs a `ClusterAddress` with the specified IP address and port.
     *
     * @param address the `InetAddress` representing the IP address
     * @param port the port number
     */
    constructor(address: InetAddress?, port: Int) : super(address, port)

    /**
     * Constructs a `ClusterAddress` with the specified hostname and port.
     *
     * @param hostname the hostname as a `String`
     * @param port the port number
     */
    constructor(hostname: String, port: Int) : super(hostname, port)

    /**
     * Returns a string representation of the cluster address in the format "IP:port".
     *
     * @return a string representation of the address
     */
    override fun toString(): String {
        return String.format("%s:%d", address?.hostString, port)
    }
}
