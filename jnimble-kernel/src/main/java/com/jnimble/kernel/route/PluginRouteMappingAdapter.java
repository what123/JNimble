package com.jnimble.kernel.route;

/**
 * Adapter boundary for future Spring MVC dynamic handler mappings.
 * Provides methods to mount and unmount plugin routes in the MVC infrastructure.
 */
public interface PluginRouteMappingAdapter {

    /**
     * Mounts a plugin route in the MVC handler mapping.
     *
     * @param route the registered plugin route to mount
     */
    void mount(RegisteredPluginRoute route);

    /**
     * Unmounts a plugin route from the MVC handler mapping.
     *
     * @param route the registered plugin route to unmount
     */
    void unmount(RegisteredPluginRoute route);
}
