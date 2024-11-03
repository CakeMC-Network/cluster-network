package net.cakemc.library.cluster.codec;

import net.cakemc.library.cluster.address.ClusterIdRegistry;

/**
 * Represents the synchronization content within the cluster framework.
 *
 * <p>The {@code SyncContent} class encapsulates data necessary for synchronizing
 * information across cluster nodes, including a unique key, version number,
 * aware node identifiers, and the associated content.</p>
 */
public class SyncContent {

	private final String key;                          // Unique identifier for the sync content
	private final long version;                        // Version number of the sync content
	private final ClusterIdRegistry awareIds;          // Array of aware node identifiers
	private final byte[] content;                      // Associated content in byte array format

	/**
	 * Constructs a {@code SyncContent} object with the specified parameters.
	 *
	 * @param key      the unique key identifying this sync content
	 * @param version  the version number of this sync content
	 * @param awareIds array of aware node identifiers
	 * @param content  the associated content in byte array format
	 */
	public SyncContent(String key, long version, ClusterIdRegistry awareIds, byte[] content) {
		this.key = key;
		this.version = version;
		this.awareIds = awareIds;
		this.content = content;
	}

	/**
	 * Retrieves the unique key associated with this sync content.
	 *
	 * @return the unique key for this sync content
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Retrieves the version number of this sync content.
	 *
	 * @return the version number of this sync content
	 */
	public long getVersion() {
		return version;
	}

	/**
	 * Retrieves the array of aware node identifiers.
	 *
	 * @return an array of aware node identifiers
	 */
	public short[] getAwareIdsRaw() {
		return awareIds.getIds();
	}

	/**
	 * Retrieves the array of aware node identifiers.
	 *
	 * @return an array of aware node identifiers
	 */
	public ClusterIdRegistry getAwareIds() {
		return awareIds;
	}

	/**
	 * Adds a single aware node identifier to the existing array of aware IDs.
	 *
	 * <p>If the aware IDs array is null, this method does nothing.</p>
	 *
	 * @param node the aware node identifier to add
	 */
	public void addAwareId(short node) {
		if (awareIds == null) {
			return;
		}
		awareIds.addAll(node);
	}

	/**
	 * Adds multiple aware node identifiers to the existing array of aware IDs.
	 *
	 * <p>If the aware IDs array is null, this method does nothing.</p>
	 *
	 * @param nodes an array of aware node identifiers to add
	 */
	public void addAwareId(short[] nodes) {
		if (awareIds == null) {
			return;
		}

		awareIds.addAll(nodes);
	}

	/**
	 * Adds multiple aware node identifiers to the existing array of aware IDs.
	 *
	 * <p>If the aware IDs array is null, this method does nothing.</p>
	 *
	 * @param nodes an array of aware node identifiers to add
	 */
	public void addAwareId(ClusterIdRegistry nodes) {
		if (awareIds == null) {
			return;
		}

		awareIds.addAll(nodes.getIds());
	}

	/**
	 * Retrieves the associated content as a byte array.
	 *
	 * @return the associated content in byte array format
	 */
	public byte[] getContent() {
		return content;
	}

	@Override
	public int hashCode() {
		return this.getKey().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SyncContent s) {
			if (s.getKey() == null || getKey() == null) {
				return false;
			}
			return s.getKey().equals(getKey());
		}
		return false;
	}

	@Override
	public String toString() {
		return "key=" + key +
		       ", version=" + version +
		       ", awareNodes=" + this.awareIds;
	}
}
