package com.jnimble.platform.permission;

import java.util.Collection;
import java.util.Optional;

/**
 * Service interface for managing permissions in the system.
 *
 * <p>Permissions are registered by plugins and managed centrally. This service
 * provides methods for registering, querying, and checking permission availability.
 * Permission grants to roles are managed by {@link RoleService}.</p>
 */
public interface PermissionService {

    /**
     * Registers permissions declared by a plugin.
     *
     * <p>Existing permissions for the plugin are replaced. Permissions are
     * made available for role assignment after registration.</p>
     *
     * @param pluginId    the plugin identifier
     * @param permissions the collection of permission definitions to register
     * @throws IllegalArgumentException if pluginId is blank
     */
    void registerPluginPermissions(String pluginId, Collection<PermissionDefinition> permissions);

    /**
     * Marks all permissions registered by a plugin as unavailable.
     *
     * <p>Called when a plugin is disabled or uninstalled. Existing role grants
     * are preserved but marked as unavailable.</p>
     *
     * @param pluginId the plugin identifier
     */
    void markPluginPermissionsUnavailable(String pluginId);

    /**
     * Lists all permissions grouped by plugin.
     *
     * @return a collection of permission groups, one per plugin
     */
    Collection<PluginPermissionGroup> listPermissionsByPlugin();

    /**
     * Finds a permission by its code.
     *
     * @param permissionCode the unique permission code
     * @return an {@link Optional} containing the permission record if found, empty otherwise
     */
    Optional<PermissionRecord> findPermission(String permissionCode);

    /**
     * Checks if a permission is currently available (registered and active).
     *
     * @param permissionCode the permission code to check
     * @return {@code true} if the permission is available
     */
    boolean isPermissionAvailable(String permissionCode);

    /**
     * Checks if any permission with the given code exists in the system.
     *
     * @param permissionCode the permission code to check
     * @return {@code true} if the permission exists
     */
    boolean hasPermission(String permissionCode);
}
