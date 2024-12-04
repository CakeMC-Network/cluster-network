package test

import net.cakemc.library.cluster.SynchronisationType
import net.cakemc.library.cluster.api.Authentication.Companion.make
import net.cakemc.library.cluster.api.ClusterContext.Companion.make
import net.cakemc.library.cluster.api.MemberIdentifier.Companion.of
import net.cakemc.library.cluster.api.SoftPublication

object ClusterNetworkTest {

  @JvmStatic
  fun main(args: Array<String>) {
    val first = Thread {
      startClusterNode(1, 7001)
    }
    first.start()

    val second = Thread {
      startClusterNode(2, 7002)
    }
    second.start()
  }

  fun startClusterNode(port: Int, id: Int) {
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
        .priorities(1, 2)
        // own information
        .identifier(id, "127.0.0.1", port)
        // cluster infrastructure
        .members(
          of(1, "127.0.0.1", 7001),
          of(2, "127.0.0.1", 7002),
        ) // authing inside the cluster
        .authentication(authentication)
        .get()
  }

}