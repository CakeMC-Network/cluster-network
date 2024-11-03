package net.cakemc.library.cluster

import io.netty.util.AttributeKey

/**
 * Enum representing the various channel attributes used in the synchronization process.
 *
 *
 * The `ChannelAttributes` enum defines keys that can be associated with
 * Netty channels to store and retrieve various state information. Each enum constant
 * corresponds to a specific attribute that can be used within a Netty channel's context.
 */
enum class ChannelAttributes(name: String) {
    SYNC_SESSION("SYNC_SESSION"),
    STARTUP_STATE_KEY("STARTUP_STATE_KEY"),
    PLANNED_CLOSE("PLANNED_CLOSE"),
    STARTUP_STATE("STARTUP_STATE"),
    PLANNED_CLOSE_KEY("PLANNED_CLOSE_KEY"),
    ATTRIBUTE_KEY("ATTRIBUTE_KEY");

    // Create an AttributeKey for each enum constant
    private val key: AttributeKey<*> = AttributeKey.valueOf<Any>(name)

    /**
     * Retrieves the `AttributeKey` associated with this enum constant.
     *
     *
     * This method returns the `AttributeKey` as a specific type, allowing for
     * type-safe operations on the channel attributes.
     *
     * @param <T> the type of the attribute value
     * @return the `AttributeKey` for this attribute
    </T> */
    fun <T> getKey(): AttributeKey<T> {
        return key as AttributeKey<T>
    }
}
