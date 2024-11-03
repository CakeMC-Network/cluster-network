package net.cakemc.library.cluster.api

/**
 * The `Authentication` class represents the authentication details used in a cluster environment.
 * It includes an authentication key and a flag indicating whether authentication is used. It also
 * provides a builder for constructing `Authentication` instances.
 */
class Authentication
/**
 * Constructs an `Authentication` instance with the specified authentication key and
 * flag indicating whether to use authentication.
 *
 * @param authKey          the authentication key used for verification.
 * @param useAuthentication `true` if authentication should be used, `false` otherwise.
 */(
    /**
     * Returns the authentication key.
     *
     * @return the authentication key as a [String].
     */
    val authKey: String,
    /**
     * Returns whether authentication is being used.
     *
     * @return `true` if authentication is used, `false` otherwise.
     */
    val isUseAuthentication: Boolean
) {
    /**
     * Constructs an `AuthenticationBuilder` instance with default values.
     * By default, `useAuthentication` is set to `false`, and `authKey` is an empty string.
     */
    /**
     * The `AuthenticationBuilder` class provides a convenient way to construct
     * `Authentication` instances by setting the necessary fields step by step.
     */
    class AuthenticationBuilder {
        private var authKey = ""
        private var useAuthentication = false

        /**
         * Sets the authentication key to the provided value.
         *
         * @param key the authentication key to set.
         * @return this `AuthenticationBuilder` instance, to allow method chaining.
         */
        fun withKey(key: String): AuthenticationBuilder {
            this.authKey = key
            return this
        }

        /**
         * Enables authentication by setting the `useAuthentication` flag to `true`.
         *
         * @return this `AuthenticationBuilder` instance, to allow method chaining.
         */
        fun useVerification(): AuthenticationBuilder {
            this.useAuthentication = true
            return this
        }

        /**
         * Constructs and returns a new `Authentication` instance using the values
         * set in the builder.
         *
         * @return a new `Authentication` instance.
         */
        fun get(): Authentication {
            return Authentication(authKey, useAuthentication)
        }
    }

    companion object {
        /**
         * Creates a new instance of `AuthenticationBuilder`, a builder class used to construct
         * an `Authentication` object.
         *
         * @return a new `AuthenticationBuilder` instance.
         */
        @kotlin.jvm.JvmStatic
        fun make(): AuthenticationBuilder {
            return AuthenticationBuilder()
        }
    }
}
