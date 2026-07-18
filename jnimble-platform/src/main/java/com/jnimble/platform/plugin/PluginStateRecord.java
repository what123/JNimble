package com.jnimble.platform.plugin;

import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;

import java.time.Instant;

/**
 * Immutable record representing the persisted state of a plugin.
 *
 * @param pluginId       the unique plugin identifier
 * @param name           the plugin name
 * @param version        the plugin version
 * @param source         the source type of the plugin (classpath, directory, jar)
 * @param artifactPath   the path to the plugin artifact (if applicable)
 * @param enabled        whether the plugin is currently enabled
 * @param status         the current plugin status
 * @param descriptorJson the raw JSON descriptor
 * @param descriptorHash the hash of the descriptor for change detection
 * @param lastError      the last error message (if any)
 * @param installedAt    the timestamp when the plugin was installed
 * @param lastStartedAt  the timestamp when the plugin was last started
 * @param lastStoppedAt  the timestamp when the plugin was last stopped
 * @param createdAt      the timestamp when this record was created
 * @param updatedAt      the timestamp when this record was last updated
 */
public record PluginStateRecord(
        String pluginId,
        String name,
        String version,
        PluginSource source,
        String artifactPath,
        boolean enabled,
        PluginStatus status,
        String descriptorJson,
        String descriptorHash,
        String lastError,
        Instant installedAt,
        Instant lastStartedAt,
        Instant lastStoppedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
