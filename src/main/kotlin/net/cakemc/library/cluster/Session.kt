package net.cakemc.library.cluster

import java.net.SocketAddress

interface Session {
    /**
     * Gets the remote address of the session.
     *
     * @return the remote SocketAddress
     */
    val remoteAddress: SocketAddress

    /**
     * Gets the local address of the session.
     *
     * @return the local SocketAddress
     */
    val localAddress: SocketAddress

    /**
     * Checks if the session is in the startup phase.
     *
     * @return true if the session is in startup, false otherwise
     */
    val isInStartup: Boolean
}
