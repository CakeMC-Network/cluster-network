package net.cakemc.library.cluster;

import net.cakemc.library.cluster.ClusterMember.MemberState;
import net.cakemc.library.cluster.cache.ClusterStore;
import net.cakemc.library.cluster.cache.DefaultMemberStore;
import net.cakemc.library.cluster.cache.DefaultPublicationStore;
import net.cakemc.library.cluster.cache.PublicationStore;
import net.cakemc.library.cluster.codec.ClusterPublication;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.Command;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.SyncMode;
import net.cakemc.library.cluster.config.SyncConfig;
import net.cakemc.library.cluster.handler.ClusterPublicationHandler;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;
import net.cakemc.library.cluster.handler.SyncResult;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.util.*;

/**
 * Context for managing synchronization within a cluster.
 *
 * <p>This class is responsible for handling the synchronization of members in a cluster,
 * managing clusterMember states, and providing functionality for message storage and retrieval.</p>
 */
public class SyncContext extends Context {
	public final short ownId; // The identifier for this clusterMember
	public boolean inStartup = true; // Flag indicating if the context is in startup phase

	private final ClusterStore clusterStore; // Store for cluster members
	private final PublicationStore messageStore; // Store for publication messages

	public final SyncConfig config; // Configuration for synchronization
	public volatile long virtualLastModified = System.currentTimeMillis(); // Timestamp for last modification
	public volatile ClusterSnapshot snapshot = null; // Snapshot of the cluster's current state

	/**
	 * Constructs a new SyncContext with the given clusterMember ID and configuration.
	 *
	 * @param ownId the ID of the clusterMember
	 */
	public SyncContext(short ownId) {
		this(ownId, new SyncConfig());
	}

	public SyncContext(short ownId, SyncConfig config) {
		this.ownId = ownId;
		this.config = config;
		clusterStore = new DefaultMemberStore();
		messageStore = new DefaultPublicationStore();

		ClusterMember clusterMember = getMemberById(ownId);
		if (clusterMember == null) {
			final short[] awareIds = new short[0];
			clusterMember = new ClusterMember(ownId, null, true, "",
			                                  System.currentTimeMillis(),
			                                  awareIds, MemberState.VALID
			);
			updateMember(clusterMember);
		}
	}

	/**
	 * Creates a new SyncNetworkHandler for synchronization.
	 *
	 * @return a new instance of SyncNetworkHandler
	 */
	@Override public SyncNetworkHandler make() {
		SyncNetworkHandler sync = new SyncNetworkHandler(this);
		sync.selfMember = this.getOwnInfo();
		return sync;
	}

	/**
	 * Creates a new SyncNetworkHandler with a specified synchronization type.
	 *
	 * @param type the type of synchronization to perform
	 * @return a new instance of SyncNetworkHandler
	 */
	@Override public SyncNetworkHandler make(SynchronisationType type) {
		SyncNetworkHandler s = new SyncNetworkHandler(this, type);
		inStartup = false;
		return s;
	}

	/**
	 * Retrieves aware nodes for a specific message key and version.
	 *
	 * @param key the message key
	 * @param version the version number
	 * @return an array of aware node identifiers
	 */
	@Override public short[] getAwareNodesRaw(String key, long version) {
		return messageStore.getAwareNodes(key, version);
	}

	/**
	 * Retrieves aware nodes for a specific message key and version.
	 *
	 * @param key the message key
	 * @param version the version number
	 * @return an array of aware node identifiers
	 */
	@Override public ClusterIdRegistry getAwareNodes(String key, long version) {
		return new ClusterIdRegistry(messageStore.getAwareNodes(key, version));
	}

	/**
	 * Updates the aware nodes for a specific message key and version.
	 *
	 * @param key the message key
	 * @param version the version number
	 * @param awareNodes an array of aware node identifiers
	 */
	@Override public void addAwareNodesRaw(String key, long version, short[] awareNodes) {
		messageStore.updateAwareNodes(key, version, awareNodes);
	}

	@Override
	public void addAwareNodes(String key, long version, ClusterIdRegistry awareNodes) {
		messageStore.updateAwareNodes(key, version, awareNodes.getIds());
	}

	/**
	 * Retrieves a clusterMember by its identifier.
	 *
	 * @param id the identifier of the clusterMember
	 * @return the ClusterMember object, or null if not found
	 */
	@Override public ClusterMember getMemberById(short id) {
		return clusterStore.getClusterMember(id);
	}

	/**
	 * Updates the state of a clusterMember in the cluster.
	 *
	 * @param clusterMember the ClusterMember object to update
	 */
	@Override public void updateMember(ClusterMember clusterMember) {
		if (clusterMember == null) {
			return;
		}
		clusterMember.addAwareId(ownId);

		if (!clusterMember.getAwareIds().contains(clusterMember.getId())) {
			ClusterMember mem = clusterStore.getClusterMember(clusterMember.getId());
			if (mem != null && !mem.getKey().equals(clusterMember.getKey())) {
				clusterMember.addKeyChain(mem.getKeyChain());
			}
		}
		virtualLastModified = System.currentTimeMillis();
		clusterStore.updateClusterMember(clusterMember);
		invalidateSnapshot();
	}

	/**
	 * Synchronizes the cluster with a specific clusterMember.
	 *
	 * @param node the clusterMember to synchronize with
	 * @return true if synchronization was successful, false otherwise
	 */
	@Override public boolean syncCluster(ClusterMember node) {
		return syncCluster(node, SynchronisationType.UNI_CAST_BALANCE);
	}

