package com.jnimble.kernel.route;

/**
 * Exception thrown when two plugins attempt to register routes with the same
 * path and HTTP method, causing a conflict.
 */
public class PluginRouteConflictException extends RuntimeException {

    /**
     * Creates a new {@code PluginRouteConflictException} with the specified message.
     *
     * @param message the detail message describing the conflict
     */
    public PluginRouteConflictException(String message) {
        super(message);
    }
}
