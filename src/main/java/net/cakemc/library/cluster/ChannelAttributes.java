package net.cakemc.library.cluster;

import io.netty.util.AttributeKey;

/**
 * Enum representing the various channel attributes used in the synchronization process.
 *
 * <p>The {@code ChannelAttributes} enum defines keys that can be associated with
 * Netty channels to store and retrieve various state information. Each enum constant
 * corresponds to a specific attribute that can be used within a Netty channel's context.</p>
 */
public enum ChannelAttributes {
	SYNC_SESSION("SYNC_SESSION"),
	STARTUP_STATE_KEY("STARTUP_STATE_KEY"),
	PLANNED_CLOSE("PLANNED_CLOSE"),
	STARTUP_STATE("STARTUP_STATE"),
	PLANNED_CLOSE_KEY("PLANNED_CLOSE_KEY"),
	ATTRIBUTE_KEY("ATTRIBUTE_KEY");

	// Create an AttributeKey for each enum constant
	private final AttributeKey<?> key;

	/**
	 * Constructor for the {@code ChannelAttributes} enum.
	 *
	 * <p>This constructor initializes the {@code AttributeKey} for the enum constant
	 * using the provided name.</p>
	 *
	 * @param name the name of the attribute key
	 */
	ChannelAttributes(String name) {
		this.key = AttributeKey.valueOf(name);
	}

	/**
	 * Retrieves the {@code AttributeKey} associated with this enum constant.
	 *
	 * <p>This method returns the {@code AttributeKey} as a specific type, allowing for
	 * type-safe operations on the channel attributes.</p>
	 *
	 * @param <T> the type of the attribute value
	 * @return the {@code AttributeKey} for this attribute
	 */
	public <T> AttributeKey<T> getKey() {
		return (AttributeKey<T>) key;
	}
}
