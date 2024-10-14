package net.cakemc.library.cluster.api;

import net.cakemc.library.cluster.SynchronisationType;
import net.cakemc.library.cluster.codec.ClusterPublication;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.handler.PublicationHandler;

import java.util.*;

/**
 * Builder class for constructing a cluster configuration.
 * This class provides a fluent interface to set various parameters for the cluster,
 * including authentication, synchronization type, publication type, and member addresses.
 */
public class ClusterBuilder {
	/**
	 * Constant representing the authenticator state.
	 */
	public static final int AUTHENTICATOR = 1 << 1;

	/**
	 * Constant representing the cluster type state.
	 */
	public static final int CLUSTER_TYPE = 1 << 2;

	/**
	 * Constant representing the publication type state.
	 */
	public static final int PUBLICATION_TYPE = 1 << 3;

	/**
	 * Constant representing the identifier state.
	 */
	public static final int IDENTIFIER = 1 << 4;

	/**
	 * Constant representing the members state.
	 */
	public static final int MEMBERS = 1 << 5;

	private final StateMachine stateMachine;

	private final Set<String> channels;
	private final Set<MemberIdentifier> members;
	private MemberIdentifier ownAddress;
	private final Map<String, PublicationHandler> handlerMap;

	private final short id;
	private Class<? extends Publication> publicationType;
	private Authentication authentication;
	private short[] prioritised;
	private SynchronisationType type;

	/**
	 * Constructs a new ClusterBuilder with the specified identifier.
	 *
	 * @param id the identifier for the cluster
	 */
	public ClusterBuilder(int id) {
		this.id = (short) id;

		this.stateMachine = new StateMachine();

		channels = new HashSet<>();
		handlerMap = new HashMap<>();
		members = new HashSet<>();

		// default initial
		this.withDefaultPublicationType();
		this.defaultAuthentication();
	}

	/**
	 * Sets the identifier for the cluster member.
	 *
	 * @param id the identifier of the member
	 * @param host the host address of the member
	 * @param port the port of the member
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder identifier(int id, String host, int port) {
		this.ownAddress = new MemberIdentifier(id, host, port);
		stateMachine.addState(IDENTIFIER);
		return this;
	}

	/**
	 * Subscribes a publication handler to a specific channel.
	 *
	 * @param channel the channel to subscribe to
	 * @param publicationHandler the handler for publications on the channel
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder subscribe(String channel, PublicationHandler publicationHandler) {
		this.handlerMap.put(channel, publicationHandler);
		return this;
	}

	/**
	 * Registers a channel for the cluster.
	 *
	 * @param channel the channel to register
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder register(String channel) {
		this.channels.add(channel);
		return this;
	}

	/**
	 * Sets the synchronization type for the cluster.
	 *
	 * @param type the synchronization type to set
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder type(SynchronisationType type) {
		this.type = type;
		stateMachine.addState(CLUSTER_TYPE);
		return this;
	}

	/**
	 * Adds member identifiers to the cluster.
	 *
	 * @param addresses the member identifiers to add
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder members(MemberIdentifier... addresses) {
		this.members.addAll(Arrays.asList(addresses));
		stateMachine.addState(MEMBERS);
		return this;
	}

	/**
	 * Sets the authentication method for the cluster.
	 *
	 * @param authentication the authentication to set
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder authentication(Authentication authentication) {
		this.authentication = authentication;
		stateMachine.addState(AUTHENTICATOR);
		return this;
	}

	/**
	 * Sets the default authentication method for the cluster.
	 *
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder defaultAuthentication() {
		this.authentication = new Authentication("", true);
		stateMachine.addState(AUTHENTICATOR);
		return this;
	}

	/**
	 * Sets the prioritization order for the cluster members.
	 *
	 * @param prioritised the array of prioritized identifiers
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder priorities(int... prioritised) {
		short[] priority = new short[prioritised.length];
		for (int index = 0; index < prioritised.length; index++) {
			priority[index] = (short) prioritised[index];
		}
		this.prioritised = priority;
		return this;
	}

	/**
	 * Sets the publication type for the cluster.
	 *
	 * @param publicationInstance the class of the publication type to set
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder withPublicationType(Class<? extends Publication> publicationInstance) {
		this.publicationType = publicationInstance;
		stateMachine.addState(PUBLICATION_TYPE);
		return this;
	}

	/**
	 * Sets the default publication type for the cluster.
	 *
	 * @return the current instance of ClusterBuilder
	 */
	public ClusterBuilder withDefaultPublicationType() {
		this.publicationType = ClusterPublication.class;
		stateMachine.addState(PUBLICATION_TYPE);
		return this;
	}

	/**
	 * Builds the cluster context based on the current configuration.
	 *
	 * @return the constructed ClusterContext
	 * @throws IllegalArgumentException if any required states are missing
	 */
	public ClusterContext get() {
		if (!this.stateMachine.hasState(AUTHENTICATOR)) {
			throw new IllegalArgumentException("authenticator missing in cluster context-builder");
		}
		if (!this.stateMachine.hasState(CLUSTER_TYPE)) {
			throw new IllegalArgumentException("cluster-type missing in cluster context-builder");
		}
		if (!this.stateMachine.hasState(PUBLICATION_TYPE)) {
			throw new IllegalArgumentException("publication-type missing in cluster context-builder");
		}
		if (!this.stateMachine.hasState(IDENTIFIER)) {
			throw new IllegalArgumentException("identifier missing in cluster context-builder");
		}
		if (!this.stateMachine.hasState(MEMBERS)) {
			throw new IllegalArgumentException("members missing in cluster context-builder");
		}

		ClusterContext context = new ClusterContext();
		context.id = this.id;
		context.authentication = this.authentication;
		context.channels = this.channels;
		context.type = this.type;
		context.prioritised = this.prioritised;
		context.handlerMap = this.handlerMap;
		context.publicationType = this.publicationType;
		context.members = this.members;
		context.ownAddress = this.ownAddress;

		context.createInternalCluster();
		return context;
	}
}
