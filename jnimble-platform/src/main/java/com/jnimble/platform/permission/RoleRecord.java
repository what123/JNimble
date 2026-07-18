package com.jnimble.platform.permission;

import java.time.Instant;

/**
 * Immutable record representing a role in the system.
 *
 * @param id        the unique role identifier
 * @param code      the unique role code
 * @param name      the human-readable role name
 * @param status    the role status
 * @param createdAt the timestamp when the role was created
 * @param updatedAt the timestamp when the role was last updated
 */
public record RoleRecord(
        String id,
        String code,
        String name,
        RoleStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Checks if the role is active.
     *
     * @return {@code true} if status is {@link RoleStatus#ACTIVE}
     */
    public boolean active() {
        return status == RoleStatus.ACTIVE;
    }

    /**
     * Returns a copy with an updated status.
     *
     * @param status    the new status
     * @param updatedAt the update timestamp
     * @return a new record with the updated status
     */
    public RoleRecord withStatus(RoleStatus status, Instant updatedAt) {
        return new RoleRecord(id, code, name, status, createdAt, updatedAt);
    }
}
