package com.jnimble.platform.permission;

/**
 * Enum representing the status of a permission.
 */
public enum PermissionStatus {

    /** The permission is registered and available for role assignment. */
    AVAILABLE,

    /** The permission is unavailable (plugin disabled or uninstalled). */
    UNAVAILABLE
}
