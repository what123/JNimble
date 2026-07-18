package com.jnimble.kernel.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory {@link PluginClassLoaderRegistry}.
 */
public class InMemoryPluginClassLoaderRegistry implements PluginClassLoaderRegistry {

    private final Map<String, ClassLoader> classLoaders = new ConcurrentHashMap<>();

    @Override
    public void register(String pluginId, ClassLoader classLoader) {
        classLoaders.put(
                PluginIds.requireValid(pluginId, "Plugin id"),
                java.util.Objects.requireNonNull(classLoader, "classLoader"));
    }

    @Override
    public void unregister(String pluginId, ClassLoader classLoader) {
        if (pluginId == null || classLoader == null) {
            return;
        }
        classLoaders.remove(pluginId, classLoader);
    }

    @Override
    public Optional<ClassLoader> find(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(classLoaders.get(pluginId));
    }

    @Override
    public Map<String, ClassLoader> snapshot() {
        return Map.copyOf(classLoaders);
    }
}
