package net.cakemc.library.cluster.fallback.endpoint

/**
 * Represents the type of endpoint in the cluster communication.
 *
 *
 * The `EndpointType` enum defines the different roles that endpoints can have
 * in the cluster architecture. This can be used to distinguish between server and
 * client endpoints when managing connections and handling backPackets.
 *
 *
 *  * **SERVER**: Represents an endpoint that acts as a server, capable of accepting
 * connections from clients and handling their requests.
 *  * **CLIENT**: Represents an endpoint that acts as a client, initiating connections
 * to servers and sending requests.
 *
 */
enum class EndpointType {
    /**
     * Represents a server endpoint.
     */
    SERVER,

    /**
     * Represents a client endpoint.
     */
    CLIENT
}
