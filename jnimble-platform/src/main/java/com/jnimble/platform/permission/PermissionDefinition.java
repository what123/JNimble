package com.jnimble.platform.permission;

import com.jnimble.sdk.plugin.PluginPermission;

/**
 * Immutable record defining a permission declared by a plugin.
 *
 * <p>This is used during plugin registration to define new permissions
 * before they are persisted as {@link PermissionRecord} instances.</p>
 *
 * @param code             the unique permission code
 * @param name             the human-readable permission name
 * @param nameKey          the i18n message key for the name
 * @param description      the permission description (optional)
 * @param descriptionKey   the i18n message key for the description (optional)
 */
public record PermissionDefinition(
        String code,
        String name,
        String nameKey,
        String description,
        String descriptionKey
) {

    /**
     * Creates a permission definition without description.
     *
     * @param code    the unique permission code
     * @param name    the human-readable permission name
     * @param nameKey the i18n message key for the name
     */
    public PermissionDefinition(String code, String name, String nameKey) {
        this(code, name, nameKey, null, null);
    }

    /**
     * Creates a permission definition from a plugin permission descriptor.
     *
     * @param permission the plugin permission to convert
     * @return a new permission definition
     */
    public static PermissionDefinition from(PluginPermission permission) {
        return new PermissionDefinition(
                permission.code(),
                permission.name(),
                permission.nameKey(),
                permission.description(),
                permission.descriptionKey()
        );
    }
}
