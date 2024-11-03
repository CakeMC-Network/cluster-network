package net.cakemc.library.cluster.config

import java.net.NetworkInterface
import java.security.SecureRandom
import java.time.Instant
import kotlin.concurrent.Volatile

/**
 * The `Snowflake` class generates unique identifiers (IDs) based on a
 * combination of the current timestamp, a node identifier, and a sequence number.
 * This implementation follows a design similar to Twitter's Snowflake ID generation
 * strategy.
 */
class Snowflake {
    private val nodeId: Long // Unique identifier for the node
    private val customEpoch: Long // Custom epoch for timestamp calculations

    @Volatile
    private var lastTimestamp = -1L // Last generated timestamp

    @Volatile
    private var sequence = 0L // Current sequence number

    /**
     * Constructs a `Snowflake` instance with a specified node ID and custom epoch.
     *
     * @param nodeId      The unique identifier for the node (should be between 0 and 1023).
     * @param customEpoch The custom epoch in milliseconds.
     * @throws IllegalArgumentException if nodeId is out of range.
     */
    constructor(nodeId: Long, customEpoch: Long) {
        require(!(nodeId < 0 || nodeId > MAX_NODE_ID)) {
            String.format(
                "NodeId must be between %d and %d",
                0,
                MAX_NODE_ID
            )
        }
        this.nodeId = nodeId
        this.customEpoch = customEpoch
    }

    /**
     * Constructs a `Snowflake` instance with a generated node ID and the default epoch.
     */
    constructor() {
        this.nodeId = generateNodeId()
        this.customEpoch = DEFAULT_CUSTOM_EPOCH
    }

    /**
     * Generates the next unique ID.
     *
     * @return A unique long identifier.
     * @throws IllegalStateException if the system clock is invalid (i.e., the timestamp goes backward).
     */
    @kotlin.jvm.Synchronized
    fun nextId(): Long {
        var currentTimestamp = currentTimestamp

        check(currentTimestamp >= lastTimestamp) { "Invalid system clock!" }

        // Update the sequence for the current timestamp
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                currentTimestamp = waitForNextMillis(currentTimestamp)
            }
        } else {
            sequence = 0 // Reset sequence for new timestamp
        }

        lastTimestamp = currentTimestamp

        return ((currentTimestamp shl (NODE_ID_BITS + SEQUENCE_BITS))
                or (nodeId shl SEQUENCE_BITS)
                or sequence)
    }

    val currentTimestamp: Long
        /**
         * Gets the current timestamp adjusted by the custom epoch.
         *
         * @return The current timestamp in milliseconds since the custom epoch.
         */
        get() = Instant.now().toEpochMilli() - customEpoch

    /**
     * Waits for the next millisecond, ensuring that the timestamp has advanced.
     *
     * @param currentTimestamp The current timestamp to compare against.
     * @return The next valid timestamp.
     */
    private fun waitForNextMillis(currentTimestamp: Long): Long {
        var currentTimestamp = currentTimestamp
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = this.currentTimestamp
        }
        return currentTimestamp
    }

    /**
     * Creates a unique node ID based on the MAC address or a random number if unavailable.
     *
     * @return A unique node ID.
     */
    private fun generateNodeId(): Long {
        var nodeId: Long
        try {
            val macAddressBuilder = StringBuilder()
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val mac = networkInterface.hardwareAddress
                if (mac != null) {
                    for (macByte in mac) {
                        macAddressBuilder.append(String.format("%02X", macByte))
                    }
                }
            }
            nodeId = macAddressBuilder.toString().hashCode().toLong()
        } catch (e: Exception) {
            nodeId = SecureRandom().nextInt().toLong()
        }
        return nodeId and MAX_NODE_ID // Ensure nodeId is within range
    }

    companion object {
        private const val NODE_ID_BITS = 10 // Number of bits for the node ID
        private const val SEQUENCE_BITS = 12 // Number of bits for the sequence number

        private const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1 // Maximum node ID value
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1 // Maximum sequence number value

        private const val DEFAULT_CUSTOM_EPOCH = 1420070400000L // Default epoch for ID generation
    }
}
