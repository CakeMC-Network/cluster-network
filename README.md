# Cluster-Network

Creating a cluster-node:
````java

package test;

import net.cakemc.library.cluster.SynchronisationType;
import net.cakemc.library.cluster.api.Authentication;
import net.cakemc.library.cluster.api.ClusterContext;
import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.api.SoftPublication;

public class Example {

  public static void main(String[] args) {
    // creating authentication "config"
    Authentication authentication = Authentication
            .make()
            .useVerification()
            .withKey("my-key")
            .get();

    // creating a cluster Context
    ClusterContext context = ClusterContext
            // own node id
            .make(1)
            // registering a outgoing channel
            .register("test")
            // registering the publication type
            .withPublicationType(SoftPublication.class)
            // cluster type
            .type(SynchronisationType.RING)
            // prioritised nodes
            .priorities(1, 2, 3)
            // own information
            .identifier(1, "127.0.0.1", 7001)
            // cluster infrastructure
            .members(
                    MemberIdentifier.of(1, "127.0.0.1", 7001),
                    MemberIdentifier.of(2, "127.0.0.1", 7002),
                    MemberIdentifier.of(3, "127.0.0.1", 7003)
            )
            // authing inside the cluster
            .authentication(authentication)
            .get();

    // subscribing to a channel and listening to publications
    context.subscribe("test", (session, message, withNodes, out) -> {
      // incoming publications of other nodes to this channel
      return true;
    });

    // creating a publisher 
    context.publisher()
            // skipping own id
            .skip(1)
            // sending type
            .synchronisationType(SynchronisationType.UNI_CAST)
            // returning result
            .callBack((session, message, withNodes, out) -> {
              // will be called on a result
              return true;
            })
            // sending a publication
            .release(new SoftPublication(
                    "test", new byte[]{ 1, 3, 3, 7 },
                    1
            ));
  }

}
````