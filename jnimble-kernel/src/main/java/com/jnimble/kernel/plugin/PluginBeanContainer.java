package com.jnimble.kernel.plugin;

import java.util.Map;

/**
 * Runtime bean container owned by one plugin.
 *
 * <p>The kernel deliberately does not depend on a concrete dependency-injection
 * implementation. The starter supplies the Spring child-context implementation,
 * while tests and non-Spring embedders can use the default resolver-backed
 * container.</p>
 */
public interface PluginBeanContainer extends AutoCloseable {

    /**
     * Resolves a bean visible to the plugin.
     *
     * @param type required bean type
     * @param <T> bean type
     * @return resolved bean
     */
    <T> T resolve(Class<T> type);

    /**
     * Returns beans exported to plugins that depend on this plugin.
     *
     * @return immutable or read-only bean map keyed by bean name
     */
    default Map<String, Object> exportedBeans() {
        return Map.of();
    }

    /**
     * Publishes runtime integrations such as HTTP endpoints.
     */
    default void activate() {
    }

    /**
     * Withdraws runtime integrations before plugin beans are destroyed.
     */
    default void deactivate() {
    }

    @Override
    default void close() throws Exception {
    }
}
