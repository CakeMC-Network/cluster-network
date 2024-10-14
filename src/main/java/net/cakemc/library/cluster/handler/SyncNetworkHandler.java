package net.cakemc.library.cluster.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.cakemc.library.cluster.fallback.endpoint.EndpointType;
import net.cakemc.library.cluster.*;
import net.cakemc.library.cluster.ClusterMember.MemberState;
import net.cakemc.library.cluster.address.ClusterIdRegistry;
import net.cakemc.library.cluster.codec.*;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.MessageType;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.SyncMode;
import net.cakemc.library.cluster.network.AbstractNetworkClient;
import net.cakemc.library.cluster.network.NetworkClient;
import net.cakemc.library.cluster.network.NetworkSession;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * The SyncNetworkHandler class is responsible for managing synchronization
 * operations in a cluster network. It extends SimpleChannelInboundHandler
 * to handle incoming sync publication messages and provides mechanisms for
 * configuring synchronization settings, including target nodes, publication
 * types, and callbacks.
 * This class supports both server and client modes, enabling flexible
 * synchronization strategies based on the context of the network operation.
 *
 * <p>
 * Key functionalities include:
 * <ul>
 *   <li>Configuring nodes to include or exclude from synchronization.</li>
 *   <li>Specifying publication types for messages.</li>
 *   <li>Handling synchronization callbacks and responses.</li>
 *   <li>Processing and managing the state of cluster members during sync operations.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * SyncNetworkHandler handler = new SyncNetworkHandler(context);
 * handler.withCluster(nodeIds)
 *        .withPublicationType(MyPublication.class)
 *        .sync(publicationMessage);
 * </pre>
 * </p>
 *
 * @see SimpleChannelInboundHandler
 * @see DefaultSyncPublication
 */
public class SyncNetworkHandler extends SimpleChannelInboundHandler<DefaultSyncPublication> {

	private static final String SELF_ATTRIBUTE_KEY = "self";
	private static final String SESSION_INITIATED_KEY = "initiated";
	private static final String START_UP_STATE_KEY = "startup_state";

	// Public fields
	public EndpointType endpointType;
	public SyncMode mode = SyncMode.SYNC_MESSAGE;
	public Context syncContext;
	public ClusterMember selfMember;

	// Private fields
	private SynchronisationType sync;
	private PublicationHandler callback;

	private List<AbstractNetworkClient> sessions;
	private Set<Integer> invalidClients;
	private int currentSocket = 0;
	private boolean nonAsync = false;
	private Object nonasyncLock = new Object();
	private short numberOfTriedAttempts = 0;
	private boolean startupState;

	private boolean isInclusive = true;
	private boolean withBalance = false;

	// publication instance
	private Class<? extends Publication> publicationInstance;

	// synchronisation
	private Map<String, SyncResult> syncFeature;
	private Map<String, SyncContent> syncContents;

	// register
	private ClusterIdRegistry ids;
	private ClusterIdRegistry expectedNodes;

	/**
	 * Constructs a SyncNetworkHandler for server mode with the given context.
	 *
	 * @param ctx The context for synchronization.
	 */
	public SyncNetworkHandler(Context ctx) {
		this.endpointType = EndpointType.SERVER;
		this.syncContext = ctx;
		this.selfMember = ctx.getOwnInfo();
	}

	/**
	 * Constructs a SyncNetworkHandler for client mode with the given context and sync type.
	 *
	 * @param ctx  The context for synchronization.
	 * @param sync The sync type for this handler.
	 */
	public SyncNetworkHandler(Context ctx, SynchronisationType sync) {
		this.endpointType = EndpointType.CLIENT;
		this.sync = sync;
		this.syncContext = ctx;
		this.selfMember = ctx.getOwnInfo();
		this.invalidClients = new HashSet<>();
	}

	/**
	 * Sets the synchronization mode to cluster.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	private SyncNetworkHandler withModeCluster() {
		this.mode = SyncMode.SYNC_CLUSTER;
		return this;
	}

	/**
	 * Enables balancing for synchronization operations.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	private SyncNetworkHandler withBalance() {
		this.withBalance = true;
		return this;
	}

	/**
	 * Sets the callback handler for synchronization results.
	 *
	 * @param callback The callback handler to be set.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	public SyncNetworkHandler withCallBack(PublicationHandler callback) {
		this.callback = callback;
		return this;
	}

	/**
	 * Specifies the nodes with which to synchronize messages. If no nodes are specified,
	 * synchronization will be attempted with all nodes.
	 *
	 * @param ids The IDs of the nodes to include in synchronization.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	public SyncNetworkHandler withCluster(short... ids) {
		this.isInclusive = true;
		this.ids = new ClusterIdRegistry(ids);
		return this;
	}

	/**
	 * Sets the publication type for the messages to be synchronized.
	 *
	 * @param publicationInstance The class type of the publication.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	public SyncNetworkHandler withPublicationType(Class<? extends Publication> publicationInstance) {
		this.publicationInstance = publicationInstance;
		return this;
	}

	/**
	 * Specifies the nodes to exclude from synchronization.
	 *
	 * @param ids The IDs of the nodes to exclude.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	public SyncNetworkHandler withoutCluster(short... ids) {
		this.isInclusive = false;
		this.ids = new ClusterIdRegistry(ids);
		return this;
	}

	/**
	 * Synchronizes a single publication message.
	 *
	 * @param msg The publication message to synchronize.
	 *
	 * @return This SyncNetworkHandler for method chaining.
	 */
	public SyncNetworkHandler sync(Publication msg) {
		return sync(Collections.singletonList(msg));
	}

	/**
	 * Checks if balancing is enabled and sets the callback accordingly.
	 *
	 * @return True if balancing is enabled, otherwise false.
	 */
	private boolean checkWithBalanceAndSetCallback() {
		if (this.withBalance) {
			this.callback.result(null);
			return true;
		}
		return false;
	}

	/**
	 * Performs inverse ID operations on the specified cluster members and updates
	 * the cluster ID registry accordingly.
	 *
	 * @param cluster           The list of cluster members to evaluate.
	 * @param clusterIdRegistry The registry to update with new IDs.
	 */
	private void performInverseIds(List<ClusterMember> cluster, ClusterIdRegistry clusterIdRegistry) {
		// Create a set to hold existing IDs
		ClusterIdRegistry existingIds = new ClusterIdRegistry();

		// Add existing IDs to the set
		for (short id : ids.getIds()) {
			existingIds.add(id);
		}

		// Filter members not in existingIds and add their IDs to the registry
		for (ClusterMember clusterMember : cluster) {
			if (!existingIds.contains(clusterMember.getId())) {
				clusterIdRegistry.add(clusterMember.getId());
				sessions.add(clusterMember.createSynchSession(this));
			}
		}
	}

	/**
	 * Performs inverse ID operations on the specified cluster members and applies a
	 * consumer to each member that meets the criteria.
	 *
	 * @param cluster           The list of cluster members to evaluate.
	 * @param clusterIdRegistry The registry to update with new IDs.
	 * @param consumer          The consumer to apply to each eligible cluster member.
	 */
	private void performInverseIds(
		 List<ClusterMember> cluster, ClusterIdRegistry clusterIdRegistry,
		 Consumer<ClusterMember> consumer
	) {
		// Create a set to hold existing IDs
		ClusterIdRegistry existingIds = new ClusterIdRegistry();

		// Add existing IDs to the set
		for (short id : ids.getIds()) {
			existingIds.add(id);
		}

		// Filter members not in existingIds and apply the consumer
		for (ClusterMember clusterMember : cluster) {
			if (!existingIds.contains(clusterMember.getId())) {
				clusterIdRegistry.add(clusterMember.getId());
				consumer.accept(clusterMember);
			}
		}
	}

	/**
	 * Starts synchronizing a batch of messages with the cluster. This method should be called
	 * after you have specified the callback, encoder, and destinations.
	 *
	 * @param publications The list of publications to sync.
	 *
	 * @return The current instance of SyncNetworkHandler.
	 */
	public SyncNetworkHandler sync(List<Publication> publications) {
		this.endpointType = EndpointType.CLIENT;

		// Validate input parameters before proceeding
		if (publications == null || publications.isEmpty() || this.callback == null ||
		    (mode == SyncMode.SYNC_MESSAGE && this.publicationInstance == null)) {
			nonasyncLock = null;
			checkWithBalanceAndSetCallback();
			return this;
		}

		ClusterSnapshot clusterSnapshot = syncContext.getSnapshot();
		if (clusterSnapshot == null) {
			nonasyncLock = null;
			checkWithBalanceAndSetCallback();
			return this;
		}

		// Handle balance-based synchronization
		if (SynchronisationType.isBalanceType(this.sync)) {
			if (!this.withBalance) {
				syncWithBalance(publications);
				return this;
			}
		}

		List<ClusterMember> aliveClusterMembers = clusterSnapshot.getAliveCluster();
		sessions = new ArrayList<>();

		// Handle Unicast types of synchronization
		if (SynchronisationType.isUnicastType(this.sync)) {
			handleUnicastSynchronization(aliveClusterMembers);
		}
		// Handle Ring-based types of synchronization
		else if (SynchronisationType.isRingType(this.sync)) {
			handleRingSynchronization(aliveClusterMembers);
		}

		// Prepare message for synchronization
		startupState = syncContext.isInStartup();
		DefaultSyncPublication syncMessage = prepareSyncPublication(publications);

		// Publish to the appropriate sessions
		if (sessions != null && !sessions.isEmpty()) {
			publishMessage(syncMessage);
		} else {
			nonasyncLock = null;
			createResult();
		}

		return this;
	}

	/**
	 * Handles the logic for Unicast synchronization.
	 *
	 * @param aliveClusterMembers The list of currently alive cluster members.
	 */
	private void handleUnicastSynchronization(List<ClusterMember> aliveClusterMembers) {
		if (ids == null || ids.isEmpty()) {
			if (checkWithBalanceAndSetCallback()) {
				return;
			}
			populateSessionsForAllClusterMembers(aliveClusterMembers);
		} else {
			if (isInclusive) {
				populateSessionsForSpecifiedIds(aliveClusterMembers, ids);
			} else {
				ClusterIdRegistry invertedIds = new ClusterIdRegistry();
				performInverseIds(aliveClusterMembers, invertedIds);
				this.ids = invertedIds;
			}
		}
	}

	/**
	 * Handles the logic for Ring synchronization.
	 *
	 * @param aliveClusterMembers The list of currently alive cluster members.
	 */
	private void handleRingSynchronization(List<ClusterMember> aliveClusterMembers) {
		if (ids == null || ids.isEmpty()) {
			if (checkWithBalanceAndSetCallback()) {
				return;
			}
			populateSessionsForAllClusterMembers(aliveClusterMembers);
		} else {
			if (isInclusive) {
				expectedNodes = new ClusterIdRegistry();
				populateSessionsForSpecifiedIds(aliveClusterMembers, ids);
			} else {
				expectedNodes = new ClusterIdRegistry();
				performInverseIds(aliveClusterMembers, expectedNodes);
				this.ids = expectedNodes;
			}
		}
	}

