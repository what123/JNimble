package com.jnimble.sdk.route;

import com.jnimble.sdk.hook.RegistrationHandle;

/**
 * Registry used by plugins to expose routes while the plugin is enabled.
 */
public interface RouteRegistry {

    /**
     * Registers a plugin route.
     *
     * @param route route definition under the plugin namespace
     * @return handle used by the runtime to remove the route
     */
    RegistrationHandle register(RouteDefinition route);
}
