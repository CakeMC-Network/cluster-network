package net.cakemc.library.cluster;

import net.cakemc.library.cluster.address.ClusterAddress;
import net.cakemc.library.cluster.handler.SyncNetworkHandler;
import net.cakemc.library.cluster.network.NetworkClient;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The type ClusterMember.
 */
public class ClusterMember implements Node {

	/**
	 * The enum ClusterMember state.
	 */
// Define an enum for clusterMember states
	public enum MemberState {
		/**
		 * Deleted clusterMember state.
		 */
		DELETED(0), // STATE_DEL
		/**
		 * Valid clusterMember state.
		 */
		VALID(1),   // STATE_VLD
		/**
		 * Down clusterMember state.
		 */
		DOWN(2);    // STATE_DWN

		private final byte value;

		MemberState(int value) {
			this.value = (byte) value;
		}

		/**
		 * Gets value.
		 *
		 * @return the value
		 */
		public byte getValue() {
			return value;
		}

		/**
		 * From value clusterMember state.
		 *
		 * @param value the value
		 *
		 * @return the clusterMember state
		 */
		public static MemberState fromValue(byte value) {
			for (MemberState state : MemberState.values()) {
				if (state.getValue() == value) {
					return state;
				}
			}
			throw new IllegalArgumentException("Invalid state value: " + value);
		}
	}

	/**
	 * The constant SYNCED.
	 */
	public static final byte SYNCED = 0;
	/**
	 * The constant NOT_SYNCED.
	 */
	public static final byte NOT_SYNCED = -1;
	/**
	 * The constant MONITOR_INTERVAL.
	 */
	public static final short MONITOR_INTERVAL = 60;
	/**
	 * The constant MONITOR_DELAY.
	 */
	public static final short MONITOR_DELAY = 10;

	private final ReentrantReadWriteLock idsLock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock keysLock = new ReentrantReadWriteLock();
	private Set<ClusterAddress> syncAddresses;
	private byte version = 0;
	private short id;
	private boolean authByKey = true;
	private LinkedList<String> keyChain;
	private volatile long lastModified = 0;
	private ClusterIdRegistry awareIds = null;
	private MemberState state; // Changed to use enum
	private String name = "";
	private short schedule = NOT_SYNCED;
	private short monitorInterval = MONITOR_INTERVAL;
	private short monitorDelay = MONITOR_DELAY;
	private short reportInterval = 0;
	private short reportDelay = 0;
	private int currentSocketIndex = 0;

	/**
	 * Instantiates a new ClusterMember.
	 */
	public ClusterMember() {
	}

	/**
	 * Instantiates a new ClusterMember.
	 *
	 * @param id            the id
	 * @param syncAddresses the sync addresses
	 * @param authByKey     the auth by key
	 * @param key           the key
	 * @param lastModified  the last modified
	 * @param awareIds      the aware ids
	 * @param state         the state
	 */
	public ClusterMember(
		 final short id,
		 final Set<ClusterAddress> syncAddresses,
		 final boolean authByKey,
		 final String key,
		 final long lastModified,
		 final short[] awareIds,
		 final MemberState state // Changed to use enum
	) {
		this.id = id;
		this.syncAddresses = syncAddresses;
		this.authByKey = authByKey;
		this.keyChain = new LinkedList<>();
		this.keyChain.add(key);
		this.awareIds = new ClusterIdRegistry(awareIds);
		this.state = state;
		this.lastModified = lastModified;
		this.name = createName(id);
	}

	/**
	 * Instantiates a new ClusterMember.
	 *
	 * @param id            the id
	 * @param syncAddresses the sync addresses
	 * @param authByKey     the auth by key
	 * @param key           the key
	 * @param lastModified  the last modified
	 * @param awareIds      the aware ids
	 * @param state         the state
	 */
	public ClusterMember(
		 final short id,
		 final Set<ClusterAddress> syncAddresses,
		 final boolean authByKey,
		 final String key,
		 final long lastModified,
		 final ClusterIdRegistry awareIds,
		 final MemberState state // Changed to use enum
	) {
		this.id = id;
		this.syncAddresses = syncAddresses;
		this.authByKey = authByKey;
		this.keyChain = new LinkedList<>();
		this.keyChain.add(key);
		this.awareIds = awareIds;
		this.state = state;
		this.lastModified = lastModified;
		this.name = createName(id);
	}

	/**
	 * Instantiates a new ClusterMember.
	 *
	 * @param id            the id
	 * @param syncAddresses the sync addresses
	 * @param authByKey     the auth by key
	 * @param key           the key
	 * @param lastModified  the last modified
	 * @param awareIds      the aware ids
	 * @param state         the state
	 */
	public ClusterMember(
		 final short id,
		 final List<ClusterAddress> syncAddresses,
		 final boolean authByKey,
		 final String key,
		 final long lastModified,
		 final ClusterIdRegistry awareIds,
		 final MemberState state // Changed to use enum
	) {
		this.id = id;
		this.syncAddresses = new HashSet<>(syncAddresses);
		this.authByKey = authByKey;
		this.keyChain = new LinkedList<>();
		this.keyChain.add(key);
		this.awareIds = awareIds;
		this.state = state;
		this.lastModified = lastModified;
		this.name = createName(id);
	}

