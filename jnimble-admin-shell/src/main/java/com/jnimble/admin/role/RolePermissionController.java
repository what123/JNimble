package com.jnimble.admin.role;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.permission.PermissionRecord;
import com.jnimble.platform.permission.PermissionService;
import com.jnimble.platform.permission.PluginPermissionGroup;
import com.jnimble.platform.permission.RolePermissionGrant;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.platform.permission.SuperAdminPermissionService;
import com.jnimble.platform.permission.SystemPermissions;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for role and permission management.
 *
 * <p>Provides endpoints for listing roles, editing role permissions,
 * and managing role assignments. All operations require appropriate permissions.</p>
 */
@Controller
public class RolePermissionController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AdminAuditRecorder auditRecorder;
    private final ControllerAuthorization authorization;
    private final MessageSource messageSource;
    private final SuperAdminPermissionService superAdminPermissionService;

    /**
     * Creates a new role permission controller.
     *
     * @param roleService                 the role service
     * @param permissionService           the permission service
     * @param auditRecorder               the audit recorder
     * @param authorization               the authorization service
     * @param messageSource               the message source for i18n
     * @param superAdminPermissionService the super admin permission service
     */
    public RolePermissionController(
            RoleService roleService,
            PermissionService permissionService,
            AdminAuditRecorder auditRecorder,
            ControllerAuthorization authorization,
            MessageSource messageSource,
            SuperAdminPermissionService superAdminPermissionService
    ) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.auditRecorder = auditRecorder;
        this.authorization = authorization;
        this.messageSource = messageSource;
        this.superAdminPermissionService = superAdminPermissionService;
    }

    /**
     * Lists all roles.
     *
     * @param model the view model
     * @return the role list template name
     */
    @GetMapping("/admin/roles")
    public String listRoles(Model model) {
        authorization.requirePermission(SystemPermissions.ROLE_VIEW);
        List<RoleRecord> roles = roleService.listRoles().stream()
                .sorted(Comparator.comparing(RoleRecord::code))
                .toList();
        model.addAttribute("roles", roles);
        model.addAttribute("activeNav", "roles");
        return "page/role/list";
    }

    /**
     * Displays the create role form.
     *
     * @param model the view model
     * @return the create role template name
     */
    @GetMapping("/admin/roles/create")
    public String createRoleForm(Model model) {
        authorization.requirePermission(SystemPermissions.ROLE_MANAGE);
        model.addAttribute("permissionGroups", buildPermissionGroups(null));
        model.addAttribute("messageSource", messageSource);
        model.addAttribute("activeNav", "roles");
        return "page/role/create";
    }

    /**
     * Creates a role and redirects to its permission editor.
     *
     * @param name               human-readable role name
     * @param permissionCodes    selected permission codes, or {@code null} when none are selected
     * @param redirectAttributes redirect attributes used for result notices
     * @return redirect to the new role or back to the create form when creation fails
     */
    @PostMapping("/admin/roles")
    public String createRole(
            @RequestParam String name,
            @RequestParam(name = "permissionCodes", required = false) List<String> permissionCodes,
            RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.ROLE_MANAGE);
        try {
            String roleName = requireRoleFormValue(name, "角色名称", 255);
            String roleCode = normalizeRoleName(roleName);
            if (roleService.findRoleByCode(roleCode).isPresent()) {
                roleCode = roleCode + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            }
            RoleRecord role = roleService.createRole(roleCode, roleName);

            Set<String> availablePermissions = permissionService.listPermissionsByPlugin().stream()
                    .map(PluginPermissionGroup::permissions)
                    .flatMap(Collection::stream)
                    .filter(PermissionRecord::available)
                    .map(PermissionRecord::code)
                    .collect(Collectors.toSet());
            if (permissionCodes != null) {
                for (String permissionCode : permissionCodes) {
                    if (availablePermissions.contains(permissionCode)) {
                        roleService.grantPermission(role.id(), permissionCode);
                    }
                }
            }

            String message = "角色已创建。";
            redirectAttributes.addFlashAttribute("message", message);
            auditRecorder.success(AuditActions.ROLE_CREATE, "role", role.id(), message);
            return "redirect:/admin/roles/" + role.id();
        } catch (RuntimeException ex) {
            String message = "创建角色失败：" + failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", message);
            redirectAttributes.addFlashAttribute("createRoleName", name);
            redirectAttributes.addFlashAttribute("createPermissionCodes", permissionCodes);
            auditRecorder.failure(AuditActions.ROLE_CREATE, "role", name, message);
            return "redirect:/admin/roles/create";
        }
    }

    private static String normalizeRoleName(String name) {
        return name.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private String requireRoleFormValue(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空。");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过 " + maxLength + " 个字符。");
        }
        return normalized;
    }

    /**
     * Displays the role edit form with name and permissions.
     *
     * @param roleId the role ID
     * @param model  the view model
     * @return the role edit template name
     * @throws ResponseStatusException if role not found
     */
    @GetMapping("/admin/roles/{roleId}")
    public String editRolePermissions(@PathVariable String roleId, Model model) {
        authorization.requirePermission(SystemPermissions.ROLE_VIEW);
        RoleRecord role = findRoleOrThrow(roleId);
        model.addAttribute("role", role);
        model.addAttribute("permissionGroups", buildPermissionGroups(roleId));
        model.addAttribute("messageSource", messageSource);
        model.addAttribute("activeNav", "roles");
        return "page/role/edit";
    }

    /**
     * Saves the role name and permission grants.
     */
    @PostMapping("/admin/roles/{roleId}/permissions")
    public String saveRolePermissions(
            @PathVariable String roleId,
            @RequestParam String name,
            @RequestParam(name = "permissionCodes", required = false) List<String> permissionCodes,
            RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.ROLE_MANAGE);
        try {
            findRoleOrThrow(roleId);

            String roleName = requireRoleFormValue(name, "角色名称", 255);
            roleService.updateRoleName(roleId, roleName);

            Set<String> submittedPermissions = permissionCodes == null
                    ? Set.of()
                    : new HashSet<>(permissionCodes);
            Set<String> availablePermissions = permissionService.listPermissionsByPlugin().stream()
                    .map(PluginPermissionGroup::permissions)
                    .flatMap(Collection::stream)
                    .filter(PermissionRecord::available)
                    .map(PermissionRecord::code)
                    .collect(Collectors.toSet());
            if (superAdminPermissionService.isSuperAdminRole(roleId)) {
                submittedPermissions = availablePermissions;
            }
            Set<String> currentPermissions = roleService.listRolePermissions(roleId).stream()
                    .map(RolePermissionGrant::permissionCode)
                    .collect(Collectors.toSet());

            for (String permissionCode : submittedPermissions) {
                if (availablePermissions.contains(permissionCode) && !currentPermissions.contains(permissionCode)) {
                    roleService.grantPermission(roleId, permissionCode);
                }
            }

            for (String permissionCode : currentPermissions) {
                if (availablePermissions.contains(permissionCode) && !submittedPermissions.contains(permissionCode)) {
                    roleService.revokePermission(roleId, permissionCode);
                }
            }

            redirectAttributes.addFlashAttribute("message", "角色已保存。");
            auditRecorder.success(AuditActions.ROLE_PERMISSIONS_UPDATE, "role", roleId, "角色已保存。");
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", message);
            auditRecorder.failure(AuditActions.ROLE_PERMISSIONS_UPDATE, "role", roleId, message);
        }

        return "redirect:/admin/roles/" + roleId;
    }

    private RoleRecord findRoleOrThrow(String roleId) {
        return roleService.findRole(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleId));
    }

    private String failureMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private List<PluginPermissionGroupView> buildPermissionGroups(String roleId) {
        Set<String> grantedPermissions = roleId == null
                ? Set.of()
                : roleService.listRolePermissions(roleId).stream()
                    .map(RolePermissionGrant::permissionCode)
                    .collect(Collectors.toSet());

        return permissionService.listPermissionsByPlugin().stream()
                .sorted(Comparator.comparing(PluginPermissionGroup::pluginId))
                .map(group -> new PluginPermissionGroupView(
                        group.pluginId(),
                        group.permissions().stream()
                                .sorted(Comparator.comparing(PermissionRecord::code))
                                .map(permission -> PermissionView.from(permission,
                                        grantedPermissions.contains(permission.code())))
                                .toList()))
                .toList();
    }

    /**
     * Permission group prepared specifically for rendering the edit page.
     *
     * @param pluginId    owner plugin ID
     * @param permissions permissions with role-specific grant state
     */
    public record PluginPermissionGroupView(String pluginId, List<PermissionView> permissions) {
    }

    /**
     * Permission row prepared specifically for rendering the edit page.
     *
     * @param code      permission code
     * @param name      fallback display name
     * @param nameKey   localized display-name key
     * @param available whether its owner plugin currently exposes the permission
     * @param granted   whether the role has a persisted grant
     */
    public record PermissionView(
            String code,
            String name,
            String nameKey,
            boolean available,
            boolean granted) {

        private static PermissionView from(PermissionRecord permission, boolean granted) {
            return new PermissionView(
                    permission.code(),
                    permission.name(),
                    permission.nameKey(),
                    permission.available(),
                    granted);
        }

        /**
         * Resolves the localized name and falls back to the stored name or permission code.
         *
         * @param messageSource application message source
         * @return display name for the current locale
         */
        public String displayName(MessageSource messageSource) {
            if (nameKey != null && !nameKey.isBlank()) {
                try {
                    return messageSource.getMessage(nameKey.trim(), null, LocaleContextHolder.getLocale());
                } catch (NoSuchMessageException ignored) {
                    // Use the stored fallback below.
                }
            }
            return name == null || name.isBlank() ? code : name;
        }
    }
}
