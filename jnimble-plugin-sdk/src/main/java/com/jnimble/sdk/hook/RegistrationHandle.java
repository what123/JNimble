package com.jnimble.sdk.hook;

import java.util.Optional;

/**
 * Handle returned when a plugin registers a runtime contribution.
 *
 * <p>The platform keeps these handles while a plugin is enabled and invokes
 * {@link #unregister()} when the plugin is disabled, reloaded, or rolled back
 * after a failed startup.</p>
 */
@FunctionalInterface
public interface RegistrationHandle {

    /**
     * Removes the registered contribution. Implementations should be
     * idempotent so repeated rollback attempts are safe.
     */
    void unregister();

    /**
     * Stable identifier assigned by the runtime, when available.
     */
    default Optional<String> registrationId() {
        return Optional.empty();
    }

    /**
     * Kind of contribution represented by this handle.
     */
    default RegistrationType type() {
        return RegistrationType.UNKNOWN;
    }
}
