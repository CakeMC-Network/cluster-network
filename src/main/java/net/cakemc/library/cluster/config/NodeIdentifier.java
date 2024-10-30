package net.cakemc.library.cluster.config;

import java.net.InetSocketAddress;

/**
 * Record representing a node identifier in the cluster, containing the node's ID,
 * hostname, and port number.
 *
 * <p>The NodeIdentifier is used to uniquely identify a node in the cluster communication
 * system. It provides methods for creating an instance from a host string and obtaining
 * an InetSocketAddress for network communication.</p>
 *
 * @param id   the unique ID of the node
 * @param host the hostname or IP address of the node
 * @param port the port number of the node
 */
public record NodeIdentifier(int id, String host, int port) {

	/**
	 * Creates a NodeIdentifier from a host string formatted as "id:host:port".
	 *
	 * @param hostString the string representing the node in the format "id:host:port"
	 * @return a new NodeIdentifier instance based on the provided host string
	 * @throws NumberFormatException if the id or port cannot be parsed as numbers
	 */
	public static NodeIdentifier of(String hostString) {
		String[] elements = hostString.split(":");

		short id = Short.parseShort(elements[0]);
		String host = elements[1];
		int port = Integer.parseInt(elements[2]);

		return new NodeIdentifier(id, host, port);
	}

}
