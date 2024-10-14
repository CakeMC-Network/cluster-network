package net.cakemc.library.cluster.cache;

import net.cakemc.library.cluster.ClusterMember;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a default implementation of the {@link ClusterStore} interface, using a
 * {@link HashMap} to store and manage {@link ClusterMember} objects in a cluster. The {@code DefaultMemberStore}
 * acts as an in-memory store where each clusterMember is identified by a unique {@link Serializable} key, typically
 * representing the clusterMember's ID.
 *
 * <p>This class is suitable for systems where cluster membership information needs to be efficiently
 * updated, retrieved, and iterated over. It provides core functionality for managing cluster nodes,
 * including adding/updating nodes, retrieving a node by its ID, iterating over all nodes, and
 * closing the store by clearing the cache.</p>
 *
 * <p>The store can be easily extended or adapted for more complex storage mechanisms if needed.</p>
 *
 * @see ClusterMember
 * @see ClusterStore
 * @see ClusterMemberCallback
 */
public class DefaultMemberStore implements ClusterStore {

	/**
	 * The internal cache used to store the {@link ClusterMember} objects. Each clusterMember is keyed by its unique
	 * {@code Serializable} ID, which is retrieved from the clusterMember using {@link ClusterMember#getId()}.
	 */
	private final Map<Serializable, ClusterMember> cache;

	/**
	 * Constructs a new {@code DefaultMemberStore} with an empty cache.
	 *
	 * <p>Uses a {@link HashMap} to store members in memory, where the key is the clusterMember's ID and the
	 * value is the corresponding {@link ClusterMember} object.</p>
	 */
	public DefaultMemberStore() {
		this.cache = new HashMap<>();
	}

	/**
	 * Adds or updates a {@link ClusterMember} in the store. If the clusterMember with the same ID already exists, it will
	 * be replaced with the new one. Otherwise, the clusterMember is added to the store.
	 *
	 * @param node the {@link ClusterMember} object to be added or updated. The {@code ClusterMember}'s ID, retrieved by
	 *             {@link ClusterMember#getId()}, is used as the key in the cache. This parameter cannot be null.
	 *
	 * <p>Usage example:</p>
	 * <pre>
	 *   ClusterMember node = new ClusterMember(...);
	 *   memberStore.updateClusterMember(node);
	 * </pre>
	 */
	@Override
	public void updateClusterMember(ClusterMember node) {
		cache.put(node.getId(), node);
	}

	/**
	 * Retrieves a {@link ClusterMember} from the store using its unique ID.
	 *
	 * @param id the unique identifier of the {@link ClusterMember}, typically as a {@code short}. This ID is used
	 *           to look up the clusterMember in the cache.
	 * @return the {@link ClusterMember} associated with the given ID, or {@code null} if no clusterMember exists with
	 *         the specified ID.
	 *
	 * <p>Usage example:</p>
	 * <pre>
	 *   ClusterMember clusterMember = memberStore.getClusterMember(5);
	 * </pre>
	 */
	@Override
	public ClusterMember getClusterMember(short id) {
		return cache.get(id);
	}

	/**
	 * Clears the store by removing all {@link ClusterMember} objects from the cache.
	 *
	 * <p>This method should be called to release resources and prepare the store for shutdown. Once this
	 * method is called, the store is effectively empty, and any subsequent lookups will return {@code null}.</p>
	 *
	 * <p>Usage example:</p>
	 * <pre>
	 *   memberStore.close();
	 * </pre>
	 */
	@Override
	public void close() {
		cache.clear();
	}

	/**
	 * Iterates over all {@link ClusterMember} objects in the store and applies the specified callback to each.
	 * The {@code ClusterMemberCallback} is executed sequentially for each clusterMember in the store.
	 *
	 * @param callback the {@link ClusterMemberCallback} that is called for each {@link ClusterMember} in the store.
	 *                 This callback cannot be null.
	 *
	 * <p>Usage example:</p>
	 * <pre>
	 *   memberStore.forAll(clusterMember -> {
	 *       // Custom action for each clusterMember
	 *       System.out.println(clusterMember);
	 *   });
	 * </pre>
	 */
	@Override
	public void forAll(ClusterMemberCallback callback) {
		for (ClusterMember clusterMember : cache.values()) {
			callback.next(clusterMember);
		}
	}

	/**
	 * Retrieves a {@link Collection} of all {@link ClusterMember} objects currently stored in the cache.
	 * This method allows direct access to the underlying members without requiring iteration through
	 * a callback.
	 *
	 * @return a {@link Collection} of {@link ClusterMember} objects, or an empty collection if no members are present.
	 *
	 * <p>Usage example:</p>
	 * <pre>
	 *   Collection<ClusterMember> allMembers = memberStore.getAllMembers();
	 * </pre>
	 */
	public Collection<ClusterMember> getAllMembers() {
		return cache.values();
	}
}
