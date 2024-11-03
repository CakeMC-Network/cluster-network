package net.cakemc.library.cluster;

import java.net.SocketAddress;

public interface Session {

	/**
	 * Gets the remote address of the session.
	 *
	 * @return the remote SocketAddress
	 */
	SocketAddress getRemoteAddress();

	/**
	 * Gets the local address of the session.
	 *
	 * @return the local SocketAddress
	 */
	SocketAddress getLocalAddress();

	/**
	 * Checks if the session is in the startup phase.
	 *
	 * @return true if the session is in startup, false otherwise
	 */
	boolean isInStartup();
}
