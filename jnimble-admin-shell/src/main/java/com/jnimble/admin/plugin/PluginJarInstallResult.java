package com.jnimble.admin.plugin;

import com.jnimble.sdk.plugin.PluginDescriptor;

import java.nio.file.Path;

/**
 * Record representing the result of a plugin JAR installation or replacement operation.
 *
 * @param descriptor  the plugin descriptor loaded from the JAR
 * @param storedPath  the path where the JAR was stored
 * @param replaced    true if an existing plugin was replaced, false for new installations
 */
public record PluginJarInstallResult(
        PluginDescriptor descriptor,
        Path storedPath,
        boolean replaced
) {
}
