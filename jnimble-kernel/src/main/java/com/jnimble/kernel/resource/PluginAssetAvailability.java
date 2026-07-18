package com.jnimble.kernel.resource;

/**
 * Enum representing the availability status of a plugin asset.
 */
public enum PluginAssetAvailability {

    /** The asset is available and the owning plugin is enabled. */
    AVAILABLE,

    /** The asset exists but the owning plugin is disabled. */
    PLUGIN_DISABLED,

    /** No asset matches the given request path. */
    NOT_FOUND
}
