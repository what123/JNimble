package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginBoot;

/**
 * Represents a loaded plugin boot instance along with its associated classloader.
 * Implements {@link AutoCloseable} to support cleanup of plugin resources.
 */
public interface LoadedPluginBoot extends AutoCloseable {

    /**
     * Returns the loaded plugin boot instance.
     *
     * @return the plugin boot instance
     */
    PluginBoot boot();

    /**
     * Returns the classloader used to load the plugin boot class.
     *
     * @return the plugin's classloader
     */
    default ClassLoader classLoader() {
        return boot().getClass().getClassLoader();
    }

    @Override
    default void close() throws Exception {
    }
}
