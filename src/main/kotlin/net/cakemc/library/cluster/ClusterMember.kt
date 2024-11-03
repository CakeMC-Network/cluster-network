package net.cakemc.library.cluster

import net.cakemc.library.cluster.address.ClusterAddress
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.handler.SyncNetworkHandler
import net.cakemc.library.cluster.network.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.Volatile

/**
 * The type ClusterMember.
 */
class ClusterMember : Node {
  /**
   * The enum ClusterMember state.
   */
  // Define an enum for clusterMember states
  enum class MemberState(value: Int) {
    /**
     * Deleted clusterMember state.
     */
    DELETED(0),  // STATE_DEL

    /**
     * Valid clusterMember state.
     */
    VALID(1),  // STATE_VLD

    /**
     * Down clusterMember state.
     */
    DOWN(2); // STATE_DWN

    /**
     * Gets value.
     *
     * @return the value
     */
    val value: Byte = value.toByte()

    companion object {
      /**
       * From value clusterMember state.
       *
       * @param value the value
       *
       * @return the clusterMember state
       */
      fun fromValue(value: Byte): MemberState {
        for (state in entries) {
          if (state.value == value) {
            return state
          }
        }
        throw IllegalArgumentException("Invalid state value: $value")
      }
    }
  }

  /**
   * Gets ids lock.
   *
   * @return the ids lock
   */
  val idsLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  /**
   * Gets keys lock.
   *
   * @return the keys lock
   */
  val keysLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  /**
   * Gets sync addresses.
   *
   * @return the sync addresses
   */
  var syncAddresses: MutableSet<ClusterAddress>? = null
  override var version: Byte = 0

  /**
   * Sets id.
   *
   * @param id the id
   */
  override var id: Short = 0
  /**
   * Is auth by key boolean.
   *
   * @return the boolean
   */
  /**
   * Sets auth by key.
   *
   * @param authByKey the auth by key
   */
  var isAuthByKey: Boolean = true
  var keyChain: LinkedList<String?>? = null
  /**
   * Gets last modified.
   *
   * @return the last modified
   */
  /**
   * Sets last modified.
   *
   * @param lastModified the last modified
   */
  @Volatile
  var lastModified: Long = 0
  var awareIds: ClusterIdRegistry? = null
  /**
   * Gets state.
   *
   * @return the state
   */
  /**
   * Sets state.
   *
   * @param state the state
   */
  var state: MemberState? = null // Changed to use enum
  /**
   * Gets name.
   *
   * @return the name
   */
  /**
   * Sets name.
   *
   * @param name the name
   */
  var name: String = ""
  /**
   * Gets schedule.
   *
   * @return the schedule
   */
  /**
   * Sets schedule.
   *
   * @param schedule the schedule
   */
  var schedule: Short = NOT_SYNCED.toShort()
  /**
   * Gets monitor interval.
   *
   * @return the monitor interval
   */
  /**
   * Sets monitor interval.
   *
   * @param monitorInterval the monitor interval
   */
  var monitorInterval: Short = MONITOR_INTERVAL
  /**
   * Gets monitor delay.
   *
   * @return the monitor delay
   */
  /**
   * Sets monitor delay.
   *
   * @param monitorDelay the monitor delay
   */
  var monitorDelay: Short = MONITOR_DELAY
  /**
   * Gets report interval.
   *
   * @return the report interval
   */
  /**
   * Sets report interval.
   *
   * @param reportInterval the report interval
   */
  var reportInterval: Short = 0
  /**
   * Gets report delay.
   *
   * @return the report delay
   */
  /**
   * Sets report delay.
   *
   * @param reportDelay the report delay
   */
  var reportDelay: Short = 0
  /**
   * Gets current socket index.
   *
   * @return the current socket index
   */
  /**
   * Sets current socket index.
   *
   * @param currentSocketIndex the current socket index
   */
  var currentSocketIndex: Int = 0

