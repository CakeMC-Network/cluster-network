package net.cakemc.library.cluster.config;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

/**
 * The {@code Snowflake} class generates unique identifiers (IDs) based on a
 * combination of the current timestamp, a node identifier, and a sequence number.
 * This implementation follows a design similar to Twitter's Snowflake ID generation
 * strategy.
 */
public class Snowflake {
	private static final int NODE_ID_BITS = 10;  // Number of bits for the node ID
	private static final int SEQUENCE_BITS = 12;  // Number of bits for the sequence number

	private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;  // Maximum node ID value
	private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;  // Maximum sequence number value

	private static final long DEFAULT_CUSTOM_EPOCH = 1420070400000L;  // Default epoch for ID generation

	private final long nodeId;  // Unique identifier for the node
	private final long customEpoch;  // Custom epoch for timestamp calculations

	private volatile long lastTimestamp = -1L;  // Last generated timestamp
	private volatile long sequence = 0L;  // Current sequence number

	/**
	 * Constructs a {@code Snowflake} instance with a specified node ID and custom epoch.
	 *
	 * @param nodeId      The unique identifier for the node (should be between 0 and 1023).
	 * @param customEpoch The custom epoch in milliseconds.
	 * @throws IllegalArgumentException if nodeId is out of range.
	 */
	public Snowflake(long nodeId, long customEpoch) {
		if (nodeId < 0 || nodeId > MAX_NODE_ID) {
			throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, MAX_NODE_ID));
		}
		this.nodeId = nodeId;
		this.customEpoch = customEpoch;
	}

	/**
	 * Constructs a {@code Snowflake} instance with a generated node ID and the default epoch.
	 */
	public Snowflake() {
		this.nodeId = generateNodeId();
		this.customEpoch = DEFAULT_CUSTOM_EPOCH;
	}

	/**
	 * Generates the next unique ID.
	 *
	 * @return A unique long identifier.
	 * @throws IllegalStateException if the system clock is invalid (i.e., the timestamp goes backward).
	 */
	public synchronized long nextId() {
		long currentTimestamp = getCurrentTimestamp();

		if (currentTimestamp < lastTimestamp) {
			throw new IllegalStateException("Invalid system clock!");
		}

		// Update the sequence for the current timestamp
		if (currentTimestamp == lastTimestamp) {
			sequence = (sequence + 1) & MAX_SEQUENCE;
			if (sequence == 0) {
				currentTimestamp = waitForNextMillis(currentTimestamp);
			}
		} else {
			sequence = 0;  // Reset sequence for new timestamp
		}

		lastTimestamp = currentTimestamp;

		return (currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS))
		       | (nodeId << SEQUENCE_BITS)
		       | sequence;
	}

	/**
	 * Gets the current timestamp adjusted by the custom epoch.
	 *
	 * @return The current timestamp in milliseconds since the custom epoch.
	 */
	public long getCurrentTimestamp() {
		return Instant.now().toEpochMilli() - customEpoch;
	}

	/**
	 * Waits for the next millisecond, ensuring that the timestamp has advanced.
	 *
	 * @param currentTimestamp The current timestamp to compare against.
	 * @return The next valid timestamp.
	 */
	private long waitForNextMillis(long currentTimestamp) {
		while (currentTimestamp <= lastTimestamp) {
			currentTimestamp = getCurrentTimestamp();
		}
		return currentTimestamp;
	}

	/**
	 * Creates a unique node ID based on the MAC address or a random number if unavailable.
	 *
	 * @return A unique node ID.
	 */
	private long generateNodeId() {
		long nodeId;
		try {
			StringBuilder macAddressBuilder = new StringBuilder();
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				byte[] mac = networkInterface.getHardwareAddress();
				if (mac != null) {
					for (byte macByte : mac) {
						macAddressBuilder.append(String.format("%02X", macByte));
					}
				}
			}
			nodeId = macAddressBuilder.toString().hashCode();
		} catch (Exception e) {
			nodeId = new SecureRandom().nextInt();
		}
		return nodeId & MAX_NODE_ID;  // Ensure nodeId is within range
	}
}
