package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginLifecycleEvent;
import com.jnimble.sdk.plugin.PluginStatus;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot of a plugin's runtime state.
 *
 * @param pluginId          the plugin identifier
 * @param descriptor        the plugin descriptor
 * @param source            the plugin source type
 * @param artifactPath      the artifact path (null for classpath plugins)
 * @param status            the current plugin status
 * @param registrationCount the number of active registrations (hooks, routes, etc.)
 * @param lastError         the last error message (if any)
 * @param installedAt       the installation timestamp
 * @param lastStartedAt     the last start timestamp
 * @param lastStoppedAt     the last stop timestamp
 * @param events            the list of lifecycle events
 */
public record PluginRuntimeSnapshot(
        String pluginId,
        PluginDescriptor descriptor,
        PluginSource source,
        Path artifactPath,
        PluginStatus status,
        int registrationCount,
        String lastError,
        Instant installedAt,
        Instant lastStartedAt,
        Instant lastStoppedAt,
        List<PluginLifecycleEvent> events
) {
}
