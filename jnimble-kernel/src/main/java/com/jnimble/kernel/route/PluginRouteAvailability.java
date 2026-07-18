package com.jnimble.kernel.route;

/**
 * Enum representing the availability status of a plugin route.
 */
public enum PluginRouteAvailability {

    /** The route is available and the owning plugin is enabled. */
    AVAILABLE,

    /** The route exists but the owning plugin is disabled. */
    PLUGIN_DISABLED,

    /** No route matches the given path and method. */
    NOT_FOUND
}
