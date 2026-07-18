package com.jnimble.platform.permission;

import java.util.Collection;
import java.util.Optional;

/**
 * Service interface for managing roles and role-based access control.
 *
 * <p>Roles are used to group permissions and assign them to subjects (users).
 * This service provides methods for role CRUD, permission grants, and
 * subject-role assignments.</p>
 */
public interface RoleService {

    /**
     * Creates a new role.
     *
     * @param code the unique role code
     * @param name the human-readable role name
     * @return the created role record
     * @throws IllegalArgumentException if code already exists
     */
    RoleRecord createRole(String code, String name);

    /**
     * Finds a role by its ID.
     *
     * @param roleId the role ID
     * @return an {@link Optional} containing the role record if found, empty otherwise
     */
    Optional<RoleRecord> findRole(String roleId);

    /**
     * Finds a role by its code.
     *
     * @param code the role code
     * @return an {@link Optional} containing the role record if found, empty otherwise
     */
    Optional<RoleRecord> findRoleByCode(String code);

    /**
     * Lists all roles in the system.
     *
     * @return a collection of all role records
     */
    Collection<RoleRecord> listRoles();

    /**
     * Updates the name of a role.
     *
     * @param roleId the role ID
     * @param name   the new role name
     * @throws IllegalArgumentException if role not found
     */
    void updateRoleName(String roleId, String name);

    /**
     * Disables a role, preventing its permissions from being enforced.
     *
     * @param roleId the role ID to disable
     * @throws IllegalArgumentException if role not found
     */
    void disableRole(String roleId);

    /**
     * Grants a permission to a role.
     *
     * @param roleId         the role ID
     * @param permissionCode the permission code to grant
     * @throws IllegalArgumentException if role or permission not found
     */
    void grantPermission(String roleId, String permissionCode);

    /**
     * Revokes a permission from a role.
     *
     * @param roleId         the role ID
     * @param permissionCode the permission code to revoke
     * @throws IllegalArgumentException if role not found
     */
    void revokePermission(String roleId, String permissionCode);

    /**
     * Lists all permissions granted to a role.
     *
     * @param roleId the role ID
     * @return a collection of permission grants
     */
    Collection<RolePermissionGrant> listRolePermissions(String roleId);

    /**
     * Marks a permission grant as available for a specific permission code.
     *
     * @param permissionCode the permission code to mark available
     */
    void markPermissionGrantsAvailable(String permissionCode);

    /**
     * Marks all grants of a specific permission code as unavailable.
     *
     * @param permissionCode the permission code to mark unavailable
     */
    void markPermissionGrantsUnavailable(String permissionCode);

    /**
     * Assigns a role to a subject (user).
     *
     * @param subjectId the subject identifier (typically username)
     * @param roleId    the role ID to assign
     * @throws IllegalArgumentException if role not found
     */
    void grantRoleToSubject(String subjectId, String roleId);

    /**
     * Revokes a role from a subject (user).
     *
     * @param subjectId the subject identifier (typically username)
     * @param roleId    the role ID to revoke
     */
    void revokeRoleFromSubject(String subjectId, String roleId);

    /**
     * Lists all roles assigned to a subject.
     *
     * @param subjectId the subject identifier
     * @return a collection of subject-role grants
     */
    Collection<SubjectRoleGrant> listSubjectRoles(String subjectId);

    /**
     * Lists all subject-role grants in the system.
     *
     * @return a collection of all subject-role grants
     */
    Collection<SubjectRoleGrant> listSubjectRoles();

    /**
     * Lists all subjects assigned to a specific role.
     *
     * @param roleId the role ID
     * @return a collection of subject-role grants
     */
    Collection<SubjectRoleGrant> listRoleSubjects(String roleId);

    /**
     * Checks if a subject has a specific permission through any of their assigned roles.
     *
     * @param subjectId      the subject identifier
     * @param permissionCode the permission code to check
     * @return {@code true} if the subject has the permission
     */
    boolean subjectHasPermission(String subjectId, String permissionCode);
}