	/**
	 * Synchronizes the cluster with a specific clusterMember using a specified synchronization type.
	 *
	 * @param node the clusterMember to synchronize with
	 * @param withType the type of synchronization to use
	 * @return true if synchronization was successful, false otherwise
	 */
	@Override public boolean syncCluster(ClusterMember node, SynchronisationType withType) {
		if (node == null) {
			return false;
		}

		updateMember(node);
		SyncNetworkHandler handler = make(withType)
			 .withoutCluster(ownId)
			 .withCallBack(new ClusterPublicationHandler(this));

		handler.mode = SyncMode.SYNC_CLUSTER;
		final List<Publication> messages = new ArrayList<>();
		ClusterSnapshot snapshot = getSnapshot();

		if (snapshot != null) {
			for (ClusterMember n : snapshot.cluster) {
				ClusterPublication msg = new ClusterPublication(n.getId(),
				                                                n.isAuthByKey(), n.getKey(),
				                                                n.getLastModified(), n.getSyncAddresses(),
				                                                n.isValid() ? Command.COMMAND_TAKE_THIS :
				                                                Command.COMMAND_DEL_THIS
				);
				messages.add(msg);
			}
		}

		Map<String, SyncResult> feature = handler.sync(messages).get();
		if (feature == null) {
			if (node.getId() == ownId) {
				inStartup = false;
				return true;
			}
			return false;
		}
		if (feature.get(String.valueOf(node.getId())).isSuccessful()) {
			inStartup = false;
			return true;
		}
		return false;
	}

	/**
	 * Checks if the cluster is fully synchronized for a specific key.
	 *
	 * @param key the key to check synchronization status
	 * @return true if fully synchronized, false otherwise
	 */
	@Override public boolean isFullySynced(String key) {
		return false; // Placeholder for actual implementation
	}

	/**
	 * Checks if a quorum is synchronized for a specific key.
	 *
	 * @param key the key to check synchronization status
	 * @return true if quorum is synchronized, false otherwise
	 */
	@Override public boolean isQuorumSynced(String key) {
		return false; // Placeholder for actual implementation
	}

	/**
	 * Retrieves the current snapshot of the cluster.
	 *
	 * @return the current ClusterSnapshot
	 */
	@Override public ClusterSnapshot getSnapshot() {
		if (snapshot != null) {
			return snapshot;
		}

		final ClusterSnapshot tmpMonitor = new ClusterSnapshot();
		clusterStore.forAll(node -> {
			if (node.isValid()) {
				tmpMonitor.validClusterIDs.add(node.getId());
				tmpMonitor.validCluster.add(node);
				tmpMonitor.aliveCluster.add(node);
			} else if (!node.isDown()) {
				tmpMonitor.aliveCluster.add(node);
				if (node.getId() != ownId) {
					tmpMonitor.inValidClusterIDs.add(node.getId());
				}
			} else if (node.getId() != ownId) {
				tmpMonitor.inValidClusterIDs.add(node.getId());
			}
			tmpMonitor.cluster.add(node);
			tmpMonitor.idClusterMap.put(node.getId(), node);
		});

		return snapshot = tmpMonitor;
	}

	/**
	 * Invalidates the current snapshot of the cluster.
	 */
	private void invalidateSnapshot() {
		snapshot = null;
	}

	/**
	 * Changes the state of a clusterMember and updates the cluster snapshot.
	 *
	 * @param node the clusterMember whose state is changing
	 * @param state the new state of the clusterMember
	 */
	@Override public void synchronizedStateChange(ClusterMember node, MemberState state) {
		node.setState(state);
		invalidateSnapshot();
		virtualLastModified = System.currentTimeMillis();
	}

	/**
	 * Retrieves information about this clusterMember.
	 *
	 * @return the ClusterMember object representing this clusterMember
	 */
	@Override public ClusterMember getOwnInfo() {
		return clusterStore.getClusterMember(ownId);
	}

	/**
	 * Resets the key for a clusterMember identified by its ID.
	 *
	 * @param id the ID of the clusterMember
	 * @param key the new key to set
	 * @return true if the key was successfully reset, false otherwise
	 */
	@Override public boolean resetNodeKeyById(short id, String key) {
		ClusterMember node = getMemberById(id);
		if (node != null && key != null) {
			if (!node.isValid()) {
				throw new IllegalStateException("node is invalid state");
			}

			if (node.getKey().equals(key)) {
				return true;
			}
			node.resetKey(key);
			clusterStore.updateClusterMember(node);
		}
		return false;
	}

	/**
	 * Checks if the context is in the startup phase.
	 *
	 * @return true if in startup, false otherwise
	 */
	@Override public boolean isInStartup() {
		return this.inStartup;
	}

	/**
	 * Retrieves the last modification timestamp of the cluster.
	 *
	 * @return the last modified timestamp
	 */
	@Override public long getClusterLastModified() {
		return virtualLastModified;
	}

	/**
	 * Sets the last modification timestamp for the cluster.
	 *
	 * @param virtualLastModified the new last modified timestamp
	 */
	@Override public void setVirtualLastModified(long virtualLastModified) {
		this.virtualLastModified = virtualLastModified;
	}

	/**
	 * Retrieves the synchronization configuration.
	 *
	 * @return the SyncConfig object
	 */
	@Override public SyncConfig getConfig() {
		return config;
	}

	@Override public void setInStartup(boolean inStartup) {
		this.inStartup = inStartup;
	}

}
