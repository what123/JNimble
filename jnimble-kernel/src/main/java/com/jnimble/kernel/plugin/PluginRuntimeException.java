package com.jnimble.kernel.plugin;

/**
 * Runtime exception for plugin-related errors.
 *
 * <p>Thrown when a plugin operation fails, such as loading, starting,
 * or stopping a plugin.</p>
 */
public class PluginRuntimeException extends RuntimeException {

    /**
     * Creates a new plugin runtime exception with a message.
     *
     * @param message the error message
     */
    public PluginRuntimeException(String message) {
        super(message);
    }

    /**
     * Creates a new plugin runtime exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public PluginRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
