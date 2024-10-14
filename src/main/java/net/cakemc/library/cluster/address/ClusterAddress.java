package net.cakemc.library.cluster.address;

import java.net.InetAddress;

/**
 * Represents a network address for a cluster node, including the IP address and port.
 *
 * <p>The {@code ClusterAddress} class extends {@link StorableSocketAddress} to provide
 * a convenient way to store and manage addresses in a cluster environment.</p>
 */
public class ClusterAddress extends StorableSocketAddress {

	/**
	 * Default constructor for {@code ClusterAddress}.
	 *
	 * <p>This constructor initializes the address with default values.</p>
	 */
	public ClusterAddress() {
		super(); // Explicitly call the superclass constructor
	}

	/**
	 * Constructs a {@code ClusterAddress} with the specified IP address and port.
	 *
	 * @param address the {@code InetAddress} representing the IP address
	 * @param port the port number
	 */
	public ClusterAddress(InetAddress address, int port) {
		super(address, port);
	}

	/**
	 * Constructs a {@code ClusterAddress} with the specified hostname and port.
	 *
	 * @param hostname the hostname as a {@code String}
	 * @param port the port number
	 */
	public ClusterAddress(String hostname, int port) {
		super(hostname, port);
	}

	/**
	 * Returns a string representation of the cluster address in the format "IP:port".
	 *
	 * @return a string representation of the address
	 */
	@Override
	public String toString() {
		return String.format("%s:%d", getAddress().getHostAddress(), getPort());
	}
}
