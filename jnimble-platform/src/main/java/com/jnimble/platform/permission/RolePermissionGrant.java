package com.jnimble.platform.permission;

import java.time.Instant;

/**
 * Immutable record representing a permission grant to a role.
 *
 * @param roleId         the role identifier
 * @param permissionCode the permission code
 * @param status         the grant status
 * @param grantedAt      the timestamp when the permission was granted
 * @param updatedAt      the timestamp when the grant was last updated
 */
public record RolePermissionGrant(
        String roleId,
        String permissionCode,
        PermissionStatus status,
        Instant grantedAt,
        Instant updatedAt
) {

    /**
     * Checks if this grant is currently available.
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
    public RolePermissionGrant withStatus(PermissionStatus status, Instant updatedAt) {
        return new RolePermissionGrant(roleId, permissionCode, status, grantedAt, updatedAt);
    }
}
