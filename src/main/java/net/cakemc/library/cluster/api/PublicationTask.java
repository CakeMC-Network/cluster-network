package net.cakemc.library.cluster.api;

import net.cakemc.library.cluster.Session;
import net.cakemc.library.cluster.SynchronisationType;
import net.cakemc.library.cluster.address.ClusterIdRegistry;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.PublicationBundle;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;
import net.cakemc.library.cluster.handler.PublicationHandler;
import net.cakemc.library.cluster.handler.SyncResult;

import java.util.Arrays;
import java.util.Map;

/**
 * The {@code PublicationTask} class handles the dispatch and synchronization of publications
 * within a cluster environment. It provides methods for configuring publication types, skipping
 * specific nodes, and setting synchronization callbacks. This class implements the
 * {@link PublicationHandler} interface to handle publication callbacks and results.
 */
public class PublicationTask implements PublicationHandler {

	private final ClusterContext context;
	private Publication message;

	private PublicationHandler handler;
	private SynchronisationType type;
	private Class<? extends Publication> publicationType;
	private short[] nodesToSkip;

	public long dispatchTime;

	/**
	 * Constructs a {@code PublicationTask} with the given cluster context.
	 *
	 * @param context the {@link ClusterContext} instance that provides context for the publication task.
	 */
	public PublicationTask(ClusterContext context) {
		this.context = context;

		this.nodesToSkip = new short[0];
		this.type = context.getType();
		this.handler = this;

		this.publicationType = SoftPublication.class;
	}

	/**
	 * Sets a callback handler for the publication task.
	 *
	 * @param handler the {@link PublicationHandler} instance to be set as the callback handler.
	 * @return the current {@code PublicationTask} instance.
	 */
	public PublicationTask callBack(PublicationHandler handler) {
		this.handler = handler;
		return this;
	}

	/**
	 * Sets the synchronization type for the publication task.
	 *
	 * @param type the {@link SynchronisationType} to be used for synchronization.
	 * @return the current {@code PublicationTask} instance.
	 */
	public PublicationTask synchronisationType(SynchronisationType type) {
		this.type = type;
		return this;
	}

	/**
	 * Sets the publication type for the task.
	 *
	 * @param publicationType the class of the {@link Publication} type to be used.
	 * @return the current {@code PublicationTask} instance.
	 */
	public PublicationTask publicationType(Class<? extends Publication> publicationType) {
		this.publicationType = publicationType;
		return this;
	}

	/**
	 * Skips the specified nodes during the publication process.
	 *
	 * @param nodesToSkip an array of node IDs to be skipped during publication.
	 * @return the current {@code PublicationTask} instance.
	 */
	public PublicationTask skip(int... nodesToSkip) {
		short[] toSkip = new short[nodesToSkip.length];
		for (int index = 0; index < nodesToSkip.length; index++) {
			toSkip[index] = (short) nodesToSkip[index];
		}
		this.nodesToSkip = toSkip;
		return this;
	}

	/**
	 * Releases the provided publication for synchronization across the cluster.
	 *
	 * @param publication the {@link Publication} instance to be synchronized.
	 */
	public void release(Publication publication) {
		context.getContext()
		       .make(type)
		       .withCallBack(handler)
		       .withoutCluster(nodesToSkip)
		       .withPublicationType(publication.getClass())
		       .sync(publication);

		if (publication instanceof RingBackPacket ringBackPacket)
			this.context.getBackUpEndpoint().dispatchPacketToRing(ringBackPacket);
	}

	/**
	 * Releases multiple publications for synchronization across the cluster.
	 *
	 * @param publications an array of {@link Publication} instances to be synchronized.
	 */
	public void releaseMulti(Publication... publications) {
		context.getContext()
		       .make(type)
		       .withCallBack(handler)
		       .withoutCluster(nodesToSkip)
		       .withPublicationType(publicationType)
		       .sync(Arrays.stream(publications).toList());

		for (Publication publication : publications) {
			if (publication instanceof RingBackPacket ringBackPacket)
				this.context.getBackUpEndpoint().dispatchPacketToRing(ringBackPacket);
		}
	}

	/**
	 * Handles the callback for the publication process.
	 * This method is part of the {@link PublicationHandler} interface and determines
	 * whether the callback is successful or not.
	 *
	 * @param session   the {@link Session} associated with the callback.
	 * @param message   the {@link Publication} message associated with the callback.
	 * @param withNodes the {@link ClusterIdRegistry} containing the cluster nodes.
	 * @param out       the {@link PublicationBundle} containing the outgoing publication bundle.
	 * @return {@code false} as the default implementation.
	 */
	@Override
	public boolean callBack(Session session, Publication message, ClusterIdRegistry withNodes, PublicationBundle out) {
		return false;
	}

	/**
	 * Processes the result of the publication synchronization.
	 * This method is part of the {@link PublicationHandler} interface.
	 *
	 * @param syncFeature a map containing the {@link SyncResult} for each synchronization feature.
	 */
	@Override
	public void result(Map<String, SyncResult> syncFeature) {

	}
}
