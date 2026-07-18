package com.jnimble.kernel.plugin;

import java.util.Map;
import java.util.Optional;

/**
 * Live registry of class loaders owned by enabled plugins.
 */
public interface PluginClassLoaderRegistry {

    /**
     * Registers a class loader for a plugin.
     *
     * @param pluginId    the plugin identifier
     * @param classLoader the class loader to register
     */
    void register(String pluginId, ClassLoader classLoader);

    /**
     * Unregisters a class loader for a plugin.
     *
     * @param pluginId    the plugin identifier
     * @param classLoader the class loader to unregister
     */
    void unregister(String pluginId, ClassLoader classLoader);

    /**
     * Finds the registered class loader for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return an optional containing the class loader if found
     */
    Optional<ClassLoader> find(String pluginId);

    /**
     * Returns an immutable snapshot of all registered class loaders.
     *
     * @return a map of plugin ID to class loader
     */
    Map<String, ClassLoader> snapshot();

    /**
     * Returns a new in-memory implementation of the registry.
     *
     * @return an in-memory class loader registry
     */
    static PluginClassLoaderRegistry inMemory() {
        return new InMemoryPluginClassLoaderRegistry();
    }
}
