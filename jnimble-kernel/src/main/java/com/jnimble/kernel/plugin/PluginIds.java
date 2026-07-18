package com.jnimble.kernel.plugin;

import java.util.regex.Pattern;

/**
 * Utility class for validating plugin identifiers.
 *
 * <p>Plugin IDs must match the pattern {@code ^[a-z][a-z0-9-]*$}.</p>
 */
public final class PluginIds {

    /** The pattern for valid plugin IDs. */
    public static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    private PluginIds() {
    }

    /**
     * Validates and normalizes a plugin ID.
     *
     * @param pluginId the plugin ID to validate
     * @return the normalized plugin ID
     * @throws IllegalArgumentException if the ID is blank or invalid
     */
    public static String requireValid(String pluginId) {
        return requireValid(pluginId, "pluginId");
    }

    /**
     * Validates and normalizes a plugin ID with a custom field name.
     *
     * @param pluginId the plugin ID to validate
     * @param field    the field name for error messages
     * @return the normalized plugin ID
     * @throws IllegalArgumentException if the ID is blank or invalid
     */
    public static String requireValid(String pluginId, String field) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = pluginId.trim();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " must match " + PATTERN.pattern());
        }
        return normalized;
    }

    /**
     * Checks if a plugin ID is valid.
     *
     * @param pluginId the plugin ID to check
     * @return {@code true} if the ID is valid
     */
    public static boolean isValid(String pluginId) {
        return pluginId != null && PATTERN.matcher(pluginId).matches();
    }
}
