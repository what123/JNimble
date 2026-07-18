package com.jnimble.kernel.migration;

import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginMigration;

import java.util.Objects;

/**
 * Effective migration settings for one plugin.
 *
 * @param pluginId         the plugin identifier
 * @param enabled          whether migration is enabled
 * @param location         the migration script location
 * @param historyTable     the Flyway history table name
 * @param baselineOnMigrate whether to baseline on first migration
 * @param failOnError      whether to fail on migration errors
 */
public record PluginMigrationConfig(
        String pluginId,
        boolean enabled,
        String location,
        String historyTable,
        boolean baselineOnMigrate,
        boolean failOnError
) {

    /** Default prefix for migration script locations. */
    public static final String DEFAULT_LOCATION_PREFIX = "classpath:db/migration/plugin/";

    /** Default prefix for Flyway history tables. */
    public static final String DEFAULT_HISTORY_TABLE_PREFIX = "flyway_schema_history_";

    /**
     * Compact constructor with validation.
     */
    public PluginMigrationConfig {
        pluginId = requireNonBlank(pluginId, "pluginId");
        location = requireNonBlank(location, "location");
        historyTable = requireNonBlank(historyTable, "historyTable");
    }

    /**
     * Creates a config from a plugin descriptor.
     *
     * @param descriptor the plugin descriptor
     * @return the migration config
     * @throws NullPointerException if descriptor is null
     */
    public static PluginMigrationConfig from(PluginDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor is required");

        String pluginId = requireNonBlank(descriptor.id(), "descriptor.id");
        PluginMigration migration = descriptor.migration();
        if (migration == null) {
            return defaults(pluginId);
        }

        return new PluginMigrationConfig(
                pluginId,
                valueOrDefault(migration.enabled(), false),
                valueOrDefault(migration.location(), defaultLocation(pluginId)),
                valueOrDefault(migration.table(), defaultHistoryTable(pluginId)),
                valueOrDefault(migration.baselineOnMigrate(), true),
                valueOrDefault(migration.failOnError(), true)
        );
    }

    /**
     * Returns default config for a plugin (migration disabled).
     *
     * @param pluginId the plugin identifier
     * @return default config
     */
    public static PluginMigrationConfig defaults(String pluginId) {
        String normalizedPluginId = requireNonBlank(pluginId, "pluginId");
        return new PluginMigrationConfig(
                normalizedPluginId,
                false,
                defaultLocation(normalizedPluginId),
                defaultHistoryTable(normalizedPluginId),
                true,
                true
        );
    }

    /**
     * Returns the default migration location for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return the location path
     */
    public static String defaultLocation(String pluginId) {
        return DEFAULT_LOCATION_PREFIX + requireNonBlank(pluginId, "pluginId");
    }

    /**
     * Returns the default history table name for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return the table name
     */
    public static String defaultHistoryTable(String pluginId) {
        return DEFAULT_HISTORY_TABLE_PREFIX + normalizePluginId(pluginId);
    }

    /**
     * Normalizes a plugin ID for use in table names.
     *
     * @param pluginId the plugin identifier
     * @return the normalized ID
     */
    public static String normalizePluginId(String pluginId) {
        return requireNonBlank(pluginId, "pluginId").replace('-', '_');
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean valueOrDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String requireNonBlank(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
