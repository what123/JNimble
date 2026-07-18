package com.jnimble.sdk.plugin;

/**
 * Database migration settings declared by a plugin.
 *
 * @param enabled whether plugin migrations should run when the plugin is enabled
 * @param location migration script location
 * @param table plugin-specific Flyway history table
 * @param baselineOnMigrate whether the migration runner should baseline existing schemas
 * @param failOnError whether migration failure should fail plugin enablement
 */
public record PluginMigration(
        Boolean enabled,
        String location,
        String table,
        Boolean baselineOnMigrate,
        Boolean failOnError
) {
}
