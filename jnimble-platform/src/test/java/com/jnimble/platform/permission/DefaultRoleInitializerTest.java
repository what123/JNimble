package com.jnimble.platform.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.platform.auth.AuthProperties;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

/**
 * DefaultRoleInitializer 单元测试。
 *
 * <p>验证默认角色初始化逻辑，包括权限注册、角色创建和权限授予。</p>
 */
class DefaultRoleInitializerTest {

    private AuthProperties authProperties;
    private PermissionService permissionService;
    private RoleService roleService;
    private SuperAdminPermissionService superAdminPermissionService;
    private DefaultRoleInitializer initializer;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        permissionService = mock(PermissionService.class);
        roleService = mock(RoleService.class);
        superAdminPermissionService = mock(SuperAdminPermissionService.class);
        initializer = new DefaultRoleInitializer(
                authProperties, permissionService, roleService, superAdminPermissionService);
    }

    /**
     * 测试初始化时注册系统权限。
     */
    @Test
    void runShouldRegisterSystemPermissions() {
        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(
                new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now())));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(permissionService).registerPluginPermissions(SystemPermissions.OWNER, SystemPermissions.definitions());
    }

    /**
     * 测试初始化时创建默认角色（如果不存在）。
     */
    @Test
    void runShouldCreateRoleWhenRoleDoesNotExist() {
        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.empty());
        when(roleService.createRole("ADMIN", "ADMIN")).thenReturn(
                new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).findRoleByCode("ADMIN");
        verify(roleService).createRole("ADMIN", "ADMIN");
    }

    /**
     * 测试初始化时角色已存在则不创建。
     */
    @Test
    void runShouldNotCreateRoleWhenRoleAlreadyExists() {
        RoleRecord existingRole = new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(existingRole));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).findRoleByCode("ADMIN");
        verify(roleService, org.mockito.Mockito.never()).createRole(anyString(), anyString());
    }

    /**
     * 测试初始化时授予所有可用权限给角色。
     */
    @Test
    void runShouldGrantAllAvailablePermissionsToRole() {
        RoleRecord role = new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(role));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(superAdminPermissionService).grantAllAvailablePermissions("1");
    }

    /**
     * 测试初始化时将角色授予默认用户。
     */
    @Test
    void runShouldGrantRoleToDefaultUser() {
        RoleRecord role = new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.of(role));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).grantRoleToSubject("admin", "1");
    }

    /**
     * 测试使用自定义角色配置。
     */
    @Test
    void runShouldUseCustomRoleConfiguration() {
        authProperties.getDefaultUser().setRole("SUPER_ADMIN");

        when(roleService.findRoleByCode("SUPER_ADMIN")).thenReturn(Optional.empty());
        when(roleService.createRole("SUPER_ADMIN", "SUPER_ADMIN")).thenReturn(
                new RoleRecord("2", "SUPER_ADMIN", "SUPER_ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).findRoleByCode("SUPER_ADMIN");
        verify(roleService).createRole("SUPER_ADMIN", "SUPER_ADMIN");
    }

    /**
     * 测试角色代码规范化（小写转大写）。
     */
    @Test
    void runShouldNormalizeRoleCodeToUpperCase() {
        authProperties.getDefaultUser().setRole("admin");

        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.empty());
        when(roleService.createRole("ADMIN", "ADMIN")).thenReturn(
                new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).findRoleByCode("ADMIN");
        verify(roleService).createRole("ADMIN", "ADMIN");
    }

    /**
     * 测试空角色代码默认使用 ADMIN。
     */
    @Test
    void runShouldUseDefaultAdminRoleWhenRoleCodeIsBlank() {
        authProperties.getDefaultUser().setRole("");

        when(roleService.findRoleByCode("ADMIN")).thenReturn(Optional.empty());
        when(roleService.createRole("ADMIN", "ADMIN")).thenReturn(
                new RoleRecord("1", "ADMIN", "ADMIN", RoleStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(roleService).findRoleByCode("ADMIN");
    }

    /**
     * 测试初始化器实现 ApplicationRunner 接口。
     */
    @Test
    void shouldImplementApplicationRunner() {
        assertThat(initializer).isInstanceOf(org.springframework.boot.ApplicationRunner.class);
    }
}