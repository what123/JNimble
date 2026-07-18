package com.jnimble.sdk.plugin;

/**
 * Persistent plugin status values used by platform implementations.
 */
public enum PluginStatus {
    /** The plugin has been discovered but is not yet installed. */
    DISCOVERED,
    /** The plugin has been installed. */
    INSTALLED,
    /** The plugin is currently enabled and running. */
    ENABLED,
    /** The plugin has been disabled. */
    DISABLED,
    /** The plugin is in a failed state. */
    FAILED,
    /** The plugin failed during migration. */
    MIGRATION_FAILED,
    /** The plugin is incompatible with the current platform version. */
    INCOMPATIBLE,
    /** The plugin has been uninstalled. */
    UNINSTALLED
}