	private String createName(short id) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String text = "Node-" + id * 256;
			md.update(text.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();
			return encodeToHexString(digest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Encode to hex string string.
	 *
	 * @param bytes the bytes
	 *
	 * @return the string
	 */
	public String encodeToHexString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(b & 0xFF);
			if (hex.length() < 2) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	/**
	 * Create synch session network client.
	 *
	 * @param handler the handler
	 *
	 * @return the network client
	 */
	public NetworkClient createSynchSession(SyncNetworkHandler handler) {
		return new NetworkClient(this, handler);
	}

	/**
	 * Gets sync addresses.
	 *
	 * @return the sync addresses
	 */
	public Set<ClusterAddress> getSyncAddresses() {
		return syncAddresses;
	}

	/**
	 * Sets sync addresses.
	 *
	 * @param syncAddresses the sync addresses
	 */
	public void setSyncAddresses(Set<ClusterAddress> syncAddresses) {
		this.syncAddresses = syncAddresses;
	}

	@Override
	public short getId() {
		return id;
	}

	/**
	 * Is auth by key boolean.
	 *
	 * @return the boolean
	 */
	public boolean isAuthByKey() {
		return authByKey;
	}

	/**
	 * Sets authenticate by key.
	 *
	 * @param authByKey the auth by key
	 */
	protected void setAuthenticateByKey(boolean authByKey) {
		this.authByKey = authByKey;
	}

	/**
	 * Gets key.
	 *
	 * @return the key
	 *
	 * @throws NoSuchElementException the no such element exception
	 */
	public String getKey() throws NoSuchElementException {
		try {
			keysLock.readLock().lock();
			return keyChain.getFirst();
		} catch (Exception e) {
			throw e;
		} finally {
			keysLock.readLock().unlock();
		}
	}

	/**
	 * Gets key chain.
	 *
	 * @return the key chain
	 */
	public List<String> getKeyChain() {
		try {
			keysLock.readLock().lock();
			return keyChain;
		} finally {
			keysLock.readLock().unlock();
		}
	}

	/**
	 * Add key.
	 *
	 * @param key the key
	 */
	public void addKey(String key) {
		try {
			keysLock.writeLock().lock();
			keyChain.addFirst(key);
		} finally {
			keysLock.writeLock().unlock();
		}
	}

	/**
	 * Add key chain.
	 *
	 * @param keys the keys
	 */
	public void addKeyChain(List<String> keys) {
		try {
			keysLock.writeLock().lock();
			keyChain.addAll(keyChain.size(), keys);
		} finally {
			keysLock.writeLock().unlock();
		}
	}

	/**
	 * Reset key.
	 *
	 * @param key the key
	 */
	public void resetKey(String key) {
		try {
			keysLock.writeLock().lock();
			keyChain.clear();
			keyChain.addFirst(key);
		} finally {
			keysLock.writeLock().unlock();
		}
	}

	/**
	 * Gets last modified.
	 *
	 * @return the last modified
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Sets last modified.
	 *
	 * @param lastModified the last modified
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Get aware ids short [ ].
	 *
	 * @return the short [ ]
	 */
	public ClusterIdRegistry getAwareIds() {
		try {
			idsLock.readLock().lock();
			return awareIds;
		} finally {
			idsLock.readLock().unlock();
		}
	}

	/**
	 * Sets aware ids.
	 *
	 * @param awareIds the aware ids
	 */
	public void setAwareIds(short[] awareIds) {
		this.awareIds = new ClusterIdRegistry(awareIds);
	}


	/**
	 * Add aware id boolean.
	 *
	 * @param id the id
	 *
	 * @return the boolean
	 */
	public boolean addAwareId(final short id) {
		try {
			idsLock.writeLock().lock();

			if (this.awareIds == null) {
				this.awareIds = new ClusterIdRegistry(id);
				return true;
			}
			if (awareIds.contains(id)) {
				return false;
			}

			return awareIds.add(id);

		} finally {
			idsLock.writeLock().unlock();
		}
	}

	/**
	 * Add aware id boolean.
	 *
	 * @param ids the ids
	 *
	 * @return the boolean
	 */
	public boolean addAwareIdRaw(final short[] ids) {
		try {
			idsLock.writeLock().lock();

			if (this.awareIds == null) {
				this.awareIds = new ClusterIdRegistry(ids);
				return true;
			}

			return this.awareIds.addAll(ids);
		} finally {
			idsLock.writeLock().unlock();
		}
	}

	/**
	 * Add aware id boolean.
	 *
	 * @param ids the ids
	 *
	 * @return the boolean
	 */
	public boolean addAwareId(final ClusterIdRegistry ids) {
		try {
			idsLock.writeLock().lock();

			if (this.awareIds == null) {
				this.awareIds = ids;
				return true;
			}

			return this.awareIds.addAll(ids.getIds());
		} finally {
			idsLock.writeLock().unlock();
		}
	}

	/**
	 * Gets state.
	 *
	 * @return the state
	 */
	public MemberState getState() {
		return state;
	}

	/**
	 * Sets state.
	 *
	 * @param state the state
	 */
	protected void setState(MemberState state) {
		this.state = state;
	}

	/**
	 * Is deleted boolean.
	 *
	 * @return the boolean
	 */
	public boolean isDeleted() {
		return state == MemberState.DELETED;
	}

	/**
	 * Is down boolean.
	 *
	 * @return the boolean
	 */
	public boolean isDown() {
		return state == MemberState.DOWN;
	}

	/**
	 * Is valid boolean.
	 *
	 * @return the boolean
	 */
	public boolean isValid() {
		return state == MemberState.VALID;
	}

	/**
	 * Is fully scheduled boolean.
	 *
	 * @return the boolean
	 */
	public boolean isFullyScheduled() {
		return schedule == SYNCED;
	}

	/**
	 * Is scheduled boolean.
	 *
	 * @return the boolean
	 */
	public boolean isScheduled() {
		return schedule > NOT_SYNCED;
	}

	/**
	 * Sets scheduled.
	 *
	 * @param scheduled the scheduled
	 */
	public void setScheduled(boolean scheduled) {
		this.schedule = scheduled ? SYNCED : NOT_SYNCED;
	}

	/**
	 * Gets schedule.
	 *
	 * @return the schedule
	 */
	public short getSchedule() {
		return schedule;
	}

	/**
	 * Sets schedule.
	 *
	 * @param schedule the schedule
	 */
	public void setSchedule(short schedule) {
		this.schedule = schedule;
	}

	@Override
	public String toString() {
		return "id=" + this.id +
		       ", state=" + state +
		       ", version=" + lastModified +
		       ", sync addrs:" + syncAddresses +
		       ", monitor delay=" + monitorDelay +
		       ", monitor interval=" + monitorInterval +
		       ", report delay=" + reportDelay +
		       ", report interval=" + reportInterval;
	}

	@Override public byte getVersion() {
		return version;
	}

	@Override public void setVersion(byte version) {
		this.version = version;
	}

	/**
	 * Gets ids lock.
	 *
	 * @return the ids lock
	 */
	public ReentrantReadWriteLock getIdsLock() {
		return idsLock;
	}

	/**
	 * Gets keys lock.
	 *
	 * @return the keys lock
	 */
	public ReentrantReadWriteLock getKeysLock() {
		return keysLock;
	}

	/**
	 * Sets id.
	 *
	 * @param id the id
	 */
	public void setId(short id) {
		this.id = id;
	}

	/**
	 * Sets auth by key.
	 *
	 * @param authByKey the auth by key
	 */
	public void setAuthByKey(boolean authByKey) {
		this.authByKey = authByKey;
	}

	/**
	 * Sets key chain.
	 *
	 * @param keyChain the key chain
	 */
	public void setKeyChain(LinkedList<String> keyChain) {
		this.keyChain = keyChain;
	}

	/**
	 * Gets name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets name.
	 *
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets monitor interval.
	 *
	 * @return the monitor interval
	 */
	public short getMonitorInterval() {
		return monitorInterval;
	}

	/**
	 * Sets monitor interval.
	 *
	 * @param monitorInterval the monitor interval
	 */
	public void setMonitorInterval(short monitorInterval) {
		this.monitorInterval = monitorInterval;
	}

	/**
	 * Gets monitor delay.
	 *
	 * @return the monitor delay
	 */
	public short getMonitorDelay() {
		return monitorDelay;
	}

	/**
	 * Sets monitor delay.
	 *
	 * @param monitorDelay the monitor delay
	 */
	public void setMonitorDelay(short monitorDelay) {
		this.monitorDelay = monitorDelay;
	}

	/**
	 * Gets report interval.
	 *
	 * @return the report interval
	 */
	public short getReportInterval() {
		return reportInterval;
	}

	/**
	 * Sets report interval.
	 *
	 * @param reportInterval the report interval
	 */
	public void setReportInterval(short reportInterval) {
		this.reportInterval = reportInterval;
	}

	/**
	 * Gets report delay.
	 *
	 * @return the report delay
	 */
	public short getReportDelay() {
		return reportDelay;
	}

	/**
	 * Sets report delay.
	 *
	 * @param reportDelay the report delay
	 */
	public void setReportDelay(short reportDelay) {
		this.reportDelay = reportDelay;
	}

	/**
	 * Gets current socket index.
	 *
	 * @return the current socket index
	 */
	public int getCurrentSocketIndex() {
		return currentSocketIndex;
	}

	/**
	 * Sets current socket index.
	 *
	 * @param currentSocketIndex the current socket index
	 */
	public void setCurrentSocketIndex(int currentSocketIndex) {
		this.currentSocketIndex = currentSocketIndex;
	}
}
