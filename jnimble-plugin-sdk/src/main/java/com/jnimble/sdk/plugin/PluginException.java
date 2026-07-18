package com.jnimble.sdk.plugin;

/**
 * Base unchecked exception for plugin lifecycle failures.
 */
public class PluginException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message describing the failure
     */
    public PluginException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message describing the failure
     * @param cause   the underlying cause of the failure
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
