package net.cakemc.library.cluster.api

import io.netty.channel.Channel
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler
import net.cakemc.library.cluster.network.NetworkSession

class ClusterPublicationHandler(private val context: ClusterContext) : AbstractConnectionHandler() {
  override fun handlePacket(sender: Channel, ringPacket: Publication) {
    context.protocolBundle?.let {
      context.registry?.let { currentId ->
        context.callBack(
          NetworkSession(sender), ringPacket,
          currentId, it
        )
      }
    }
  }
}
