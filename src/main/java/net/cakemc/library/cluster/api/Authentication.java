package net.cakemc.library.cluster.api;

/**
 * The {@code Authentication} class represents the authentication details used in a cluster environment.
 * It includes an authentication key and a flag indicating whether authentication is used. It also
 * provides a builder for constructing {@code Authentication} instances.
 */
public class Authentication {

	private final String authKey;
	private final boolean useAuthentication;

	/**
	 * Constructs an {@code Authentication} instance with the specified authentication key and
	 * flag indicating whether to use authentication.
	 *
	 * @param authKey          the authentication key used for verification.
	 * @param useAuthentication {@code true} if authentication should be used, {@code false} otherwise.
	 */
	public Authentication(String authKey, boolean useAuthentication) {
		this.authKey = authKey;
		this.useAuthentication = useAuthentication;
	}

	/**
	 * Creates a new instance of {@code AuthenticationBuilder}, a builder class used to construct
	 * an {@code Authentication} object.
	 *
	 * @return a new {@code AuthenticationBuilder} instance.
	 */
	public static AuthenticationBuilder make() {
		return new AuthenticationBuilder();
	}

	/**
	 * Returns the authentication key.
	 *
	 * @return the authentication key as a {@link String}.
	 */
	public String getAuthKey() {
		return authKey;
	}

	/**
	 * Returns whether authentication is being used.
	 *
	 * @return {@code true} if authentication is used, {@code false} otherwise.
	 */
	public boolean isUseAuthentication() {
		return useAuthentication;
	}

	/**
	 * The {@code AuthenticationBuilder} class provides a convenient way to construct
	 * {@code Authentication} instances by setting the necessary fields step by step.
	 */
	public static class AuthenticationBuilder {

		private String authKey;
		private boolean useAuthentication;

		/**
		 * Constructs an {@code AuthenticationBuilder} instance with default values.
		 * By default, {@code useAuthentication} is set to {@code false}, and {@code authKey} is an empty string.
		 */
		public AuthenticationBuilder() {
			useAuthentication = false;
			authKey = "";
		}

		/**
		 * Sets the authentication key to the provided value.
		 *
		 * @param key the authentication key to set.
		 * @return this {@code AuthenticationBuilder} instance, to allow method chaining.
		 */
		public AuthenticationBuilder withKey(String key) {
			this.authKey = key;
			return this;
		}

		/**
		 * Enables authentication by setting the {@code useAuthentication} flag to {@code true}.
		 *
		 * @return this {@code AuthenticationBuilder} instance, to allow method chaining.
		 */
		public AuthenticationBuilder useVerification() {
			this.useAuthentication = true;
			return this;
		}

		/**
		 * Constructs and returns a new {@code Authentication} instance using the values
		 * set in the builder.
		 *
		 * @return a new {@code Authentication} instance.
		 */
		public Authentication get() {
			return new Authentication(authKey, useAuthentication);
		}

	}

}
