package com.jnimble.kernel.migration;

/**
 * Exception thrown when a plugin migration operation fails.
 * Wraps the underlying cause of the migration failure.
 */
public class PluginMigrationException extends RuntimeException {

    /**
     * Creates a new {@code PluginMigrationException} with the specified message.
     *
     * @param message the detail message
     */
    public PluginMigrationException(String message) {
        super(message);
    }

    /**
     * Creates a new {@code PluginMigrationException} with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public PluginMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
