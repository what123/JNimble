package com.jnimble.sdk.plugin;

import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to values saved by the generated plugin configuration page.
 * Plugins can obtain this service through Spring injection or
 * {@code context.bean(PluginConfiguration.class)}.
 */
public interface PluginConfiguration {

    /**
     * Looks up a configuration value for the given plugin and key.
     *
     * @param pluginId the target plugin identifier
     * @param key      the configuration key
     * @return an {@link Optional} containing the value, or empty if not set
     */
    Optional<String> find(String pluginId, String key);

    /**
     * Returns all configuration key-value pairs for the given plugin.
     *
     * @param pluginId the target plugin identifier
     * @return a map of all configured key-value pairs, never {@code null}
     */
    Map<String, String> values(String pluginId);

    /**
     * Looks up a value and falls back to the given default if absent.
     *
     * @param pluginId the target plugin identifier
     * @param key      the configuration key
     * @param fallback the fallback value when the key is not configured
     * @return the configured value, or {@code fallback} if absent
     */
    default String getOrDefault(String pluginId, String key, String fallback) {
        return find(pluginId, key).orElse(fallback);
    }

    /**
     * Looks up a boolean value and falls back to the given default if absent.
     *
     * @param pluginId the target plugin identifier
     * @param key      the configuration key
     * @param fallback the fallback value when the key is not configured
     * @return the parsed boolean value, or {@code fallback} if absent
     */
    default boolean getBoolean(String pluginId, String key, boolean fallback) {
        return find(pluginId, key).map(Boolean::parseBoolean).orElse(fallback);
    }
}
