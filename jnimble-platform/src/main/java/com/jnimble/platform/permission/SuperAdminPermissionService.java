package com.jnimble.platform.permission;

import com.jnimble.platform.auth.AuthProperties;
import java.util.Collection;
import org.springframework.stereotype.Component;

/**
 * Service for managing super admin role and granting all available permissions.
 * Handles the special case of the super admin role which has unrestricted access.
 */
@Component
public class SuperAdminPermissionService {

    private final AuthProperties authProperties;
    private final PermissionService permissionService;
    private final RoleService roleService;

    public SuperAdminPermissionService(
            AuthProperties authProperties,
            PermissionService permissionService,
            RoleService roleService
    ) {
        this.authProperties = authProperties;
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    public boolean isSuperAdminRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return false;
        }
        return roleService.findRole(roleId.trim())
                .map(RoleRecord::code)
                .map(this::isSuperAdminRoleCode)
                .orElse(false);
    }

    public boolean isSuperAdminRoleCode(String roleCode) {
        return normalizeRoleCode(roleCode).equals(superAdminRoleCode());
    }

    public void grantAllAvailablePermissions() {
        roleService.findRoleByCode(superAdminRoleCode())
                .ifPresent(role -> grantAllAvailablePermissions(role.id()));
    }

    public void grantAllAvailablePermissions(String roleId) {
        allAvailablePermissionCodes().forEach(permissionCode -> roleService.grantPermission(roleId, permissionCode));
    }

    public Collection<String> allAvailablePermissionCodes() {
        return permissionService.listPermissionsByPlugin().stream()
                .map(PluginPermissionGroup::permissions)
                .flatMap(Collection::stream)
                .filter(PermissionRecord::available)
                .map(PermissionRecord::code)
                .toList();
    }

    private String superAdminRoleCode() {
        return normalizeRoleCode(authProperties.getDefaultUser().getRole());
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "ADMIN";
        }
        return roleCode.trim().toUpperCase();
    }
}
