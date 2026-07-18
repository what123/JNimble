package com.jnimble.kernel.route;

import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;

/**
 * Record representing a registered plugin route with its metadata and availability status.
 *
 * @param registrationId unique identifier for this route registration
 * @param pluginId       the ID of the plugin that owns this route
 * @param fullPath       the full request path including namespace prefix
 * @param method         the HTTP method for this route
 * @param definition     the original route definition
 * @param pluginEnabled  whether the owning plugin is currently enabled
 */
public record RegisteredPluginRoute(
        String registrationId,
        String pluginId,
        String fullPath,
        RouteMethod method,
        RouteDefinition definition,
        boolean pluginEnabled
) {
}
