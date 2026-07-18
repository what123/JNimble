package com.jnimble.platform.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.kernel.migration.PluginMigrationExecutor;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.plugin.PluginStateStore;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginPermission;
import com.jnimble.sdk.plugin.PluginStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link PermissionAwarePluginRuntimeService} 的单元测试。
 *
 * <p>验证 install、enable、disable、uninstall 等生命周期操作中
 * 权限注册、权限不可用标记以及快照持久化的联动逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
class PermissionAwarePluginRuntimeServiceTest {

    @Mock
    private PluginRuntimeService delegate;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PluginStateStore pluginStateStore;

    @Mock
    private SuperAdminPermissionService superAdminPermissionService;

    private PermissionAwarePluginRuntimeService service;

    private static final String PLUGIN_ID = "test-plugin";

    @BeforeEach
    void setUp() {
        service = new PermissionAwarePluginRuntimeService(
                delegate, permissionService, pluginStateStore, superAdminPermissionService);
    }

    // ======================== install ========================

    @Test
    @DisplayName("install 应先注册权限再委托 delegate 并保存快照")
    void installSynchronizesPermissionsThenDelegates() {
        PluginDescriptor descriptor = createDescriptor(true);
        when(delegate.find(PLUGIN_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(installedSnapshot()));

        service.install(descriptor);

        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(delegate).install(descriptor);
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("install 时插件已启用应抛出 IllegalStateException")
    void installRejectsWhenPluginAlreadyEnabled() {
        PluginDescriptor descriptor = createDescriptor(false);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        assertThatThrownBy(() -> service.install(descriptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot install enabled plugin");
    }

    @Test
    @DisplayName("install(descriptor, path) 无权限时不应抛出异常")
    void installWithArtifactPathSynchronizesPermissions() {
        PluginDescriptor descriptor = createDescriptor(false);
        Path artifactPath = mock(Path.class);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.empty());

        service.install(descriptor, artifactPath);

        verify(delegate).install(descriptor, artifactPath);
        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
    }

    @Test
    @DisplayName("install 时 descriptor 的 permissions 为 null 应传入空集合")
    void installWithNullPermissionsRegistersEmptyCollection() {
        PluginDescriptor descriptor = createDescriptor(false, null);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.empty());

        service.install(descriptor);

        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
    }

    // ======================== enable ========================

    @Test
    @DisplayName("enable 应先委托 delegate 再同步权限并保存快照")
    void enableDelegatesThenSynchronizesPermissions() {
        PluginDescriptor descriptor = createDescriptor(true);
        PluginRuntimeSnapshot snapshot = enabledSnapshot();
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(snapshot));

        PluginMigrationExecutor migrationExecutor = mock(PluginMigrationExecutor.class);
        service.enable(descriptor, migrationExecutor);

        InOrder order = inOrder(delegate, permissionService, pluginStateStore);
        verify(delegate).enable(descriptor, migrationExecutor);
        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
        verify(superAdminPermissionService).grantAllAvailablePermissions();
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("enable(pluginId) 通过 ID 启用应同步权限")
    void enableByIdSynchronizesPermissions() {
        PluginRuntimeSnapshot snapshot = enabledSnapshot();
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(snapshot));

        service.enable(PLUGIN_ID);

        verify(delegate).enable(PLUGIN_ID);
        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
        verify(superAdminPermissionService).grantAllAvailablePermissions();
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("enable 时权限同步失败应禁用插件并标记权限不可用")
    void enableRollsBackOnPermissionSyncFailure() {
        PluginDescriptor descriptor = createDescriptor(true);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));
        doThrow(new RuntimeException("perm error"))
                .when(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());

        PluginMigrationExecutor migrationExecutor = mock(PluginMigrationExecutor.class);

        assertThatThrownBy(() -> service.enable(descriptor, migrationExecutor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("perm error");

        verify(delegate).disable(PLUGIN_ID);
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
    }

    // ======================== disable ========================

    @Test
    @DisplayName("disable 应先委托 delegate 再标记权限不可用并保存快照")
    void disableDelegatesThenMarksPermissionsUnavailable() {
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        service.disable(PLUGIN_ID);

        InOrder order = inOrder(delegate, permissionService, pluginStateStore);
        verify(delegate).disable(PLUGIN_ID);
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("disable 即使 delegate 抛出异常也应标记权限不可用")
    void disableMarksUnavailableEvenOnDelegateFailure() {
        doThrow(new RuntimeException("disable error"))
                .when(delegate).disable(PLUGIN_ID);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        assertThatThrownBy(() -> service.disable(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("disable error");

        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(pluginStateStore).save(any());
    }

    // ======================== uninstall ========================

    @Test
    @DisplayName("uninstall 应先委托 delegate 再标记权限不可用并保存快照")
    void uninstallDelegatesThenMarksPermissionsUnavailable() {
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(installedSnapshot()));

        service.uninstall(PLUGIN_ID);

        InOrder order = inOrder(delegate, permissionService, pluginStateStore);
        verify(delegate).uninstall(PLUGIN_ID);
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("uninstall(cleanData) 应透传清理标记并保存快照")
    void uninstallWithCleanDataDelegatesAndMarksPermissionsUnavailable() {
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(installedSnapshot()));

        service.uninstall(PLUGIN_ID, true);

        verify(delegate).uninstall(PLUGIN_ID, true);
        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("uninstall 即使 delegate 抛出异常也应标记权限不可用")
    void uninstallMarksUnavailableEvenOnDelegateFailure() {
        doThrow(new RuntimeException("uninstall error"))
                .when(delegate).uninstall(PLUGIN_ID);

        assertThatThrownBy(() -> service.uninstall(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("uninstall error");

        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
    }

    // ======================== reload ========================

    @Test
    @DisplayName("reload 应委托 delegate 并同步权限")
    void reloadDelegatesThenSynchronizesPermissions() {
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        service.reload(PLUGIN_ID);

        verify(delegate).reload(PLUGIN_ID);
        verify(permissionService).registerPluginPermissions(eq(PLUGIN_ID), anyCollection());
        verify(pluginStateStore).save(any());
    }

    @Test
    @DisplayName("reload 失败时应标记权限不可用并抛出异常")
    void reloadMarksUnavailableOnFailure() {
        doThrow(new RuntimeException("reload error"))
                .when(delegate).reload(PLUGIN_ID);
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        assertThatThrownBy(() -> service.reload(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("reload error");

        verify(permissionService).markPluginPermissionsUnavailable(PLUGIN_ID);
        verify(pluginStateStore).save(any());
    }

    // ======================== recordRuntimeError ========================

    @Test
    @DisplayName("recordRuntimeError 应委托 delegate 并保存快照")
    void recordRuntimeErrorDelegatesAndSavesSnapshot() {
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(enabledSnapshot()));

        service.recordRuntimeError(PLUGIN_ID, "something went wrong");

        verify(delegate).recordRuntimeError(PLUGIN_ID, "something went wrong");
        verify(pluginStateStore).save(any());
    }

    // ======================== find / list ========================

    @Test
    @DisplayName("find 应直接委托 delegate")
    void findDelegatesToDelegate() {
        PluginRuntimeSnapshot snapshot = enabledSnapshot();
        when(delegate.find(PLUGIN_ID)).thenReturn(Optional.of(snapshot));

        Optional<PluginRuntimeSnapshot> result = service.find(PLUGIN_ID);

        assertThat(result).contains(snapshot);
    }

    @Test
    @DisplayName("list 应直接委托 delegate")
    void listDelegatesToDelegate() {
        when(delegate.list()).thenReturn(List.of(enabledSnapshot()));

        assertThat(service.list()).hasSize(1);
    }

    // ======================== 辅助方法 ========================

    private PluginDescriptor createDescriptor(boolean withPermissions) {
        return createDescriptor(withPermissions,
                withPermissions
                        ? List.of(new PluginPermission("test-plugin.READ", "读取", "read.key"))
                        : null);
    }

    private PluginDescriptor createDescriptor(boolean withMigration, List<PluginPermission> permissions) {
        return new PluginDescriptor(
                "1.0",
                PLUGIN_ID,
                "Test Plugin",
                null,
                null,
                null,
                "1.0.0",
                "0.1.x",
                null,
                null,
                "com.example.TestPlugin",
                null,
                permissions,
                null
        );
    }

    private PluginRuntimeSnapshot enabledSnapshot() {
        return new PluginRuntimeSnapshot(
                PLUGIN_ID,
                createDescriptor(true),
                null,
                null,
                PluginStatus.ENABLED,
                0,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private PluginRuntimeSnapshot installedSnapshot() {
        return new PluginRuntimeSnapshot(
                PLUGIN_ID,
                createDescriptor(false),
                null,
                null,
                PluginStatus.INSTALLED,
                0,
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
