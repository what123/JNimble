package com.jnimble.sdk.plugin;

/**
 * Optional admin entry exposed by a plugin.
 *
 * @param entry plugin-relative route, for example {@code /items}
 * @param labelKey optional i18n key describing the destination
 * @param permission permission required to expose the entry
 */
public record PluginAdminDescriptor(
        String entry,
        String labelKey,
        String permission
) {
}
