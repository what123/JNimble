package com.jnimble.kernel.resource;

/**
 * Exception thrown when two plugins attempt to register assets with the same
 * request path, causing a conflict.
 */
public class PluginAssetConflictException extends RuntimeException {

    /**
     * Creates a new {@code PluginAssetConflictException} with the specified message.
     *
     * @param message the detail message describing the conflict
     */
    public PluginAssetConflictException(String message) {
        super(message);
    }
}
