package net.cakemc.library.cluster;

import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.util.*;

/**
 * Represents a snapshot of the cluster's state, capturing information about the members
 * and their statuses, including valid, invalid, and alive members.
 *
 * <p>The `ClusterSnapshot` class provides methods to access different views of the cluster,
 * such as the set of valid, invalid, and alive members, as well as a map for quick lookup
 * of members by their ID.</p>
 *
 * <p>Each clusterMember of the cluster is represented by a {@link ClusterMember} object, and the snapshot
 * allows for filtering members based on different checks, such as whether a clusterMember is valid
 * or marked as down.</p>
 */
public class ClusterSnapshot extends Snapshot {

	// Constants for different clusterMember validation checks
	public final static int MEMBER_CHECK_NONE = 0;
	public final static int MEMBER_CHECK_VALID_OR_DOWN = 1;
	public final static int MEMBER_CHECK_VALID = 2;

	// Arrays holding the IDs of valid and invalid cluster members
	public ClusterIdRegistry validClusterIDs;
	public ClusterIdRegistry inValidClusterIDs;

	// Lists holding the valid, alive, and all members of the cluster
	public List<ClusterMember> validCluster;
	public List<ClusterMember> aliveCluster;
	public List<ClusterMember> cluster;

	// Map for quick lookup of members by their ID
	public Map<Short, ClusterMember> idClusterMap;

	/**
	 * Constructs a new `ClusterSnapshot` with empty lists and maps for managing the cluster members.
	 */
	public ClusterSnapshot() {
		validCluster = new ArrayList<>();
		aliveCluster = new ArrayList<>();
		cluster = new ArrayList<>();
		validClusterIDs = new ClusterIdRegistry();
		inValidClusterIDs = new ClusterIdRegistry();
		idClusterMap = new HashMap<>();
	}

	/**
	 * Retrieves a {@link ClusterMember} from the cluster by its ID, applying an optional clusterMember check.
	 *
	 * @param id the ID of the clusterMember to retrieve
	 * @param memberCheck one of the clusterMember check constants to apply (e.g., {@link #MEMBER_CHECK_VALID})
	 * @return the {@link ClusterMember} object if it exists and passes the check, or null otherwise
	 */
	@Override public ClusterMember getById(short id, int memberCheck) {
		ClusterMember clusterMember = idClusterMap.get(id);
		if (clusterMember == null) {
			return null;
		}
		if (memberCheck == MEMBER_CHECK_NONE) {
			return clusterMember;
		} else if (memberCheck == MEMBER_CHECK_VALID_OR_DOWN) {
			if (clusterMember.isValid() || clusterMember.isDown()) {
				return clusterMember;
			}
		}
		if (clusterMember.isValid()) {
			return clusterMember;
		}
		return null;
	}

	/**
	 * Retrieves the IDs of members marked as valid.
	 * A valid clusterMember is one that is neither deleted nor marked as down.
	 *
	 * @return an array of IDs of valid members
	 */
	@Override public short[] getValidClusterIDs() {
		return validClusterIDs.getIds();
	}

	/**
	 * Retrieves the IDs of members marked as invalid.
	 * A valid clusterMember is one that is neither deleted nor marked as down.
	 *
	 * @return an array of IDs of invalid members
	 */
	@Override public short[] getInValidClusterIDs() {
		return inValidClusterIDs.getIds();
	}

	/**
	 * Retrieves a list of members marked as valid.
	 * A valid clusterMember is one that is neither deleted nor marked as down.
	 *
	 * @return a list of valid {@link ClusterMember} objects
	 */
	@Override public List<ClusterMember> getValidCluster() {
		return validCluster;
	}

	/**
	 * Retrieves a list of members that are alive.
	 * A clusterMember is considered alive if it is not marked as down,
	 * meaning even a deleted clusterMember can still be alive.
	 *
	 * @return a list of alive {@link ClusterMember} objects
	 */
	@Override public List<ClusterMember> getAliveCluster() {
		return aliveCluster;
	}

	/**
	 * Retrieves the full list of members in the cluster.
	 *
	 * @return a list of all {@link ClusterMember} objects in the cluster
	 */
	@Override public List<ClusterMember> getCluster() {
		return cluster;
	}
}
