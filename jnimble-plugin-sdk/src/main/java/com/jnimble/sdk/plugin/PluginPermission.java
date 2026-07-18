package com.jnimble.sdk.plugin;

/**
 * Permission contributed by a plugin to the platform permission catalog.
 *
 * @param code stable permission code, normally prefixed with the plugin id
 * @param name fallback display name
 * @param nameKey optional i18n key for the display name
 * @param description optional fallback description
 * @param descriptionKey optional i18n key for the description
 */
public record PluginPermission(
        String code,
        String name,
        String nameKey,
        String description,
        String descriptionKey
) {

    /**
     * Creates a permission with no description.
     *
     * @param code    stable permission code, normally prefixed with the plugin id
     * @param name    fallback display name
     * @param nameKey optional i18n key for the display name
     */
    public PluginPermission(String code, String name, String nameKey) {
        this(code, name, nameKey, null, null);
    }
}
