package com.jnimble.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.admin.role.RolePermissionController;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.PermissionRecord;
import com.jnimble.platform.permission.PermissionService;
import com.jnimble.platform.permission.PermissionStatus;
import com.jnimble.platform.permission.PluginPermissionGroup;
import com.jnimble.platform.permission.RolePermissionGrant;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.platform.permission.SuperAdminPermissionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * RolePermissionController 单元测试。
 *
 * <p>使用 MockMvc 独立模式测试角色和权限管理端点的
 * 列表查看、详情编辑和权限操作行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SuperAdminPermissionService superAdminPermissionService;

    @BeforeEach
    void setUp() {
        RolePermissionController controller = new RolePermissionController(
                roleService, permissionService, auditRecorder, authorization, messageSource, superAdminPermissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 测试获取角色列表页面，验证返回正确视图和 Model 属性。
     */
    @Test
    void listRolesReturnsRoleListView() throws Exception {
        RoleRecord role = new RoleRecord("r1", "admin", "Administrator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.listRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/role/list"))
                .andExpect(model().attribute("activeNav", "roles"))
                .andExpect(model().attributeExists("roles"));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试获取角色详情页面，验证返回正确视图和 Model 属性。
     */
    @Test
    void editRolePermissionsReturnsDetailView() throws Exception {
        RoleRecord role = new RoleRecord("r1", "admin", "Administrator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.findRole("r1")).thenReturn(Optional.of(role));
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());

        mockMvc.perform(get("/admin/roles/{roleId}", "r1"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/role/edit"))
                .andExpect(model().attribute("role", role))
                .andExpect(model().attribute("activeNav", "roles"))
                .andExpect(model().attributeExists("permissionGroups"));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试角色不存在时返回 404 状态码。
     */
    @Test
    void editRolePermissionsNotFoundThrows404() throws Exception {
        when(roleService.findRole("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/roles/{roleId}", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    /**
     * 测试角色列表按 code 排序后返回。
     */
    @Test
    void listRolesReturnsSortedByCode() throws Exception {
        RoleRecord roleB = new RoleRecord("r2", "beta", "Beta Role",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        RoleRecord roleA = new RoleRecord("r1", "alpha", "Alpha Role",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.listRoles()).thenReturn(List.of(roleB, roleA));

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("roles", List.of(roleA, roleB)));
    }

    /**
     * 测试角色列表为空时返回空列表。
     */
    @Test
    void listRolesWithEmptyListReturnsEmptyRoles() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of());

        mockMvc.perform(get("/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("roles", List.of()));
    }

    /**
     * 测试获取创建角色页面，返回正确视图和 Model 属性。
     */
    @Test
    void createRoleFormReturnsCreateView() throws Exception {
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());

        mockMvc.perform(get("/admin/roles/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/role/create"))
                .andExpect(model().attribute("activeNav", "roles"))
                .andExpect(model().attributeExists("permissionGroups"));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试创建角色成功后直接进入新角色的权限配置页。
     */
    @Test
    void createRoleRedirectsToPermissionEditor() throws Exception {
        Instant now = Instant.now();
        RoleRecord role = new RoleRecord("r2", "收银员", "收银员",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, now, now);
        when(roleService.findRoleByCode("收银员")).thenReturn(Optional.empty());
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());
        when(roleService.createRole("收银员", "收银员")).thenReturn(role);

        mockMvc.perform(post("/admin/roles")
                        .param("name", "收银员"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/r2"))
                .andExpect(flash().attribute("message", "角色已创建。"));

        verify(roleService).createRole("收银员", "收银员");
        verify(auditRecorder).success("role.create", "role", "r2", "角色已创建。");
    }

    /**
     * 测试创建角色时附带权限。
     */
    @Test
    void createRoleWithPermissions() throws Exception {
        Instant now = Instant.now();
        RoleRecord role = new RoleRecord("r2", "收银员", "收银员",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, now, now);
        PermissionRecord manage = permission("menu.manage", PermissionStatus.AVAILABLE, now);
        PermissionRecord view = permission("menu.view", PermissionStatus.AVAILABLE, now);
        when(roleService.findRoleByCode("收银员")).thenReturn(Optional.empty());
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(
                new PluginPermissionGroup("menu-manager", List.of(view, manage))));
        when(roleService.createRole("收银员", "收银员")).thenReturn(role);

        mockMvc.perform(post("/admin/roles")
                        .param("name", "收银员")
                        .param("permissionCodes", "menu.view", "menu.manage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/r2"));

        verify(roleService).createRole("收银员", "收银员");
        verify(roleService).grantPermission("r2", "menu.view");
        verify(roleService).grantPermission("r2", "menu.manage");
    }

    /**
     * 测试后端校验角色名称，不能只依赖浏览器 required 属性。
     */
    @Test
    void createRoleRejectsBlankName() throws Exception {
        mockMvc.perform(post("/admin/roles")
                        .param("name", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/create"))
                .andExpect(flash().attribute("error", "创建角色失败：角色名称不能为空。"))
                .andExpect(flash().attribute("createRoleName", "   "));

        verify(roleService, never()).createRole(any(), any());
    }

    /**
     * 测试获取角色详情时验证权限服务被调用。
     */
    @Test
    void editRolePermissionsVerifiesPermissionService() throws Exception {
        RoleRecord role = new RoleRecord("r1", "admin", "Administrator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.findRole("r1")).thenReturn(Optional.of(role));
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());

        mockMvc.perform(get("/admin/roles/{roleId}", "r1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("permissionGroups", List.of()));

        verify(permissionService).listPermissionsByPlugin();
    }

    /**
     * 测试详情页将角色授权状态合并到权限视图，避免模板读取不存在的属性。
     */
    @Test
    void editRolePermissionsBuildsRoleSpecificPermissionView() throws Exception {
        Instant now = Instant.now();
        RoleRecord role = new RoleRecord("r1", "operator", "Operator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, now, now);
        PermissionRecord manage = permission("menu.manage", PermissionStatus.AVAILABLE, now);
        PermissionRecord view = permission("menu.view", PermissionStatus.AVAILABLE, now);
        when(roleService.findRole("r1")).thenReturn(Optional.of(role));
        when(roleService.listRolePermissions("r1")).thenReturn(List.of(
                new RolePermissionGrant("r1", "menu.view", PermissionStatus.AVAILABLE, now, now)));
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(
                new PluginPermissionGroup("menu-manager", List.of(view, manage))));

        mockMvc.perform(get("/admin/roles/{roleId}", "r1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("permissionGroups", List.of(
                        new RolePermissionController.PluginPermissionGroupView("menu-manager", List.of(
                                new RolePermissionController.PermissionView(
                                        "menu.manage", "menu.manage", null, true, false),
                                new RolePermissionController.PermissionView(
                                        "menu.view", "menu.view", null, true, true))))));
    }

    /**
     * 测试保存时只同步可用权限，插件停用后的不可用授权必须保留。
     */
    @Test
    void saveRolePermissionsSynchronizesOnlyAvailablePermissions() throws Exception {
        Instant now = Instant.now();
        RoleRecord role = new RoleRecord("r1", "operator", "Operator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, now, now);
        when(roleService.findRole("r1")).thenReturn(Optional.of(role));
        when(roleService.listRolePermissions("r1")).thenReturn(List.of(
                new RolePermissionGrant("r1", "menu.manage", PermissionStatus.AVAILABLE, now, now),
                new RolePermissionGrant("r1", "legacy.view", PermissionStatus.UNAVAILABLE, now, now)));
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(
                new PluginPermissionGroup("menu-manager", List.of(
                        permission("menu.view", PermissionStatus.AVAILABLE, now),
                        permission("menu.manage", PermissionStatus.AVAILABLE, now))),
                new PluginPermissionGroup("legacy", List.of(
                        permission("legacy.view", PermissionStatus.UNAVAILABLE, now)))));

        mockMvc.perform(post("/admin/roles/{roleId}/permissions", "r1")
                        .param("name", "Operator")
                        .param("permissionCodes", "menu.view", "legacy.view", "unknown.permission"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/r1"))
                .andExpect(flash().attribute("message", "角色已保存。"));

        verify(roleService).updateRoleName("r1", "Operator");
        verify(roleService).grantPermission("r1", "menu.view");
        verify(roleService).revokePermission("r1", "menu.manage");
        verify(roleService, never()).grantPermission("r1", "legacy.view");
        verify(roleService, never()).revokePermission("r1", "legacy.view");
        verify(roleService, never()).grantPermission("r1", "unknown.permission");
        verify(auditRecorder).success(any(), eq("role"), eq("r1"), eq("角色已保存。"));
    }

    /**
     * 测试超级管理员提交空选择时仍会补齐全部可用权限。
     */
    @Test
    void saveRolePermissionsKeepsAllAvailablePermissionsForSuperAdmin() throws Exception {
        Instant now = Instant.now();
        RoleRecord role = new RoleRecord("r1", "admin", "Administrator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, now, now);
        when(roleService.findRole("r1")).thenReturn(Optional.of(role));
        when(roleService.listRolePermissions("r1")).thenReturn(List.of());
        when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(
                new PluginPermissionGroup("menu-manager", List.of(
                        permission("menu.view", PermissionStatus.AVAILABLE, now),
                        permission("menu.removed", PermissionStatus.UNAVAILABLE, now)))));
        when(superAdminPermissionService.isSuperAdminRole("r1")).thenReturn(true);

        mockMvc.perform(post("/admin/roles/{roleId}/permissions", "r1")
                        .param("name", "Administrator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles/r1"));

        verify(roleService).updateRoleName("r1", "Administrator");
        verify(roleService).grantPermission("r1", "menu.view");
        verify(roleService, never()).grantPermission("r1", "menu.removed");
        verify(roleService, never()).revokePermission(any(), any());
    }

    private PermissionRecord permission(String code, PermissionStatus status, Instant now) {
        String pluginId = code.substring(0, code.indexOf('.'));
        return new PermissionRecord(pluginId, code, code, null, null, null, status, now);
    }
}
