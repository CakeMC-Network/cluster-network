package net.cakemc.library.cluster.handler

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey
import net.cakemc.library.cluster.*
import net.cakemc.library.cluster.ClusterMember.MemberState
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.fallback.endpoint.EndpointType
import net.cakemc.library.cluster.network.AbstractNetworkClient
import net.cakemc.library.cluster.network.NetworkClient
import net.cakemc.library.cluster.network.NetworkSession
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.util.*
import java.util.function.Consumer

/**
 * The SyncNetworkHandler class is responsible for managing synchronization
 * operations in a cluster network. It extends SimpleChannelInboundHandler
 * to handle incoming sync publication messages and provides mechanisms for
 * configuring synchronization settings, including target nodes, publication
 * types, and callbacks.
 * This class supports both server and client modes, enabling flexible
 * synchronization strategies based on the context of the network operation.
 *
 *
 *
 * Key functionalities include:
 *
 *  * Configuring nodes to include or exclude from synchronization.
 *  * Specifying publication types for messages.
 *  * Handling synchronization callbacks and responses.
 *  * Processing and managing the state of cluster members during sync operations.
 *
 *
 *
 *
 *
 * Example usage:
 * <pre>
 * SyncNetworkHandler handler = new SyncNetworkHandler(context);
 * handler.withCluster(nodeIds)
 * .withPublicationType(MyPublication.class)
 * .sync(publicationMessage);
</pre> *
 *
 *
 * @see SimpleChannelInboundHandler
 *
 * @see DefaultSyncPublication
 */
class SyncNetworkHandler : SimpleChannelInboundHandler<DefaultSyncPublication> {
  // Public fields
  var endpointType: EndpointType
  var mode: DefaultSyncPublication.SyncMode = DefaultSyncPublication.SyncMode.SYNC_MESSAGE
  var syncContext: Context
  var selfMember: ClusterMember?

  // Private fields
  private var sync: SynchronisationType? = null
  private var callback: PublicationHandler? = null

  private var sessions: MutableList<AbstractNetworkClient>? = null
  private var invalidClients: MutableSet<Int>? = null
  private var currentSocket = 0
  private var nonAsync = false
  private var nonasyncLock: Any? = Any()
  private var numberOfTriedAttempts: Short = 0
  private var startupState = false

  private var isInclusive = true
  private var withBalance = false

  // publication instance
  private var publicationInstance: Class<out Publication>? = null

  // synchronisation
  private var syncFeature: MutableMap<String?, SyncResult>? = null
  private var syncContents: MutableMap<String?, SyncContent?>? = null

  // register
  private var ids: ClusterIdRegistry? = null
  private var expectedNodes: ClusterIdRegistry? = null

  /**
   * Constructs a SyncNetworkHandler for server mode with the given context.
   *
   * @param ctx The context for synchronization.
   */
  constructor(ctx: Context) {
    this.endpointType = EndpointType.SERVER
    this.syncContext = ctx
    this.selfMember = ctx.ownInfo
  }

  /**
   * Constructs a SyncNetworkHandler for client mode with the given context and sync type.
   *
   * @param ctx  The context for synchronization.
   * @param sync The sync type for this handler.
   */
  constructor(ctx: Context, sync: SynchronisationType?) {
    this.endpointType = EndpointType.CLIENT
    this.sync = sync
    this.syncContext = ctx
    this.selfMember = ctx.ownInfo
    this.invalidClients = HashSet()
  }

  /**
   * Sets the synchronization mode to cluster.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  private fun withModeCluster(): SyncNetworkHandler {
    this.mode = DefaultSyncPublication.SyncMode.SYNC_CLUSTER
    return this
  }

  /**
   * Enables balancing for synchronization operations.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  private fun withBalance(): SyncNetworkHandler {
    this.withBalance = true
    return this
  }

  /**
   * Sets the callback handler for synchronization results.
   *
   * @param callback The callback handler to be set.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  fun withCallBack(callback: PublicationHandler?): SyncNetworkHandler {
    this.callback = callback
    return this
  }

  /**
   * Specifies the nodes with which to synchronize messages. If no nodes are specified,
   * synchronization will be attempted with all nodes.
   *
   * @param ids The IDs of the nodes to include in synchronization.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  fun withCluster(vararg ids: Short): SyncNetworkHandler {
    this.isInclusive = true
    this.ids = ClusterIdRegistry(*ids)
    return this
  }

  /**
   * Sets the publication type for the messages to be synchronized.
   *
   * @param publicationInstance The class type of the publication.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  fun withPublicationType(publicationInstance: Class<out Publication>?): SyncNetworkHandler {
    this.publicationInstance = publicationInstance
    return this
  }

  /**
   * Specifies the nodes to exclude from synchronization.
   *
   * @param ids The IDs of the nodes to exclude.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  fun withoutCluster(vararg ids: Short): SyncNetworkHandler {
    this.isInclusive = false
    this.ids = ClusterIdRegistry(*ids)
    return this
  }

  /**
   * Synchronizes a single publication message.
   *
   * @param msg The publication message to synchronize.
   *
   * @return This SyncNetworkHandler for method chaining.
   */
  fun sync(msg: Publication): SyncNetworkHandler {
    return sync(listOf(msg))
  }

  /**
   * Checks if balancing is enabled and sets the callback accordingly.
   *
   * @return True if balancing is enabled, otherwise false.
   */
  private fun checkWithBalanceAndSetCallback(): Boolean {
    if (this.withBalance) {
      callback!!.result(null)
      return true
    }
    return false
  }

