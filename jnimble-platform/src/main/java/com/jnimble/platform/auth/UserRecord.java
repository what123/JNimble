package com.jnimble.platform.auth;

import java.time.Instant;

/**
 * Immutable record representing a user account in the system.
 *
 * @param id            the unique user identifier
 * @param username      the unique username
 * @param passwordHash  the encoded password hash
 * @param displayName   the human-readable display name
 * @param status        the account status
 * @param createdAt     the timestamp when the account was created
 * @param updatedAt     the timestamp when the account was last updated
 */
public record UserRecord(
        String id,
        String username,
        String passwordHash,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Checks if the user account is active.
     *
     * @return {@code true} if status is {@link UserStatus#ACTIVE}
     */
    public boolean active() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Returns the subject identifier for authorization purposes.
     *
     * @return the username
     */
    public String subjectId() {
        return username;
    }

    /**
     * Returns a copy with an updated display name.
     *
     * @param displayName the new display name
     * @param updatedAt   the update timestamp
     * @return a new record with the updated display name
     */
    public UserRecord withDisplayName(String displayName, Instant updatedAt) {
        return new UserRecord(id, username, passwordHash, displayName, status, createdAt, updatedAt);
    }

    /**
     * Returns a copy with an updated password hash.
     *
     * @param passwordHash the new password hash
     * @param updatedAt    the update timestamp
     * @return a new record with the updated password hash
     */
    public UserRecord withPasswordHash(String passwordHash, Instant updatedAt) {
        return new UserRecord(id, username, passwordHash, displayName, status, createdAt, updatedAt);
    }

    /**
     * Returns a copy with an updated status.
     *
     * @param status    the new status
     * @param updatedAt the update timestamp
     * @return a new record with the updated status
     */
    public UserRecord withStatus(UserStatus status, Instant updatedAt) {
        return new UserRecord(id, username, passwordHash, displayName, status, createdAt, updatedAt);
    }
}
