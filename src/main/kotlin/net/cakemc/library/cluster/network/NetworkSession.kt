package net.cakemc.library.cluster.network

import io.netty.channel.Channel
import net.cakemc.library.cluster.ChannelAttributes
import net.cakemc.library.cluster.Session
import java.net.SocketAddress

/**
 * Represents a network session associated with a Netty channel.
 *
 *
 * The NetworkSession class encapsulates details about the remote and local addresses
 * of a connection, along with its startup state. It implements the Session interface to
 * provide a standard way to access session-related information.
 */
class NetworkSession(channel: Channel) : Session {
    /**
     * Retrieves the remote address of the session.
     *
     * @return the remote SocketAddress of the connected peer
     */
    override val remoteAddress: SocketAddress = channel.remoteAddress()

    /**
     * Retrieves the local address of the session.
     *
     * @return the local SocketAddress of this session
     */
    override val localAddress: SocketAddress = channel.localAddress()

    /**
     * Checks if the session is in the startup state.
     *
     * @return true if the session is in startup, false otherwise
     */

    override val isInStartup: Boolean =
        channel.attr(ChannelAttributes.STARTUP_STATE.getKey<Any>()).get() as Boolean
}
