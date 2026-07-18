package com.jnimble.kernel.resource;

/**
 * Adapter boundary for future Spring MVC resource handler mappings.
 * Provides methods to mount and unmount plugin assets in the MVC infrastructure.
 */
public interface PluginAssetMappingAdapter {

    /**
     * Mounts a plugin asset in the MVC resource handler mapping.
     *
     * @param asset the registered plugin asset to mount
     */
    void mount(RegisteredPluginAsset asset);

    /**
     * Unmounts a plugin asset from the MVC resource handler mapping.
     *
     * @param asset the registered plugin asset to unmount
     */
    void unmount(RegisteredPluginAsset asset);
}
