package com.jnimble.sdk.plugin;

/**
 * A dependency on another plugin.
 *
 * @param pluginId required plugin identifier
 * @param version compatible plugin version expression such as {@code 1.x}, {@code >=1.2.0}, or {@code *}
 * @param required whether a missing dependency prevents this plugin from enabling
 */
public record PluginDependency(
        String pluginId,
        String version,
        boolean required
) {
}
