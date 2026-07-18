package com.jnimble.sdk.plugin;

import java.util.List;

/**
 * A single platform-rendered plugin configuration field.
 *
 * @param key stable key used for persistence and SDK lookup
 * @param label fallback field label
 * @param labelKey optional i18n key for the label
 * @param description optional fallback help text
 * @param descriptionKey optional i18n key for the help text
 * @param placeholder optional fallback placeholder
 * @param placeholderKey optional i18n key for the placeholder
 * @param type input control type
 * @param required whether a value must be configured
 * @param defaultValue optional default value represented as text
 * @param options options used by {@link PluginConfigurationFieldType#SELECT}
 */
public record PluginConfigurationField(
        String key,
        String label,
        String labelKey,
        String description,
        String descriptionKey,
        String placeholder,
        String placeholderKey,
        PluginConfigurationFieldType type,
        boolean required,
        String defaultValue,
        List<PluginConfigurationOption> options
) {
}