	/**
	 * Populates the sessions list with synchronization sessions for all members in the cluster.
	 *
	 * @param aliveClusterMembers The list of currently alive cluster members.
	 */
	private void populateSessionsForAllClusterMembers(List<ClusterMember> aliveClusterMembers) {
		if (!aliveClusterMembers.isEmpty()) {
			ids = new ClusterIdRegistry();
			expectedNodes = new ClusterIdRegistry();
			for (ClusterMember member : aliveClusterMembers) {
				ids.add(member.getId());
				sessions.add(member.createSynchSession(this));
				expectedNodes.add(member.getId());
			}
		}
	}

	/**
	 * Populates the sessions list with synchronization sessions for all members matching the specified IDs.
	 *
	 * @param aliveClusterMembers The list of currently alive cluster members.
	 * @param specifiedIds        The IDs of the cluster members to populate.
	 */
	private void populateSessionsForSpecifiedIds(List<ClusterMember> aliveClusterMembers, ClusterIdRegistry specifiedIds) {
		for (short id : specifiedIds.getIds()) {
			ClusterMember member = syncContext.getSnapshot().getById(id, ClusterSnapshot.MEMBER_CHECK_VALID);
			if (member != null) {
				sessions.add(member.createSynchSession(this));
				expectedNodes.add(id);
			}
		}
	}

	/**
	 * Prepares the message for synchronization, populating the sync contents and features.
	 *
	 * @param publications The list of publications to sync.
	 *
	 * @return A DefaultSyncPublication object prepared for synchronization.
	 */
	private DefaultSyncPublication prepareSyncPublication(List<Publication> publications) {
		DefaultSyncPublication syncMessage = new DefaultSyncPublication();
		syncMessage.setSyncType(sync);
		syncMessage.setId(selfMember.getId());
		syncMessage.setInStartup(startupState);
		syncMessage.setSyncMode(mode);
		syncMessage.setType(MessageType.TYPE_CHECK);

		syncContents = new HashMap<>();
		syncFeature = new HashMap<>();

		for (Publication publication : publications) {
			ClusterIdRegistry awareIds = determineAwareNodes(publication);
			syncContents.put(publication.getKey(), new SyncContent(publication.getKey(),
			                                                       publication.getVersion(), awareIds, publication.serialize()
			));

			SyncResult syncResult = new SyncResult();
			syncResult.addSyncedMember(awareIds);
			syncFeature.put(publication.getKey(), syncResult);
		}

		syncMessage.setContents(syncContents.values());
		return syncMessage;
	}

	/**
	 * Determines which nodes are aware of the specified message.
	 *
	 * @param publication The publication message to analyze.
	 *
	 * @return A ClusterIdRegistry containing the IDs of the aware nodes.
	 */
	private ClusterIdRegistry determineAwareNodes(Publication publication) {
		if (this.mode == SyncMode.SYNC_MESSAGE) {
			if (publication.getKey() != null && !publication.getKey().isEmpty() && publication.getVersion() > 0) {
				return addAndGetAwareNodesOfMessage(publication);
			}
		} else if (publication.getKey() != null && !publication.getKey().isEmpty() && publication.getVersion() > 0) {
			ClusterPublication clusterPublication = (ClusterPublication) publication;
			ClusterMember memberNode = syncContext.getMemberById(clusterPublication.getId());
			return memberNode == null ? null : memberNode.getAwareIds();
		}
		return null;
	}

	/**
	 * Publishes the message to the sessions.
	 *
	 * @param syncMessage The message to be published.
	 */
	private void publishMessage(DefaultSyncPublication syncMessage) {
		if (SynchronisationType.isRingType(sync) || sync == SynchronisationType.UNI_CAST_ONE_OF) {
			sessions.getFirst().publish(syncMessage);
		} else if (SynchronisationType.isUnicastType(sync)) {
			for (AbstractNetworkClient session : sessions) {
				session.publish(syncMessage);
			}
		}
	}

	/**
	 * Adds and retrieves the aware nodes for the specified message.
	 *
	 * @param publication The publication message to analyze.
	 *
	 * @return A ClusterIdRegistry containing the IDs of the aware nodes.
	 */
	private ClusterIdRegistry addAndGetAwareNodesOfMessage(Publication publication) {
		ClusterIdRegistry awareNodeIds = syncContext.getAwareNodes(publication.getKey(), publication.getVersion());

		if (awareNodeIds == null || awareNodeIds.isEmpty()) {
			awareNodeIds = new ClusterIdRegistry(syncContext.getOwnInfo().getId()); // Initialize with the current node's ID
			syncContext.addAwareNodes(publication.getKey(), publication.getVersion(), awareNodeIds); // Store updated awareNodeIds
		}

		return awareNodeIds;
	}

