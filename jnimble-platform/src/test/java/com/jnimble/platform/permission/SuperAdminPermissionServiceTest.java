package com.jnimble.platform.permission;

import com.jnimble.platform.auth.AuthProperties;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SuperAdminPermissionService} 的单元测试。
 *
 * <p>测试超级管理员角色判定、全部权限授予与撤销功能。</p>
 */
@ExtendWith(MockitoExtension.class)
class SuperAdminPermissionServiceTest {

    @Mock
    private AuthProperties authProperties;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RoleService roleService;

    private SuperAdminPermissionService service;

    private static final Instant NOW = Instant.parse("2026-06-30T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new SuperAdminPermissionService(authProperties, permissionService, roleService);
    }

    private void stubDefaultAdminRole() {
        AuthProperties.DefaultUser defaultUser = new AuthProperties.DefaultUser();
        defaultUser.setRole("ADMIN");
        when(authProperties.getDefaultUser()).thenReturn(defaultUser);
    }

    // ======================== isSuperAdminRole ========================

    @Nested
    @DisplayName("isSuperAdminRole 方法测试")
    class IsSuperAdminRoleTests {

        @Test
        @DisplayName("角色 ID 对应 ADMIN 角色时应返回 true")
        void shouldReturnTrueForAdminRole() {
            stubDefaultAdminRole();
            RoleRecord adminRole = new RoleRecord("r1", "ADMIN", "管理员", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRole("r1")).thenReturn(Optional.of(adminRole));

            assertThat(service.isSuperAdminRole("r1")).isTrue();
        }

        @Test
        @DisplayName("角色 ID 对应非 ADMIN 角色时应返回 false")
        void shouldReturnFalseForNonAdminRole() {
            stubDefaultAdminRole();
            RoleRecord userRole = new RoleRecord("r2", "USER", "普通用户", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRole("r2")).thenReturn(Optional.of(userRole));

            assertThat(service.isSuperAdminRole("r2")).isFalse();
        }

        @Test
        @DisplayName("角色 ID 为 null 时应返回 false")
        void shouldReturnFalseWhenRoleIdIsNull() {
            assertThat(service.isSuperAdminRole(null)).isFalse();
        }

        @Test
        @DisplayName("角色 ID 为空白时应返回 false")
        void shouldReturnFalseWhenRoleIdIsBlank() {
            assertThat(service.isSuperAdminRole("  ")).isFalse();
        }

        @Test
        @DisplayName("角色 ID 不存在时应返回 false")
        void shouldReturnFalseWhenRoleNotFound() {
            when(roleService.findRole("nonexistent")).thenReturn(Optional.empty());

            assertThat(service.isSuperAdminRole("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("角色 code 为空白时应默认为 ADMIN")
        void shouldDefaultToAdminWhenRoleCodeIsBlank() {
            stubDefaultAdminRole();
            RoleRecord blankCodeRole = new RoleRecord("r3", "  ", "空角色", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRole("r3")).thenReturn(Optional.of(blankCodeRole));

            assertThat(service.isSuperAdminRole("r3")).isTrue();
        }
    }

    // ======================== isSuperAdminRoleCode ========================

    @Nested
    @DisplayName("isSuperAdminRoleCode 方法测试")
    class IsSuperAdminRoleCodeTests {

        @Test
        @DisplayName("角色 code 为 ADMIN 时应返回 true")
        void shouldReturnTrueForAdminCode() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode("ADMIN")).isTrue();
        }

        @Test
        @DisplayName("小写 admin 应被规范化为 ADMIN 后匹配")
        void shouldMatchLowerCaseAdmin() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode("admin")).isTrue();
        }

