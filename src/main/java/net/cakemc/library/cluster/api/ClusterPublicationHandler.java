package net.cakemc.library.cluster.api;

import io.netty.channel.Channel;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler;
import net.cakemc.library.cluster.network.NetworkSession;

public class ClusterPublicationHandler extends AbstractConnectionHandler {

	private final ClusterContext context;

	public ClusterPublicationHandler(ClusterContext context) {
		this.context = context;
	}

	@Override
	public void handlePacket(Channel sender, Publication ringPacket) {
		this.context.callBack(
			 new NetworkSession(sender), ringPacket,
			 context.getRegistry(), context.getProtocolBundle()
		);
	}

}
