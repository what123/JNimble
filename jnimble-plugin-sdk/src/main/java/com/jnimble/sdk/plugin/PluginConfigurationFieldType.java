package com.jnimble.sdk.plugin;

/**
 * Controls supported by the platform's generated plugin configuration form.
 */
public enum PluginConfigurationFieldType {
    /** Single-line text input. */
    TEXT,
    /** Password / sensitive value input. */
    SECRET,
    /** Numeric input. */
    NUMBER,
    /** Checkbox / toggle input. */
    BOOLEAN,
    /** Drop-down selector backed by {@link PluginConfigurationOption}. */
    SELECT,
    /** Multi-line text area. */
    TEXTAREA
}
