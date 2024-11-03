package net.cakemc.library.cluster.api

import net.cakemc.library.cluster.*
import net.cakemc.library.cluster.ClusterMember.MemberState
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.codec.PublicationBundle
import net.cakemc.library.cluster.codec.SyncProtocolBundle
import net.cakemc.library.cluster.config.ClusterIdentificationContext
import net.cakemc.library.cluster.config.NodeIdentifier
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.fallback.BackUpClusterNode
import net.cakemc.library.cluster.fallback.endpoint.codec.PublicationCodec
import net.cakemc.library.cluster.fallback.endpoint.handler.AbstractConnectionHandler
import net.cakemc.library.cluster.handler.PublicationHandler
import net.cakemc.library.cluster.handler.SyncNetworkHandler
import net.cakemc.library.cluster.handler.SyncResult
import net.cakemc.library.cluster.network.NetworkServer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * The ClusterContext class manages the state and behavior of a cluster, including
 * handling publications and managing cluster members. It provides methods for
 * creating and configuring the cluster context, subscribing to channels, and
 * handling incoming publications.
 */
class ClusterContext
/**
 * Constructs a new ClusterContext instance.
 */
  : PublicationHandler {
  var id: Short = 0

  // publishing
  lateinit var channels: MutableSet<String>
  lateinit var handlerMap: MutableMap<String, PublicationHandler>

  // identification
  lateinit var members: Set<MemberIdentifier>
  lateinit var ownAddress: MemberIdentifier

  // config
  lateinit var publicationType: Class<out Publication>
  lateinit var authentication: Authentication
  lateinit var prioritised: ShortArray

  /**
   * Retrieves the current snapshot of the cluster context.
   *
   * @return the current [ClusterSnapshot] instance.
   */
  // snapshot
  lateinit var snapshot: ClusterSnapshot

  // type
  lateinit var type: SynchronisationType

  // internal usage
  lateinit private var identifiers: MutableList<NodeIdentifier>
  lateinit private var clusterMembers: MutableList<ClusterMember>

  lateinit var context: SyncContext
  lateinit var clusterIdentificationContext: ClusterIdentificationContext

  lateinit var registry: ClusterIdRegistry
  lateinit var networkServer: NetworkServer
  lateinit var handler: SyncNetworkHandler
  lateinit var backUpEndpoint: AbstractBackUpEndpoint

  var protocolBundle: SyncProtocolBundle? = null
    private set

  lateinit private var connectionHandler: AbstractConnectionHandler

  /**
   * Creates an internal cluster context, initializing necessary components, and
   * starting the network server and backup endpoint.
   */
  fun createInternalCluster() {
    context = SyncContext(id)

    handler = context
      .make()
      .withCallBack(this)
      .withPublicationType(publicationType)

    PublicationCodec.Companion.publicationType = publicationType

    identifiers = ArrayList()
    clusterMembers = ArrayList()

    for (identifier in members) {
      (identifiers as ArrayList<NodeIdentifier>).add(
        NodeIdentifier(
          identifier.id.toInt(),
          identifier.address.toString(),
          identifier.id.toInt()
        )
      )
    }

    this.protocolBundle = SyncProtocolBundle()

    registry = ClusterIdRegistry()
    (identifiers as ArrayList<NodeIdentifier>).forEach(Consumer { nodeIdentifier: NodeIdentifier -> registry.add(nodeIdentifier.id.toShort()) })

    for (identifier in members) {
      val member = ClusterMember(
        identifier.id,
        members.stream().map { obj: MemberIdentifier -> obj.address }.toList(),
        authentication.isUseAuthentication, authentication.authKey,
        System.currentTimeMillis(), registry,
        MemberState.VALID
      )

      (clusterMembers as ArrayList<ClusterMember>).add(member)
    }

    connectionHandler = ClusterPublicationHandler(this)

    clusterIdentificationContext = ClusterIdentificationContext()
    clusterIdentificationContext.socketConfigs = (identifiers as ArrayList<NodeIdentifier>)

    networkServer = NetworkServer(handler, clusterIdentificationContext)
    EXECUTOR_SERVICE.execute { networkServer.start() }

    (clusterMembers as ArrayList<ClusterMember>).forEach(Consumer { memberIdentifier: ClusterMember? ->
      context.syncCluster(
        memberIdentifier,
        this.type
      )
    })

    backUpEndpoint = BackUpClusterNode(
      ownAddress, members.stream().toList(), authentication.authKey
    )

    (backUpEndpoint as BackUpClusterNode).connectionManager.registerPacketHandler(
      connectionHandler as ClusterPublicationHandler
    )

    EXECUTOR_SERVICE.execute { (backUpEndpoint as BackUpClusterNode).start() }

    snapshot = context.getOrCreateSnapshot()
  }

  /**
   * Creates a new publication task for the cluster context.
   *
   * @return a new instance of [PublicationTask] associated with this context.
   */
  fun publisher(): PublicationTask {
    val publicationTask = PublicationTask(
      this
    )

    publicationTask.dispatchTime = System.currentTimeMillis()

    return publicationTask
  }

  /**
   * Subscribes a publication handler to the specified channel.
   *
   * @param channel            the channel to subscribe to.
   * @param publicationHandler the handler to handle publications for this channel.
   * @return the current instance of [ClusterContext] for chaining.
   */
  fun subscribe(channel: String, publicationHandler: PublicationHandler): ClusterContext {
    handlerMap.put(channel, publicationHandler)
    return this
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
  override fun callBack(
    session: Session?,
    message: Publication,
    withNodes: ClusterIdRegistry,
    out: PublicationBundle
  ): Boolean {
    var state = false
    if (message is SoftPublication) {
      for ((key, value) in handlerMap) {
        if (!key.equals(message.key, ignoreCase = true)) continue

        state = value.callBack(
          session, message, withNodes, out
        )
      }
    }
    return state
  }

  /**
   * Handles the result of a synchronization feature.
   *
   * @param syncFeature a map containing synchronization results keyed by feature names.
   */
  override fun result(syncFeature: Map<String?, SyncResult>?) {
    // no results handeling
  }

  fun getIdentifiers(): List<NodeIdentifier> {
    return identifiers
  }

  fun setIdentifiers(identifiers: MutableList<NodeIdentifier>) {
    this.identifiers = identifiers
  }

  fun getClusterMembers(): List<ClusterMember> {
    return clusterMembers
  }

  fun setClusterMembers(clusterMembers: MutableList<ClusterMember>) {
    this.clusterMembers = clusterMembers
  }

  companion object {
    /**
     * An ExecutorService for managing threads for the cluster context.
     */
    val EXECUTOR_SERVICE: ExecutorService = Executors.newCachedThreadPool(
      object : ThreadFactory {
        private val task = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
          val thread = Thread(
            runnable, "cluster-context-${task.incrementAndGet()}"
          )
          thread.isDaemon = true
          thread.priority = Thread.NORM_PRIORITY

          return thread
        }
      }
    )

    /**
     * Creates a new [ClusterBuilder] with the specified cluster ID.
     *
     * @param id the unique identifier for the cluster.
     * @return a new instance of [ClusterBuilder].
     */
    @kotlin.jvm.JvmStatic
    fun make(id: Int): ClusterBuilder {
      return ClusterBuilder(id)
    }
  }
}
