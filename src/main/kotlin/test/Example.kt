package test

import net.cakemc.library.cluster.Session
import net.cakemc.library.cluster.SynchronisationType
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.api.Authentication.Companion.make
import net.cakemc.library.cluster.api.ClusterContext.Companion.make
import net.cakemc.library.cluster.api.MemberIdentifier.Companion.of
import net.cakemc.library.cluster.api.SoftPublication
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.codec.PublicationBundle

object Example {
  @JvmStatic
  fun main(args: Array<String>) {
    // creating authentication "config"
    val authentication = make()
      .useVerification()
      .withKey("my-key")
      .get()

    // creating a cluster Context
    val context =
      // own node id
      make(1)
        // registering an outgoing channel
        .register("test")
        // registering the publication type
        .withPublicationType(SoftPublication::class.java)
        // cluster type
        .type(SynchronisationType.RING)
        // prioritised nodes
        .priorities(1, 2, 3)
        // own information
        .identifier(1, "127.0.0.1", 7001)
        // cluster infrastructure
        .members(
          of(1, "127.0.0.1", 7001),
          of(2, "127.0.0.1", 7002),
          of(3, "127.0.0.1", 7003)
        ) // authing inside the cluster
        .authentication(authentication)
        .get()

    // subscribing to a channel and listening to publications
    context.subscribe(
      "test"
    ) { session: Session?, message: Publication?, withNodes: ClusterIdRegistry?, out: PublicationBundle? -> true }

    // creating a publisher
    context.publisher()
      // skipping own id
      .skip(1)
      // sending type
      .synchronisationType(SynchronisationType.UNI_CAST)
      // returning result
      .callBack { session: Session?, message: Publication?, withNodes: ClusterIdRegistry?, out: PublicationBundle? -> true }
      // sending a publication
      .release(
        SoftPublication(
          "test", byteArrayOf(1, 3, 3, 7),
          1
        )
      )
  }
}