package net.cakemc.cluster.endpoint;

/**
 * Represents the type of endpoint in the cluster communication.
 *
 * <p>The {@code EndpointType} enum defines the different roles that endpoints can have
 * in the cluster architecture. This can be used to distinguish between server and
 * client endpoints when managing connections and handling packets.</p>
 *
 * <ul>
 *     <li><b>SERVER</b>: Represents an endpoint that acts as a server, capable of accepting
 *     connections from clients and handling their requests.</li>
 *     <li><b>CLIENT</b>: Represents an endpoint that acts as a client, initiating connections
 *     to servers and sending requests.</li>
 * </ul>
 */
public enum EndpointType {
	/**
	 * Represents a server endpoint.
	 */
	SERVER,

	/**
	 * Represents a client endpoint.
	 */
	CLIENT
}
