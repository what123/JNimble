package com.jnimble.platform.permission;

import com.jnimble.platform.auth.AuthProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initializes the default admin role and permissions on application startup.
 *
 * <p>Registers system permissions, creates the default admin role if needed,
 * grants all permissions to the role, and assigns the role to the default admin user.</p>
 */
@Component
@Order(1)
public class DefaultRoleInitializer implements ApplicationRunner {

    private final AuthProperties authProperties;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final SuperAdminPermissionService superAdminPermissionService;

    /**
     * Creates a new default role initializer.
     *
     * @param authProperties             the authentication properties
     * @param permissionService          the permission service
     * @param roleService                the role service
     * @param superAdminPermissionService the super admin permission service
     */
    public DefaultRoleInitializer(
            AuthProperties authProperties,
            PermissionService permissionService,
            RoleService roleService,
            SuperAdminPermissionService superAdminPermissionService
    ) {
        this.authProperties = authProperties;
        this.permissionService = permissionService;
        this.roleService = roleService;
        this.superAdminPermissionService = superAdminPermissionService;
    }

    /**
     * Initializes system permissions and the default admin role.
     *
     * @param args the application arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        permissionService.registerPluginPermissions(SystemPermissions.OWNER, SystemPermissions.definitions());
        String roleCode = authProperties.getDefaultUser().getRole();
        RoleRecord role = ensureRole(roleCode);
        superAdminPermissionService.grantAllAvailablePermissions(role.id());
        roleService.grantRoleToSubject(authProperties.getDefaultUser().getUsername(), role.id());
    }

    private RoleRecord ensureRole(String roleCode) {
        String normalizedCode = normalizeRoleCode(roleCode);
        return roleService.findRoleByCode(normalizedCode)
                .orElseGet(() -> roleService.createRole(normalizedCode, normalizedCode));
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "ADMIN";
        }
        return roleCode.trim().toUpperCase();
    }
}
