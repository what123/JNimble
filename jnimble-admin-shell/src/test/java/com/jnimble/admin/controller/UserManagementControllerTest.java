package com.jnimble.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.admin.user.UserManagementController;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.platform.permission.SubjectRoleGrant;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * UserManagementController 单元测试。
 *
 * <p>使用 MockMvc 独立模式测试用户管理端点的 CRUD 操作、
 * 权限检查和异常处理行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ObjectProvider<UserAccountService> userAccountServiceProvider;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private RoleService roleService;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private ControllerAuthorization authorization;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        UserManagementController controller = new UserManagementController(
                (ObjectProvider) userAccountServiceProvider, roleService, auditRecorder, authorization);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        lenient().when(roleService.listRoles()).thenReturn(List.of());
        lenient().when(roleService.listSubjectRoles()).thenReturn(List.of());
        lenient().when(roleService.listSubjectRoles(anyString())).thenReturn(List.of());
    }

    /**
     * 测试获取用户列表页面，验证返回正确视图和 Model 属性。
     */
    @Test
    void listUsersReturnsUserListView() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(userAccountService);
        UserRecord user = new UserRecord("id1", "user1", "hash", "User One",
                com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now());
        when(userAccountService.listUsers()).thenReturn(List.of(user));
        RoleRecord role = role("r1", "CASHIER", "收银员", true);
        when(roleService.listRoles()).thenReturn(List.of(role));
        when(roleService.listSubjectRoles()).thenReturn(List.of(
                new SubjectRoleGrant("user1", "r1", Instant.now())));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/user/list"))
                .andExpect(model().attribute("activeNav", "users"))
                .andExpect(model().attribute("userStoreAvailable", true))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("roles", List.of(role)))
                .andExpect(model().attribute("userRoleIds", Map.of("user1", "r1")));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试用户服务不可用时返回 userStoreAvailable=false。
     */
    @Test
    void listUsersWhenServiceUnavailableReturnsEmptyList() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(null);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("userStoreAvailable", false))
                .andExpect(model().attribute("users", List.of()));
    }

    /**
     * 测试创建用户成功场景，验证重定向和审计记录。
     */
    @Test
    void createUserSuccessRedirectsWithSuccessMessage() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(userAccountService);
        RoleRecord cashier = role("r2", "CASHIER", "收银员", true);
        RoleRecord disabled = role("r3", "OLD_ROLE", "旧角色", false);
        when(roleService.listRoles()).thenReturn(List.of(disabled, cashier));

        mockMvc.perform(post("/admin/users")
                        .param("username", "newuser")
                        .param("password", "password123")
                        .param("displayName", "New User")
                        .param("roleIds", "r2", "r3", "unknown"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "用户已创建。"));

        verify(authorization).requirePermission(any());
        verify(userAccountService).createUser("newuser", "password123", "New User");
        verify(roleService).grantRoleToSubject("newuser", "r2");
        verify(roleService, never()).grantRoleToSubject("newuser", "r3");
        verify(roleService, never()).grantRoleToSubject("newuser", "unknown");
        verify(auditRecorder).success(any(), any(), any(), any());
    }

    /**
     * 测试创建用户失败时返回错误信息。
     */
    @Test
    void createUserFailureRedirectsWithErrorMessage() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(userAccountService);
        doThrow(new RuntimeException("用户名已存在")).when(userAccountService)
                .createUser(any(), any(), any());

        mockMvc.perform(post("/admin/users")
                        .param("username", "existinguser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", "用户名已存在"));

        verify(auditRecorder).failure(any(), any(), any(), any());
    }

    /**
     * 测试编辑用户一次性保存显示名、密码和角色。
     */
    @Test
    void updateProfileSavesDisplayNamePasswordAndRoles() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(userAccountService);
        RoleRecord admin = role("r1", "ADMIN", "管理员", true);
        RoleRecord cashier = role("r2", "CASHIER", "收银员", true);
        RoleRecord kitchen = role("r3", "KITCHEN", "后厨", true);
        when(roleService.listRoles()).thenReturn(List.of(admin, cashier, kitchen));
        when(roleService.listSubjectRoles("user1")).thenReturn(List.of(
                new SubjectRoleGrant("user1", "r1", Instant.now()),
                new SubjectRoleGrant("user1", "r2", Instant.now())));

        when(userAccountService.changeUsername("user1", "user1")).thenReturn(
                new UserRecord("id1", "user1", "hash", "User One",
                        com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now()));
        when(userAccountService.updateDisplayName("user1", "前台员工")).thenReturn(
                new UserRecord("id1", "user1", "hash", "前台员工",
                        com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now()));

        mockMvc.perform(post("/admin/users/{username}/profile", "user1")
                        .param("newUsername", "user1")
                        .param("displayName", "前台员工")
                        .param("password", "newpassword")
                        .param("roleIds", "r2", "r3", "unknown"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "用户资料已保存。"));

        verify(userAccountService).changeUsername("user1", "user1");
        verify(userAccountService).updateDisplayName("user1", "前台员工");
        verify(userAccountService).resetPassword("user1", "newpassword");
        verify(roleService).grantRoleToSubject("user1", "r3");
        verify(roleService).revokeRoleFromSubject("user1", "r1");
        verify(roleService, never()).grantRoleToSubject("user1", "unknown");
        verify(roleService, never()).revokeRoleFromSubject("user1", "r2");
    }

    /**
     * 测试密码为空时不重置密码。
     */
    @Test
    void updateProfileSkipsPasswordWhenBlank() throws Exception {
        when(userAccountServiceProvider.getIfAvailable()).thenReturn(userAccountService);

        when(userAccountService.changeUsername("user1", "user1")).thenReturn(
                new UserRecord("id1", "user1", "hash", "User One",
                        com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now()));
        when(userAccountService.updateDisplayName("user1", "新名称")).thenReturn(
                new UserRecord("id1", "user1", "hash", "新名称",
                        com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now()));

        mockMvc.perform(post("/admin/users/{username}/profile", "user1")
                        .param("newUsername", "user1")
                        .param("displayName", "新名称"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "用户资料已保存。"));

        verify(userAccountService).changeUsername("user1", "user1");
        verify(userAccountService).updateDisplayName("user1", "新名称");
        verify(userAccountService, never()).resetPassword(any(), any());
        verify(roleService, never()).grantRoleToSubject(any(), any());
        verify(roleService, never()).revokeRoleFromSubject(any(), any());
    }

    private RoleRecord role(String id, String code, String name, boolean active) {
        Instant now = Instant.now();
        return new RoleRecord(id, code, name,
                active
                        ? com.jnimble.platform.permission.RoleStatus.ACTIVE
                        : com.jnimble.platform.permission.RoleStatus.DISABLED,
                now, now);
    }
}