  /**
   * Performs inverse ID operations on the specified cluster members and updates
   * the cluster ID registry accordingly.
   *
   * @param cluster           The list of cluster members to evaluate.
   * @param clusterIdRegistry The registry to update with new IDs.
   */
  private fun performInverseIds(cluster: MutableList<ClusterMember>, clusterIdRegistry: ClusterIdRegistry) {
    // Create a set to hold existing IDs
    val existingIds = ClusterIdRegistry()

    // Add existing IDs to the set
    for (id in ids!!.ids) {
      existingIds.add(id)
    }

    // Filter members not in existingIds and add their IDs to the registry
    for (clusterMember in cluster) {
      if (!existingIds.contains(clusterMember.id)) {
        clusterIdRegistry.add(clusterMember.id)
        sessions!!.add(clusterMember.createSynchSession(this))
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
  private fun performInverseIds(
    cluster: List<ClusterMember>, clusterIdRegistry: ClusterIdRegistry,
    consumer: Consumer<ClusterMember>
  ) {
    // Create a set to hold existing IDs
    val existingIds = ClusterIdRegistry()

    // Add existing IDs to the set
    for (id in ids!!.ids) {
      existingIds.add(id)
    }

    // Filter members not in existingIds and apply the consumer
    for (clusterMember in cluster) {
      if (!existingIds.contains(clusterMember.id)) {
        clusterIdRegistry.add(clusterMember.id)
        consumer.accept(clusterMember)
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
  fun sync(publications: List<Publication>?): SyncNetworkHandler {
    this.endpointType = EndpointType.CLIENT

    // Validate input parameters before proceeding
    if (publications == null || publications.isEmpty() || this.callback == null ||
      (mode == DefaultSyncPublication.SyncMode.SYNC_MESSAGE && this.publicationInstance == null)
    ) {
      nonasyncLock = null
      checkWithBalanceAndSetCallback()
      return this
    }

    val clusterSnapshot = syncContext.getOrCreateSnapshot()

    // Handle balance-based synchronization
    if (isBalanceType(this.sync)) {
      if (!this.withBalance) {
        syncWithBalance(publications)
        return this
      }
    }

    println(clusterSnapshot)
    println(clusterSnapshot.aliveCluster)

    val aliveClusterMembers: MutableList<ClusterMember?> = clusterSnapshot.aliveCluster
    sessions = ArrayList()

    // Handle Unicast types of synchronization
    if (isUnicastType(this.sync)) {
      handleUnicastSynchronization(aliveClusterMembers.filterNotNull().toMutableList())
    } else if (isRingType(this.sync)) {
      handleRingSynchronization(aliveClusterMembers.filterNotNull().toMutableList())
    }

    // Prepare message for synchronization
    startupState = syncContext.isInStartup
    val syncMessage = prepareSyncPublication(publications)

    // Publish to the appropriate sessions
    if (sessions != null && !(sessions as ArrayList<AbstractNetworkClient>).isEmpty()) {
      publishMessage(syncMessage)
    } else {
      nonasyncLock = null
      createResult()
    }

    return this
  }

  /**
   * Handles the logic for Unicast synchronization.
   *
   * @param aliveClusterMembers The list of currently alive cluster members.
   */
  private fun handleUnicastSynchronization(aliveClusterMembers: MutableList<ClusterMember>) {
    if (ids == null || ids!!.isEmpty) {
      if (checkWithBalanceAndSetCallback()) {
        return
      }
      populateSessionsForAllClusterMembers(aliveClusterMembers)
    } else {
      if (isInclusive) {
        populateSessionsForSpecifiedIds(aliveClusterMembers, ids!!)
      } else {
        val invertedIds = ClusterIdRegistry()
        performInverseIds(aliveClusterMembers, invertedIds)
        this.ids = invertedIds
      }
    }
  }

  /**
   * Handles the logic for Ring synchronization.
   *
   * @param aliveClusterMembers The list of currently alive cluster members.
   */
  private fun handleRingSynchronization(aliveClusterMembers: MutableList<ClusterMember>) {
    if (ids == null || ids!!.isEmpty) {
      if (checkWithBalanceAndSetCallback()) {
        return
      }
      populateSessionsForAllClusterMembers(aliveClusterMembers)
    } else {
      if (isInclusive) {
        expectedNodes = ClusterIdRegistry()
        populateSessionsForSpecifiedIds(aliveClusterMembers, ids!!)
      } else {
        expectedNodes = ClusterIdRegistry()
        performInverseIds(aliveClusterMembers, expectedNodes!!)
        this.ids = expectedNodes
      }
    }
  }

  /**
   * Populates the sessions list with synchronization sessions for all members in the cluster.
   *
   * @param aliveClusterMembers The list of currently alive cluster members.
   */
  private fun populateSessionsForAllClusterMembers(aliveClusterMembers: List<ClusterMember>) {
    if (!aliveClusterMembers.isEmpty()) {
      ids = ClusterIdRegistry()
      expectedNodes = ClusterIdRegistry()
      for (member in aliveClusterMembers) {
        ids!!.add(member.id)
        sessions!!.add(member.createSynchSession(this))
        expectedNodes!!.add(member.id)
      }
    }
  }

  /**
   * Populates the sessions list with synchronization sessions for all members matching the specified IDs.
   *
   * @param aliveClusterMembers The list of currently alive cluster members.
   * @param specifiedIds        The IDs of the cluster members to populate.
   */
  private fun populateSessionsForSpecifiedIds(
    aliveClusterMembers: List<ClusterMember?>,
    specifiedIds: ClusterIdRegistry
  ) {
    for (id in specifiedIds.ids) {
      val member = syncContext.snapshot!!.getById(id, ClusterSnapshot.MEMBER_CHECK_VALID)
      if (member != null) {
        sessions!!.add(member.createSynchSession(this))
        expectedNodes!!.add(id)
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
  private fun prepareSyncPublication(publications: List<Publication>): DefaultSyncPublication {
    val syncMessage = DefaultSyncPublication()
    syncMessage.syncType = sync
    syncMessage.id = selfMember!!.id
    syncMessage.isInStartup = startupState
    syncMessage.syncMode = mode
    syncMessage.type = DefaultSyncPublication.MessageType.TYPE_CHECK

    syncContents = HashMap()
    syncFeature = HashMap()

    for (publication in publications) {
      val awareIds = determineAwareNodes(publication)
      (syncContents as HashMap<String?, SyncContent?>)[publication.key] = SyncContent(
        publication.key,
        publication.version, awareIds,
        publication.serialize()
      )

      val syncResult = SyncResult()
      syncResult.addSyncedMember(awareIds)
      (syncFeature as HashMap<String?, SyncResult>)[publication.key] = syncResult
    }

    syncMessage.setContents(syncContents!!.values.filterNotNull().toMutableList())
    return syncMessage
  }

  /**
   * Determines which nodes are aware of the specified message.
   *
   * @param publication The publication message to analyze.
   *
   * @return A ClusterIdRegistry containing the IDs of the aware nodes.
   */
  private fun determineAwareNodes(publication: Publication): ClusterIdRegistry? {
    if (this.mode == DefaultSyncPublication.SyncMode.SYNC_MESSAGE) {
      publication.key
      if (!publication.key.isEmpty() && publication.version > 0) {
        return addAndGetAwareNodesOfMessage(publication)
      }
    } else {
      publication.key
      if (!publication.key.isEmpty() && publication.version > 0) {
        val clusterPublication = publication as ClusterPublication
        val memberNode = syncContext.getMemberById(clusterPublication.id)
        return memberNode?.getAwareIdsLock()
      }
    }
    return null
  }

  /**
   * Publishes the message to the sessions.
   *
   * @param syncMessage The message to be published.
   */
  private fun publishMessage(syncMessage: DefaultSyncPublication) {
    if (isRingType(sync) || sync == SynchronisationType.UNI_CAST_ONE_OF) {
      sessions!!.first().publish(syncMessage)
    } else if (isUnicastType(sync)) {
      for (session in sessions!!) {
        session.publish(syncMessage)
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
  private fun addAndGetAwareNodesOfMessage(publication: Publication): ClusterIdRegistry {
    var awareNodeIds = syncContext.getAwareNodes(publication.key, publication.version)

    if (awareNodeIds == null || awareNodeIds.isEmpty) {
      awareNodeIds = ClusterIdRegistry(syncContext.ownInfo!!.id) // Initialize with the current node's ID
      syncContext.addAwareNodes(publication.key, publication.version, awareNodeIds) // Store updated awareNodeIds
    }

    return awareNodeIds
  }

  /**
   * Blocks execution until the synchronization result is generated.
   *
   * @return A map containing SyncFeature results.
   */
  fun get(): Map<String?, SyncResult>? {
    this.nonAsync = true
    if (nonasyncLock == null) {
      return this.syncFeature
    }
    synchronized(nonasyncLock!!) {
      while (nonAsync) {
        try {
          (nonasyncLock as Object).wait()
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      }
    }
    return this.syncFeature
  }

  @Throws(Exception::class)
  override fun channelActive(ctx: ChannelHandlerContext) {
    super.channelActive(ctx)
    if (endpointType == EndpointType.SERVER) {
      ctx.pipeline().addLast(SyncNettyDecoder())
      ctx.pipeline().addLast(SyncNettyEncoder())
    }

    val peer = ctx.channel().remoteAddress() as InetSocketAddress
    ctx.channel().attr(ChannelAttributes.STARTUP_STATE_KEY.getKey<Any>()).set(syncContext.isInStartup)
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
  private fun createSimpleResponse(
    messageType: DefaultSyncPublication.MessageType,
    startupStateFromSession: Boolean?,
    syncMode: DefaultSyncPublication.SyncMode
  ): DefaultSyncPublication {
    val response = DefaultSyncPublication()
    response.id = selfMember!!.id
    if (startupStateFromSession != null) {
      response.isInStartup = startupStateFromSession
    }
    response.syncMode = syncMode
    response.type = messageType
    return response
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
  private fun createCompleteResponse(
    messageType: DefaultSyncPublication.MessageType,
    startupStateFromSession: Boolean?,
    syncMode: DefaultSyncPublication.SyncMode,
    synchronisationType: SynchronisationType?,
    sequence: Byte
  ): DefaultSyncPublication {
    val response = createSimpleResponse(messageType, startupStateFromSession, syncMode)
    response.syncType = synchronisationType
    if (sequence > -1) {
      response.sequence = sequence
    }
    return response
  }

  /**
   * Fills the response contents with the provided synchronization content and associated response.
   *
   * @param response         The cluster publication response to process.
   * @param syncContent      The synchronization content associated with the response.
   * @param responseContents The collection of response contents to be updated.
   */
  private fun fillSyncContents(
    response: ClusterPublication?,
    syncContent: SyncContent,
    responseContents: MutableCollection<SyncContent>
  ) {
    if (response != null) {
      val member = syncContext.getMemberById(response.id)
      val awareIds = if ((member != null)) member.getAwareIdsLock() else null

      responseContents.add(
        SyncContent(
          syncContent.key,
          response.version,
          awareIds,
          response.serialize()
        )
      )
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
  private fun decodeGetCallbackResult(
    callback: PublicationHandler,
    session: Channel,
    decoded: Publication,
    data: ByteArray?,
    awareIds: ClusterIdRegistry,
    output: SyncProtocolBundle
  ): Boolean {
    if (data != null) {
      decoded.deserialize(Unpooled.copiedBuffer(data))
    }
    return callback.callBack(NetworkSession(session), decoded, awareIds, output)
  }

  private val aliveMemberIds: ClusterIdRegistry?
    /**
     * Retrieves the IDs of all alive cluster members.
     *
     * @return A ClusterIdRegistry containing the IDs of alive members, or null if none are alive.
     */
    get() {
      val snapshot = syncContext.snapshot
      val aliveMembers: List<ClusterMember?> = snapshot!!.aliveCluster
      if (!aliveMembers.isEmpty()) {
        val aliveMemberIds = ClusterIdRegistry()
        for (member in aliveMembers) {
          aliveMemberIds.add(member!!.id)
        }
        return aliveMemberIds
      }
      return null
    }

  /**
   * Determines the appropriate synchronization type for the given publication.
   *
   * @param publication The synchronization publication to evaluate.
   *
   * @return The appropriate SynchronisationType based on the publication's current type.
   */
  private fun getProperRingType(publication: DefaultSyncPublication): SynchronisationType {
    if (publication.syncType == SynchronisationType.RING_QUORUM ||
      publication.syncType == SynchronisationType.RING_BALANCE_QUORUM
    ) {
      return SynchronisationType.RING_BALANCE_QUORUM // Return quorum type if applicable
    }
    return SynchronisationType.RING_BALANCE // Default return type
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
  private fun fillCallbackResult(
    result: Boolean,
    message: Publication,
    responseContents: MutableCollection<SyncContent>,
    responses: List<Publication>?,
    ringMsgToScMap: Map<String, String?>,
    awareIds: ClusterIdRegistry?,
    syncResult: SyncResult
  ) {
    if (result) {
      if (responses == null) {
        // Means it synced successfully
        responseContents.add(SyncContent(ringMsgToScMap[message.key], message.version, awareIds, null))
      }
    } else {
      if (responses == null) {
        val failedMembers = syncResult.failedMembers
        failedMembers.add(selfMember!!.id)
        responseContents.add(SyncContent(ringMsgToScMap[message.key], 0, failedMembers, null))
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
  @Throws(
    IllegalAccessException::class,
    InstantiationException::class,
    NoSuchMethodException::class,
    InvocationTargetException::class
  )
  private fun handleMessageSyncListener(
    session: Channel,
    message: DefaultSyncPublication,
    member: ClusterMember?,
    startupStateFromSession: Boolean?,
    isFirstMessage: Boolean
  ) {
    // Check if the cluster member is valid
    if (member == null || !member.isValid) {
      val response = createSimpleResponse(
        DefaultSyncPublication.MessageType.TYPE_NOT_VALID_EDGE,
        startupStateFromSession,
        mode
      )
      session.writeAndFlush(response)
      session.close()
      return
    }

    // Retrieve the message contents
    val contents: Collection<SyncContent> = message.getContents()

    val responseContents: MutableSet<SyncContent> = HashSet()
    val ringMessages: MutableList<Publication> = ArrayList()
    val isRingSync = isRingType(message.syncType)
    val ringMessageToSyncContentMap: MutableMap<String, String?>? = if (isRingSync) HashMap() else null

    for (syncContent in contents) {
      val contentData = syncContent.content ?: continue

      // Decode message and retrieve callback results
      val decodedMessage =
        publicationInstance!!.getDeclaredConstructor().newInstance()
      val output = SyncProtocolBundle()
      decodeGetCallbackResult(callback!!, session, decodedMessage, contentData, syncContent.awareIds!!, output)
      val responses = output.getMessages()

      val awareNodes = if ((syncContent.awareIds != null)) syncContent.awareIds else ClusterIdRegistry()
      awareNodes.add(selfMember!!.id)
      syncContext.addAwareNodes(decodedMessage.key, decodedMessage.version, awareNodes)

      if (isRingSync) {
        ringMessages.add(decodedMessage)
        ringMessageToSyncContentMap!![decodedMessage.key] = syncContent.key
      } else {
        addResponseContents(responseContents, responses, syncContent.key, awareNodes)
      }
    }

    if (!ringMessages.isEmpty()) {
      handleRingSync(session, message, ringMessages, responseContents, ringMessageToSyncContentMap!!)
    }

    sendResponse(session, message, responseContents, isRingSync, isFirstMessage)
  }

  /**
   * Adds the responses to the specified response contents collection.
   *
   * @param responseContents The collection of response contents to be updated.
   * @param responses        The list of responses to process.
   * @param key              The key associated with the synchronization content.
   * @param awareNodes       The cluster ID registry of aware nodes associated with the responses.
   */
  private fun addResponseContents(
    responseContents: MutableSet<SyncContent>,
    responses: List<Publication>?,
    key: String?,
    awareNodes: ClusterIdRegistry?
  ) {
    if (responses != null) {
      for (response in responses) {
        if (response != null) {
          responseContents.add(
            SyncContent(
              key, response.version, awareNodes, response.serialize()
            )
          )
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
  private fun handleRingSync(
    session: Channel,
    message: DefaultSyncPublication,
    ringMessages: List<Publication>,
    responseContents: MutableSet<SyncContent>,
    ringMessageToSyncContentMap: Map<String, String?>
  ) {
    val ringType = getProperRingType(message)
    val syncResults = SyncNetworkHandler(syncContext, ringType)
      .withCallBack(callback)
      .withPublicationType(publicationInstance)
      .withoutCluster(message.id, syncContext.ownInfo!!.id)
      .sync(ringMessages)
      .get()

    if (syncResults == null) {
      session.writeAndFlush(
        createSimpleResponse(
          DefaultSyncPublication.MessageType.TYPE_FAILED_RING,
          null,
          DefaultSyncPublication.SyncMode.SYNC_MESSAGE
        )
      )
      session.close()
      return
    }

    for (ringMessage in ringMessages) {
      val result = syncResults[ringMessage.key]
      val awareIds = syncContext.getAwareNodes(ringMessage.key, ringMessage.version)

      if (result!!.isSuccessful) {
        processSuccessfulSync(
          result, session, ringMessage,
          responseContents, ringMessageToSyncContentMap, awareIds
        )
      } else {
        responseContents.add(
          SyncContent(
            ringMessageToSyncContentMap[ringMessage.key],
            0,
            result.failedMembers,
            null
          )
        )
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
  private fun processSuccessfulSync(
    result: SyncResult?,
    session: Channel,
    message: Publication,
    responseContents: MutableSet<SyncContent>,
    ringMsgToSyncContentMap: Map<String, String?>,
    awareIds: ClusterIdRegistry
  ) {
    val syncProtocolBundle = SyncProtocolBundle()
    val success = decodeGetCallbackResult(callback!!, session, message, null, awareIds, syncProtocolBundle)
    val responses = syncProtocolBundle.getMessages()

    if (success && responses != null) {
      for (response in responses) {
        if (response != null) {
          responseContents.add(
            SyncContent(
              ringMsgToSyncContentMap[message.key],
              response.version,
              awareIds,
              response.serialize()
            )
          )
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
  private fun sendResponse(
    session: Channel,
    msg: DefaultSyncPublication,
    responseContents: Set<SyncContent>,
    isRing: Boolean,
    isFirstMessage: Boolean
  ) {
    if (responseContents.isEmpty()) {
      session.writeAndFlush(
        createCompleteResponse(
          DefaultSyncPublication.MessageType.TYPE_OK,
          null,
          DefaultSyncPublication.SyncMode.SYNC_MESSAGE,
          msg.syncType,
          0.toByte()
        )
      )
    } else {
      val response = createCompleteResponse(
        DefaultSyncPublication.MessageType.TYPE_CHECK,
        null,
        DefaultSyncPublication.SyncMode.SYNC_MESSAGE,
        msg.syncType,
        (msg.sequence + 1).toByte()
      )
      response.setContents(responseContents)
      if (isRing && isFirstMessage) {
        response.setExpectedIds(aliveMemberIds)
      }
      session.writeAndFlush(response)
    }
  }

  /**
   * Handles incoming cluster synchronization messages and processes them accordingly.
   *
   * @param session        The channel session associated with the incoming message.
   * @param message        The incoming synchronization publication message.
   * @param isFirstMessage Indicates if this is the first message in the synchronization.
   */
  private fun handleClusterSyncListener(session: Channel, message: DefaultSyncPublication, isFirstMessage: Boolean) {
    syncContext.isInStartup = false

    val contents: Collection<SyncContent> = message.getContents()
    if (contents == null) {
      session.close()
      return
    }

    val isRingSync = isRingType(message.syncType)
    val ringMessageToSyncContentMap: MutableMap<String, String?>? = if (isRingSync) HashMap() else null

    val clusterCallback = ClusterPublicationHandler(syncContext)
    val responseContents: MutableCollection<SyncContent> = ArrayList()
    val ringMessages: MutableList<Publication> = ArrayList()

    for (syncContent in contents) {
      val messageData = syncContent.content

      if (messageData == null) {
        handleNullMessage(syncContent, message)
        continue
      }

      val decodedMessage: Publication = ClusterPublication()
      val output = SyncProtocolBundle()
      val decodeResult = decodeGetCallbackResult(
        clusterCallback, session, decodedMessage, messageData,
        syncContent.awareIds!!, output
      )
      val responses = output.getMessages()

      if (decodeResult && isRingSync) {
        ringMessages.add(decodedMessage)
        ringMessageToSyncContentMap!![decodedMessage.key] = syncContent.key
      }

      if (responses != null) {
        for (publication in responses) {
          val response = publication as ClusterPublication
          fillSyncContents(response, syncContent, responseContents)
        }
      }
    }

    if (!ringMessages.isEmpty()) {
      handleRingMessages(
        session, message, isFirstMessage, ringMessages,
        ringMessageToSyncContentMap!!, responseContents
      )
    } else {
      sendFinalResponse(session, responseContents, message, isRingSync, isFirstMessage)
    }
  }

  /**
   * Handles null messages by updating the aware IDs of the corresponding cluster member.
   *
   * @param syncContent The synchronization content that contains the null message.
   * @param message     The original synchronization publication message.
   */
  private fun handleNullMessage(syncContent: SyncContent, message: DefaultSyncPublication) {
    if (syncContent.version > 0) {
      val clusterMember = syncContext.getMemberById(syncContent.key!!.toShort())
      if (clusterMember != null) {
        clusterMember.addAwareId(selfMember!!.id)
        clusterMember.addAwareId(message.id)
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
  private fun handleRingMessages(
    session: Channel,
    message: DefaultSyncPublication,
    isFirstMessage: Boolean,
    ringMessages: List<Publication>,
    ringMessageToSyncContentMap: Map<String, String?>,
    responseContents: MutableCollection<SyncContent>
  ) {
    val callbackHandler = this.callback
    val synchronisationType = getProperRingType(message)
    val aliveMemberIds = aliveMemberIds

    val syncResults = SyncNetworkHandler(syncContext, synchronisationType)
      .withCallBack(callbackHandler)
      .withPublicationType(publicationInstance)
      .withoutCluster(message.id, syncContext.ownInfo!!.id)
      .withModeCluster()
      .sync(ringMessages)
      .get()

    if (syncResults == null) {
      sendErrorResponse(session, message)
      return
    }

    for (ringMessage in ringMessages) {
      val result = syncResults[ringMessage.key]
      if (result != null && result.isSuccessful) {
        processSuccessfulSync(
          session,
          callbackHandler!!, ringMessage, ringMessageToSyncContentMap, responseContents, result
        )
      } else {
        responseContents.add(
          SyncContent(
            ringMessageToSyncContentMap[ringMessage.key],
            0,
            result?.failedMembers,
            null
          )
        )
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
  private fun processSuccessfulSync(
    session: Channel,
    callbackHandler: PublicationHandler,
    message: Publication,
    ringMessageToSyncContentMap: Map<String, String?>,
    responseContents: MutableCollection<SyncContent>,
    syncResult: SyncResult
  ) {
    val syncProtocolBundle = SyncProtocolBundle()
    val clusterMember = syncContext.getMemberById((message as ClusterPublication).id)
    val awareIds = if ((clusterMember != null)) clusterMember.getAwareIdsLock() else null

    val isSuccess = decodeGetCallbackResult(
      callbackHandler, session, message, null,
      awareIds!!, syncProtocolBundle
    )
    val responses = syncProtocolBundle.getMessages()

    fillCallbackResult(
      isSuccess,
      message,
      responseContents,
      responses,
      ringMessageToSyncContentMap,
      awareIds,
      syncResult
    )

    for (response in responses!!) {
      val responseMember = syncContext.getMemberById((response as ClusterPublication).id)
      val responseAwareIds = if ((responseMember != null)) responseMember.getAwareIdsLock() else null

      responseContents.add(
        SyncContent(
          ringMessageToSyncContentMap[message.key],
          response.version,
          responseAwareIds,
          response.serialize()
        )
      )
    }
  }

  /**
   * Sends an error response to the specified session and closes the session.
   *
   * @param session The channel session to which the error response will be sent.
   * @param msg     The original synchronization publication message that triggered the error response.
   */
  private fun sendErrorResponse(session: Channel, msg: DefaultSyncPublication) {
    val errorMsg = createCompleteResponse(
      DefaultSyncPublication.MessageType.TYPE_FAILED_RING,
      null,
      DefaultSyncPublication.SyncMode.SYNC_CLUSTER,
      msg.syncType,
      (-1.toByte()).toByte()
    )
    session.writeAndFlush(errorMsg)
    session.close()
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
  private fun sendFinalResponse(
    session: Channel,
    responseContents: Collection<SyncContent>,
    msg: DefaultSyncPublication,
    isRing: Boolean,
    isFirstMessage: Boolean
  ) {
    val response: DefaultSyncPublication
    if (responseContents.isEmpty()) {
      response = createCompleteResponse(
        DefaultSyncPublication.MessageType.TYPE_OK,
        null,
        DefaultSyncPublication.SyncMode.SYNC_CLUSTER,
        msg.syncType,
        0.toByte()
      )
    } else {
      response = createCompleteResponse(
        DefaultSyncPublication.MessageType.TYPE_CHECK,
        null,
        DefaultSyncPublication.SyncMode.SYNC_CLUSTER,
        msg.syncType,
        (msg.sequence + 1).toByte()
      )
      response.setContents(responseContents)

      if (isRing && isFirstMessage) {
        val nodesForRingUpdate = aliveMemberIds
        response.setExpectedIds(nodesForRingUpdate)
      }
    }

    session.writeAndFlush(response)
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
  @Throws(
    IllegalAccessException::class,
    InstantiationException::class,
    InvocationTargetException::class,
    NoSuchMethodException::class
  )
  private fun handleListener(context: Channel, message: DefaultSyncPublication) {
    val startupStateFromSession = context.attr(AttributeKey.valueOf<Any>(START_UP_STATE_KEY)).get() as Boolean

    // Handle different message types
    if (message.type == DefaultSyncPublication.MessageType.TYPE_OK) {
      context.close()
      return
    }

    if (message.isInStartup && startupStateFromSession != null) {
      context.writeAndFlush(
        createSimpleResponse(
          DefaultSyncPublication.MessageType.TYPE_BOTH_STARTUP,
          startupStateFromSession,
          mode
        )
      )
      context.close()
      return
    }

    if (isBadMessage(context, message)) {
      context.close()
      return
    }

    val isFirstMessage = initializeSession(context, message, startupStateFromSession)

    var clusterMember: ClusterMember? =
      context.attr(AttributeKey.valueOf<Any>(SELF_ATTRIBUTE_KEY)).get() as ClusterMember
    if (clusterMember == null) {
      clusterMember = syncContext.getMemberById(message.id)
      context.attr(AttributeKey.valueOf<Any>(SELF_ATTRIBUTE_KEY)).set(clusterMember)
    }

    if (clusterMember != null && clusterMember.isDown) {
      syncContext.synchronizedStateChange(clusterMember, MemberState.DELETED)
    }

    // Handle specific sync modes
    when (message.syncMode) {
      DefaultSyncPublication.SyncMode.SYNC_MESSAGE -> handleMessageSyncListener(
        context,
        message,
        clusterMember,
        startupStateFromSession,
        isFirstMessage
      )

      DefaultSyncPublication.SyncMode.SYNC_CLUSTER -> handleClusterSyncListener(context, message, isFirstMessage)
      else -> {}
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
  private fun isBadMessage(ctx: Channel, publication: DefaultSyncPublication): Boolean {
    if (publication.type == DefaultSyncPublication.MessageType.TYPE_BAD_ID ||
      publication.type == DefaultSyncPublication.MessageType.TYPE_BAD_SEQ ||
      publication.type == DefaultSyncPublication.MessageType.TYPE_FAILED_RING ||
      publication.sequence > DefaultSyncPublication.SEQ_MAX
    ) {
      if (publication.sequence > DefaultSyncPublication.SEQ_MAX) {
        val startupStateFromSession = ctx.attr(AttributeKey.valueOf<Any>(START_UP_STATE_KEY)).get() as Boolean
        ctx.writeAndFlush(
          createSimpleResponse(
            DefaultSyncPublication.MessageType.TYPE_BAD_SEQ,
            startupStateFromSession,
            mode
          )
        )
      }
      return true
    }
    return false
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
  private fun initializeSession(
    ctx: Channel,
    publication: DefaultSyncPublication,
    startupStateFromSession: Boolean?
  ): Boolean {
    if (!ctx.hasAttr(AttributeKey.valueOf<Any>(SESSION_INITIATED_KEY))) {
      ctx.attr(AttributeKey.valueOf<Any>(SESSION_INITIATED_KEY)).set(true)
      if (selfMember!!.key != null && Objects.requireNonNull(
          selfMember!!.getKeyChainLock()
        )!!.stream().anyMatch { o: String? -> Objects.requireNonNull(publication.getKeyChain())!!.contains(o) }
      ) {
        val response = createSimpleResponse(
          DefaultSyncPublication.MessageType.TYPE_BAD_KEY, startupStateFromSession, mode
        )
        ctx.writeAndFlush(response)
        ctx.close()
        return false
      }
      return true // Indicates that this is the first message
    }
    return false // Indicates that this is not the first message
  }

  /**
   * Handles the sender logic for processing incoming synchronization messages.
   *
   * @param session The channel session associated with the sender.
   * @param message The incoming synchronization publication message.
   *
   * @throws Exception If an error occurs during processing.
   */
  @Throws(Exception::class)
  private fun handleSender(session: Channel, message: DefaultSyncPublication) {
    val synchronizationClient = session.attr(ChannelAttributes.SYNC_SESSION.getKey<Any>()).get() as NetworkClient

    val isRingSynchronization = isRingType(this.sync)

    if (isRingSynchronization) {
      handleRingSync(message)
    }

    if (isControlMessage(message)) {
      handleControlMessage(session, synchronizationClient, message)
      return
    }

    if (isInvalidSequence(message)) {
      handleInvalidSequence(session, synchronizationClient)
      return
    }

    // Both branches were calling the same method, so this was simplified
    processSynchronizationMessage(session, message, synchronizationClient, isRingSynchronization)
  }

  /**
   * Handles synchronization for ring-based communication by updating expected node IDs.
   *
   * @param publication The incoming synchronization publication message.
   */
  private fun handleRingSync(publication: DefaultSyncPublication) {
    if (publication.expectedIds != null) {
      expectedNodes!!.removeAll(*publication.expectedIds!!.ids)
    } else {
      expectedNodes!!.remove(publication.id)
    }
  }

  /**
   * Checks if the provided message is a control message.
   *
   * @param publication The synchronization publication message to check.
   *
   * @return true if the message is a control message; otherwise, false.
   */
  private fun isControlMessage(publication: DefaultSyncPublication): Boolean {
    val type = publication.type
    return type == DefaultSyncPublication.MessageType.TYPE_OK || type == DefaultSyncPublication.MessageType.TYPE_BOTH_STARTUP || type == DefaultSyncPublication.MessageType.TYPE_NOT_VALID_EDGE || type == DefaultSyncPublication.MessageType.TYPE_BAD_ID || type == DefaultSyncPublication.MessageType.TYPE_BAD_SEQ || type == DefaultSyncPublication.MessageType.TYPE_BAD_KEY
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
  @Throws(Exception::class)
  private fun handleControlMessage(
    session: Channel,
    synchronizationClient: NetworkClient,
    controlMessage: DefaultSyncPublication
  ) {
    if (controlMessage.type == DefaultSyncPublication.MessageType.TYPE_OK) {
      numberOfTriedAttempts++
      createResult()
      session.attr(ChannelAttributes.PLANNED_CLOSE.getKey<Any>())
      session.close()
      return
    }

    workCallback(synchronizationClient, HandlerState.UNPROPER)

    if (shouldCloseOnControlMessage(controlMessage)) {
      session.attr(ChannelAttributes.PLANNED_CLOSE.getKey<Any>())
      session.close()
    }
  }

  /**
   * Determines whether the session should be closed based on the type of control message received.
   *
   * @param publication The control message being evaluated.
   *
   * @return true if the session should be closed; otherwise, false.
   */
  private fun shouldCloseOnControlMessage(publication: DefaultSyncPublication): Boolean {
    return publication.type == DefaultSyncPublication.MessageType.TYPE_BAD_KEY ||
            publication.type == DefaultSyncPublication.MessageType.TYPE_FAILED_RING
  }

  /**
   * Checks if the provided sequence number of the publication message is invalid.
   *
   * @param publication The synchronization publication message to check.
   *
   * @return true if the sequence number is invalid; otherwise, false.
   */
  private fun isInvalidSequence(publication: DefaultSyncPublication): Boolean {
    return publication.sequence < 0 || publication.sequence > DefaultSyncPublication.SEQ_MAX
  }


  /**
   * Handles an invalid sequence by sending an error response and closing the session.
   *
   * @param session    The channel session to which the response is sent.
   * @param syncClient The network client associated with the synchronization.
   *
   * @throws Exception If an error occurs during response creation or session closure.
   */
  @Throws(Exception::class)
  private fun handleInvalidSequence(session: Channel, syncClient: NetworkClient) {
    val response = createSimpleResponse(DefaultSyncPublication.MessageType.TYPE_BAD_SEQ, startupState, mode)
    session.writeAndFlush(response)
    session.attr(ChannelAttributes.PLANNED_CLOSE.getKey<Any>())
    session.close()
    workCallback(syncClient, HandlerState.UNPROPER)
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
  @Throws(Exception::class)
  private fun processSynchronizationMessage(
    session: Channel,
    publicationMessage: DefaultSyncPublication,
    synchronizationClient: NetworkClient,
    isRingSynchronization: Boolean
  ) {
    val syncContents: Collection<SyncContent> = publicationMessage.getContents()
    if (syncContents == null) {
      closeWithCallback(session, synchronizationClient)
      return
    }

    val responseContents: MutableSet<SyncContent> = HashSet()
    val failedContents: MutableMap<String?, SyncContent?> = HashMap()
    val numberOfNullResponses = 0

    for (syncContent in syncContents) {
      val messageData = syncContent.content
      if (messageData == null) {
        handleNullContent(
          publicationMessage,
          syncContent,
          responseContents,
          failedContents,
          isRingSynchronization
        )
        continue
      }

      val decodedMessage =
        publicationInstance!!.getDeclaredConstructor().newInstance()
      val outputBundle = SyncProtocolBundle()
      val decodeSuccess = decodeGetCallbackResult(
        callback!!, session, decodedMessage, messageData,
        syncContent.awareIds!!, outputBundle
      )
      handleDecodedMessage(
        publicationMessage, syncContent, decodeSuccess,
        decodedMessage, outputBundle, responseContents,
        failedContents, isRingSynchronization
      )
    }

    if (checkAndPrepareFailedResult(
        session, isRingSynchronization, responseContents,
        failedContents, numberOfNullResponses
      )
    ) {
      return
    }

    sendSyncResponse(session, publicationMessage, responseContents, isRingSynchronization)
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
  @Throws(Exception::class)
  private fun processClusterSynchronization(
    session: Channel,
    publicationMessage: DefaultSyncPublication,
    synchronizationClient: NetworkClient,
    isRingSynchronization: Boolean
  ) {
    val syncContents: Collection<SyncContent> = publicationMessage.getContents()
    if (syncContents == null) {
      closeWithCallback(session, synchronizationClient)
      return
    }

    val responseContents: MutableSet<SyncContent> = HashSet()
    val failedContents: MutableMap<String?, SyncContent?> = HashMap()
    val numberOfNullResponses = 0

    for (syncContent in syncContents) {
      val messageData = syncContent.content
      if (messageData == null) {
        handleNullContent(
          publicationMessage, syncContent, responseContents,
          failedContents, isRingSynchronization
        )
        continue
      }

      val decodedMessage = ClusterPublication()
      val outputBundle = SyncProtocolBundle()
      val decodeSuccess = decodeGetCallbackResult(
        ClusterPublicationHandler(syncContext), session, decodedMessage, messageData,
        syncContent.awareIds!!, outputBundle
      )
      handleDecodedMessage(
        publicationMessage, syncContent, decodeSuccess,
        decodedMessage, outputBundle, responseContents,
        failedContents, isRingSynchronization
      )
    }

    if (checkAndPrepareFailedResult(
        session, isRingSynchronization,
        responseContents, failedContents, numberOfNullResponses
      )
    ) {
      return
    }

    sendSyncResponse(session, publicationMessage, responseContents, isRingSynchronization)
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
  private fun handleNullContent(
    publicationMessage: DefaultSyncPublication, syncContent: SyncContent, responseContents: Set<SyncContent>,
    failedContents: MutableMap<String?, SyncContent?>, isRingSynchronization: Boolean
  ) {
    var awareMembers = syncContent.awareIds
    if (awareMembers == null) {
      awareMembers = ClusterIdRegistry()
    }

    awareMembers.add(publicationMessage.id)
    val syncResult = syncFeature!![syncContent.key]

    if (syncContent.version > 0) {
      fillSyncResultForVersionGreaterThanZero(
        publicationMessage, isRingSynchronization, awareMembers, syncResult!!
      )
      awareMembers.add(selfMember!!.id)
      if (syncContent.key != null && !syncContent.key.isEmpty()) {
        syncContext.addAwareNodes(syncContent.key, syncContent.version, awareMembers)
      }
    } else {
      handleNullDataOfStarterHandler(
        publicationMessage, isRingSynchronization, failedContents,
        syncContent, awareMembers, syncResult!!
      )
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
  private fun handleDecodedMessage(
    publicationMessage: DefaultSyncPublication, syncContent: SyncContent, decodeSuccess: Boolean,
    decodedMessage: Publication, outputBundle: SyncProtocolBundle, responseContents: MutableSet<SyncContent>,
    failedContents: MutableMap<String?, SyncContent?>, isRingSynchronization: Boolean
  ) {
    val responses = outputBundle.getMessages()

    if (!decodeSuccess) {
      if (isRingSynchronization) {
        failedContents[syncContent.key] = syncContents!![syncContent.key]
      }
      if (responses != null) {
        for (response in responses) {
          if (response != null) {
            val awareNodes = syncContext.getAwareNodes(response.key, response.version)
            responseContents.add(
              SyncContent(
                response.key,
                response.version,
                awareNodes,
                response.serialize()
              )
            )
          }
        }
      } else {
        handleResponseContentsForNullResponses(
          publicationMessage,
          isRingSynchronization,
          responseContents,
          syncContent
        )
      }
    } else {
      var awareNodes = syncContent.awareIds
      if (awareNodes == null) {
        awareNodes = ClusterIdRegistry()
      }
      awareNodes.addAll(selfMember!!.id, publicationMessage.id)

      if (syncContent.version > 0 && syncContent.key != null && !syncContent.key.isEmpty()) {
        syncContext.addAwareNodes(syncContent.key, syncContent.version, awareNodes)
      }
      syncResultHandleStarter(publicationMessage, isRingSynchronization, syncContent, awareNodes)

      if (responses != null) {
        for (response in responses) {
          if (response != null) {
            awareNodes = syncContext.getAwareNodes(response.key, response.version)
            responseContents.add(
              SyncContent(
                syncContent.key,
                response.version,
                awareNodes,
                response.serialize()
              )
            )
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
  @Throws(Exception::class)
  private fun closeWithCallback(session: Channel, syncClient: NetworkClient) {
    session.attr(ChannelAttributes.PLANNED_CLOSE.getKey<Any>())
    session.close()
    workCallback(syncClient, HandlerState.UNPROPER)
  }

  /**
   * Sends a synchronization response to the specified channel with the given message and contents.
   *
   * @param session          the channel to send the response to
   * @param msg              the original synchronization message
   * @param responseContents the contents to include in the response
   * @param isRingSync       indicates if the synchronization is in ring mode
   */
  private fun sendSyncResponse(
    session: Channel,
    msg: DefaultSyncPublication,
    responseContents: Set<SyncContent>,
    isRingSync: Boolean
  ) {
    val response = createCompleteResponse(
      DefaultSyncPublication.MessageType.TYPE_CHECK, null,
      DefaultSyncPublication.SyncMode.SYNC_MESSAGE, this.sync, (msg.sequence + 1).toByte()
    )
    response.setContents(responseContents)
    session.writeAndFlush(response)
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
  private fun checkAndPrepareFailedResult(
    session: Channel, isRing: Boolean, responseContents: Set<SyncContent>,
    faildContents: MutableMap<String?, SyncContent?>, numberOfNull: Int
  ): Boolean {
    if (isRing) {
      if (expectedNodes!!.isEmpty) {
        this.syncContents = faildContents
      }
    }

    val responsesSize = responseContents.size

    if (responsesSize == 0 || numberOfNull == responsesSize) {
      numberOfTriedAttempts++
      createResult()
      session.attr(ChannelAttributes.PLANNED_CLOSE.getKey<Any>())
      session.close()
      return true
    }
    return false
  }

  /**
   * Initiates the handling of synchronization results based on the provided publication message.
   *
   * @param publicationMessage the synchronization publication message
   * @param isRing             indicates if the synchronization is in ring mode
   * @param syncContent        the synchronization content associated with the result
   * @param awareNodes         the nodes that are aware of the synchronization
   */
  private fun syncResultHandleStarter(
    publicationMessage: DefaultSyncPublication, isRing: Boolean,
    syncContent: SyncContent, awareNodes: ClusterIdRegistry
  ) {
    // Get the SyncResult associated with the key in SyncContent
    val syncResult = syncFeature!![syncContent.key]

    // Ensure SyncResult is not null before proceeding
    if (syncResult != null) {
      val expectedIds = publicationMessage.expectedIds
      // If in ring mode, update expectedIds with awareNodes
      if (isRing && expectedIds != null) {
        expectedIds.addAll(*awareNodes.ids)
        for (id in expectedIds.ids) {
          if (expectedIds.contains(id)) { // Check if id is in expectedIds
            syncResult.removeFailedMember(id)
            syncResult.addSyncedMember(id)
          }
        }
      } else {
        // If not in ring mode, sync directly with awareNodes
        for (id in awareNodes.ids) {
          if (awareNodes.contains(id)) { // Check if id is in awareNodes
            syncResult.removeFailedMember(id)
            syncResult.addSyncedMember(id)
          }
        }
      }
    }

    // Ensure expectedNodes is not null and has elements before proceeding
    if (this.expectedNodes != null && !expectedNodes!!.isEmpty) {
      val syncContentFromMap = syncContents!![syncContent.key]
      syncContentFromMap?.addAwareId(syncContent.awareIds!!)
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
  private fun handleResponseContentsForNullResponses(
    defaultSyncPublication: DefaultSyncPublication,
    isRingMode: Boolean,
    responseContents: MutableSet<SyncContent>,
    syncContent: SyncContent
  ) {
    // Initialize failed IDs with the current member's ID
    val failedMemberIds = ClusterIdRegistry(selfMember!!.id)
    val expectedMemberIds = defaultSyncPublication.expectedIds // Get expected IDs
    val syncResult = syncFeature!![syncContent.key] // Get the sync result for the given key

    // Check if we're in ring mode
    if (isRingMode && expectedMemberIds != null) {
      // Loop through expectedMemberIds and manage the failed member list
      for (memberId in expectedMemberIds.ids) {
        if (expectedMemberIds.contains(memberId)) { // Check if memberId is in expectedMemberIds
          syncResult!!.addFailedMember(memberId)
          syncResult.removeSyncedMember(memberId)
        }
      }
    } else {
      // Not in ring mode, handle the current message ID
      syncResult!!.addFailedMember(defaultSyncPublication.id)
      syncResult.removeSyncedMember(defaultSyncPublication.id)
    }

    // Add a new SyncContent entry for the response contents
    responseContents.add(SyncContent(syncContent.key, 0, failedMemberIds, null))
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
  private fun handleNullDataOfStarterHandler(
    publicationMessage: DefaultSyncPublication, isRing: Boolean,
    failedContents: MutableMap<String?, SyncContent?>, syncContent: SyncContent,
    awareMembers: ClusterIdRegistry, syncResult: SyncResult
  ) {
    if (isRing) {
      for (id in ids!!.ids) {
        if (awareMembers.contains(id)) { // Check if id is in awareMembers
          syncResult.addFailedMember(id)
          syncResult.removeSyncedMember(id)
        }
      }
    } else {
      syncResult.addFailedMember(publicationMessage.id)
      syncResult.removeSyncedMember(publicationMessage.id)
    }

    failedContents[syncContent.key] = syncContents!![syncContent.key]
  }

  /**
   * Fills the synchronization result for publications with a version greater than zero.
   *
   * @param publicationMessage the synchronization publication message
   * @param isRing             indicates if the synchronization is in ring mode
   * @param awareNodes         the nodes that are aware of the synchronization
   * @param syncResult         the synchronization result object
   */
  private fun fillSyncResultForVersionGreaterThanZero(
    publicationMessage: DefaultSyncPublication, isRing: Boolean,
    awareNodes: ClusterIdRegistry, syncResult: SyncResult
  ) {
    if (isRing) {
      for (id in ids!!.ids) {
        if (awareNodes.contains(id)) { // Check if id is in awareNodes
          syncResult.addSyncedMember(id)
          syncResult.removeFailedMember(id)
        }
      }
    } else {
      syncResult.addSyncedMember(publicationMessage.id)
      syncResult.removeFailedMember(publicationMessage.id)
    }
  }

  @Throws(Exception::class)
  override fun channelRead0(ctx: ChannelHandlerContext, message: DefaultSyncPublication) {
    val session = ctx.channel()
    if (selfMember == null) {
      return
    }

    if (endpointType == EndpointType.SERVER) {
      handleListener(session, message)
    } else {
      handleSender(session, message)
    }
  }

  @Throws(Exception::class)
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    if (cause is IOException) {
      return
    }

    if (endpointType == EndpointType.CLIENT) {
      val peer = (ctx.channel().remoteAddress() as InetSocketAddress)
      val sync = ctx.channel().attr(ChannelAttributes.SYNC_SESSION.getKey<Any>())
        .get() as NetworkClient // Use appropriate key

      workCallback(sync, HandlerState.WORK_FAILED)
    }
  }

  @Throws(Exception::class)
  override fun channelInactive(ctx: ChannelHandlerContext) {
    super.channelInactive(ctx)
    if (endpointType == EndpointType.CLIENT && !ctx.channel()
        .hasAttr(ChannelAttributes.PLANNED_CLOSE_KEY.getKey<Any>())
    ) { // Use appropriate key
      val sync = ctx.channel().attr(ChannelAttributes.ATTRIBUTE_KEY.getKey<Any>())
        .get() as NetworkClient // Use appropriate key

      workCallback(sync, HandlerState.UNPROPER) // Assuming link is a class clusterMember or passed in some way
    }
  }

  /**
   * Callback method that handles work completion for a given network client.
   *
   * @param session      the network client session
   * @param handlerState the state of the handler after the work is completed
   */
  fun workCallback(session: NetworkClient, handlerState: HandlerState) {
    if (sessions == null) {
      return
    }

    try {
      when (sync) {
        SynchronisationType.RING, SynchronisationType.RING_QUORUM, SynchronisationType.RING_BALANCE, SynchronisationType.RING_BALANCE_QUORUM, SynchronisationType.UNI_CAST_ONE_OF -> handleRingOrUnicastCallback(
          session,
          handlerState
        )

        SynchronisationType.UNI_CAST, SynchronisationType.UNI_CAST_QUORUM, SynchronisationType.UNI_CAST_BALANCE, SynchronisationType.UNI_CAST_BALANCE_QUORUM -> handleUniCastCallback(
          session,
          handlerState
        )

        else -> {}
      }

      createResult()
    } catch (throwable: Throwable) {
      throw RuntimeException(throwable)
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
  @Throws(Exception::class)
  private fun handleRingOrUnicastCallback(session: NetworkClient, handlerState: HandlerState) {
    if (handlerState == HandlerState.WORK_FAILED || handlerState == HandlerState.UNPROPER) {
      val clusterMember = session.member
      if (clusterMember != null && clusterMember.state == MemberState.DELETED) {
        syncContext.synchronizedStateChange(clusterMember, MemberState.DOWN)
        invalidClients!!.add(currentSocket)
        if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
          // Use ShortArrayHelper to remove the clusterMember ID from expectedNodes
          expectedNodes!!.remove(session.memberId)
        }
      }
    }

    if (handlerState == HandlerState.WORK_FAILED) {
      handleFailedWork(session)
    } else if (handlerState == HandlerState.UNPROPER) {
      handleImproperWork(session)
    }
  }

  /**
   * Handles the scenario when work for a session fails.
   *
   * @param session the network client session
   *
   * @throws Exception if an error occurs while processing the failed work
   */
  @Throws(Exception::class)
  private fun handleFailedWork(session: NetworkClient) {
    session.setupNextClient()

    if (session.isAllTried) {
      if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
        // Use ShortArrayHelper to remove the clusterMember ID from expectedNodes
        expectedNodes!!.remove(session.memberId)
      }
      numberOfTriedAttempts++
      addFailedNodeToSyncFeatures(session.member.id)
    }
    failureOrUnProperSocketWorkAround()
  }

  /**
   * Handles the scenario when work for a session is improper.
   *
   * @param session the network client session
   */
  private fun handleImproperWork(session: NetworkClient) {
    if (!session.isImproper) {
      session.isImproper = true
      invalidClients!!.add(currentSocket)
      if (sync != SynchronisationType.UNI_CAST_ONE_OF) {
        expectedNodes!!.remove(session.memberId)
      }
      numberOfTriedAttempts++
      addFailedNodeToSyncFeatures(session.member.id)
      failureOrUnProperSocketWorkAround()
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
  @Throws(Exception::class)
  private fun handleUniCastCallback(session: NetworkClient, handlerState: HandlerState) {
    if (handlerState == HandlerState.WORK_FAILED || handlerState == HandlerState.UNPROPER) {
      val clusterMember = session.member
      if (clusterMember != null && clusterMember.state == MemberState.DELETED) {
        syncContext.synchronizedStateChange(clusterMember, MemberState.DOWN)
        numberOfTriedAttempts++
        addFailedNodeToSyncFeatures(clusterMember.id)
      }
    }

    if (handlerState == HandlerState.WORK_FAILED) {
      handleFailedWork(session)
    } else if (handlerState == HandlerState.UNPROPER) {
      handleImproperWork(session)
    }
  }


  /**
   * Handles cases where the current socket is not functioning properly
   * or has failed. It iterates through the available sockets and
   * publishes a response message if a valid socket is found.
   */
  private fun failureOrUnProperSocketWorkAround() {
    val lastSocket = currentSocket
    currentSocket = (++currentSocket) % sessions!!.size
    val isAllTried = sessions!![currentSocket].isAllTried
    val isImproperSocket = invalidClients!!.contains(currentSocket)

    val isExpectedNode = expectedNodes!!.contains(sessions!![currentSocket].memberId)

    while (isAllTried || isImproperSocket ||
      (sync != SynchronisationType.UNI_CAST_ONE_OF && !isExpectedNode)
    ) {
      if (currentSocket == lastSocket) {
        sessions = null
        fixMonitorLastModified()
        createResult()
        return
      }
      currentSocket = (++currentSocket) % sessions!!.size
    }
    val message = createCompleteResponse(
      DefaultSyncPublication.MessageType.TYPE_CHECK,
      startupState, mode, sync, (-1.toByte()).toByte()
    )
    message.setContents(syncContents!!.values.filterNotNull())
    sessions!![currentSocket].publish(message)
  }

  /**
   * Creates synchronization results based on the current state of the session.
   * It processes sync results if certain conditions are met or handles
   * incomplete synchronization otherwise.
   */
  private fun createResult() {
    // Check if we should proceed with creating results based on current state
    if (shouldCreateResult()) {
      processSyncResults()
    } else if (expectedNodes != null && !syncContents!!.isEmpty()) {
      // Handle case when it is a ring but not everything is complete
      handleIncompleteSync()
    }
  }

  /**
   * Determines whether results should be created based on the state of the sessions.
   *
   * @return true if results can be created; false otherwise
   */
  private fun shouldCreateResult(): Boolean {
    return sessions == null ||
            (expectedNodes != null && expectedNodes!!.isEmpty) || ids == null || numberOfTriedAttempts.toInt() == ids!!.size() ||
            (numberOfTriedAttempts > 0 && sync == SynchronisationType.UNI_CAST_ONE_OF)
  }

  /**
   * Processes synchronization results and evaluates each sync result.
   */
  private fun processSyncResults() {
    val syncingNodesLength = if ((ids == null)) 0 else ids!!.size()
    val isQuorum = isQuorumSync

    for ((_, result) in syncFeature!!) {
      evaluateSyncResult(syncingNodesLength, isQuorum, result)
    }

    if (nonAsync) {
      notifyNonAsync()
    }

    if (callback != null) {
      callback!!.result(syncFeature)
    }
  }

  private val isQuorumSync: Boolean
    /**
     * Checks if the current synchronization type is quorum-based.
     *
     * @return true if the synchronization type is quorum-based; false otherwise
     */
    get() = sync == SynchronisationType.UNI_CAST_QUORUM || sync == SynchronisationType.RING_QUORUM ||
            sync == SynchronisationType.RING_BALANCE_QUORUM || sync == SynchronisationType.UNI_CAST_BALANCE_QUORUM

  /**
   * Evaluates the synchronization result based on the number of syncing nodes
   * and whether quorum is required.
   *
   * @param syncingNodesLength the number of nodes currently syncing
   * @param isQuorum           indicates if quorum synchronization is used
   * @param result             the sync result to evaluate
   */
  private fun evaluateSyncResult(syncingNodesLength: Int, isQuorum: Boolean, result: SyncResult) {
    if (syncingNodesLength == 0) {
      result.isSuccessful = false
      return
    }

    if (isQuorum) {
      if (result.failedMembers.size() * 2 < syncingNodesLength) {
        result.isSuccessful = true
      } else {
        result.isSuccessful = result.syncedMembers.size() * 2 > syncingNodesLength
      }
    } else {
      if (result.syncedMembers.size() > result.failedMembers.size()) {
        result.isSuccessful = true
      } else if (sync == SynchronisationType.UNI_CAST_ONE_OF && !result.failedMembers.isEmpty) {
        result.isSuccessful = false
      } else {
        result.isSuccessful = result.failedMembers.size() < syncingNodesLength
      }
    }
  }

  /**
   * Notifies that a non-async operation has completed.
   */
  private fun notifyNonAsync() {
    nonAsync = false
    (nonasyncLock as Object).notify()
  }

  /**
   * Handles cases of incomplete synchronization by iterating through
   * the session's sockets and checking for valid members.
   */
  private fun handleIncompleteSync() {
    if (sessions!!.isEmpty()) {
      handleAllNodesTried()
      return
    }

    currentSocket = (currentSocket + 1) % sessions!!.size
    val lastSocket = currentSocket

    while (sessions!![currentSocket].isAllTried ||
      invalidClients!!.contains(currentSocket) ||
      !expectedNodes!!.contains(sessions!![currentSocket].memberId)
    ) {
      if (currentSocket == lastSocket) {
        handleAllNodesTried()
        return
      }

      currentSocket = (currentSocket + 1) % sessions!!.size

      // Ensure currentSocket is valid before accessing
      if (sessions!!.size == 0) {
        handleAllNodesTried()
        return
      }
    }

    publishSyncCheck()
  }


  /**
   * Handles the scenario where all nodes have been tried but not completed.
   */
  private fun handleAllNodesTried() {
    sessions = null
    fixMonitorLastModified()
    createResult() // Restart the process
  }

  /**
   * Publishes a synchronization check message to the current socket.
   */
  private fun publishSyncCheck() {
    val message = DefaultSyncPublication()
    message.id = selfMember!!.id
    message.isInStartup = startupState
    message.syncMode = mode
    message.syncType = sync
    message.type = DefaultSyncPublication.MessageType.TYPE_CHECK
    message.setContents(syncContents!!.values.filterNotNull())
    sessions!![currentSocket].publish(message)
  }

  /**
   * Adds a failed node to the synchronization features for further processing.
   *
   * @param failedNodeId the ID of the failed node to add
   */
  private fun addFailedNodeToSyncFeatures(failedNodeId: Short) {
    for ((_, syncResult) in syncFeature!!) {
      syncResult.addFailedMember(failedNodeId)
    }
  }

  /**
   * Initiates synchronization with balance based on the given publications.
   *
   * @param publications a list of publications to synchronize
   */
  private fun syncWithBalance(publications: List<Publication>) {
    endpointType = EndpointType.CLIENT

    val clusterSnapshot = syncContext.snapshot
    if (clusterSnapshot == null) {
      nonasyncLock = null // Reset the lock if the snapshot is unavailable
      return
    }

    this.syncFeature = HashMap()
    val clusterMembers = prepareCluster(clusterSnapshot) ?: return

    val callbackHandler = createCallbackHandler()

    if (isUniCastBalanceSync) {
      handleUniCastBalanceSync(publications, clusterSnapshot, clusterMembers, callbackHandler)
    } else if (isRingBalanceSync) {
      handleRingBalanceSync(publications, clusterSnapshot, clusterMembers, callbackHandler)
    }
  }

  private val isUniCastBalanceSync: Boolean
    /**
     * Checks if the current synchronization type is unicast balance synchronization.
     *
     * @return true if the synchronization type is either [SynchronisationType.UNI_CAST_BALANCE] or
     * [SynchronisationType.UNI_CAST_BALANCE_QUORUM]; false otherwise.
     */
    get() = this.sync == SynchronisationType.UNI_CAST_BALANCE || this.sync == SynchronisationType.UNI_CAST_BALANCE_QUORUM

  private val isRingBalanceSync: Boolean
    /**
     * Checks if the current synchronization type is ring balance synchronization.
     *
     * @return true if the synchronization type is either [SynchronisationType.RING_BALANCE] or
     * [SynchronisationType.RING_BALANCE_QUORUM]; false otherwise.
     */
    get() = this.sync == SynchronisationType.RING_BALANCE || this.sync == SynchronisationType.RING_BALANCE_QUORUM

  /**
   * Prepares a list of cluster members based on the provided cluster snapshot.
   *
   * @param clusterSnapshot the current snapshot of the cluster
   *
   * @return a list of cluster members, or null if no members are found
   */
  private fun prepareCluster(clusterSnapshot: ClusterSnapshot): MutableList<ClusterMember> {
    var cluster: MutableList<ClusterMember?>? = null

    if (ids == null || ids!!.isEmpty) {
      cluster = clusterSnapshot.aliveCluster
      if (!cluster.isEmpty()) {
        ids = ClusterIdRegistry()
        for (e in cluster) {
          if (e != null) {
            ids!!.add(e.id)
          }
        }
      }
    } else {
      // For the ring balance case, use the cluster directly
      cluster = clusterSnapshot.aliveCluster
      if (!isInclusive) {
        val idss = ClusterIdRegistry()
        performInverseIds(cluster.filterNotNull().toMutableList(), idss)
        this.ids = idss
      }
    }

    return cluster
      .filterNotNull()
      .toMutableList()
  }

  /**
   * Creates a callback handler for handling synchronization results.
   *
   * @return a [PublicationHandler] that processes sync results and callback actions
   */
  private fun createCallbackHandler(): PublicationHandler {
    return object : PublicationHandler {
      private var successCount: Short = 0 // Count of successful results

      @Synchronized
      override fun result(syncFeature: Map<String?, SyncResult>?) {
        successCount++
        if (syncFeature != null) {
          this@SyncNetworkHandler.syncFeature!!.putAll(syncFeature)
        }

        if (successCount.toInt() == ids!!.size()) {
          this@SyncNetworkHandler.numberOfTriedAttempts = successCount // Update the number of attempts
          this@SyncNetworkHandler.createResult()
        }
      }

      override fun callBack(
        session: Session?, message: Publication,
        withNodes: ClusterIdRegistry, out: PublicationBundle
      ): Boolean {
        return this@SyncNetworkHandler.callback != null && callback!!
          .callBack(session, message, withNodes, out)
      }
    }
  }

  /**
   * Handles the synchronization process for unicast balance synchronization.
   *
   * @param publications    a list of publications to send
   * @param clusterSnapshot the current snapshot of the cluster
   * @param clusterMembers  a list of cluster members to synchronize with
   * @param callbackHandler the handler to be called upon message sending completion
   */
  private fun handleUniCastBalanceSync(
    publications: List<Publication>, clusterSnapshot: ClusterSnapshot,
    clusterMembers: List<ClusterMember>, callbackHandler: PublicationHandler
  ) {
    val messagesGroupedByNode: MutableMap<Short, List<Publication>> = HashMap()

    if (ids!!.isEmpty) {
      fillMessagesForNewCluster(publications, clusterMembers, messagesGroupedByNode)
    } else {
      fillMessagesForExistingCluster(publications, clusterSnapshot, messagesGroupedByNode)
    }

    if (messagesGroupedByNode.isEmpty()) {
      nonasyncLock = null // Reset the lock if no messages are available
      createSyncResultForUnsentMessages(publications)
      return
    }

    sendMessagesToClusterNodes(messagesGroupedByNode, callbackHandler)
  }

  /**
   * Fills the map with publications grouped by cluster member IDs for a new cluster.
   *
   * @param publications     a list of publications to be filled
   * @param clusterMembers   a list of cluster members to check against
   * @param messagesByNodeId a map to store publications grouped by cluster member IDs
   */
  private fun fillMessagesForNewCluster(
    publications: List<Publication>, clusterMembers: List<ClusterMember>,
    messagesByNodeId: MutableMap<Short, List<Publication>>
  ) {
    for (clusterMember in clusterMembers) {
      fillMessagesPerNode(publications, messagesByNodeId, clusterMember)
    }
  }

  /**
   * Fills the map with publications grouped by cluster member IDs for an existing cluster.
   *
   * @param publications     a list of publications to be filled
   * @param clusterSnapshot  the current snapshot of the cluster
   * @param messagesByNodeId a map to store publications grouped by cluster member IDs
   */
  private fun fillMessagesForExistingCluster(
    publications: List<Publication>, clusterSnapshot: ClusterSnapshot,
    messagesByNodeId: MutableMap<Short, List<Publication>>
  ) {
    if (isInclusive) {
      for (clusterMemberId in ids!!.ids) {
        val clusterMember = clusterSnapshot.getById(clusterMemberId, 2)
        if (clusterMember != null) {
          fillMessagesPerNode(publications, messagesByNodeId, clusterMember)
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
  private fun sendMessagesToClusterNodes(
    messagesByNodeId: Map<Short, List<Publication>>,
    callbackHandler: PublicationHandler
  ) {
    for ((key, value) in messagesByNodeId) {
      val syncNetworkHandler = SyncNetworkHandler(syncContext, this.sync)
        .withCluster(key)
        .withCallBack(callbackHandler)
        .withPublicationType(this.publicationInstance)
        .withBalance()

      syncNetworkHandler.mode = this.mode
      syncNetworkHandler.sync(value)
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
  private fun handleRingBalanceSync(
    publications: List<Publication>, clusterSnapshot: ClusterSnapshot,
    clusterMembersList: List<ClusterMember>, callbackHandler: PublicationHandler
  ) {
    val activeClusterMembers: MutableSet<ClusterMember> = HashSet()

    if (ids!!.isEmpty) {
      activeClusterMembers.addAll(clusterMembersList)
    } else if (isInclusive) {
      for (id in ids!!.ids) {
        val member = clusterSnapshot.getById(id, ClusterSnapshot.MEMBER_CHECK_VALID)
        if (member != null) {
          activeClusterMembers.add(member)
        }
      }
    }

    if (!activeClusterMembers.isEmpty()) {
      val messageToUnawareMembersMap = mapMessagesToNotAwareMembers(publications, activeClusterMembers)
      if (!messageToUnawareMembersMap.isEmpty()) {
        sendMessagesForRingBalance(messageToUnawareMembersMap, callbackHandler)
      } else {
        createSyncResultForUnsentMessages(publications)
        nonasyncLock = null // Reset the lock if no messages to send
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
   * [ClusterIdRegistry] of unaware cluster members
   */
  private fun mapMessagesToNotAwareMembers(
    publications: List<Publication>,
    clusterMembers: Set<ClusterMember>
  ): Map<Publication, ClusterIdRegistry> {
    val messageToUnawareMembersMap: MutableMap<Publication, ClusterIdRegistry> = HashMap()

    for (publication in publications) {
      val unawareMembersForMessage = ClusterIdRegistry()
      for (clusterMember in clusterMembers) {
        if (!isClusterMemberAwareOfPublication(publication, clusterMember)) {
          unawareMembersForMessage.add(clusterMember.id)
        }
      }

      if (!unawareMembersForMessage.isEmpty) {
        messageToUnawareMembersMap[publication] = unawareMembersForMessage
      } else {
        val syncResult = SyncResult()
        syncResult.isSuccessful = true
        syncFeature!![publication.key] = syncResult
      }
    }

    return messageToUnawareMembersMap
  }

  /**
   * Sends messages to cluster members who are not aware of them, ensuring ring balance.
   *
   * @param messageToNotAwareMembers a map of publications to their corresponding unaware cluster members
   * @param callBackHandler          the handler to be called upon message sending completion
   */
  private fun sendMessagesForRingBalance(
    messageToNotAwareMembers: Map<Publication, ClusterIdRegistry>,
    callBackHandler: PublicationHandler
  ) {
    val processedMessages: MutableSet<String> = HashSet()
    val finalMessagesToMembers: MutableMap<List<Publication>, ClusterIdRegistry> = HashMap()

    for ((key, value) in messageToNotAwareMembers) {
      if (value.isEmpty || processedMessages.contains(key.key)) {
        continue
      }

      val messageList: MutableList<Publication> = ArrayList()
      processedMessages.add(key.key)
      messageList.add(key)

      for ((key1, value1) in messageToNotAwareMembers) {
        if (value1.isEmpty || processedMessages.contains(key1.key)) {
          continue
        }

        if (value.containsAll(*value1.ids)) {
          messageList.add(key1)
          processedMessages.add(key1.key)
        }
      }
      finalMessagesToMembers[messageList] = value
    }

    for ((key, value) in finalMessagesToMembers) {
      val handler = SyncNetworkHandler(syncContext, this.sync)
        .withCluster(*value.ids)
        .withCallBack(callBackHandler)
        .withPublicationType(this.publicationInstance)
        .withBalance()
      handler.mode = this.mode
      handler.sync(key)
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
  private fun isClusterMemberAwareOfPublication(
    publication: Publication,
    targetClusterMember: ClusterMember
  ): Boolean {
    val awareMemberIds: ClusterIdRegistry // Initialize as an empty array

    if (this.mode == DefaultSyncPublication.SyncMode.SYNC_MESSAGE) {
      awareMemberIds =
        addAndGetAwareNodesOfMessage(publication) // Assuming this method returns a ClusterIdRegistry
    } else {
      val clusterPublication = publication as ClusterPublication
      val memberNode = syncContext.getMemberById(clusterPublication.id)
      awareMemberIds =
        if ((memberNode == null)) ClusterIdRegistry() else memberNode.getAwareIdsLock()!! // Initialize as an empty array if memberNode is null
    }

    return awareMemberIds.contains(targetClusterMember.id)
  }

  /**
   * Creates a sync result for a list of publications that were not sent.
   *
   * @param publications a list of publications that were not sent
   */
  private fun createSyncResultForUnsentMessages(publications: List<Publication>) {
    for (publication in publications) {
      val syncResult = SyncResult()
      syncResult.isSuccessful = true
      syncFeature!![publication.key] = syncResult
    }
  }

  /**
   * Fills a map with publications that a specified cluster member is unaware of.
   *
   * @param publications                  a list of all publications
   * @param publicationsByClusterMemberId a map to fill with publications grouped by cluster member IDs
   * @param targetClusterMember           the cluster member to check awareness for
   */
  private fun fillMessagesPerNode(
    publications: List<Publication>, publicationsByClusterMemberId: MutableMap<Short, List<Publication>>,
    targetClusterMember: ClusterMember
  ) {
    val filteredPublications: MutableList<Publication> = ArrayList()
    for (publication in publications) {
      if (isClusterMemberAwareOfPublication(publication, targetClusterMember)) continue
      filteredPublications.add(publication)
    }

    if (!filteredPublications.isEmpty()) {
      publicationsByClusterMemberId[targetClusterMember.id] = filteredPublications
    }
  }

  /**
   * Fixes the last modified timestamp for monitoring purposes.
   * This method currently has no implementation.
   */
  fun fixMonitorLastModified() {
    // Implementation to be added.
  }


  /**
   * Checks if the given SynchronisationType is a balance type.
   *
   * @param synchronisationType the SynchronisationType to check.
   * @return true if the synchronisationType is a balance type; false otherwise.
   */
  fun isBalanceType(synchronisationType: SynchronisationType?): Boolean {
    return synchronisationType == SynchronisationType.RING_BALANCE || synchronisationType == SynchronisationType.RING_BALANCE_QUORUM || synchronisationType == SynchronisationType.UNI_CAST_BALANCE || synchronisationType == SynchronisationType.UNI_CAST_BALANCE_QUORUM
  }

  /**
   * Checks if the given SynchronisationType is a unicast type.
   *
   * @param synchronisationType the SynchronisationType to check.
   * @return true if the synchronisationType is a unicast type; false otherwise.
   */
  fun isUnicastType(synchronisationType: SynchronisationType?): Boolean {
    return synchronisationType == SynchronisationType.UNI_CAST || synchronisationType == SynchronisationType.UNI_CAST_QUORUM || synchronisationType == SynchronisationType.UNI_CAST_BALANCE || synchronisationType == SynchronisationType.UNI_CAST_BALANCE_QUORUM || synchronisationType == SynchronisationType.UNI_CAST_ONE_OF
  }

  /**
   * Checks if the given SynchronisationType is a ring type.
   *
   * @param synchronisationType the SynchronisationType to check.
   * @return true if the synchronisationType is a ring type; false otherwise.
   */
  fun isRingType(synchronisationType: SynchronisationType?): Boolean {
    return synchronisationType == SynchronisationType.RING || synchronisationType == SynchronisationType.RING_QUORUM || synchronisationType == SynchronisationType.RING_BALANCE || synchronisationType == SynchronisationType.RING_BALANCE_QUORUM
  }

  companion object {
    private const val SELF_ATTRIBUTE_KEY = "self"
    private const val SESSION_INITIATED_KEY = "initiated"
    private const val START_UP_STATE_KEY = "startup_state"
  }
}