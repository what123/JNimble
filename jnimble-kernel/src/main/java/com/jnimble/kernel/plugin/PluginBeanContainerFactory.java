package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginDescriptor;

import java.util.Map;
import java.util.Objects;

/**
 * Creates the dependency-injection container for an enabled plugin.
 */
@FunctionalInterface
public interface PluginBeanContainerFactory {

    /**
     * Creates a bean container for a plugin.
     *
     * @param descriptor       the plugin descriptor
     * @param classLoader      the plugin class loader
     * @param dependencies     the dependency plugin containers, keyed by plugin ID
     * @param platformResolver the resolver for platform beans
     * @return the created bean container
     */
    PluginBeanContainer create(
            PluginDescriptor descriptor,
            ClassLoader classLoader,
            Map<String, PluginBeanContainer> dependencies,
            PluginBeanResolver platformResolver
    );

    /**
     * Resolver-backed implementation used when no plugin container integration is installed.
     */
    static PluginBeanContainerFactory resolverBacked() {
        return (descriptor, classLoader, dependencies, platformResolver) -> {
            Objects.requireNonNull(platformResolver, "platformResolver");
            return new PluginBeanContainer() {
                @Override
                public <T> T resolve(Class<T> type) {
                    return platformResolver.resolve(type);
                }
            };
        };
    }
}
