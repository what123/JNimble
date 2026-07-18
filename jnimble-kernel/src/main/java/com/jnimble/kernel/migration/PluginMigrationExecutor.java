package com.jnimble.kernel.migration;

import com.jnimble.sdk.plugin.PluginDescriptor;

import java.nio.file.Path;

/**
 * Interface for executing plugin database migrations.
 *
 * <p>Implementations are responsible for running migration scripts
 * when a plugin is enabled.</p>
 */
public interface PluginMigrationExecutor {

    /**
     * Runs migrations for a plugin.
     *
     * @param descriptor the plugin descriptor
     */
    void migrate(PluginDescriptor descriptor);

    /**
     * Runs migrations for a plugin with a specific class loader.
     *
     * @param descriptor  the plugin descriptor
     * @param classLoader the class loader to use
     */
    default void migrate(PluginDescriptor descriptor, ClassLoader classLoader) {
        migrate(descriptor);
    }

    /**
     * Cleans plugin-owned database objects.
     *
     * @param descriptor the plugin descriptor
     */
    default void clean(PluginDescriptor descriptor) {
    }

    /**
     * Cleans plugin-owned database objects with a specific class loader.
     *
     * @param descriptor  the plugin descriptor
     * @param classLoader the class loader to use
     */
    default void clean(PluginDescriptor descriptor, ClassLoader classLoader) {
        clean(descriptor);
    }

    /**
     * Cleans plugin-owned database objects from an optional plugin artifact.
     *
     * @param descriptor   the plugin descriptor
     * @param artifactPath the plugin artifact path
     */
    default void clean(PluginDescriptor descriptor, Path artifactPath) {
        clean(descriptor);
    }

    /**
     * Returns a no-op executor that skips migrations.
     *
     * @return a no-op executor
     */
    static PluginMigrationExecutor noop() {
        return descriptor -> {
        };
    }
}
