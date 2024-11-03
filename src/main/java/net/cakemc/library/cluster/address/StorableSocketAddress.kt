package net.cakemc.library.cluster.address;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Represents a storable socket address that can be serialized and deserialized.
 *
 * <p>This class implements the {@link Externalizable} interface, allowing instances
 * of this class to be serialized to and deserialized from a byte stream.</p>
 */
public abstract class StorableSocketAddress implements Externalizable {

	private InetSocketAddress address;

	/**
	 * Default constructor for Externalizable.
	 * <p>This constructor is required for the {@link Externalizable} interface.</p>
	 */
	public StorableSocketAddress() {
		// Required for Externalizable
	}

	/**
	 * Constructs a {@code StorableSocketAddress} with the specified InetAddress and port.
	 *
	 * @param address the InetAddress of the socket
	 * @param port the port number
	 */
	public StorableSocketAddress(final InetAddress address, final int port) {
		this.address = new InetSocketAddress(address, port);
	}

	/**
	 * Constructs a {@code StorableSocketAddress} with the specified hostname and port.
	 *
	 * @param hostname the hostname of the socket
	 * @param port the port number
	 */
	public StorableSocketAddress(final String hostname, final int port) {
		this.address = new InetSocketAddress(hostname, port);
	}

	/**
	 * Retrieves the InetAddress of the socket.
	 *
	 * @return the InetAddress of the socket
	 */
	public InetAddress getAddress() {
		return address.getAddress();
	}

	/**
	 * Retrieves the port number of the socket.
	 *
	 * @return the port number
	 */
	public int getPort() {
		return address.getPort();
	}

	/**
	 * Writes the socket address to the specified ObjectOutput stream.
	 *
	 * @param out the ObjectOutput stream to write the address to
	 * @throws IOException if an I/O error occurs during writing
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		byte[] addressBytes = getAddress().getAddress();
		out.writeShort(addressBytes.length);
		out.write(addressBytes);
		out.writeInt(getPort());
	}

	/**
	 * Reads the socket address from the specified ObjectInput stream.
	 *
	 * @param in the ObjectInput stream to read the address from
	 * @throws IOException if an I/O error occurs during reading
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException {
		short length = in.readShort();
		InetAddress inetAddress = null;

		if (length > 0) {
			byte[] addressBytes = new byte[length];
			in.readFully(addressBytes);
			inetAddress = InetAddress.getByAddress(addressBytes);
		}

		int port = in.readInt();
		this.address = new InetSocketAddress(inetAddress, port);
	}

	/**
	 * Compares this socket address to the specified object for equality.
	 *
	 * @param obj the object to compare to
	 * @return {@code true} if the specified object is equal to this socket address, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof StorableSocketAddress other) {
			return this.address.equals(other.address);
		}
		return false;
	}

	/**
	 * Returns a hash code value for this socket address.
	 *
	 * @return a hash code value for this socket address
	 */
	@Override
	public int hashCode() {
		return address != null ? address.hashCode() : 0;
	}

	/**
	 * Returns a string representation of this socket address.
	 *
	 * @return a string representation of this socket address
	 */
	@Override
	public String toString() {
		return address.toString();
	}
}
