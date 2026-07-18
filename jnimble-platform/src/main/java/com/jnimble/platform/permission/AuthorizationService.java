package com.jnimble.platform.permission;

/**
 * Service interface for checking authorization based on permissions.
 *
 * <p>Provides methods to determine if a subject or role has a specific permission.
 * This is a read-only service used for permission checks without modifying state.</p>
 */
public interface AuthorizationService {

    /**
     * Checks if a subject has a specific permission.
     *
     * @param subjectId      the subject identifier (typically username)
     * @param permissionCode the permission code to check
     * @return {@code true} if the subject has the permission
     */
    boolean hasPermission(String subjectId, String permissionCode);

    /**
     * Checks if a role has a specific permission.
     *
     * @param roleId         the role ID
     * @param permissionCode the permission code to check
     * @return {@code true} if the role has the permission
     */
    boolean roleHasPermission(String roleId, String permissionCode);
}