  /**
   * Instantiates a new ClusterMember.
   */
  constructor()

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
  constructor(
    id: Short,
    syncAddresses: MutableSet<ClusterAddress>?,
    authByKey: Boolean,
    key: String?,
    lastModified: Long,
    awareIds: ShortArray,
    state: MemberState? // Changed to use enum
  ) {
    this.id = id
    this.syncAddresses = syncAddresses
    this.isAuthByKey = authByKey
    this.keyChain = LinkedList()
    keyChain!!.add(key)
    this.awareIds = ClusterIdRegistry(*awareIds)
    this.state = state
    this.lastModified = lastModified
    this.name = createName(id)
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
  constructor(
    id: Short,
    syncAddresses: MutableSet<ClusterAddress>?,
    authByKey: Boolean,
    key: String?,
    lastModified: Long,
    awareIds: ClusterIdRegistry?,
    state: MemberState? // Changed to use enum
  ) {
    this.id = id
    this.syncAddresses = syncAddresses
    this.isAuthByKey = authByKey
    this.keyChain = LinkedList()
    keyChain!!.add(key)
    this.awareIds = awareIds
    this.state = state
    this.lastModified = lastModified
    this.name = createName(id)
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
  constructor(
    id: Short,
    syncAddresses: List<ClusterAddress?>,
    authByKey: Boolean,
    key: String?,
    lastModified: Long,
    awareIds: ClusterIdRegistry?,
    state: MemberState? // Changed to use enum
  ) {
    this.id = id
    this.syncAddresses = HashSet(syncAddresses)
    this.isAuthByKey = authByKey
    this.keyChain = LinkedList()
    keyChain!!.add(key)
    this.awareIds = awareIds
    this.state = state
    this.lastModified = lastModified
    this.name = createName(id)
  }

  private fun createName(id: Short): String {
    try {
      val md = MessageDigest.getInstance("SHA-256")
      val text = "Node-" + id * 256
      md.update(text.toByteArray(StandardCharsets.UTF_8))
      val digest = md.digest()
      return encodeToHexString(digest)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return ""
  }

  /**
   * Encode to hex string string.
   *
   * @param bytes the bytes
   *
   * @return the string
   */
  fun encodeToHexString(bytes: ByteArray): String {
    val hexString = StringBuilder()
    for (b in bytes) {
      val hex = Integer.toHexString(b.toInt() and 0xFF)
      if (hex.length < 2) {
        hexString.append('0')
      }
      hexString.append(hex)
    }
    return hexString.toString()
  }

  /**
   * Create synch session network client.
   *
   * @param handler the handler
   *
   * @return the network client
   */
  fun createSynchSession(handler: SyncNetworkHandler?): NetworkClient {
    return NetworkClient(this, handler)
  }

  @get:Throws(NoSuchElementException::class)
  val key: String?
    /**
     * Gets key.
     *
     * @return the key
     *
     * @throws NoSuchElementException the no such element exception
     */
    get() {
      try {
        keysLock.readLock().lock()
        return keyChain!!.first
      } catch (e: Exception) {
        throw e
      } finally {
        keysLock.readLock().unlock()
      }
    }

  /**
   * Gets key chain.
   *
   * @return the key chain
   */
  fun getKeyChainLock(): List<String?>? {
    try {
      keysLock.readLock().lock()
      return keyChain
    } finally {
      keysLock.readLock().unlock()
    }
  }

  /**
   * Add key.
   *
   * @param key the key
   */
  fun addKey(key: String?) {
    try {
      keysLock.writeLock().lock()
      keyChain!!.addFirst(key)
    } finally {
      keysLock.writeLock().unlock()
    }
  }

  /**
   * Add key chain.
   *
   * @param keys the keys
   */
  fun addKeyChain(keys: List<String?>?) {
    try {
      keysLock.writeLock().lock()
      keyChain!!.addAll(keyChain!!.size, keys!!)
    } finally {
      keysLock.writeLock().unlock()
    }
  }

  /**
   * Reset key.
   *
   * @param key the key
   */
  fun resetKey(key: String?) {
    try {
      keysLock.writeLock().lock()
      keyChain!!.clear()
      keyChain!!.addFirst(key)
    } finally {
      keysLock.writeLock().unlock()
    }
  }

  /**
   * Get aware ids short [ ].
   *
   * @return the short [ ]
   */
  fun getAwareIdsLock(): ClusterIdRegistry? {
    try {
      idsLock.readLock().lock()
      return awareIds
    } finally {
      idsLock.readLock().unlock()
    }
  }

  /**
   * Sets aware ids.
   *
   * @param awareIds the aware ids
   */
  fun setAwareIds(awareIds: ShortArray) {
    this.awareIds = ClusterIdRegistry(*awareIds)
  }


  /**
   * Add aware id boolean.
   *
   * @param id the id
   *
   * @return the boolean
   */
  fun addAwareId(id: Short): Boolean {
    try {
      idsLock.writeLock().lock()

      if (this.awareIds == null) {
        this.awareIds = ClusterIdRegistry(id)
        return true
      }
      if (awareIds!!.contains(id)) {
        return false
      }

      return awareIds!!.add(id)
    } finally {
      idsLock.writeLock().unlock()
    }
  }

  /**
   * Add aware id boolean.
   *
   * @param ids the ids
   *
   * @return the boolean
   */
  fun addAwareIdRaw(ids: ShortArray): Boolean {
    try {
      idsLock.writeLock().lock()

      if (this.awareIds == null) {
        this.awareIds = ClusterIdRegistry(*ids)
        return true
      }

      return awareIds!!.addAll(*ids)
    } finally {
      idsLock.writeLock().unlock()
    }
  }

  /**
   * Add aware id boolean.
   *
   * @param ids the ids
   *
   * @return the boolean
   */
  fun addAwareId(ids: ClusterIdRegistry): Boolean {
    try {
      idsLock.writeLock().lock()

      if (this.awareIds == null) {
        this.awareIds = ids
        return true
      }

      return awareIds!!.addAll(*ids.ids)
    } finally {
      idsLock.writeLock().unlock()
    }
  }

  val isDeleted: Boolean
    /**
     * Is deleted boolean.
     *
     * @return the boolean
     */
    get() = state == MemberState.DELETED

  val isDown: Boolean
    /**
     * Is down boolean.
     *
     * @return the boolean
     */
    get() = state == MemberState.DOWN

  val isValid: Boolean
    /**
     * Is valid boolean.
     *
     * @return the boolean
     */
    get() = state == MemberState.VALID

  val isFullyScheduled: Boolean
    /**
     * Is fully scheduled boolean.
     *
     * @return the boolean
     */
    get() = schedule == SYNCED.toShort()

  var isScheduled: Boolean
    /**
     * Is scheduled boolean.
     *
     * @return the boolean
     */
    get() = schedule > NOT_SYNCED
    /**
     * Sets scheduled.
     *
     * @param scheduled the scheduled
     */
    set(scheduled) {
      this.schedule =
        (if (scheduled) SYNCED else NOT_SYNCED).toShort()
    }

  override fun toString(): String {
    return "id=" + this.id +
            ", state=" + state +
            ", version=" + lastModified +
            ", sync addrs:" + syncAddresses +
            ", monitor delay=" + monitorDelay +
            ", monitor interval=" + monitorInterval +
            ", report delay=" + reportDelay +
            ", report interval=" + reportInterval
  }

  companion object {
    /**
     * The constant SYNCED.
     */
    const val SYNCED: Byte = 0

    /**
     * The constant NOT_SYNCED.
     */
    const val NOT_SYNCED: Byte = -1

    /**
     * The constant MONITOR_INTERVAL.
     */
    const val MONITOR_INTERVAL: Short = 60

    /**
     * The constant MONITOR_DELAY.
     */
    const val MONITOR_DELAY: Short = 10
  }
}
