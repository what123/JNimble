package com.jnimble.sdk.plugin;

/**
 * Lifecycle phases exposed by the minimal plugin protocol.
 */
public enum PluginLifecyclePhase {
    /** The plugin is being installed. */
    INSTALLING,
    /** The plugin has been installed. */
    INSTALLED,
    /** The plugin is being enabled. */
    ENABLING,
    /** The plugin has been enabled and is running. */
    ENABLED,
    /** The plugin is being disabled. */
    DISABLING,
    /** The plugin has been disabled. */
    DISABLED,
    /** The plugin is being reloaded. */
    RELOADING,
    /** The plugin is being uninstalled. */
    UNINSTALLING,
    /** The plugin has been uninstalled. */
    UNINSTALLED,
    /** The plugin has entered a failed state. */
    FAILED
}
