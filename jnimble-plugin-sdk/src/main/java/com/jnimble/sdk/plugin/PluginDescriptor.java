package com.jnimble.sdk.plugin;

import java.util.List;

/**
 * Descriptor parsed from {@code META-INF/jnimble-plugin.json}.
 *
 * @param schemaVersion descriptor schema version
 * @param id globally unique plugin id
 * @param name fallback display name
 * @param nameKey optional i18n key for display name
 * @param description optional fallback description
 * @param descriptionKey optional i18n key for description
 * @param version plugin semantic version
 * @param platformVersion compatible platform version expression
 * @param author optional author display value
 * @param website optional website URL
 * @param bootClass fully qualified class name implementing {@link PluginBoot}
 * @param i18n optional i18n descriptor
 * @param admin optional admin entry descriptor
 * @param spring optional Spring child-context descriptor
 * @param dependencies declared plugin dependencies
 * @param configuration optional declarative configuration form descriptor
 * @param permissions declared plugin permissions
 * @param migration optional migration descriptor
 */
public record PluginDescriptor(
        String schemaVersion,
        String id,
        String name,
        String nameKey,
        String description,
        String descriptionKey,
        String version,
        String platformVersion,
        String author,
        String website,
        String bootClass,
        PluginI18n i18n,
        PluginAdminDescriptor admin,
        PluginSpringDescriptor spring,
        List<PluginDependency> dependencies,
        PluginConfigurationDescriptor configuration,
        List<PluginPermission> permissions,
        PluginMigration migration
) {

    /**
     * Creates a descriptor with no {@code configuration} form.
     *
     * @param schemaVersion descriptor schema version
     * @param id            globally unique plugin id
     * @param name          fallback display name
     * @param nameKey       optional i18n key for display name
     * @param description   optional fallback description
     * @param descriptionKey optional i18n key for description
     * @param version       plugin semantic version
     * @param platformVersion compatible platform version expression
     * @param author        optional author display value
     * @param website       optional website URL
     * @param bootClass     fully qualified class name implementing {@link PluginBoot}
     * @param i18n          optional i18n descriptor
     * @param admin         optional admin entry descriptor
     * @param spring        optional Spring child-context descriptor
     * @param dependencies  declared plugin dependencies
     * @param permissions   declared plugin permissions
     * @param migration     optional migration descriptor
     */
    public PluginDescriptor(
            String schemaVersion,
            String id,
            String name,
            String nameKey,
            String description,
            String descriptionKey,
            String version,
            String platformVersion,
            String author,
            String website,
            String bootClass,
            PluginI18n i18n,
            PluginAdminDescriptor admin,
            PluginSpringDescriptor spring,
            List<PluginDependency> dependencies,
            List<PluginPermission> permissions,
            PluginMigration migration
    ) {
        this(
                schemaVersion, id, name, nameKey, description, descriptionKey,
                version, platformVersion, author, website, bootClass, i18n,
                admin, spring, dependencies, null, permissions, migration
        );
    }

    /**
     * Creates a descriptor with no Spring context or configuration form.
     *
     * @param schemaVersion descriptor schema version
     * @param id            globally unique plugin id
     * @param name          fallback display name
     * @param nameKey       optional i18n key for display name
     * @param description   optional fallback description
     * @param descriptionKey optional i18n key for description
     * @param version       plugin semantic version
     * @param platformVersion compatible platform version expression
     * @param author        optional author display value
     * @param website       optional website URL
     * @param bootClass     fully qualified class name implementing {@link PluginBoot}
     * @param i18n          optional i18n descriptor
     * @param admin         optional admin entry descriptor
     * @param permissions   declared plugin permissions
     * @param migration     optional migration descriptor
     */
    public PluginDescriptor(
            String schemaVersion,
            String id,
            String name,
            String nameKey,
            String description,
            String descriptionKey,
            String version,
            String platformVersion,
            String author,
            String website,
            String bootClass,
            PluginI18n i18n,
            PluginAdminDescriptor admin,
            List<PluginPermission> permissions,
            PluginMigration migration
    ) {
        this(
                schemaVersion, id, name, nameKey, description, descriptionKey,
                version, platformVersion, author, website, bootClass, i18n,
                admin, null, null, null, permissions, migration
        );
    }

    /**
     * Creates a descriptor with no admin, Spring context, dependencies,
     * or configuration form.
     *
     * @param schemaVersion descriptor schema version
     * @param id            globally unique plugin id
     * @param name          fallback display name
     * @param nameKey       optional i18n key for display name
     * @param description   optional fallback description
     * @param descriptionKey optional i18n key for description
     * @param version       plugin semantic version
     * @param platformVersion compatible platform version expression
     * @param author        optional author display value
     * @param website       optional website URL
     * @param bootClass     fully qualified class name implementing {@link PluginBoot}
     * @param i18n          optional i18n descriptor
     * @param permissions   declared plugin permissions
     * @param migration     optional migration descriptor
     */
    public PluginDescriptor(
            String schemaVersion,
            String id,
            String name,
            String nameKey,
            String description,
            String descriptionKey,
            String version,
            String platformVersion,
            String author,
            String website,
            String bootClass,
            PluginI18n i18n,
            List<PluginPermission> permissions,
            PluginMigration migration
    ) {
        this(
                schemaVersion,
                id,
                name,
                nameKey,
                description,
                descriptionKey,
                version,
                platformVersion,
                author,
                website,
                bootClass,
                i18n,
                null,
                null,
                null,
                null,
                permissions,
                migration
        );
    }
}
