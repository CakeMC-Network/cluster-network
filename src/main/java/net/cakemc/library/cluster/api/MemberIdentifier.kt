package net.cakemc.library.cluster.api;

import net.cakemc.library.cluster.address.ClusterAddress;

/**
 * The {@code MemberIdentifier} class represents a member in a cluster, identified by a unique ID and associated
 * with a {@link ClusterAddress}. This class provides utility methods for creating member identifiers and accessing
 * their respective IDs and addresses.
 */
public class MemberIdentifier {

	private final short id;
	private final ClusterAddress address;

	/**
	 * Constructs a {@code MemberIdentifier} with the specified ID and {@link ClusterAddress}.
	 *
	 * @param id      the unique ID of the member.
	 * @param address the {@link ClusterAddress} of the member.
	 */
	public MemberIdentifier(int id, ClusterAddress address) {
		this.id = (short) id;
		this.address = address;
	}

	/**
	 * Constructs a {@code MemberIdentifier} with the specified ID, host, and port.
	 *
	 * @param id    the unique ID of the member.
	 * @param host  the host address of the member.
	 * @param port  the port number of the member.
	 */
	MemberIdentifier(int id, String host, int port) {
		this.id = (short) id;
		this.address = new ClusterAddress(host, port);
	}

	/**
	 * Creates a new {@code MemberIdentifier} with the specified ID, host, and port.
	 *
	 * @param id    the unique ID of the member.
	 * @param host  the host address of the member.
	 * @param port  the port number of the member.
	 * @return a new {@code MemberIdentifier} with the given ID, host, and port.
	 */
	public static MemberIdentifier of(int id, String host, int port) {
		return new MemberIdentifier(id, host, port);
	}

	/**
	 * Creates a new {@code MemberIdentifier} with the specified ID and {@link ClusterAddress}.
	 *
	 * @param id      the unique ID of the member.
	 * @param address the {@link ClusterAddress} of the member.
	 * @return a new {@code MemberIdentifier} with the given ID and address.
	 */
	public static MemberIdentifier of(int id, ClusterAddress address) {
		return new MemberIdentifier(id, address);
	}

	/**
	 * Returns the unique ID of the member.
	 *
	 * @return the unique ID of the member.
	 */
	public short getId() {
		return id;
	}

	/**
	 * Returns the {@link ClusterAddress} of the member.
	 *
	 * @return the {@link ClusterAddress} of the member.
	 */
	public ClusterAddress getAddress() {
		return address;
	}
}
