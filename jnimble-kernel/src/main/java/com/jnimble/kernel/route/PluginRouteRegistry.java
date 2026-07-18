package com.jnimble.kernel.route;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import com.jnimble.sdk.route.RouteRegistry;
import java.util.List;
import java.util.Optional;

/**
 * Registry for plugin routes.
 *
 * <p>Manages route registrations from plugins, including scoped registries
 * for individual plugins and availability checking.</p>
 */
public interface PluginRouteRegistry {

    /** The namespace prefix for plugin routes. */
    String ROUTE_NAMESPACE_PREFIX = "/admin/plugins";

    /**
     * Returns a scoped route registry for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return a scoped registry
     */
    RouteRegistry scoped(String pluginId);

    /**
     * Registers a route for a plugin.
     *
     * @param pluginId the plugin identifier
     * @param route    the route definition
     * @return a registration handle
     */
    RegistrationHandle register(String pluginId, RouteDefinition route);

    /**
     * Enables all routes for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    void enablePlugin(String pluginId);

    /**
     * Disables all routes for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    void disablePlugin(String pluginId);

    /**
     * Checks route availability for a request path and method.
     *
     * @param requestPath the request path
     * @param method      the HTTP method
     * @return the availability result
     */
    PluginRouteAvailability availability(String requestPath, RouteMethod method);

    /**
     * Finds a route matching the request path and method.
     *
     * @param requestPath the request path
     * @param method      the HTTP method
     * @return an optional containing the registered route if found
     */
    Optional<RegisteredPluginRoute> find(String requestPath, RouteMethod method);

    /**
     * Lists all registered routes.
     *
     * @return list of all routes
     */
    List<RegisteredPluginRoute> routes();

    /**
     * Lists all routes for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return list of routes
     */
    List<RegisteredPluginRoute> routes(String pluginId);
}
