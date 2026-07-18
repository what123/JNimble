package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginDescriptor;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for loading plugin boot classes.
 *
 * <p>Implementations are responsible for loading the plugin's main class
 * and creating a {@link LoadedPluginBoot} instance.</p>
 */
public interface PluginBootLoader {

    /**
     * Loads a plugin boot instance from the classpath.
     *
     * @param descriptor the plugin descriptor
     * @return the loaded plugin boot
     * @throws PluginRuntimeException if the boot class cannot be loaded
     */
    LoadedPluginBoot load(PluginDescriptor descriptor);

    /**
     * Loads a plugin boot instance from a JAR file.
     *
     * @param descriptor   the plugin descriptor
     * @param artifactPath the path to the JAR file
     * @return the loaded plugin boot
     * @throws PluginRuntimeException if the boot class cannot be loaded
     */
    default LoadedPluginBoot load(PluginDescriptor descriptor, Path artifactPath) {
        return load(descriptor);
    }

    /**
     * Loads a plugin while making already enabled dependency class loaders visible.
     * Existing boot-loader implementations remain compatible through this default method.
     *
     * @param descriptor plugin descriptor
     * @param artifactPath plugin artifact, or {@code null} for classpath mode
     * @param dependencyClassLoaders enabled dependency class loaders in descriptor order
     * @return loaded plugin boot
     */
    default LoadedPluginBoot load(
            PluginDescriptor descriptor,
            Path artifactPath,
            List<ClassLoader> dependencyClassLoaders
    ) {
        return load(descriptor, artifactPath);
    }
}
