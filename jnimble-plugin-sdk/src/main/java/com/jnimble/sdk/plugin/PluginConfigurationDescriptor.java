package com.jnimble.sdk.plugin;

import java.util.List;

/**
 * Declarative configuration form contributed by a plugin descriptor.
 *
 * @param title fallback form title
 * @param titleKey optional i18n key for the form title
 * @param description optional fallback form description
 * @param descriptionKey optional i18n key for the form description
 * @param fields ordered configuration fields rendered by the platform
 */
public record PluginConfigurationDescriptor(
        String title,
        String titleKey,
        String description,
        String descriptionKey,
        List<PluginConfigurationField> fields
) {
}
