package test;

import net.cakemc.library.cluster.*;
import net.cakemc.library.cluster.api.Authentication;
import net.cakemc.library.cluster.api.ClusterContext;
import net.cakemc.library.cluster.api.MemberIdentifier;
import net.cakemc.library.cluster.api.SoftPublication;

public class Example {

	public static void main(String[] args) {
		Authentication authentication = Authentication
			 .make()
			 .useVerification()
			 .withKey("my-key")
			 .get();

		ClusterContext context = ClusterContext
			 .make(1)
			 .register("test")
			 .withPublicationType(SoftPublication.class)
			 .type(SynchronisationType.RING)
			 .priorities(1, 2, 3)
			 .identifier(1, "127.0.0.1", 7001)
			 .members(
				   MemberIdentifier.of(1, "127.0.0.1", 7001),
				   MemberIdentifier.of(2, "127.0.0.1", 7002),
				   MemberIdentifier.of(3, "127.0.0.1", 7003)
			 )
			 .authentication(authentication)
			 .get();

		context.subscribe("test", (session, message, withNodes, out) -> {

			return true;
		});

		context.publisher()
			 .skip(1)
			 .synchronisationType(SynchronisationType.UNI_CAST)
			 .callBack((session, message, withNodes, out) -> {

				 return true;
			 })
			 .release(new SoftPublication(
				  1l, 1l, 1,
				  "test", new byte[] { 1, 3, 3, 7 },
				  1
			 ));
	}

}
