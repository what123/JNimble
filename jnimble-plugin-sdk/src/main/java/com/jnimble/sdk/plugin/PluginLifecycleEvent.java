package com.jnimble.sdk.plugin;

import java.time.Instant;

/**
 * Minimal lifecycle event visible to SDK-level hooks.
 *
 * @param pluginId plugin id from the descriptor
 * @param phase lifecycle phase that produced the event
 * @param occurredAt event timestamp supplied by the runtime
 * @param reason optional human-readable operation reason
 */
public record PluginLifecycleEvent(
        String pluginId,
        PluginLifecyclePhase phase,
        Instant occurredAt,
        String reason
) {
}
