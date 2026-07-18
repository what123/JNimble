package com.jnimble.sdk.plugin;

import java.time.Instant;

/**
 * Runtime metadata known about a plugin without exposing platform persistence
 * implementation details.
 *
 * @param pluginId plugin id from the descriptor
 * @param name display name resolved by the runtime
 * @param version plugin version from the descriptor
 * @param source discovery or installation source
 * @param status current lifecycle status
 * @param installedAt installation timestamp, when known
 * @param lastStartedAt last successful enable timestamp, when known
 * @param lastStoppedAt last disable timestamp, when known
 * @param lastError last lifecycle error message, when known
 */
public record PluginMetadata(
        String pluginId,
        String name,
        String version,
        PluginSource source,
        PluginStatus status,
        Instant installedAt,
        Instant lastStartedAt,
        Instant lastStoppedAt,
        String lastError
) {
}
