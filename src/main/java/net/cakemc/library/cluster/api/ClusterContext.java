package net.cakemc.library.cluster.api;

import net.cakemc.library.cluster.*;
import net.cakemc.library.cluster.ClusterMember.MemberState;
import net.cakemc.library.cluster.address.ClusterIdRegistry;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.PublicationBundle;
import net.cakemc.library.cluster.codec.SyncProtocolBundle;
import net.cakemc.library.cluster.config.ClusterIdentificationContext;
import net.cakemc.library.cluster.config.NodeIdentifier;
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint;
import net.cakemc.library.cluster.fallback.BackUpClusterNode;
import net.cakemc.library.cluster.handler.PublicationHandler;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;
import net.cakemc.library.cluster.handler.SyncResult;
import net.cakemc.library.cluster.network.NetworkServer;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The ClusterContext class manages the state and behavior of a cluster, including
 * handling publications and managing cluster members. It provides methods for
 * creating and configuring the cluster context, subscribing to channels, and
 * handling incoming publications.
 */
public class ClusterContext implements PublicationHandler {

	/**
	 * An ExecutorService for managing threads for the cluster context.
	 */
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(
		 new ThreadFactory() {
			 private final AtomicInteger task = new AtomicInteger(0);

			 @Override
			 public Thread newThread(Runnable runnable) {
				 Thread thread = new Thread(runnable, "cluster-context-%s".formatted(
					  task.incrementAndGet()
				 ));
				 thread.setDaemon(true);
				 thread.setPriority(Thread.NORM_PRIORITY);

				 return thread;
			 }
		 }
	);

	public short id;

	// publishing
	public Set<String> channels;
	public Map<String, PublicationHandler> handlerMap;

	// identification
	public Set<MemberIdentifier> members;
	public MemberIdentifier ownAddress;

	// config
	public Class<? extends Publication> publicationType;
	public Authentication authentication;
	public short[] prioritised;

	// snapshot
	private ClusterSnapshot snapshot;

	// type
	public SynchronisationType type;

	// internal usage
	private List<NodeIdentifier> identifiers;
	private List<ClusterMember> clusterMembers;

	private SyncContext context;
	private ClusterIdentificationContext clusterIdentificationContext;

	private ClusterIdRegistry registry;
	private NetworkServer networkServer;
	private SyncNetworkHandler handler;
	private AbstractBackUpEndpoint backUpEndpoint;

	/**
	 * Constructs a new ClusterContext instance.
	 */
	public ClusterContext() {
	}

	/**
	 * Creates an internal cluster context, initializing necessary components, and
	 * starting the network server and backup endpoint.
	 */
	public void createInternalCluster() {
		context = new SyncContext(id);

		handler = context
			 .make()
			 .withCallBack(this)
			 .withPublicationType(SoftPublication.class);

		identifiers = new ArrayList<>();
		clusterMembers = new ArrayList<>();

		for (MemberIdentifier identifier : this.members) {
			identifiers.add(new NodeIdentifier(
				 identifier.getId(),
				 identifier.getAddress().toString(),
				 identifier.getId()
			));
		}

		registry = new ClusterIdRegistry();
		identifiers.forEach(nodeIdentifier -> registry.add((short) nodeIdentifier.id()));

		for (MemberIdentifier identifier : this.members) {
			ClusterMember member = new ClusterMember(
				 identifier.getId(),
				 this.members.stream().map(MemberIdentifier::getAddress).toList(),
				 authentication.isUseAuthentication(), authentication.getAuthKey(),
				 System.currentTimeMillis(), registry,
				 MemberState.VALID
			);

			clusterMembers.add(member);
		}

		clusterIdentificationContext = new ClusterIdentificationContext();
		clusterIdentificationContext.setSocketConfigs(identifiers);

		networkServer = new NetworkServer(handler, clusterIdentificationContext);
		EXECUTOR_SERVICE.execute(networkServer::start);

		clusterMembers.forEach(memberIdentifier -> {
			context.syncCluster(memberIdentifier, this.type);
		});

		backUpEndpoint = new BackUpClusterNode(
			 this.ownAddress, this.members.stream().toList(), authentication.getAuthKey()
		);

		backUpEndpoint.registerPublicationHandler((session, publication) -> {
			this.callBack(session, publication, registry, new SyncProtocolBundle());
		});

		EXECUTOR_SERVICE.execute(backUpEndpoint::start);

		snapshot = context.snapshot;
	}

	/**
	 * Creates a new publication task for the cluster context.
	 *
	 * @return a new instance of {@link PublicationTask} associated with this context.
	 */
	public PublicationTask publisher() {
		PublicationTask publicationTask = new PublicationTask(
			 this
		);

		publicationTask.dispatchTime = System.currentTimeMillis();

		return publicationTask;
	}

