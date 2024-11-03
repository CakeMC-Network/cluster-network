package net.cakemc.library.cluster.config

/**
 * Record representing a node identifier in the cluster, containing the node's ID,
 * hostname, and port number.
 *
 *
 * The NodeIdentifier is used to uniquely identify a node in the cluster communication
 * system. It provides methods for creating an instance from a host string and obtaining
 * an InetSocketAddress for network communication.
 *
 * @param id   the unique ID of the node
 * @param host the hostname or IP address of the node
 * @param port the port number of the node
 */
data class NodeIdentifier(val id: Int, val host: String, val port: Int) {

    companion object {
        /**
         * Creates a NodeIdentifier from a host string formatted as "id:host:port".
         *
         * @param hostString the string representing the node in the format "id:host:port"
         * @return a new NodeIdentifier instance based on the provided host string
         * @throws NumberFormatException if the id or port cannot be parsed as numbers
         */
        fun of(hostString: String): NodeIdentifier {
            val elements: Array<String> = hostString.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            val id: Short = elements[0].toShort()
            val host = elements[1]
            val port: Int = elements[2].toInt()

            return NodeIdentifier(id.toInt(), host, port)
        }
    }
}
