package com.jnimble.sdk.plugin;

/**
 * Select option declared by a plugin configuration field.
 *
 * @param value persisted option value
 * @param label fallback display label
 * @param labelKey optional i18n key for the display label
 */
public record PluginConfigurationOption(
        String value,
        String label,
        String labelKey
) {
}
