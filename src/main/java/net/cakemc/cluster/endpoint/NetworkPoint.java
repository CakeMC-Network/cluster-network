package net.cakemc.cluster.endpoint;

public abstract class NetworkPoint {

	private final String host;
	private final int port;

	public NetworkPoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public abstract void connect();

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
