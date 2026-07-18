package com.jnimble.sdk.resource;

/**
 * Static asset mapping exposed by a plugin.
 *
 * @param requestPath path relative to the plugin asset namespace
 * @param resourceLocation classpath or file location served by the runtime
 * @param cacheable whether the asset can be cached by clients
 */
public record AssetDefinition(
        String requestPath,
        String resourceLocation,
        boolean cacheable
) {

    /**
     * Creates an asset definition that is cacheable by default.
     *
     * @param requestPath      path relative to the plugin asset namespace
     * @param resourceLocation classpath or file location served by the runtime
     */
    public AssetDefinition(String requestPath, String resourceLocation) {
        this(requestPath, resourceLocation, true);
    }
}
