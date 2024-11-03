package net.cakemc.library.cluster.api

import net.cakemc.library.cluster.SynchronisationType
import net.cakemc.library.cluster.codec.*
import net.cakemc.library.cluster.handler.PublicationHandler
import java.util.*

/**
 * Builder class for constructing a cluster configuration.
 * This class provides a fluent interface to set various parameters for the cluster,
 * including authentication, synchronization type, publication type, and member addresses.
 */
class ClusterBuilder(id: Int) {
    private val stateMachine: StateMachine

    private val channels: MutableSet<String>
    private val members: MutableSet<MemberIdentifier>
    private lateinit var ownAddress: MemberIdentifier
    private val handlerMap: MutableMap<String, PublicationHandler>

    private val id: Short
    private lateinit var publicationType: Class<out Publication?>
    private lateinit var authentication: Authentication
    private lateinit var prioritised: ShortArray
    private lateinit var type: SynchronisationType

    /**
     * Constructs a new ClusterBuilder with the specified identifier.
     *
     * @param id the identifier for the cluster
     */
    init {
        this.id = id.toShort()

        this.stateMachine = StateMachine()

        channels = HashSet()
        handlerMap = HashMap()
        members = HashSet()

        // default initial
        this.withDefaultPublicationType()
        this.defaultAuthentication()
    }

    /**
     * Sets the identifier for the cluster member.
     *
     * @param id the identifier of the member
     * @param host the host address of the member
     * @param port the port of the member
     * @return the current instance of ClusterBuilder
     */
    fun identifier(id: Int, host: String, port: Int): ClusterBuilder {
        this.ownAddress = MemberIdentifier(id, host, port)
        stateMachine.addState(IDENTIFIER)
        return this
    }

    /**
     * Subscribes a publication handler to a specific channel.
     *
     * @param channel the channel to subscribe to
     * @param publicationHandler the handler for publications on the channel
     * @return the current instance of ClusterBuilder
     */
    fun subscribe(channel: String, publicationHandler: PublicationHandler): ClusterBuilder {
        handlerMap?.put(channel, publicationHandler)
        return this
    }

    /**
     * Registers a channel for the cluster.
     *
     * @param channel the channel to register
     * @return the current instance of ClusterBuilder
     */
    fun register(channel: String): ClusterBuilder {
        channels?.add(channel)
        return this
    }

    /**
     * Sets the synchronization type for the cluster.
     *
     * @param type the synchronization type to set
     * @return the current instance of ClusterBuilder
     */
    fun type(type: SynchronisationType): ClusterBuilder {
        this.type = type
        stateMachine.addState(CLUSTER_TYPE)
        return this
    }

    /**
     * Adds member identifiers to the cluster.
     *
     * @param addresses the member identifiers to add
     * @return the current instance of ClusterBuilder
     */
    fun members(vararg addresses: MemberIdentifier): ClusterBuilder {
        members?.addAll(Arrays.asList(*addresses))
        stateMachine.addState(MEMBERS)
        return this
    }

    /**
     * Sets the authentication method for the cluster.
     *
     * @param authentication the authentication to set
     * @return the current instance of ClusterBuilder
     */
    fun authentication(authentication: Authentication): ClusterBuilder {
        this.authentication = authentication
        stateMachine.addState(AUTHENTICATOR)
        return this
    }

    /**
     * Sets the default authentication method for the cluster.
     *
     * @return the current instance of ClusterBuilder
     */
    fun defaultAuthentication(): ClusterBuilder {
        this.authentication = Authentication("", true)
        stateMachine.addState(AUTHENTICATOR)
        return this
    }

    /**
     * Sets the prioritization order for the cluster members.
     *
     * @param prioritised the array of prioritized identifiers
     * @return the current instance of ClusterBuilder
     */
    fun priorities(vararg prioritised: Int): ClusterBuilder {
        val priority = ShortArray(prioritised.size)
        for (index in prioritised.indices) {
            priority[index] = prioritised[index].toShort()
        }
        this.prioritised = priority
        return this
    }

    /**
     * Sets the publication type for the cluster.
     *
     * @param publicationInstance the class of the publication type to set
     * @return the current instance of ClusterBuilder
     */
    fun withPublicationType(publicationInstance: Class<out Publication?>): ClusterBuilder {
        this.publicationType = publicationInstance
        stateMachine.addState(PUBLICATION_TYPE)
        return this
    }

    /**
     * Sets the default publication type for the cluster.
     *
     * @return the current instance of ClusterBuilder
     */
    fun withDefaultPublicationType(): ClusterBuilder {
        this.publicationType = ClusterPublication::class.java
        stateMachine.addState(PUBLICATION_TYPE)
        return this
    }

    /**
     * Builds the cluster context based on the current configuration.
     *
     * @return the constructed ClusterContext
     * @throws IllegalArgumentException if any required states are missing
     */
    fun get(): ClusterContext {
        require(stateMachine.hasState(AUTHENTICATOR)) { "authenticator missing in cluster context-builder" }
        require(stateMachine.hasState(CLUSTER_TYPE)) { "cluster-type missing in cluster context-builder" }
        require(stateMachine.hasState(PUBLICATION_TYPE)) { "publication-type missing in cluster context-builder" }
        require(stateMachine.hasState(IDENTIFIER)) { "identifier missing in cluster context-builder" }
        require(stateMachine.hasState(MEMBERS)) { "members missing in cluster context-builder" }

        val context = ClusterContext()
        context.id = this.id
        context.authentication = this.authentication
        context.channels = this.channels
        context.type = this.type
        context.prioritised = this.prioritised
        context.handlerMap = this.handlerMap
        context.publicationType = this.publicationType
        context.members = this.members
        context.ownAddress = this.ownAddress

        context.createInternalCluster()
        return context
    }

    companion object {
        /**
         * Constant representing the authenticator state.
         */
        const val AUTHENTICATOR: Int = 1 shl 1

        /**
         * Constant representing the cluster type state.
         */
        const val CLUSTER_TYPE: Int = 1 shl 2

        /**
         * Constant representing the publication type state.
         */
        const val PUBLICATION_TYPE: Int = 1 shl 3

        /**
         * Constant representing the identifier state.
         */
        const val IDENTIFIER: Int = 1 shl 4

        /**
         * Constant representing the members state.
         */
        const val MEMBERS: Int = 1 shl 5
    }
}
