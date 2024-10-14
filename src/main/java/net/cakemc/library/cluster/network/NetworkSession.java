package net.cakemc.library.cluster.network;

import net.cakemc.library.cluster.ChannelAttributes;
import net.cakemc.library.cluster.Session;
import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * Represents a network session associated with a Netty channel.
 *
 * <p>The NetworkSession class encapsulates details about the remote and local addresses
 * of a connection, along with its startup state. It implements the Session interface to
 * provide a standard way to access session-related information.</p>
 */
public class NetworkSession implements Session {

	private final SocketAddress remoteAddress;
	private final SocketAddress localAddress;
	private final boolean inStartup;

	/**
	 * Constructs a NetworkSession from a given Netty channel.
	 *
	 * @param channel the Netty channel representing the network connection
	 */
	public NetworkSession(Channel channel) {
		this.remoteAddress = channel.remoteAddress();
		this.localAddress = channel.localAddress();

		this.inStartup = (boolean) channel.attr(ChannelAttributes.STARTUP_STATE.getKey()).get();
	}

	/**
	 * Retrieves the remote address of the session.
	 *
	 * @return the remote SocketAddress of the connected peer
	 */
	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * Retrieves the local address of the session.
	 *
	 * @return the local SocketAddress of this session
	 */
	@Override
	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	/**
	 * Checks if the session is in the startup state.
	 *
	 * @return true if the session is in startup, false otherwise
	 */
	@Override
	public boolean isInStartup() {
		return inStartup;
	}
}