	/**
	 * Creates a new {@link ClusterBuilder} with the specified cluster ID.
	 *
	 * @param id the unique identifier for the cluster.
	 * @return a new instance of {@link ClusterBuilder}.
	 */
	public static ClusterBuilder make(int id) {
		return new ClusterBuilder(id);
	}

	/**
	 * Subscribes a publication handler to the specified channel.
	 *
	 * @param channel            the channel to subscribe to.
	 * @param publicationHandler the handler to handle publications for this channel.
	 * @return the current instance of {@link ClusterContext} for chaining.
	 */
	public ClusterContext subscribe(String channel, PublicationHandler publicationHandler) {
		this.handlerMap.put(channel, publicationHandler);
		return this;
	}

	/**
	 * Handles the callback for incoming publications.
	 *
	 * @param session          the session associated with the publication.
	 * @param message          the incoming publication message.
	 * @param withNodes        the registry of cluster node identifiers.
	 * @param out              the output bundle for the publication.
	 * @return true if the callback was successfully handled, false otherwise.
	 */
	@Override
	public boolean callBack(Session session, Publication message, ClusterIdRegistry withNodes, PublicationBundle out) {
		boolean state = false;
		if (message instanceof SoftPublication softPublication) {
			for (Entry<String, PublicationHandler> handler : this.handlerMap.entrySet()) {
				if (!handler.getKey().equalsIgnoreCase(softPublication.getKey()))
					continue;

				state = handler.getValue().callBack(
					 session, message, withNodes, out
				);
			}
		}
		return state;
	}

	/**
	 * Handles the result of a synchronization feature.
	 *
	 * @param syncFeature a map containing synchronization results keyed by feature names.
	 */
	@Override
	public void result(Map<String, SyncResult> syncFeature) {
		// no results handeling
	}

	/**
	 * Retrieves the current snapshot of the cluster context.
	 *
	 * @return the current {@link ClusterSnapshot} instance.
	 */
	public ClusterSnapshot getSnapshot() {
		return snapshot;
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public Set<String> getChannels() {
		return channels;
	}

	public void setChannels(Set<String> channels) {
		this.channels = channels;
	}

	public Map<String, PublicationHandler> getHandlerMap() {
		return handlerMap;
	}

	public void setHandlerMap(Map<String, PublicationHandler> handlerMap) {
		this.handlerMap = handlerMap;
	}

	public Set<MemberIdentifier> getMembers() {
		return members;
	}

	public void setMembers(Set<MemberIdentifier> members) {
		this.members = members;
	}

	public MemberIdentifier getOwnAddress() {
		return ownAddress;
	}

	public void setOwnAddress(MemberIdentifier ownAddress) {
		this.ownAddress = ownAddress;
	}

	public Class<? extends Publication> getPublicationType() {
		return publicationType;
	}

	public void setPublicationType(Class<? extends Publication> publicationType) {
		this.publicationType = publicationType;
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public short[] getPrioritised() {
		return prioritised;
	}

	public void setPrioritised(short[] prioritised) {
		this.prioritised = prioritised;
	}

	public void setSnapshot(ClusterSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public SynchronisationType getType() {
		return type;
	}

	public void setType(SynchronisationType type) {
		this.type = type;
	}

	public List<NodeIdentifier> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(List<NodeIdentifier> identifiers) {
		this.identifiers = identifiers;
	}

	public List<ClusterMember> getClusterMembers() {
		return clusterMembers;
	}

	public void setClusterMembers(List<ClusterMember> clusterMembers) {
		this.clusterMembers = clusterMembers;
	}

	public SyncContext getContext() {
		return context;
	}

	public void setContext(SyncContext context) {
		this.context = context;
	}

	public ClusterIdentificationContext getClusterIdentificationContext() {
		return clusterIdentificationContext;
	}

	public void setClusterIdentificationContext(ClusterIdentificationContext clusterIdentificationContext) {
		this.clusterIdentificationContext = clusterIdentificationContext;
	}

	public ClusterIdRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(ClusterIdRegistry registry) {
		this.registry = registry;
	}

	public NetworkServer getNetworkServer() {
		return networkServer;
	}

	public void setNetworkServer(NetworkServer networkServer) {
		this.networkServer = networkServer;
	}

	public SyncNetworkHandler getHandler() {
		return handler;
	}

	public void setHandler(SyncNetworkHandler handler) {
		this.handler = handler;
	}

	public AbstractBackUpEndpoint getBackUpEndpoint() {
		return backUpEndpoint;
	}

	public void setBackUpEndpoint(AbstractBackUpEndpoint backUpEndpoint) {
		this.backUpEndpoint = backUpEndpoint;
	}
}