        @Test
        @DisplayName("角色 code 为 USER 时应返回 false")
        void shouldReturnFalseForUserCode() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode("USER")).isFalse();
        }

        @Test
        @DisplayName("角色 code 为 null 时应默认为 ADMIN 并返回 true")
        void shouldReturnTrueWhenRoleCodeIsNull() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode(null)).isTrue();
        }

        @Test
        @DisplayName("角色 code 为空白时应默认为 ADMIN 并返回 true")
        void shouldReturnTrueWhenRoleCodeIsBlank() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode("  ")).isTrue();
        }

        @Test
        @DisplayName("角色 code 带空格时应规范化后匹配")
        void shouldTrimRoleCodeBeforeComparison() {
            stubDefaultAdminRole();
            assertThat(service.isSuperAdminRoleCode(" ADMIN ")).isTrue();
        }
    }

    // ======================== grantAllAvailablePermissions ========================

    @Nested
    @DisplayName("grantAllAvailablePermissions 方法测试")
    class GrantAllAvailablePermissionsTests {

        @Test
        @DisplayName("应授予所有可用权限给超级管理员角色")
        void shouldGrantAllPermissionsToSuperAdminRole() {
            stubDefaultAdminRole();
            RoleRecord adminRole = new RoleRecord("r1", "ADMIN", "管理员", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(adminRole));

            PluginPermissionGroup group = new PluginPermissionGroup("plugin-a", List.of(
                    perm("plugin-a.READ", PermissionStatus.AVAILABLE),
                    perm("plugin-a.WRITE", PermissionStatus.AVAILABLE),
                    perm("plugin-a.DELETE", PermissionStatus.UNAVAILABLE)));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(group));

            service.grantAllAvailablePermissions();

            verify(roleService).grantPermission("r1", "plugin-a.READ");
            verify(roleService).grantPermission("r1", "plugin-a.WRITE");
            verify(roleService, never()).grantPermission("r1", "plugin-a.DELETE");
        }

        @Test
        @DisplayName("应授予所有可用权限给指定角色 ID")
        void shouldGrantAllPermissionsToSpecifiedRoleId() {
            PluginPermissionGroup group = new PluginPermissionGroup("plugin-a", List.of(
                    perm("plugin-a.READ", PermissionStatus.AVAILABLE),
                    perm("plugin-a.WRITE", PermissionStatus.AVAILABLE)));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(group));

            service.grantAllAvailablePermissions("r2");

            verify(roleService).grantPermission("r2", "plugin-a.READ");
            verify(roleService).grantPermission("r2", "plugin-a.WRITE");
        }

        @Test
        @DisplayName("无可用权限时应不执行任何授予操作")
        void shouldNotGrantWhenNoPermissionsAvailable() {
            stubDefaultAdminRole();
            RoleRecord adminRole = new RoleRecord("r1", "ADMIN", "管理员", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(adminRole));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());

            service.grantAllAvailablePermissions();

            verify(roleService, never()).grantPermission(anyString(), anyString());
        }

        @Test
        @DisplayName("超级管理员角色不存在时应不执行任何授予操作")
        void shouldNotGrantWhenSuperAdminRoleNotFound() {
            stubDefaultAdminRole();
            when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.empty());

            service.grantAllAvailablePermissions();

            verify(roleService, never()).grantPermission(anyString(), anyString());
        }

        @Test
        @DisplayName("多个插件的权限应全部被授予")
        void shouldGrantPermissionsFromMultiplePlugins() {
            stubDefaultAdminRole();
            RoleRecord adminRole = new RoleRecord("r1", "ADMIN", "管理员", RoleStatus.ACTIVE, NOW, NOW);
            when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(adminRole));

            PluginPermissionGroup groupA = new PluginPermissionGroup("plugin-a", List.of(
                    perm("plugin-a.READ", PermissionStatus.AVAILABLE)));
            PluginPermissionGroup groupB = new PluginPermissionGroup("plugin-b", List.of(
                    perm("plugin-b.WRITE", PermissionStatus.AVAILABLE)));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(groupA, groupB));

            service.grantAllAvailablePermissions();

            verify(roleService).grantPermission("r1", "plugin-a.READ");
            verify(roleService).grantPermission("r1", "plugin-b.WRITE");
        }
    }

    // ======================== allAvailablePermissionCodes ========================

    @Nested
    @DisplayName("allAvailablePermissionCodes 方法测试")
    class AllAvailablePermissionCodesTests {

        @Test
        @DisplayName("应返回所有可用权限的 code 列表")
        void shouldReturnAllAvailablePermissionCodes() {
            PluginPermissionGroup group = new PluginPermissionGroup("plugin-a", List.of(
                    perm("plugin-a.READ", PermissionStatus.AVAILABLE),
                    perm("plugin-a.WRITE", PermissionStatus.AVAILABLE),
                    perm("plugin-a.DELETE", PermissionStatus.UNAVAILABLE)));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(group));

            Collection<String> codes = service.allAvailablePermissionCodes();

            assertThat(codes).containsExactlyInAnyOrder("plugin-a.READ", "plugin-a.WRITE");
            assertThat(codes).doesNotContain("plugin-a.DELETE");
        }

        @Test
        @DisplayName("无权限注册时应返回空集合")
        void shouldReturnEmptyWhenNoPermissions() {
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of());

            Collection<String> codes = service.allAvailablePermissionCodes();

            assertThat(codes).isEmpty();
        }

        @Test
        @DisplayName("仅返回 AVAILABLE 状态的权限 code")
        void shouldFilterByAvailableStatus() {
            PluginPermissionGroup groupA = new PluginPermissionGroup("plugin-a", List.of(
                    perm("plugin-a.A1", PermissionStatus.AVAILABLE),
                    perm("plugin-a.U1", PermissionStatus.UNAVAILABLE)));
            PluginPermissionGroup groupB = new PluginPermissionGroup("plugin-b", List.of(
                    perm("plugin-b.A2", PermissionStatus.AVAILABLE),
                    perm("plugin-b.U2", PermissionStatus.UNAVAILABLE)));
            when(permissionService.listPermissionsByPlugin()).thenReturn(List.of(groupA, groupB));

            Collection<String> codes = service.allAvailablePermissionCodes();

            assertThat(codes).containsExactlyInAnyOrder("plugin-a.A1", "plugin-b.A2");
            assertThat(codes).doesNotContain("plugin-a.U1", "plugin-b.U2");
        }
    }

    // ======================== 辅助方法 ========================

    private static PermissionRecord perm(String code, PermissionStatus status) {
        return new PermissionRecord("plugin", code, code, code + ".key",
                "desc", "desc.key", status, NOW);
    }
}
