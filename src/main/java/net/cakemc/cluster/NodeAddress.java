package net.cakemc.cluster;

import java.net.InetSocketAddress;

/**
 * Represents a network address for a node in the cluster.
 * This record encapsulates the node's unique identifier, host, and port.
 *
 * @param id   The unique identifier for the node.
 * @param host The hostname or IP address of the node.
 * @param port The port number on which the node is listening.
 */
public record NodeAddress(int id, String host, int port) {

	/**
	 * Converts this NodeAddress to an {@link InetSocketAddress}.
	 *
	 * @return An InetSocketAddress representing this NodeAddress.
	 */
	public InetSocketAddress toInet() {
		return new InetSocketAddress(host, port);
	}

	@Override
	public String toString() {
		return "NodeAddress{" +
		       "id=" + id +
		       ", host='" + host + '\'' +
		       ", port=" + port +
		       '}';
	}
}
