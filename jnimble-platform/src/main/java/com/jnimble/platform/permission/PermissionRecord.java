package com.jnimble.platform.permission;

import java.time.Instant;

/**
 * Immutable record representing a registered permission in the system.
 *
 * @param pluginId         the plugin that registered this permission
 * @param code             the unique permission code
 * @param name             the human-readable permission name
 * @param nameKey          the i18n message key for the name
 * @param description      the permission description
 * @param descriptionKey   the i18n message key for the description
 * @param status           the permission status
 * @param updatedAt        the timestamp when the permission was last updated
 */
public record PermissionRecord(
        String pluginId,
        String code,
        String name,
        String nameKey,
        String description,
        String descriptionKey,
        PermissionStatus status,
        Instant updatedAt
) {

    /**
     * Checks if the permission is currently available.
     *
     * @return {@code true} if status is {@link PermissionStatus#AVAILABLE}
     */
    public boolean available() {
        return status == PermissionStatus.AVAILABLE;
    }

    /**
     * Returns a copy with an updated status.
     *
     * @param status    the new status
     * @param updatedAt the update timestamp
     * @return a new record with the updated status
     */
    public PermissionRecord withStatus(PermissionStatus status, Instant updatedAt) {
        return new PermissionRecord(pluginId, code, name, nameKey, description, descriptionKey, status, updatedAt);
    }
}
