package net.cakemc.cluster;

import java.net.InetSocketAddress;

public record NodeAddress(long id, String host, int port) {
	public InetSocketAddress toInet() {
		return new InetSocketAddress(host, port);
	}
}
