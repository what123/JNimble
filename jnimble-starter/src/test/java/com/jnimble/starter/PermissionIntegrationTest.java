package com.jnimble.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.platform.permission.PermissionDefinition;
import com.jnimble.platform.permission.PermissionRecord;
import com.jnimble.platform.permission.PermissionService;
import com.jnimble.platform.permission.PermissionStatus;
import com.jnimble.platform.permission.RolePermissionGrant;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.platform.permission.SubjectRoleGrant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 权限系统集成测试。
 *
 * <p>测试权限的注册、角色管理、权限授予与撤销，以及主体-角色分配的完整业务流程。</p>
 * <p>使用 H2 内存数据库进行隔离测试。</p>
 */
@SpringBootTest(
        properties = {
                "jnimble.plugins.auto-enable=false",
                "jnimble.plugins.restore-enabled=false",
                "jnimble.plugins.directory-scan-enabled=false"
        })
@ActiveProfiles("test")
class PermissionIntegrationTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    private static final String TEST_PLUGIN_ID = "test-plugin";
    private static final String TEST_PERMISSION_CODE = "test-plugin.permission.read";
    private static final String TEST_PERMISSION_NAME = "测试读取权限";

    @BeforeEach
    void setUp() {
        // 清理测试数据（由内存数据库自动处理）
    }

    /**
     * 测试完整的权限生命周期：注册权限 -> 创建角色 -> 授予权限 -> 分配角色 -> 验证权限
     */
    @Test
    @DisplayName("完整权限生命周期测试")
    void shouldCompleteFullPermissionLifecycle() {
        // 1. 注册插件权限
        PermissionDefinition permissionDef = new PermissionDefinition(
                TEST_PERMISSION_CODE,
                TEST_PERMISSION_NAME,
                "test.permission.read.name"
        );
        permissionService.registerPluginPermissions(TEST_PLUGIN_ID, List.of(permissionDef));

        // 2. 验证权限已注册
        Optional<PermissionRecord> permission = permissionService.findPermission(TEST_PERMISSION_CODE);
        assertThat(permission).isPresent();
        assertThat(permission.get().code()).isEqualTo(TEST_PERMISSION_CODE);
        assertThat(permissionService.isPermissionAvailable(TEST_PERMISSION_CODE)).isTrue();

        // 3. 创建角色并授予权限（使用唯一角色代码避免重复键冲突）
        String uniqueRoleCode = "TEST_ROLE_" + UUID.randomUUID().toString().substring(0, 8);
        RoleRecord role = roleService.createRole(uniqueRoleCode, "测试角色");
        roleService.grantPermission(role.id(), TEST_PERMISSION_CODE);

        // 4. 验证角色权限
        Collection<RolePermissionGrant> rolePermissions = roleService.listRolePermissions(role.id());
        assertThat(rolePermissions).hasSize(1);
        assertThat(rolePermissions.iterator().next().permissionCode()).isEqualTo(TEST_PERMISSION_CODE);
        assertThat(rolePermissions.iterator().next().status()).isEqualTo(PermissionStatus.AVAILABLE);

        // 5. 分配角色给用户
        String testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
        roleService.grantRoleToSubject(testUserId, role.id());

        // 6. 验证用户权限
        assertThat(roleService.subjectHasPermission(testUserId, TEST_PERMISSION_CODE)).isTrue();
    }

    /**
     * 测试权限不可用时的状态变更：禁用插件权限后，角色权限应标记为不可用
     */
    @Test
    @DisplayName("插件权限禁用后角色权限状态变更测试")
    void shouldMarkPermissionsUnavailableWhenPluginDisabled() {
        // 1. 注册权限
        PermissionDefinition permissionDef = new PermissionDefinition(
                TEST_PERMISSION_CODE,
                TEST_PERMISSION_NAME,
                "test.permission.read.name"
        );
        permissionService.registerPluginPermissions(TEST_PLUGIN_ID, List.of(permissionDef));

        // 2. 创建角色并授予权限（使用唯一角色代码避免重复键冲突）
        String uniqueRoleCode = "TEST_ROLE_" + UUID.randomUUID().toString().substring(0, 8);
        RoleRecord role = roleService.createRole(uniqueRoleCode, "测试角色");
        roleService.grantPermission(role.id(), TEST_PERMISSION_CODE);

        // 3. 验证初始状态为可用
        Collection<RolePermissionGrant> grants = roleService.listRolePermissions(role.id());
        assertThat(grants).filteredOn(g -> TEST_PERMISSION_CODE.equals(g.permissionCode()))
                .singleElement()
                .extracting(RolePermissionGrant::status)
                .isEqualTo(PermissionStatus.AVAILABLE);

        // 4. 标记权限为不可用（模拟插件禁用）
        roleService.markPermissionGrantsUnavailable(TEST_PERMISSION_CODE);

        // 5. 验证权限状态变为不可用
        grants = roleService.listRolePermissions(role.id());
        assertThat(grants).filteredOn(g -> TEST_PERMISSION_CODE.equals(g.permissionCode()))
                .singleElement()
                .extracting(RolePermissionGrant::status)
                .isEqualTo(PermissionStatus.UNAVAILABLE);

        // 6. 分配角色给用户并验证权限不可用
        String testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
        roleService.grantRoleToSubject(testUserId, role.id());
        assertThat(roleService.subjectHasPermission(testUserId, TEST_PERMISSION_CODE)).isFalse();
    }

    /**
     * 测试角色禁用后权限验证：禁用角色后，该角色授予的权限应不再生效
     */
    @Test
    @DisplayName("角色禁用后权限验证测试")
    void shouldNotHavePermissionWhenRoleDisabled() {
        // 1. 注册权限
        PermissionDefinition permissionDef = new PermissionDefinition(
                TEST_PERMISSION_CODE,
                TEST_PERMISSION_NAME,
                "test.permission.read.name"
        );
        permissionService.registerPluginPermissions(TEST_PLUGIN_ID, List.of(permissionDef));

        // 2. 创建角色、授予权限、分配角色（使用唯一角色代码避免重复键冲突）
        String uniqueRoleCode = "TEST_ROLE_" + UUID.randomUUID().toString().substring(0, 8);
        RoleRecord role = roleService.createRole(uniqueRoleCode, "测试角色");
        roleService.grantPermission(role.id(), TEST_PERMISSION_CODE);
        String testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
        roleService.grantRoleToSubject(testUserId, role.id());

        // 3. 验证用户有权限
        assertThat(roleService.subjectHasPermission(testUserId, TEST_PERMISSION_CODE)).isTrue();

        // 4. 禁用角色
        roleService.disableRole(role.id());

        // 5. 验证用户权限不再生效
        assertThat(roleService.subjectHasPermission(testUserId, TEST_PERMISSION_CODE)).isFalse();

        // 6. 验证角色状态为禁用
        Optional<RoleRecord> foundRole = roleService.findRole(role.id());
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().active()).isFalse();
    }
}
