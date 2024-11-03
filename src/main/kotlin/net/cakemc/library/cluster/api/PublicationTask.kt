package net.cakemc.library.cluster.api

import net.cakemc.library.cluster.*
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.api.ClusterContext
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.handler.PublicationHandler
import net.cakemc.library.cluster.handler.SyncResult
import java.util.*

/**
 * The `PublicationTask` class handles the dispatch and synchronization of publications
 * within a cluster environment. It provides methods for configuring publication types, skipping
 * specific nodes, and setting synchronization callbacks. This class implements the
 * [PublicationHandler] interface to handle publication callbacks and results.
 */
class PublicationTask(private val context: ClusterContext) : PublicationHandler {
    private var handler: PublicationHandler
    private var type: SynchronisationType?
    private var publicationType: Class<out Publication>
    private var nodesToSkip: ShortArray

    private var channel = "_internal_"

    var dispatchTime: Long = 0

    /**
     * Constructs a `PublicationTask` with the given cluster context.
     *
     * @param context the [ClusterContext] instance that provides context for the publication task.
     */
    init {
        this.nodesToSkip = ShortArray(0)
        this.type = context.type
        this.handler = this

        this.publicationType = SoftPublication::class.java
    }

    /**
     * Sets a callback handler for the publication task.
     *
     * @param handler the [PublicationHandler] instance to be set as the callback handler.
     *
     * @return the current `PublicationTask` instance.
     */
    fun callBack(handler: PublicationHandler): PublicationTask {
        this.handler = handler
        return this
    }

    /**
     * Sets the synchronization type for the publication task.
     *
     * @param type the [SynchronisationType] to be used for synchronization.
     *
     * @return the current `PublicationTask` instance.
     */
    fun synchronisationType(type: SynchronisationType?): PublicationTask {
        this.type = type
        return this
    }

    /**
     * Sets the publication type for the task.
     *
     * @param publicationType the class of the [Publication] type to be used.
     *
     * @return the current `PublicationTask` instance.
     */
    fun publicationType(publicationType: Class<out Publication>): PublicationTask {
        this.publicationType = publicationType
        return this
    }

    /**
     * Skips the specified nodes during the publication process.
     *
     * @param nodesToSkip an array of node IDs to be skipped during publication.
     *
     * @return the current `PublicationTask` instance.
     */
    fun skip(vararg nodesToSkip: Int): PublicationTask {
        val toSkip = ShortArray(nodesToSkip.size)
        for (index in nodesToSkip.indices) {
            toSkip[index] = nodesToSkip[index].toShort()
        }
        this.nodesToSkip = toSkip
        return this
    }

    /**
     * Sets the communication channel for this publication task.
     *
     *
     * The channel specifies the path or topic within the cluster where this
     * publication task will be directed. This method allows for fluent configuration
     * by returning the current instance of `PublicationTask` after setting
     * the channel.
     *
     * @param channel the channel to be assigned to this publication task
     *
     * @return the current instance of `PublicationTask` with the updated channel
     */
    fun channel(channel: String): PublicationTask {
        this.channel = channel
        return this
    }

    /**
     * Releases the provided publication for synchronization across the cluster.
     *
     * @param publication the [Publication] instance to be synchronized.
     */
    fun release(publication: Publication) {
        publication.channel = channel

        context.context
            ?.make(type)
            ?.withCallBack(handler)
            ?.withoutCluster(*nodesToSkip)
            ?.withPublicationType(publication.javaClass)
            ?.sync(publication)

        context.backUpEndpoint
            ?.dispatchPacketToRing(publication)
    }

    /**
     * Releases multiple publications for synchronization across the cluster.
     *
     * @param publications an array of [Publication] instances to be synchronized.
     */
    fun releaseMulti(vararg publications: Publication) {
        for (publication in publications) {
            publication.channel = channel
        }

        context.context
            ?.make(type)
            ?.withCallBack(handler)
            ?.withoutCluster(*nodesToSkip)
            ?.withPublicationType(publicationType)
            ?.sync(Arrays.stream(publications).toList())

        for (publication in publications) {
            context.backUpEndpoint?.dispatchPacketToRing(publication)
        }
    }

    /**
     * Handles the callback for the publication process.
     * This method is part of the [PublicationHandler] interface and determines
     * whether the callback is successful or not.
     *
     * @param session   the [Session] associated with the callback.
     * @param message   the [Publication] message associated with the callback.
     * @param withNodes the [ClusterIdRegistry] containing the cluster nodes.
     * @param out       the [PublicationBundle] containing the outgoing publication bundle.
     *
     * @return `false` as the default implementation.
     */
    override fun callBack(
        session: Session?,
        message: Publication,
        withNodes: ClusterIdRegistry,
        out: PublicationBundle
    ): Boolean {
        return false
    }

    /**
     * Processes the result of the publication synchronization.
     * This method is part of the [PublicationHandler] interface.
     *
     * @param syncFeature a map containing the [SyncResult] for each synchronization feature.
     */
    override fun result(syncFeature: Map<String?, SyncResult>?) {
    }
}
