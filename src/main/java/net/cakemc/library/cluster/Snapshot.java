package net.cakemc.library.cluster;

import java.util.List;

public abstract class Snapshot {
	public abstract ClusterMember getById(short id, int memberCheck);

	public abstract short[] getValidClusterIDs();

	public abstract short[] getInValidClusterIDs();

	public abstract List<ClusterMember> getValidCluster();

	public abstract List<ClusterMember> getAliveCluster();

	public abstract List<ClusterMember> getCluster();
}