	/**
	 * Blocks execution until the synchronization result is generated.
	 *
	 * @return A map containing SyncFeature results.
	 */
	@SuppressWarnings(
		 {
			  "SynchronizeOnNonFinalField",
			  "CallToPrintStackTrace"
		 }
	)
	public Map<String, SyncResult> get() {
		this.nonAsync = true;
		if (nonasyncLock == null) {
			return this.syncFeature;
		}
		synchronized (nonasyncLock) {
			while (nonAsync) {
				try {
					nonasyncLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return this.syncFeature;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		if (endpointType == EndpointType.SERVER) {
			ctx.pipeline().addLast(new SyncNettyDecoder());
			ctx.pipeline().addLast(new SyncNettyEncoder());
		}

		InetSocketAddress peer = (InetSocketAddress) ctx.channel().remoteAddress();
		ctx.channel().attr(ChannelAttributes.STARTUP_STATE_KEY.getKey()).set(syncContext.isInStartup());
	}

	/**
	 * Creates a simple synchronization response message with specified parameters.
	 *
	 * @param messageType             The type of the message to be created.
	 * @param startupStateFromSession The startup state from the session, or null if not applicable.
	 * @param syncMode                The synchronization mode of the response.
	 *
	 * @return A new instance of DefaultSyncPublication containing the specified details.
	 */
	private DefaultSyncPublication createSimpleResponse(
		 DefaultSyncPublication.MessageType messageType,
		 Boolean startupStateFromSession,
		 SyncMode syncMode
	) {
		DefaultSyncPublication response = new DefaultSyncPublication();
		response.setId(selfMember.getId());
		if (startupStateFromSession != null) {
			response.setInStartup(startupStateFromSession);
		}
		response.setSyncMode(syncMode);
		response.setType(messageType);
		return response;
	}

	/**
	 * Creates a complete synchronization response message with additional parameters.
	 *
	 * @param messageType             The type of the message to be created.
	 * @param startupStateFromSession The startup state from the session, or null if not applicable.
	 * @param syncMode                The synchronization mode of the response.
	 * @param synchronisationType                The synchronization type to be set in the response.
	 * @param sequence                The sequence number of the response, must be non-negative.
	 *
	 * @return A complete instance of DefaultSyncPublication.
	 */
	private DefaultSyncPublication createCompleteResponse(
		 DefaultSyncPublication.MessageType messageType,
		 Boolean startupStateFromSession,
		 SyncMode syncMode,
		 SynchronisationType synchronisationType,
		 Byte sequence
	) {
		DefaultSyncPublication response = createSimpleResponse(messageType, startupStateFromSession, syncMode);
		response.setSyncType(synchronisationType);
		if (sequence > -1) {
			response.setSequence(sequence);
		}
		return response;
	}

	/**
	 * Fills the response contents with the provided synchronization content and associated response.
	 *
	 * @param response         The cluster publication response to process.
	 * @param syncContent      The synchronization content associated with the response.
	 * @param responseContents The collection of response contents to be updated.
	 */
	private void fillSyncContents(
		 ClusterPublication response,
		 SyncContent syncContent,
		 Collection<SyncContent> responseContents
	) {
		if (response != null) {
			ClusterMember member = syncContext.getMemberById(response.getId());
			ClusterIdRegistry awareIds = (member != null) ? member.getAwareIds() : null;

			responseContents.add(new SyncContent(
				 syncContent.getKey(),
				 response.getVersion(),
				 awareIds,
				 response.serialize()
			));
		}
	}

	/**
	 * Decodes the callback result from the given data and invokes the callback with the decoded publication.
	 *
	 * @param callback The publication handler callback to be invoked.
	 * @param session  The channel session associated with the synchronization.
	 * @param decoded  The decoded publication to be processed.
	 * @param data     The byte array containing the serialized data.
	 * @param awareIds The cluster ID registry of aware nodes.
	 * @param output   The output bundle for storing the results of the callback.
	 *
	 * @return True if the callback was successful, false otherwise.
	 */
	@SuppressWarnings("ParameterCanBeNull")
	private boolean decodeGetCallbackResult(
		 PublicationHandler callback,
		 Channel session,
		 Publication decoded,
		 byte[] data,
		 ClusterIdRegistry awareIds,
		 SyncProtocolBundle output
	) {
		if (data != null) {
			decoded.deserialize(data);
		}
		return callback.callBack(new NetworkSession(session), decoded, awareIds, output);
	}

	/**
	 * Retrieves the IDs of all alive cluster members.
	 *
	 * @return A ClusterIdRegistry containing the IDs of alive members, or null if none are alive.
	 */
	private ClusterIdRegistry getAliveMemberIds() {
		ClusterSnapshot snapshot = syncContext.getSnapshot();
		if (snapshot != null) {
			List<ClusterMember> aliveMembers = snapshot.getAliveCluster();
			if (!aliveMembers.isEmpty()) {
				ClusterIdRegistry aliveMemberIds = new ClusterIdRegistry();
				for (ClusterMember member : aliveMembers) {
					aliveMemberIds.add(member.getId());
				}
				return aliveMemberIds;
			}
		}
		return null;
	}

	/**
	 * Determines the appropriate synchronization type for the given publication.
	 *
	 * @param publication The synchronization publication to evaluate.
	 *
	 * @return The appropriate SynchronisationType based on the publication's current type.
	 */
	private SynchronisationType getProperRingType(DefaultSyncPublication publication) {
		if (publication.getSyncType() == SynchronisationType.RING_QUORUM || publication.getSyncType() == SynchronisationType.RING_BALANCE_QUORUM) {
			return SynchronisationType.RING_BALANCE_QUORUM; // Return quorum type if applicable
		}
		return SynchronisationType.RING_BALANCE; // Default return type
	}

	/**
	 * Fills the callback result based on the synchronization outcome and updates response contents.
	 *
	 * @param result           Indicates whether the synchronization was successful.
	 * @param message          The original publication message related to the synchronization.
	 * @param responseContents The collection of synchronization contents to update.
	 * @param responses        The list of responses obtained from the callback.
	 * @param ringMsgToScMap   A map linking ring message keys to sync content keys.
	 * @param awareIds         The cluster ID registry of aware nodes.
	 * @param syncResult       The result of the synchronization attempt.
	 */
	private void fillCallbackResult(
		 boolean result,
		 Publication message,
		 Collection<SyncContent> responseContents,
		 List<Publication> responses,
		 Map<String, String> ringMsgToScMap,
		 ClusterIdRegistry awareIds,
		 SyncResult syncResult
	) {
		if (result) {
			if (responses == null) {
				// Means it synced successfully
				responseContents.add(new SyncContent(ringMsgToScMap.get(message.getKey()), message.getVersion(), awareIds, null));
			}
		} else {
			if (responses == null) {
				ClusterIdRegistry failedMembers = syncResult.getFailedMembers();
				failedMembers.add(selfMember.getId());
				responseContents.add(new SyncContent(ringMsgToScMap.get(message.getKey()), 0, failedMembers, null));
			}
		}
	}

	/**
	 * Handles the synchronization message from a cluster member and processes it accordingly.
	 *
	 * @param session                 The channel session associated with the incoming message.
	 * @param message                 The incoming synchronization publication message.
	 * @param member                  The cluster member from which the message originated.
	 * @param startupStateFromSession The startup state from the session.
	 * @param isFirstMessage          Indicates if this is the first message in the synchronization process.
	 *
	 * @throws IllegalAccessException    If an access error occurs.
	 * @throws InstantiationException    If an instantiation error occurs.
	 * @throws NoSuchMethodException     If the specified method does not exist.
	 * @throws InvocationTargetException If the invoked method throws an exception.
	 */
	private void handleMessageSyncListener(
		 Channel session,
		 DefaultSyncPublication message,
		 ClusterMember member,
		 Boolean startupStateFromSession,
		 Boolean isFirstMessage
	) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Check if the cluster member is valid
		if (member == null || !member.isValid()) {
			DefaultSyncPublication response = createSimpleResponse(
				 DefaultSyncPublication.MessageType.TYPE_NOT_VALID_EDGE,
				 startupStateFromSession,
				 mode
			);
			session.writeAndFlush(response);
			session.close();
			return;
		}

		// Retrieve the message contents
		Collection<SyncContent> contents = message.getContents();
		if (contents == null) {
			session.close();
			return;
		}

		Set<SyncContent> responseContents = new HashSet<>();
		List<Publication> ringMessages = new ArrayList<>();
		boolean isRingSync = SynchronisationType.isRingType(message.getSyncType());
		Map<String, String> ringMessageToSyncContentMap = isRingSync ? new HashMap<>() : null;

		for (SyncContent syncContent : contents) {
			byte[] contentData = syncContent.getContent();
			if (contentData == null) continue;

			// Decode message and retrieve callback results
			Publication decodedMessage = this.publicationInstance.getDeclaredConstructor().newInstance();
			SyncProtocolBundle output = new SyncProtocolBundle();
			decodeGetCallbackResult(callback, session, decodedMessage, contentData, syncContent.getAwareIds(), output);
			List<Publication> responses = output.getMessages();

			ClusterIdRegistry awareNodes = (syncContent.getAwareIds() != null) ? syncContent.getAwareIds() : new ClusterIdRegistry();
			awareNodes.add(selfMember.getId());
			syncContext.addAwareNodes(decodedMessage.getKey(), decodedMessage.getVersion(), awareNodes);

			if (isRingSync) {
				ringMessages.add(decodedMessage);
				ringMessageToSyncContentMap.put(decodedMessage.getKey(), syncContent.getKey());
			} else {
				addResponseContents(responseContents, responses, syncContent.getKey(), awareNodes);
			}
		}

		if (!ringMessages.isEmpty()) {
			handleRingSync(session, message, ringMessages, responseContents, ringMessageToSyncContentMap);
		}

		sendResponse(session, message, responseContents, isRingSync, isFirstMessage);
	}

	/**
	 * Adds the responses to the specified response contents collection.
	 *
	 * @param responseContents The collection of response contents to be updated.
	 * @param responses        The list of responses to process.
	 * @param key              The key associated with the synchronization content.
	 * @param awareNodes       The cluster ID registry of aware nodes associated with the responses.
	 */
	private void addResponseContents(
		 Set<SyncContent> responseContents,
		 List<Publication> responses,
		 String key,
		 ClusterIdRegistry awareNodes
	) {
		if (responses != null) {
			for (Publication response : responses) {
				if (response != null) {
					responseContents.add(new SyncContent(
						 key, response.getVersion(), awareNodes, response.serialize()
					));
				}
			}
		}
	}


	/**
	 * Handles the ring-type synchronization operation by processing the given messages
	 * and updating the response contents based on the synchronization results.
	 *
	 * @param session                     The channel session associated with the synchronization.
	 * @param message                     The incoming synchronization publication message.
	 * @param ringMessages                The list of ring messages to synchronize.
	 * @param responseContents            The set of synchronization content to be updated.
	 * @param ringMessageToSyncContentMap A map linking ring message keys to sync content keys.
	 */
	private void handleRingSync(
		 Channel session,
		 DefaultSyncPublication message,
		 List<Publication> ringMessages,
		 Set<SyncContent> responseContents,
		 Map<String, String> ringMessageToSyncContentMap
	) {
		SynchronisationType ringType = getProperRingType(message);
		Map<String, SyncResult> syncResults = new SyncNetworkHandler(syncContext, ringType)
			 .withCallBack(callback)
			 .withPublicationType(publicationInstance)
			 .withoutCluster(message.getId(), syncContext.getOwnInfo().getId())
			 .sync(ringMessages)
			 .get();

		if (syncResults == null) {
			session.writeAndFlush(createSimpleResponse(
				 DefaultSyncPublication.MessageType.TYPE_FAILED_RING,
				 null,
				 SyncMode.SYNC_MESSAGE
			));
			session.close();
			return;
		}

		for (Publication ringMessage : ringMessages) {
			SyncResult result = syncResults.get(ringMessage.getKey());
			ClusterIdRegistry awareIds = syncContext.getAwareNodes(ringMessage.getKey(), ringMessage.getVersion());

			if (result.isSuccessful()) {
				processSuccessfulSync(
					 result, session, ringMessage,
					 responseContents, ringMessageToSyncContentMap, awareIds
				);
			} else {
				responseContents.add(new SyncContent(
					 ringMessageToSyncContentMap.get(ringMessage.getKey()),
					 0,
					 result.getFailedMembers(),
					 null
				));
			}
		}
	}

	/**
	 * Processes a successful synchronization result and updates the response contents accordingly.
	 *
	 * @param result                  The synchronization result to be processed.
	 * @param message                 The original publication message related to the synchronization.
	 * @param responseContents        The set of synchronization contents to update.
	 * @param ringMsgToSyncContentMap A map linking ring message keys to sync content keys.
	 * @param awareIds                The cluster ID registry of aware nodes.
	 */
	private void processSuccessfulSync(
		 SyncResult result,
		 Channel session,
		 Publication message,
		 Set<SyncContent> responseContents,
		 Map<String, String> ringMsgToSyncContentMap,
		 ClusterIdRegistry awareIds
	) {
		SyncProtocolBundle syncProtocolBundle = new SyncProtocolBundle();
		boolean success = decodeGetCallbackResult(callback, session, message, null, awareIds, syncProtocolBundle);
		List<Publication> responses = syncProtocolBundle.getMessages();

		if (success && responses != null) {
			for (Publication response : responses) {
				if (response != null) {
					responseContents.add(new SyncContent(
						 ringMsgToSyncContentMap.get(message.getKey()),
						 response.getVersion(),
						 awareIds,
						 response.serialize()
					));
				}
			}
		}
	}

	/**
	 * Sends the final response back to the session based on the contents of the synchronization.
	 *
	 * @param session          The channel session to send the response to.
	 * @param msg              The original synchronization publication message.
	 * @param responseContents The contents of the response to be sent.
	 * @param isRing           Indicates if the synchronization is of type ring.
	 * @param isFirstMessage   Indicates if this is the first message in the synchronization.
	 */
	private void sendResponse(
		 Channel session,
		 DefaultSyncPublication msg,
		 Set<SyncContent> responseContents,
		 boolean isRing,
		 boolean isFirstMessage
	) {
		if (responseContents.isEmpty()) {
			session.writeAndFlush(createCompleteResponse(
				 DefaultSyncPublication.MessageType.TYPE_OK,
				 null,
				 SyncMode.SYNC_MESSAGE,
				 msg.getSyncType(),
				 (byte) 0
			));
		} else {
			DefaultSyncPublication response = createCompleteResponse(
				 DefaultSyncPublication.MessageType.TYPE_CHECK,
				 null,
				 SyncMode.SYNC_MESSAGE,
				 msg.getSyncType(),
				 (byte) (msg.getSequence() + 1)
			);
			response.setContents(responseContents);
			if (isRing && isFirstMessage) {
				response.setExpectedIds(getAliveMemberIds());
			}
			session.writeAndFlush(response);
		}
	}

	/**
	 * Handles incoming cluster synchronization messages and processes them accordingly.
	 *
	 * @param session        The channel session associated with the incoming message.
	 * @param message        The incoming synchronization publication message.
	 * @param isFirstMessage Indicates if this is the first message in the synchronization.
	 */
	private void handleClusterSyncListener(Channel session, DefaultSyncPublication message, Boolean isFirstMessage) {
		syncContext.setInStartup(false);

		Collection<SyncContent> contents = message.getContents();
		if (contents == null) {
			session.close();
			return;
		}

		boolean isRingSync = SynchronisationType.isRingType(message.getSyncType());
		Map<String, String> ringMessageToSyncContentMap = isRingSync ? new HashMap<>() : null;

		ClusterPublicationHandler clusterCallback = new ClusterPublicationHandler(syncContext);
		Collection<SyncContent> responseContents = new ArrayList<>();
		List<Publication> ringMessages = new ArrayList<>();

		for (SyncContent syncContent : contents) {
			byte[] messageData = syncContent.getContent();

			if (messageData == null) {
				handleNullMessage(syncContent, message);
				continue;
			}

			Publication decodedMessage = new ClusterPublication();
			SyncProtocolBundle output = new SyncProtocolBundle();
			boolean decodeResult = decodeGetCallbackResult(clusterCallback, session, decodedMessage, messageData, syncContent.getAwareIds(), output);
			List<Publication> responses = output.getMessages();

			if (decodeResult && isRingSync) {
				ringMessages.add(decodedMessage);
				ringMessageToSyncContentMap.put(decodedMessage.getKey(), syncContent.getKey());
			}

			if (responses != null) {
				for (Publication publication : responses) {
					ClusterPublication response = (ClusterPublication) publication;
					fillSyncContents(response, syncContent, responseContents);
				}
			}
		}

		if (!ringMessages.isEmpty()) {
			handleRingMessages(session, message, isFirstMessage, ringMessages, ringMessageToSyncContentMap, responseContents);
		} else {
			sendFinalResponse(session, responseContents, message, isRingSync, isFirstMessage);
		}
	}

	/**
	 * Handles null messages by updating the aware IDs of the corresponding cluster member.
	 *
	 * @param syncContent The synchronization content that contains the null message.
	 * @param message     The original synchronization publication message.
	 */
	private void handleNullMessage(SyncContent syncContent, DefaultSyncPublication message) {
		if (syncContent.getVersion() > 0) {
			ClusterMember clusterMember = syncContext.getMemberById(Short.parseShort(syncContent.getKey()));
			if (clusterMember != null) {
				clusterMember.addAwareId(selfMember.getId());
				clusterMember.addAwareId(message.getId());
			}
		}
	}

	/**
	 * Processes the received ring messages by handling synchronization and updating response contents.
	 *
	 * @param session                     The channel session associated with the incoming message.
	 * @param message                     The original synchronization publication message.
	 * @param isFirstMessage              Indicates if this is the first message in the synchronization.
	 * @param ringMessages                The list of ring messages to synchronize.
	 * @param ringMessageToSyncContentMap A map linking ring message keys to sync content keys.
	 * @param responseContents            The set of synchronization contents to be updated.
	 */
	private void handleRingMessages(
		 Channel session,
		 DefaultSyncPublication message,
		 Boolean isFirstMessage,
		 List<Publication> ringMessages,
		 Map<String, String> ringMessageToSyncContentMap,
		 Collection<SyncContent> responseContents
	) {
		PublicationHandler callbackHandler = this.callback;
		SynchronisationType synchronisationType = getProperRingType(message);
		ClusterIdRegistry aliveMemberIds = getAliveMemberIds();

		Map<String, SyncResult> syncResults = new SyncNetworkHandler(syncContext, synchronisationType)
			 .withCallBack(callbackHandler)
			 .withPublicationType(publicationInstance)
			 .withoutCluster(message.getId(), syncContext.getOwnInfo().getId())
			 .withModeCluster()
			 .sync(ringMessages)
			 .get();

		if (syncResults == null) {
			sendErrorResponse(session, message);
			return;
		}

		for (Publication ringMessage : ringMessages) {
			SyncResult result = syncResults.get(ringMessage.getKey());
			if (result != null && result.isSuccessful()) {
				processSuccessfulSync(session, callbackHandler, ringMessage, ringMessageToSyncContentMap, responseContents, result);
			} else {
				responseContents.add(new SyncContent(
					 ringMessageToSyncContentMap.get(ringMessage.getKey()),
					 0,
					 result != null ? result.getFailedMembers() : null,
					 null
				));
			}
		}
	}

	/**
	 * Processes a successful synchronization result for ring messages and updates response contents.
	 *
	 * @param session                     The channel session associated with the synchronization.
	 * @param callbackHandler             The handler for processing synchronization callbacks.
	 * @param message                     The original publication message related to the synchronization.
	 * @param ringMessageToSyncContentMap A map linking ring message keys to sync content keys.
	 * @param responseContents            The set of synchronization contents to update.
	 * @param syncResult                  The synchronization result to be processed.
	 */
	private void processSuccessfulSync(
		 Channel session,
		 PublicationHandler callbackHandler,
		 Publication message,
		 Map<String, String> ringMessageToSyncContentMap,
		 Collection<SyncContent> responseContents,
		 SyncResult syncResult
	) {
		SyncProtocolBundle syncProtocolBundle = new SyncProtocolBundle();
		ClusterMember clusterMember = syncContext.getMemberById(((ClusterPublication) message).getId());
		ClusterIdRegistry awareIds = (clusterMember != null) ? clusterMember.getAwareIds() : null;

		boolean isSuccess = decodeGetCallbackResult(callbackHandler, session, message, null, awareIds, syncProtocolBundle);
		List<Publication> responses = syncProtocolBundle.getMessages();

		fillCallbackResult(isSuccess, message, responseContents, responses, ringMessageToSyncContentMap, awareIds, syncResult);

		for (Publication response : responses) {
			ClusterMember responseMember = syncContext.getMemberById(((ClusterPublication) response).getId());
			ClusterIdRegistry responseAwareIds = (responseMember != null) ? responseMember.getAwareIds() : null;

			responseContents.add(new SyncContent(
				 ringMessageToSyncContentMap.get(message.getKey()),
				 response.getVersion(),
				 responseAwareIds,
				 response.serialize()
			));
		}
	}

	/**
	 * Sends an error response to the specified session and closes the session.
	 *
	 * @param session The channel session to which the error response will be sent.
	 * @param msg     The original synchronization publication message that triggered the error response.
	 */
	private void sendErrorResponse(Channel session, DefaultSyncPublication msg) {
		DefaultSyncPublication errorMsg = createCompleteResponse(
			 MessageType.TYPE_FAILED_RING,
			 null,
			 SyncMode.SYNC_CLUSTER,
			 msg.getSyncType(),
			 (byte) -1
		);
		session.writeAndFlush(errorMsg);
		session.close();
	}

	/**
	 * Sends a final response to the session based on the synchronization content.
	 *
	 * @param session          The channel session to which the final response will be sent.
	 * @param responseContents The collection of sync contents to be included in the response.
	 * @param msg              The original synchronization publication message.
	 * @param isRing           Indicates if the synchronization is a ring type.
	 * @param isFirstMessage   Indicates if this is the first message in the session.
	 */
	private void sendFinalResponse(
		 Channel session,
		 Collection<SyncContent> responseContents,
		 DefaultSyncPublication msg,
		 boolean isRing,
		 boolean isFirstMessage
	) {
		DefaultSyncPublication response;
		if (responseContents.isEmpty()) {
			response = createCompleteResponse(
				 DefaultSyncPublication.MessageType.TYPE_OK,
				 null,
				 SyncMode.SYNC_CLUSTER,
				 msg.getSyncType(),
				 (byte) 0
			);
		} else {
			response = createCompleteResponse(
				 DefaultSyncPublication.MessageType.TYPE_CHECK,
				 null,
				 SyncMode.SYNC_CLUSTER,
				 msg.getSyncType(),
				 (byte) (msg.getSequence() + 1)
			);
			response.setContents(responseContents);

			if (isRing && isFirstMessage) {
				ClusterIdRegistry nodesForRingUpdate = getAliveMemberIds();
				response.setExpectedIds(nodesForRingUpdate);
			}
		}

		session.writeAndFlush(response);
	}

	/**
	 * Handles incoming synchronization messages by checking their types and processing them accordingly.
	 *
	 * @param context The channel session associated with the message.
	 * @param message The incoming synchronization publication message.
	 *
	 * @throws IllegalAccessException    If there's an issue accessing a method or field.
	 * @throws InstantiationException    If there's an issue instantiating a class.
	 * @throws InvocationTargetException If an invoked method throws an exception.
	 * @throws NoSuchMethodException     If a method cannot be found.
	 */
	private void handleListener(Channel context, DefaultSyncPublication message)
	throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {

		Boolean startupStateFromSession = (Boolean) context.attr(AttributeKey.valueOf(START_UP_STATE_KEY)).get();

		// Handle different message types
		if (message.getType() == DefaultSyncPublication.MessageType.TYPE_OK) {
			context.close();
			return;
		}

		if (message.isInStartup() && startupStateFromSession != null) {
			context.writeAndFlush(createSimpleResponse(DefaultSyncPublication.MessageType.TYPE_BOTH_STARTUP, startupStateFromSession, mode));
			context.close();
			return;
		}

		if (isBadMessage(context, message)) {
			context.close();
			return;
		}

		boolean isFirstMessage = initializeSession(context, message, startupStateFromSession);

		ClusterMember clusterMember = (ClusterMember) context.attr(AttributeKey.valueOf(SELF_ATTRIBUTE_KEY)).get();
		if (clusterMember == null) {
			clusterMember = syncContext.getMemberById(message.getId());
			context.attr(AttributeKey.valueOf(SELF_ATTRIBUTE_KEY)).set(clusterMember);
		}

		if (clusterMember != null && clusterMember.isDown()) {
			syncContext.synchronizedStateChange(clusterMember, MemberState.DELETED);
		}

		// Handle specific sync modes
		switch (message.getSyncMode()) {
			case SYNC_MESSAGE:
				handleMessageSyncListener(context, message, clusterMember, startupStateFromSession, isFirstMessage);
				break;
			case SYNC_CLUSTER:
				handleClusterSyncListener(context, message, isFirstMessage);
				break;
			default:
				// Optionally handle other cases or log an error
				break;
		}
	}

	/**
	 * Checks if the given message is considered invalid based on its type or sequence number.
	 *
	 * @param ctx         The channel context associated with the message.
	 * @param publication The synchronization publication message to check.
	 *
	 * @return true if the message is invalid; otherwise, false.
	 */
	private boolean isBadMessage(Channel ctx, DefaultSyncPublication publication) {
		if (publication.getType() == MessageType.TYPE_BAD_ID ||
		    publication.getType() == MessageType.TYPE_BAD_SEQ ||
		    publication.getType() == MessageType.TYPE_FAILED_RING ||
		    publication.getSequence() > DefaultSyncPublication.SEQ_MAX) {

			if (publication.getSequence() > DefaultSyncPublication.SEQ_MAX) {
				Boolean startupStateFromSession = (Boolean) ctx.attr(AttributeKey.valueOf(START_UP_STATE_KEY)).get();
				ctx.writeAndFlush(createSimpleResponse(MessageType.TYPE_BAD_SEQ, startupStateFromSession, mode));
			}
			return true;
		}
		return false;
	}

	/**
	 * Initializes the session by setting attributes based on the first message received.
	 *
	 * @param ctx                     The channel context associated with the session.
	 * @param publication             The synchronization publication message used for initialization.
	 * @param startupStateFromSession The startup state retrieved from the session.
	 *
	 * @return true if this is the first message; otherwise, false.
	 */
	private boolean initializeSession(Channel ctx, DefaultSyncPublication publication, Boolean startupStateFromSession) {
		if (!ctx.hasAttr(AttributeKey.valueOf(SESSION_INITIATED_KEY))) {
			ctx.attr(AttributeKey.valueOf(SESSION_INITIATED_KEY)).set(true);
			if (selfMember.getKey() != null && selfMember.getKeyChain().stream().anyMatch(publication.getKeyChain()::contains)) {
				DefaultSyncPublication response = createSimpleResponse(
					 DefaultSyncPublication.MessageType.TYPE_BAD_KEY, startupStateFromSession, mode
				);
				ctx.writeAndFlush(response);
				ctx.close();
				return false;
			}
			return true; // Indicates that this is the first message
		}
		return false; // Indicates that this is not the first message
	}

	/**
	 * Handles the sender logic for processing incoming synchronization messages.
	 *
	 * @param session The channel session associated with the sender.
	 * @param message The incoming synchronization publication message.
	 *
	 * @throws Exception If an error occurs during processing.
	 */
	private void handleSender(Channel session, DefaultSyncPublication message) throws Exception {
		NetworkClient synchronizationClient = (NetworkClient) session.attr(ChannelAttributes.SYNC_SESSION.getKey());

		boolean isRingSynchronization = SynchronisationType.isRingType(this.sync);

		if (isRingSynchronization) {
			handleRingSync(message);
		}

		if (isControlMessage(message)) {
			handleControlMessage(session, synchronizationClient, message);
			return;
		}

		if (isInvalidSequence(message)) {
			handleInvalidSequence(session, synchronizationClient);
			return;
		}

		// Both branches were calling the same method, so this was simplified
		processSynchronizationMessage(session, message, synchronizationClient, isRingSynchronization);
	}

	/**
	 * Handles synchronization for ring-based communication by updating expected node IDs.
	 *
	 * @param publication The incoming synchronization publication message.
	 */
	private void handleRingSync(DefaultSyncPublication publication) {
		if (publication.getExpectedIds() != null) {
			this.expectedNodes.removeAll(publication.getExpectedIds().getIds());
		} else {
			this.expectedNodes.remove(publication.getId());
		}
	}

	/**
	 * Checks if the provided message is a control message.
	 *
	 * @param publication The synchronization publication message to check.
	 *
	 * @return true if the message is a control message; otherwise, false.
	 */
	private boolean isControlMessage(DefaultSyncPublication publication) {
		DefaultSyncPublication.MessageType type = publication.getType();
		return type == DefaultSyncPublication.MessageType.TYPE_OK ||
		       type == DefaultSyncPublication.MessageType.TYPE_BOTH_STARTUP ||
		       type == DefaultSyncPublication.MessageType.TYPE_NOT_VALID_EDGE ||
		       type == DefaultSyncPublication.MessageType.TYPE_BAD_ID ||
		       type == DefaultSyncPublication.MessageType.TYPE_BAD_SEQ ||
		       type == DefaultSyncPublication.MessageType.TYPE_BAD_KEY;
	}

	/**
	 * Handles control messages based on their type, possibly closing the session.
	 *
	 * @param session               The channel session associated with the control message.
	 * @param synchronizationClient The network client associated with the session.
	 * @param controlMessage        The control message to be processed.
	 *
	 * @throws Exception If an error occurs during processing.
	 */
	private void handleControlMessage(Channel session, NetworkClient synchronizationClient, DefaultSyncPublication controlMessage) throws Exception {
		if (controlMessage.getType() == DefaultSyncPublication.MessageType.TYPE_OK) {
			numberOfTriedAttempts++;
			createResult();
			session.attr(ChannelAttributes.PLANNED_CLOSE.getKey());
			session.close();
			return;
		}

		workCallback(synchronizationClient, HandlerState.UNPROPER);

		if (shouldCloseOnControlMessage(controlMessage)) {
			session.attr(ChannelAttributes.PLANNED_CLOSE.getKey());
			session.close();
		}
	}

	/**
	 * Determines whether the session should be closed based on the type of control message received.
	 *
	 * @param publication The control message being evaluated.
	 *
	 * @return true if the session should be closed; otherwise, false.
	 */
	private boolean shouldCloseOnControlMessage(DefaultSyncPublication publication) {
		return publication.getType() == DefaultSyncPublication.MessageType.TYPE_BAD_KEY ||
		       publication.getType() == DefaultSyncPublication.MessageType.TYPE_FAILED_RING;
	}

	/**
	 * Checks if the provided sequence number of the publication message is invalid.
	 *
	 * @param publication The synchronization publication message to check.
	 *
	 * @return true if the sequence number is invalid; otherwise, false.
	 */
	private boolean isInvalidSequence(DefaultSyncPublication publication) {
		return publication.getSequence() < 0 || publication.getSequence() > DefaultSyncPublication.SEQ_MAX;
	}


	/**
	 * Handles an invalid sequence by sending an error response and closing the session.
	 *
	 * @param session    The channel session to which the response is sent.
	 * @param syncClient The network client associated with the synchronization.
	 *
	 * @throws Exception If an error occurs during response creation or session closure.
	 */
	private void handleInvalidSequence(Channel session, NetworkClient syncClient) throws Exception {
		DefaultSyncPublication response = createSimpleResponse(DefaultSyncPublication.MessageType.TYPE_BAD_SEQ, startupState, mode);
		session.writeAndFlush(response);
		session.attr(ChannelAttributes.PLANNED_CLOSE.getKey());
		session.close();
		workCallback(syncClient, HandlerState.UNPROPER);
	}

	/**
	 * Processes a synchronization message from the network client.
	 *
	 * @param session               The channel session for communication.
	 * @param publicationMessage    The received sync publication message.
	 * @param synchronizationClient The network client for synchronization.
	 * @param isRingSynchronization Flag indicating if the synchronization is in ring mode.
	 *
	 * @throws Exception If an error occurs during processing.
	 */
	private void processSynchronizationMessage(
		 Channel session, DefaultSyncPublication publicationMessage, NetworkClient synchronizationClient, boolean isRingSynchronization
	) throws Exception {
		Collection<SyncContent> syncContents = publicationMessage.getContents();
		if (syncContents == null) {
			closeWithCallback(session, synchronizationClient);
			return;
		}

		Set<SyncContent> responseContents = new HashSet<>();
		Map<String, SyncContent> failedContents = new HashMap<>();
		int numberOfNullResponses = 0;

		for (SyncContent syncContent : syncContents) {
			byte[] messageData = syncContent.getContent();
			if (messageData == null) {
				handleNullContent(publicationMessage, syncContent, responseContents, failedContents, isRingSynchronization);
				continue;
			}

			Publication decodedMessage = this.publicationInstance.getDeclaredConstructor().newInstance();
			SyncProtocolBundle outputBundle = new SyncProtocolBundle();
			boolean decodeSuccess = decodeGetCallbackResult(
				 callback, session, decodedMessage, messageData,
				 syncContent.getAwareIds(), outputBundle
			);
			handleDecodedMessage(
				 publicationMessage, syncContent, decodeSuccess,
				 decodedMessage, outputBundle, responseContents,
				 failedContents, isRingSynchronization
			);
		}

		if (checkAndPrepareFailedResult(session, isRingSynchronization, responseContents,
		                                failedContents, numberOfNullResponses
		)) {
			return;
		}

		sendSyncResponse(session, publicationMessage, responseContents, isRingSynchronization);
	}

	/**
	 * Processes cluster synchronization by handling the publication message and managing responses.
	 *
	 * @param session               The channel session for communication.
	 * @param publicationMessage    The received cluster sync publication message.
	 * @param synchronizationClient The network client for synchronization.
	 * @param isRingSynchronization Flag indicating if the synchronization is in ring mode.
	 *
	 * @throws Exception If an error occurs during processing.
	 */
	private void processClusterSynchronization(
		 Channel session, DefaultSyncPublication publicationMessage, NetworkClient synchronizationClient, boolean isRingSynchronization
	) throws Exception {
		Collection<SyncContent> syncContents = publicationMessage.getContents();
		if (syncContents == null) {
			closeWithCallback(session, synchronizationClient);
			return;
		}

		Set<SyncContent> responseContents = new HashSet<>();
		Map<String, SyncContent> failedContents = new HashMap<>();
		int numberOfNullResponses = 0;

		for (SyncContent syncContent : syncContents) {
			byte[] messageData = syncContent.getContent();
			if (messageData == null) {
				handleNullContent(
					 publicationMessage, syncContent, responseContents,
					 failedContents, isRingSynchronization
				);
				continue;
			}

			ClusterPublication decodedMessage = new ClusterPublication();
			SyncProtocolBundle outputBundle = new SyncProtocolBundle();
			boolean decodeSuccess = decodeGetCallbackResult(
				 new ClusterPublicationHandler(syncContext), session, decodedMessage, messageData,
				 syncContent.getAwareIds(), outputBundle
			);
			handleDecodedMessage(
				 publicationMessage, syncContent, decodeSuccess,
				 decodedMessage, outputBundle, responseContents,
				 failedContents, isRingSynchronization
			);
		}

		if (checkAndPrepareFailedResult(
			 session, isRingSynchronization,
			 responseContents, failedContents, numberOfNullResponses
		)) {
			return;
		}

		sendSyncResponse(session, publicationMessage, responseContents, isRingSynchronization);
	}

	/**
	 * Handles null content in the sync message, updating the sync result and response contents accordingly.
	 *
	 * @param publicationMessage    The sync publication message that contains the null content.
	 * @param syncContent           The sync content that is being processed.
	 * @param responseContents      The set of response contents to be populated.
	 * @param failedContents        The map of failed contents to be updated.
	 * @param isRingSynchronization Flag indicating if the synchronization is in ring mode.
	 */
	private void handleNullContent(
		 DefaultSyncPublication publicationMessage, SyncContent syncContent, Set<SyncContent> responseContents,
		 Map<String, SyncContent> failedContents, boolean isRingSynchronization
	) {
		ClusterIdRegistry awareMembers = syncContent.getAwareIds();
		if (awareMembers == null) {
			awareMembers = new ClusterIdRegistry();
		}

		awareMembers.add(publicationMessage.getId());
		SyncResult syncResult = syncFeature.get(syncContent.getKey());

		if (syncContent.getVersion() > 0) {
			fillSyncResultForVersionGreaterThanZero(
				 publicationMessage, isRingSynchronization, awareMembers, syncResult
			);
			awareMembers.add(selfMember.getId());
			if (syncContent.getKey() != null && !syncContent.getKey().isEmpty()) {
				syncContext.addAwareNodes(syncContent.getKey(), syncContent.getVersion(), awareMembers);
			}
		} else {
			handleNullDataOfStarterHandler(
				 publicationMessage, isRingSynchronization, failedContents,
				 syncContent, awareMembers, syncResult
			);
		}
	}

	/**
	 * Handles the result of the decoded message, updating response contents based on success or failure.
	 *
	 * @param publicationMessage    The sync publication message being processed.
	 * @param syncContent           The sync content being handled.
	 * @param decodeSuccess         Indicates if the decoding was successful.
	 * @param decodedMessage        The successfully decoded publication message.
	 * @param outputBundle          The bundle containing any response messages.
	 * @param responseContents      The set of response contents to be populated.
	 * @param failedContents        The map of failed contents to be updated.
	 * @param isRingSynchronization Flag indicating if the synchronization is in ring mode.
	 */
	private void handleDecodedMessage(
		 DefaultSyncPublication publicationMessage, SyncContent syncContent, boolean decodeSuccess,
		 Publication decodedMessage, SyncProtocolBundle outputBundle, Set<SyncContent> responseContents,
		 Map<String, SyncContent> failedContents, boolean isRingSynchronization
	) {
		List<Publication> responses = outputBundle.getMessages();

		if (!decodeSuccess) {
			if (isRingSynchronization) {
				failedContents.put(syncContent.getKey(), this.syncContents.get(syncContent.getKey()));
			}
			if (responses != null) {
				for (Publication response : responses) {
					if (response != null) {
						ClusterIdRegistry awareNodes = syncContext.getAwareNodes(response.getKey(), response.getVersion());
						responseContents.add(new SyncContent(response.getKey(), response.getVersion(), awareNodes, response.serialize()));
					}
				}
			} else {
				handleResponseContentsForNullResponses(publicationMessage, isRingSynchronization, responseContents, syncContent);
			}
		} else {
			ClusterIdRegistry awareNodes = syncContent.getAwareIds();
			if (awareNodes == null) {
				awareNodes = new ClusterIdRegistry();
			}
			awareNodes.addAll(selfMember.getId(), publicationMessage.getId());

			if (syncContent.getVersion() > 0 && syncContent.getKey() != null && !syncContent.getKey().isEmpty()) {
				syncContext.addAwareNodes(syncContent.getKey(), syncContent.getVersion(), awareNodes);
			}
			syncResultHandleStarter(publicationMessage, isRingSynchronization, syncContent, awareNodes);

			if (responses != null) {
				for (Publication response : responses) {
					if (response != null) {
						awareNodes = syncContext.getAwareNodes(response.getKey(), response.getVersion());
						responseContents.add(new SyncContent(syncContent.getKey(), response.getVersion(), awareNodes, response.serialize()));
					}
				}
			}
		}
	}


	/**
	 * Closes the specified channel with a callback to handle the associated network client.
	 *
	 * @param session    the channel to close
	 * @param syncClient the network client associated with the session
	 *
	 * @throws Exception if an error occurs during the closing process
	 */
	private void closeWithCallback(Channel session, NetworkClient syncClient) throws Exception {
		session.attr(ChannelAttributes.PLANNED_CLOSE.getKey());
		session.close();
		workCallback(syncClient, HandlerState.UNPROPER);
	}

	/**
	 * Sends a synchronization response to the specified channel with the given message and contents.
	 *
	 * @param session          the channel to send the response to
	 * @param msg              the original synchronization message
	 * @param responseContents the contents to include in the response
	 * @param isRingSync       indicates if the synchronization is in ring mode
	 */
	private void sendSyncResponse(Channel session, DefaultSyncPublication msg, Set<SyncContent> responseContents, boolean isRingSync) {
		DefaultSyncPublication response = createCompleteResponse(
			 DefaultSyncPublication.MessageType.TYPE_CHECK, null,
			 SyncMode.SYNC_MESSAGE, this.sync, (byte) (msg.getSequence() + 1)
		);
		response.setContents(responseContents);
		session.writeAndFlush(response);
	}

	/**
	 * Checks and prepares the result for a failed synchronization based on response contents.
	 *
	 * @param session          the channel associated with the synchronization
	 * @param isRing           indicates if the synchronization is in ring mode
	 * @param responseContents the contents received in response
	 * @param faildContents    a map of failed synchronization contents
	 * @param numberOfNull     the number of null responses received
	 *
	 * @return true if the result was prepared and the session should be closed, false otherwise
	 */
	private boolean checkAndPrepareFailedResult(
		 Channel session, boolean isRing, Set<SyncContent> responseContents,
		 Map<String, SyncContent> faildContents, int numberOfNull
	) {
		if (isRing) {
			if (this.expectedNodes.isEmpty()) {
				this.syncContents = faildContents;
			}
		}

		int responsesSize = responseContents.size();

		if (responsesSize == 0 || numberOfNull == responsesSize) {
			numberOfTriedAttempts++;
			createResult();
			session.attr(ChannelAttributes.PLANNED_CLOSE.getKey());
			session.close();
			return true;
		}
		return false;
	}

	/**
	 * Initiates the handling of synchronization results based on the provided publication message.
	 *
	 * @param publicationMessage the synchronization publication message
	 * @param isRing             indicates if the synchronization is in ring mode
	 * @param syncContent        the synchronization content associated with the result
	 * @param awareNodes         the nodes that are aware of the synchronization
	 */
	private void syncResultHandleStarter(
		 DefaultSyncPublication publicationMessage, boolean isRing,
		 SyncContent syncContent, ClusterIdRegistry awareNodes
	) {
		// Get the SyncResult associated with the key in SyncContent
		SyncResult syncResult = syncFeature.get(syncContent.getKey());

		// Ensure SyncResult is not null before proceeding
		if (syncResult != null) {
			ClusterIdRegistry expectedIds = publicationMessage.getExpectedIds();
			// If in ring mode, update expectedIds with awareNodes
			if (isRing && expectedIds != null) {
				expectedIds.addAll(awareNodes.getIds());
				for (short id : expectedIds.getIds()) {
					if (expectedIds.contains(id)) { // Check if id is in expectedIds
						syncResult.removeFailedMember(id);
						syncResult.addSyncedMember(id);
					}
				}
			} else {
				// If not in ring mode, sync directly with awareNodes
				for (short id : awareNodes.getIds()) {
					if (awareNodes.contains(id)) { // Check if id is in awareNodes
						syncResult.removeFailedMember(id);
						syncResult.addSyncedMember(id);
					}
				}
			}
		}

		// Ensure expectedNodes is not null and has elements before proceeding
		if (this.expectedNodes != null && !this.expectedNodes.isEmpty()) {
			SyncContent syncContentFromMap = this.syncContents.get(syncContent.getKey());
			if (syncContentFromMap != null) {
				// Assuming this method correctly handles adding aware IDs
				syncContentFromMap.addAwareId(syncContent.getAwareIds());
			}
		}
	}

	/**
	 * Handles the response contents for null responses received during synchronization.
	 *
	 * @param defaultSyncPublication the default synchronization publication message
	 * @param isRingMode             indicates if the synchronization is in ring mode
	 * @param responseContents       the contents received in response
	 * @param syncContent            the synchronization content associated with the response
	 */
	private void handleResponseContentsForNullResponses(
		 DefaultSyncPublication defaultSyncPublication,
		 boolean isRingMode,
		 Set<SyncContent> responseContents,
		 SyncContent syncContent
	) {
		// Initialize failed IDs with the current member's ID
		ClusterIdRegistry failedMemberIds = new ClusterIdRegistry(selfMember.getId());
		ClusterIdRegistry expectedMemberIds = defaultSyncPublication.getExpectedIds(); // Get expected IDs
		SyncResult syncResult = syncFeature.get(syncContent.getKey()); // Get the sync result for the given key

		// Check if we're in ring mode
		if (isRingMode && expectedMemberIds != null) {
			// Loop through expectedMemberIds and manage the failed member list
			for (short memberId : expectedMemberIds.getIds()) {
				if (expectedMemberIds.contains(memberId)) { // Check if memberId is in expectedMemberIds
					syncResult.addFailedMember(memberId);
					syncResult.removeSyncedMember(memberId);
				}
			}
		} else {
			// Not in ring mode, handle the current message ID
			syncResult.addFailedMember(defaultSyncPublication.getId());
			syncResult.removeSyncedMember(defaultSyncPublication.getId());
		}

		// Add a new SyncContent entry for the response contents
		responseContents.add(new SyncContent(syncContent.getKey(), 0, failedMemberIds, null));
	}

	/**
	 * Handles the null data for the starter handler based on the synchronization message.
	 *
	 * @param publicationMessage the synchronization publication message
	 * @param isRing             indicates if the synchronization is in ring mode
	 * @param failedContents     a map of failed synchronization contents
	 * @param syncContent        the synchronization content associated with the result
	 * @param awareMembers       the nodes that are aware of the synchronization
	 * @param syncResult         the synchronization result object
	 */
	private void handleNullDataOfStarterHandler(
		 DefaultSyncPublication publicationMessage, boolean isRing,
		 Map<String, SyncContent> failedContents, SyncContent syncContent,
		 ClusterIdRegistry awareMembers, SyncResult syncResult
	) {
		if (isRing) {
			for (short id : ids.getIds()) {
				if (awareMembers.contains(id)) { // Check if id is in awareMembers
					syncResult.addFailedMember(id);
					syncResult.removeSyncedMember(id);
				}
			}
		} else {
			syncResult.addFailedMember(publicationMessage.getId());
			syncResult.removeSyncedMember(publicationMessage.getId());
		}

		failedContents.put(syncContent.getKey(), this.syncContents.get(syncContent.getKey()));
	}

	/**
	 * Fills the synchronization result for publications with a version greater than zero.
	 *
	 * @param publicationMessage the synchronization publication message
	 * @param isRing             indicates if the synchronization is in ring mode
	 * @param awareNodes         the nodes that are aware of the synchronization
	 * @param syncResult         the synchronization result object
	 */
	private void fillSyncResultForVersionGreaterThanZero(
		 DefaultSyncPublication publicationMessage, boolean isRing,
		 ClusterIdRegistry awareNodes, SyncResult syncResult
	) {
		if (isRing) {
			for (short id : ids.getIds()) {
				if (awareNodes.contains(id)) { // Check if id is in awareNodes
					syncResult.addSyncedMember(id);
					syncResult.removeFailedMember(id);
				}
			}
		} else {
			syncResult.addSyncedMember(publicationMessage.getId());
			syncResult.removeFailedMember(publicationMessage.getId());
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultSyncPublication message) throws Exception {
		Channel session = ctx.channel();
		if (selfMember == null) {
			return;
		}

		if (endpointType == EndpointType.SERVER) {
			handleListener(session, message);
		} else {
			handleSender(session, message);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof IOException) {
			return;
		}

		if (endpointType == EndpointType.CLIENT) {
			InetSocketAddress peer = ((InetSocketAddress) ctx.channel().remoteAddress());
			NetworkClient sync = (NetworkClient) ctx.channel().attr(ChannelAttributes.SYNC_SESSION.getKey()).get(); // Use appropriate key
			workCallback(sync, HandlerState.WORK_FAILED);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if (endpointType == EndpointType.CLIENT && !ctx.channel().hasAttr(ChannelAttributes.PLANNED_CLOSE_KEY.getKey())) { // Use appropriate key
			NetworkClient sync = (NetworkClient) ctx.channel().attr(ChannelAttributes.ATTRIBUTE_KEY.getKey()).get(); // Use appropriate key

			workCallback(sync, HandlerState.UNPROPER); // Assuming link is a class clusterMember or passed in some way
		}
	}

	/**
	 * Callback method that handles work completion for a given network client.
	 *
	 * @param session      the network client session
	 * @param handlerState the state of the handler after the work is completed
	 */
	public void workCallback(NetworkClient session, HandlerState handlerState) {
		if (sessions == null) {
			return;
		}

		try {
			switch (sync) {
				case RING:
				case RING_QUORUM:
				case RING_BALANCE:
				case RING_BALANCE_QUORUM:
				case UNI_CAST_ONE_OF:
					handleRingOrUnicastCallback(session, handlerState);
					break;

				case UNI_CAST:
				case UNI_CAST_QUORUM:
				case UNI_CAST_BALANCE:
				case UNI_CAST_BALANCE_QUORUM:
					handleUniCastCallback(session, handlerState);
					break;

				default:
					break;
			}

			createResult();
		} catch (final Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	/**
	 * Handles callbacks for ring or unicast synchronization types based on the handler state.
	 *
	 * @param session      the network client session
	 * @param handlerState the state of the handler after the work is completed
	 *
	 * @throws Exception if an error occurs while processing the callback
	 */
	private void handleRingOrUnicastCallback(NetworkClient session, HandlerState handlerState) throws Exception {
		if (handlerState == HandlerState.WORK_FAILED || handlerState == HandlerState.UNPROPER) {
			ClusterMember clusterMember = session.getMember();
			if (clusterMember != null && clusterMember.getState() == MemberState.DELETED) {
				syncContext.synchronizedStateChange(clusterMember, MemberState.DOWN);
				invalidClients.add(currentSocket);
				if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
					// Use ShortArrayHelper to remove the clusterMember ID from expectedNodes
					this.expectedNodes.remove(session.getMemberId());
				}
			}
		}

		if (handlerState == HandlerState.WORK_FAILED) {
			handleFailedWork(session);
		} else if (handlerState == HandlerState.UNPROPER) {
			handleImproperWork(session);
		}
	}

	/**
	 * Handles the scenario when work for a session fails.
	 *
	 * @param session the network client session
	 *
	 * @throws Exception if an error occurs while processing the failed work
	 */
	private void handleFailedWork(NetworkClient session) throws Exception {
		session.setupNextClient();

		if (session.isAllTried()) {
			if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
				// Use ShortArrayHelper to remove the clusterMember ID from expectedNodes
				expectedNodes.remove(session.getMemberId());
			}
			numberOfTriedAttempts++;
			addFailedNodeToSyncFeatures(session.getMember().getId());
		}
		failureOrUnProperSocketWorkAround();
	}

	/**
	 * Handles the scenario when work for a session is improper.
	 *
	 * @param session the network client session
	 */
	private void handleImproperWork(NetworkClient session) {
		if (!session.isImproper()) {
			session.setImproper(true);
			invalidClients.add(currentSocket);
			if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
				expectedNodes.remove(session.getMemberId());
			}
			numberOfTriedAttempts++;
			addFailedNodeToSyncFeatures(session.getMember().getId());
			failureOrUnProperSocketWorkAround();
		}
	}

	/**
	 * Handles callbacks for unicast synchronization types based on the handler state.
	 *
	 * @param session      the network client session
	 * @param handlerState the state of the handler after the work is completed
	 *
	 * @throws Exception if an error occurs while processing the callback
	 */
	private void handleUniCastCallback(NetworkClient session, HandlerState handlerState) throws Exception {
		if (handlerState == HandlerState.WORK_FAILED || handlerState == HandlerState.UNPROPER) {
			ClusterMember clusterMember = session.getMember();
			if (clusterMember != null && clusterMember.getState() == MemberState.DELETED) {
				syncContext.synchronizedStateChange(clusterMember, MemberState.DOWN);
				numberOfTriedAttempts++;
				addFailedNodeToSyncFeatures(clusterMember.getId());
			}
		}

		if (handlerState == HandlerState.WORK_FAILED) {
			handleFailedWork(session);
		} else if (handlerState == HandlerState.UNPROPER) {
			handleImproperWork(session);
		}
	}


	/**
	 * Handles cases where the current socket is not functioning properly
	 * or has failed. It iterates through the available sockets and
	 * publishes a response message if a valid socket is found.
	 */
	private void failureOrUnProperSocketWorkAround() {
		int lastSocket = currentSocket;
		currentSocket = (++currentSocket) % sessions.size();
		boolean isAllTried = sessions.get(currentSocket).isAllTried();
		boolean isImproperSocket = invalidClients.contains(currentSocket);
		boolean isExpectedNode = expectedNodes.contains(sessions.get(currentSocket).getMemberId());

		while (isAllTried || isImproperSocket ||
		       (sync != SynchronisationType.UNI_CAST_ONE_OF && !isExpectedNode)) {
			if (currentSocket == lastSocket) {
				sessions = null;
				fixMonitorLastModified();
				createResult();
				return;
			}
			currentSocket = (++currentSocket) % sessions.size();
		}
		DefaultSyncPublication message = createCompleteResponse(DefaultSyncPublication.MessageType.TYPE_CHECK, startupState, mode, sync, (byte) -1);
		message.setContents(this.syncContents.values());
		sessions.get(currentSocket).publish(message);
	}

	/**
	 * Creates synchronization results based on the current state of the session.
	 * It processes sync results if certain conditions are met or handles
	 * incomplete synchronization otherwise.
	 */
	private void createResult() {
		// Check if we should proceed with creating results based on current state
		if (shouldCreateResult()) {
			processSyncResults();
		} else if (expectedNodes != null && !syncContents.isEmpty()) {
			// Handle case when it is a ring but not everything is complete
			handleIncompleteSync();
		}
	}

	/**
	 * Determines whether results should be created based on the state of the sessions.
	 *
	 * @return true if results can be created; false otherwise
	 */
	private boolean shouldCreateResult() {
		return sessions == null ||
		       (expectedNodes != null && expectedNodes.isEmpty()) ||
		       ids == null ||
		       numberOfTriedAttempts == ids.size() ||
		       (numberOfTriedAttempts > 0 && sync == SynchronisationType.UNI_CAST_ONE_OF);
	}

	/**
	 * Processes synchronization results and evaluates each sync result.
	 */
	private void processSyncResults() {
		int syncingNodesLength = (ids == null) ? 0 : ids.size();
		boolean isQuorum = isQuorumSync();

		for (Entry<String, SyncResult> entry : syncFeature.entrySet()) {
			SyncResult result = entry.getValue();
			evaluateSyncResult(syncingNodesLength, isQuorum, result);
		}

		if (nonAsync) {
			notifyNonAsync();
		}

		if (callback != null) {
			callback.result(syncFeature);
		}
	}

	/**
	 * Checks if the current synchronization type is quorum-based.
	 *
	 * @return true if the synchronization type is quorum-based; false otherwise
	 */
	private boolean isQuorumSync() {
		return sync == SynchronisationType.UNI_CAST_QUORUM ||
		       sync == SynchronisationType.RING_QUORUM ||
		       sync == SynchronisationType.RING_BALANCE_QUORUM ||
		       sync == SynchronisationType.UNI_CAST_BALANCE_QUORUM;
	}

	/**
	 * Evaluates the synchronization result based on the number of syncing nodes
	 * and whether quorum is required.
	 *
	 * @param syncingNodesLength the number of nodes currently syncing
	 * @param isQuorum           indicates if quorum synchronization is used
	 * @param result             the sync result to evaluate
	 */
	private void evaluateSyncResult(int syncingNodesLength, boolean isQuorum, SyncResult result) {
		if (syncingNodesLength == 0) {
			result.setSuccessful(false);
			return;
		}

		if (isQuorum) {
			if (result.getFailedMembers().size() * 2 < syncingNodesLength) {
				result.setSuccessful(true);
			} else {
				result.setSuccessful(result.getSyncedMembers().size() * 2 > syncingNodesLength);
			}
		} else {
			if (result.getSyncedMembers().size() > result.getFailedMembers().size()) {
				result.setSuccessful(true);
			} else if (sync == SynchronisationType.UNI_CAST_ONE_OF && !result.getFailedMembers().isEmpty()) {
				result.setSuccessful(false);
			} else {
				result.setSuccessful(result.getFailedMembers().size() < syncingNodesLength);
			}
		}
	}

	/**
	 * Notifies that a non-async operation has completed.
	 */
	private void notifyNonAsync() {
		nonAsync = false;
		nonasyncLock.notify();
	}

	/**
	 * Handles cases of incomplete synchronization by iterating through
	 * the session's sockets and checking for valid members.
	 */
	private void handleIncompleteSync() {
		if (sessions.isEmpty()) {
			handleAllNodesTried();
			return;
		}

		currentSocket = (currentSocket + 1) % sessions.size();
		int lastSocket = currentSocket;

		while (sessions.get(currentSocket).isAllTried() ||
		       invalidClients.contains(currentSocket) ||
		       !expectedNodes.contains(sessions.get(currentSocket).getMemberId())) {

			if (currentSocket == lastSocket) {
				handleAllNodesTried();
				return;
			}

			currentSocket = (currentSocket + 1) % sessions.size();

			// Ensure currentSocket is valid before accessing
			if (sessions.size() == 0) {
				handleAllNodesTried();
				return;
			}
		}

		publishSyncCheck();
	}


	/**
	 * Handles the scenario where all nodes have been tried but not completed.
	 */
	private void handleAllNodesTried() {
		sessions = null;
		fixMonitorLastModified();
		createResult(); // Restart the process
	}

	/**
	 * Publishes a synchronization check message to the current socket.
	 */
	private void publishSyncCheck() {
		DefaultSyncPublication message = new DefaultSyncPublication();
		message.setId(selfMember.getId());
		message.setInStartup(startupState);
		message.setSyncMode(mode);
		message.setSyncType(sync);
		message.setType(DefaultSyncPublication.MessageType.TYPE_CHECK);
		message.setContents(syncContents.values());
		sessions.get(currentSocket).publish(message);
	}

	/**
	 * Adds a failed node to the synchronization features for further processing.
	 *
	 * @param failedNodeId the ID of the failed node to add
	 */
	private void addFailedNodeToSyncFeatures(Short failedNodeId) {
		for (Map.Entry<String, SyncResult> syncResultEntry : syncFeature.entrySet()) {
			SyncResult syncResult = syncResultEntry.getValue();
			syncResult.addFailedMember(failedNodeId);
		}
	}

	/**
	 * Initiates synchronization with balance based on the given publications.
	 *
	 * @param publications a list of publications to synchronize
	 */
	private void syncWithBalance(List<Publication> publications) {
		endpointType = EndpointType.CLIENT;

		ClusterSnapshot clusterSnapshot = syncContext.getSnapshot();
		if (clusterSnapshot == null) {
			nonasyncLock = null; // Reset the lock if the snapshot is unavailable
			return;
		}

		this.syncFeature = new HashMap<>();
		List<ClusterMember> clusterMembers = prepareCluster(clusterSnapshot);
		if (clusterMembers == null) return;

		PublicationHandler callbackHandler = createCallbackHandler();

		if (isUniCastBalanceSync()) {
			handleUniCastBalanceSync(publications, clusterSnapshot, clusterMembers, callbackHandler);
		} else if (isRingBalanceSync()) {
			handleRingBalanceSync(publications, clusterSnapshot, clusterMembers, callbackHandler);
		}
	}

	/**
	 * Checks if the current synchronization type is unicast balance synchronization.
	 *
	 * @return true if the synchronization type is either {@link SynchronisationType#UNI_CAST_BALANCE} or
	 * {@link SynchronisationType#UNI_CAST_BALANCE_QUORUM}; false otherwise.
	 */
	private boolean isUniCastBalanceSync() {
		return this.sync == SynchronisationType.UNI_CAST_BALANCE || this.sync == SynchronisationType.UNI_CAST_BALANCE_QUORUM;
	}

	/**
	 * Checks if the current synchronization type is ring balance synchronization.
	 *
	 * @return true if the synchronization type is either {@link SynchronisationType#RING_BALANCE} or
	 * {@link SynchronisationType#RING_BALANCE_QUORUM}; false otherwise.
	 */
	private boolean isRingBalanceSync() {
		return this.sync == SynchronisationType.RING_BALANCE || this.sync == SynchronisationType.RING_BALANCE_QUORUM;
	}

	/**
	 * Prepares a list of cluster members based on the provided cluster snapshot.
	 *
	 * @param clusterSnapshot the current snapshot of the cluster
	 *
	 * @return a list of cluster members, or null if no members are found
	 */
	private List<ClusterMember> prepareCluster(ClusterSnapshot clusterSnapshot) {
		List<ClusterMember> cluster = null;

		if (ids == null || ids.isEmpty()) {
			cluster = clusterSnapshot.getAliveCluster();
			if (!cluster.isEmpty()) {
				ids = new ClusterIdRegistry();
				for (ClusterMember e : cluster) {
					ids.add(e.getId());
				}
			}
		} else {
			// For the ring balance case, use the cluster directly
			cluster = clusterSnapshot.getAliveCluster();
			if (!isInclusive) {
				ClusterIdRegistry idss = new ClusterIdRegistry();
				performInverseIds(cluster, idss);
				this.ids = idss;
			}
		}

		return cluster;
	}

	/**
	 * Creates a callback handler for handling synchronization results.
	 *
	 * @return a {@link PublicationHandler} that processes sync results and callback actions
	 */
	private PublicationHandler createCallbackHandler() {
		return new PublicationHandler() {
			private short successCount = 0; // Count of successful results

			@Override
			public synchronized void result(Map<String, SyncResult> syncResults) {
				successCount++;
				if (syncResults != null) {
					SyncNetworkHandler.this.syncFeature.putAll(syncResults);
				}

				if (successCount == SyncNetworkHandler.this.ids.size()) {
					SyncNetworkHandler.this.numberOfTriedAttempts = successCount; // Update the number of attempts
					SyncNetworkHandler.this.createResult();
				}
			}

			@Override
			public boolean callBack(Session session, Publication publication, ClusterIdRegistry targetNodes, PublicationBundle outputBundle) {
				return SyncNetworkHandler.this.callback != null && SyncNetworkHandler.this.callback.callBack(session, publication, targetNodes, outputBundle);
			}
		};
	}

	/**
	 * Handles the synchronization process for unicast balance synchronization.
	 *
	 * @param publications    a list of publications to send
	 * @param clusterSnapshot the current snapshot of the cluster
	 * @param clusterMembers  a list of cluster members to synchronize with
	 * @param callbackHandler the handler to be called upon message sending completion
	 */
	private void handleUniCastBalanceSync(
		 List<Publication> publications, ClusterSnapshot clusterSnapshot,
		 List<ClusterMember> clusterMembers, PublicationHandler callbackHandler
	) {
		Map<Short, List<Publication>> messagesGroupedByNode = new HashMap<>();

		if (ids.isEmpty()) {
			fillMessagesForNewCluster(publications, clusterMembers, messagesGroupedByNode);
		} else {
			fillMessagesForExistingCluster(publications, clusterSnapshot, messagesGroupedByNode);
		}

		if (messagesGroupedByNode.isEmpty()) {
			nonasyncLock = null; // Reset the lock if no messages are available
			createSyncResultForUnsentMessages(publications);
			return;
		}

		sendMessagesToClusterNodes(messagesGroupedByNode, callbackHandler);
	}

	/**
	 * Fills the map with publications grouped by cluster member IDs for a new cluster.
	 *
	 * @param publications     a list of publications to be filled
	 * @param clusterMembers   a list of cluster members to check against
	 * @param messagesByNodeId a map to store publications grouped by cluster member IDs
	 */
	private void fillMessagesForNewCluster(
		 List<Publication> publications, List<ClusterMember> clusterMembers,
		 Map<Short, List<Publication>> messagesByNodeId
	) {
		for (ClusterMember clusterMember : clusterMembers) {
			fillMessagesPerNode(publications, messagesByNodeId, clusterMember);
		}
	}

	/**
	 * Fills the map with publications grouped by cluster member IDs for an existing cluster.
	 *
	 * @param publications     a list of publications to be filled
	 * @param clusterSnapshot  the current snapshot of the cluster
	 * @param messagesByNodeId a map to store publications grouped by cluster member IDs
	 */
	private void fillMessagesForExistingCluster(
		 List<Publication> publications, ClusterSnapshot clusterSnapshot,
		 Map<Short, List<Publication>> messagesByNodeId
	) {
		if (isInclusive) {
			for (short clusterMemberId : ids.getIds()) {
				ClusterMember clusterMember = clusterSnapshot.getById(clusterMemberId, 2);
				if (clusterMember != null) {
					fillMessagesPerNode(publications, messagesByNodeId, clusterMember);
				}
			}
		}
	}

	/**
	 * Sends grouped messages to cluster nodes.
	 *
	 * @param messagesByNodeId a map of messages grouped by cluster member IDs
	 * @param callbackHandler  the handler to be called upon message sending completion
	 */
	private void sendMessagesToClusterNodes(Map<Short, List<Publication>> messagesByNodeId, PublicationHandler callbackHandler) {
		for (Map.Entry<Short, List<Publication>> entry : messagesByNodeId.entrySet()) {
			SyncNetworkHandler syncNetworkHandler = new SyncNetworkHandler(syncContext, this.sync)
				 .withCluster(entry.getKey())
				 .withCallBack(callbackHandler)
				 .withPublicationType(this.publicationInstance)
				 .withBalance();

			syncNetworkHandler.mode = this.mode;
			syncNetworkHandler.sync(entry.getValue());
		}
	}

	/**
	 * Handles the synchronization process for ring balance synchronization.
	 *
	 * @param publications       a list of publications to send
	 * @param clusterSnapshot    the current snapshot of the cluster
	 * @param clusterMembersList a list of cluster members to synchronize with
	 * @param callbackHandler    the handler to be called upon message sending completion
	 */
	private void handleRingBalanceSync(
		 List<Publication> publications, ClusterSnapshot clusterSnapshot,
		 List<ClusterMember> clusterMembersList, PublicationHandler callbackHandler
	) {
		Set<ClusterMember> activeClusterMembers = new HashSet<>();

		if (ids.isEmpty()) {
			activeClusterMembers.addAll(clusterMembersList);
		} else if (isInclusive) {
			for (short id : ids.getIds()) {
				ClusterMember member = clusterSnapshot.getById(id, ClusterSnapshot.MEMBER_CHECK_VALID);
				if (member != null) {
					activeClusterMembers.add(member);
				}
			}
		}

		if (!activeClusterMembers.isEmpty()) {
			Map<Publication, ClusterIdRegistry> messageToUnawareMembersMap = mapMessagesToNotAwareMembers(publications, activeClusterMembers);
			if (!messageToUnawareMembersMap.isEmpty()) {
				sendMessagesForRingBalance(messageToUnawareMembersMap, callbackHandler);
			} else {
				createSyncResultForUnsentMessages(publications);
				nonasyncLock = null; // Reset the lock if no messages to send
			}
		}
	}


	/**
	 * Maps publications to cluster members that are not aware of them.
	 *
	 * @param publications   a list of publications to check against the cluster members
	 * @param clusterMembers a set of cluster members to check for awareness of publications
	 *
	 * @return a map where the keys are publications and the values are the corresponding
	 * {@link ClusterIdRegistry} of unaware cluster members
	 */
	private Map<Publication, ClusterIdRegistry> mapMessagesToNotAwareMembers(
		 List<Publication> publications,
		 Set<ClusterMember> clusterMembers
	) {
		Map<Publication, ClusterIdRegistry> messageToUnawareMembersMap = new HashMap<>();

		for (Publication publication : publications) {
			ClusterIdRegistry unawareMembersForMessage = new ClusterIdRegistry();
			for (ClusterMember clusterMember : clusterMembers) {
				if (!isClusterMemberAwareOfPublication(publication, clusterMember)) {
					unawareMembersForMessage.add(clusterMember.getId());
				}
			}

			if (!unawareMembersForMessage.isEmpty()) {
				messageToUnawareMembersMap.put(publication, unawareMembersForMessage);
			} else {
				SyncResult syncResult = new SyncResult();
				syncResult.setSuccessful(true);
				this.syncFeature.put(publication.getKey(), syncResult);
			}
		}

		return messageToUnawareMembersMap;
	}

	/**
	 * Sends messages to cluster members who are not aware of them, ensuring ring balance.
	 *
	 * @param messageToNotAwareMembers a map of publications to their corresponding unaware cluster members
	 * @param callBackHandler          the handler to be called upon message sending completion
	 */
	private void sendMessagesForRingBalance(
		 Map<Publication, ClusterIdRegistry> messageToNotAwareMembers,
		 PublicationHandler callBackHandler
	) {
		Set<String> processedMessages = new HashSet<>();
		Map<List<Publication>, ClusterIdRegistry> finalMessagesToMembers = new HashMap<>();

		for (Entry<Publication, ClusterIdRegistry> entry : messageToNotAwareMembers.entrySet()) {
			if (entry.getValue().isEmpty() || processedMessages.contains(entry.getKey().getKey())) {
				continue;
			}

			List<Publication> messageList = new ArrayList<>();
			processedMessages.add(entry.getKey().getKey());
			messageList.add(entry.getKey());

			for (Entry<Publication, ClusterIdRegistry> innerEntry : messageToNotAwareMembers.entrySet()) {
				if (innerEntry.getValue().isEmpty() || processedMessages.contains(innerEntry.getKey().getKey())) {
					continue;
				}

				if (entry.getValue().containsAll(innerEntry.getValue().getIds())) {
					messageList.add(innerEntry.getKey());
					processedMessages.add(innerEntry.getKey().getKey());
				}
			}
			finalMessagesToMembers.put(messageList, entry.getValue());
		}

		for (Entry<List<Publication>, ClusterIdRegistry> entry : finalMessagesToMembers.entrySet()) {
			SyncNetworkHandler handler = new SyncNetworkHandler(syncContext, this.sync)
				 .withCluster(entry.getValue().getIds())
				 .withCallBack(callBackHandler)
				 .withPublicationType(this.publicationInstance)
				 .withBalance();
			handler.mode = this.mode;
			handler.sync(entry.getKey());
		}
	}

	/**
	 * Checks if a specific cluster member is aware of a given publication.
	 *
	 * @param publication         the publication to check awareness for
	 * @param targetClusterMember the cluster member whose awareness is being checked
	 *
	 * @return true if the cluster member is aware of the publication; false otherwise
	 */
	private boolean isClusterMemberAwareOfPublication(Publication publication, ClusterMember targetClusterMember) {
		ClusterIdRegistry awareMemberIds; // Initialize as an empty array

		if (this.mode == SyncMode.SYNC_MESSAGE) {
			awareMemberIds = addAndGetAwareNodesOfMessage(publication); // Assuming this method returns a ClusterIdRegistry
		} else {
			ClusterPublication clusterPublication = (ClusterPublication) publication;
			ClusterMember memberNode = syncContext.getMemberById(clusterPublication.getId());
			awareMemberIds = (memberNode == null) ? new ClusterIdRegistry() : memberNode.getAwareIds(); // Initialize as an empty array if memberNode is null
		}

		return awareMemberIds.contains(targetClusterMember.getId());
	}

	/**
	 * Creates a sync result for a list of publications that were not sent.
	 *
	 * @param publications a list of publications that were not sent
	 */
	private void createSyncResultForUnsentMessages(List<Publication> publications) {
		for (Publication publication : publications) {
			SyncResult syncResult = new SyncResult();
			syncResult.setSuccessful(true);
			this.syncFeature.put(publication.getKey(), syncResult);
		}
	}

	/**
	 * Fills a map with publications that a specified cluster member is unaware of.
	 *
	 * @param publications                  a list of all publications
	 * @param publicationsByClusterMemberId a map to fill with publications grouped by cluster member IDs
	 * @param targetClusterMember           the cluster member to check awareness for
	 */
	private void fillMessagesPerNode(
		 List<Publication> publications, Map<Short, List<Publication>> publicationsByClusterMemberId,
		 ClusterMember targetClusterMember
	) {
		List<Publication> filteredPublications = new ArrayList<>();
		for (Publication publication : publications) {
			if (isClusterMemberAwareOfPublication(publication, targetClusterMember)) continue;
			filteredPublications.add(publication);
		}

		if (!filteredPublications.isEmpty()) {
			publicationsByClusterMemberId.put(targetClusterMember.getId(), filteredPublications);
		}
	}

	/**
	 * Fixes the last modified timestamp for monitoring purposes.
	 * This method currently has no implementation.
	 */
	void fixMonitorLastModified() {
		// Implementation to be added.
	}

}
