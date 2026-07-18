package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.kernel.migration.PluginMigrationException;
import com.jnimble.kernel.migration.PluginMigrationExecutor;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginI18n;
import com.jnimble.sdk.plugin.PluginLifecycleEvent;
import com.jnimble.sdk.plugin.PluginLifecyclePhase;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DefaultPluginRuntimeService 单元测试。
 *
 * <p>验证插件生命周期管理、状态转换、错误处理及回滚逻辑。</p>
 */
class DefaultPluginRuntimeServiceTest {

    private static final String PLUGIN_ID = "test-plugin";
    private static final Instant FIXED_TIME = Instant.parse("2024-01-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));

    private PluginBootLoader bootLoader;
    private PluginBoot pluginBoot;
    private LoadedPluginBoot loadedBoot;
    private PluginMigrationExecutor migrationExecutor;
    private PluginContributionRegistries contributionRegistries;
    private PluginI18nRegistry i18nRegistry;
    private DefaultPluginRuntimeService runtimeService;
    private PluginDescriptor descriptor;

    @BeforeEach
    void setUp() {
        bootLoader = mock(PluginBootLoader.class);
        pluginBoot = mock(PluginBoot.class);
        loadedBoot = mock(LoadedPluginBoot.class);
        migrationExecutor = mock(PluginMigrationExecutor.class);
        contributionRegistries = mock(PluginContributionRegistries.class);
        i18nRegistry = mock(PluginI18nRegistry.class);

        when(bootLoader.load(any(PluginDescriptor.class))).thenReturn(loadedBoot);
        when(bootLoader.load(any(PluginDescriptor.class), nullable(Path.class))).thenReturn(loadedBoot);
        when(loadedBoot.boot()).thenReturn(pluginBoot);
        when(loadedBoot.classLoader()).thenReturn(getClass().getClassLoader());

        HookRegistry hookRegistry = NoopPluginRegistries.hooks();
        RouteRegistry routeRegistry = NoopPluginRegistries.routes();
        AssetRegistry assetRegistry = NoopPluginRegistries.assets();
        when(contributionRegistries.hooks(any(String.class))).thenReturn(hookRegistry);
        when(contributionRegistries.routes(any(String.class))).thenReturn(routeRegistry);
        when(contributionRegistries.assets(any(String.class))).thenReturn(assetRegistry);

        runtimeService = new DefaultPluginRuntimeService(
                bootLoader,
                hookRegistry,
                routeRegistry,
                assetRegistry,
                PluginBeanResolver.empty(),
                contributionRegistries,
                migrationExecutor,
                i18nRegistry,
                FIXED_CLOCK);

        descriptor = createDescriptor(PLUGIN_ID, "Test Plugin");
    }

    /**
     * 测试安装插件时使用无效的插件 ID 会抛出异常。
     */
    @Test
    void rejectsInvalidPluginIdsEvenWhenDescriptorWasNotLoadedByValidator() {
        PluginDescriptor invalidDescriptor = new PluginDescriptor(
                "1.0",
                "../crm",
                "CRM",
                null,
                null,
                null,
                "0.1.0",
                "0.1.x",
                null,
                null,
                "example.CrmPlugin",
                null,
                List.of(),
                null);

        assertThatThrownBy(() -> runtimeService.install(invalidDescriptor))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin id must match");
    }

    /**
     * 测试正常安装插件，状态应为 INSTALLED。
     */
    @Test
    void installSetsStatusToInstalled() {
        runtimeService.install(descriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);
        assertThat(snapshot.get().pluginId()).isEqualTo(PLUGIN_ID);
        assertThat(snapshot.get().descriptor()).isEqualTo(descriptor);
    }

    /**
     * 测试安装插件时记录安装时间。
     */
    @Test
    void installRecordsInstallationTimestamp() {
        runtimeService.install(descriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().installedAt()).isEqualTo(FIXED_TIME);
    }

    /**
     * 测试安装插件时生成正确的生命周期事件。
     */
    @Test
    void installGeneratesLifecycleEvents() {
        runtimeService.install(descriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        List<PluginLifecycleEvent> events = snapshot.get().events();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).phase()).isEqualTo(PluginLifecyclePhase.INSTALLING);
        assertThat(events.get(1).phase()).isEqualTo(PluginLifecyclePhase.INSTALLED);
    }

    /**
     * 测试安装插件时清除之前的错误信息。
     */
    @Test
    void installClearsLastError() {
        runtimeService.install(descriptor);
        runtimeService.recordRuntimeError(PLUGIN_ID, "some error");

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        runtimeService.install(updatedDescriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().lastError()).isNull();
    }

    /**
     * 测试重复安装已安装的插件会更新描述符。
     */
    @Test
    void installReplacesExistingDescriptorWhenNotEnabled() {
        runtimeService.install(descriptor);

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        runtimeService.install(updatedDescriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().descriptor().name()).isEqualTo("Updated Plugin");
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);
    }

    /**
     * 测试安装已启用的插件会抛出异常。
     */
    @Test
    void installThrowsWhenPluginIsEnabled() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        assertThatThrownBy(() -> runtimeService.install(updatedDescriptor))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot install enabled plugin");
    }

    /**
     * 测试安装插件时传递 artifactPath。
     */
    @Test
    void installWithArtifactPath() {
        Path artifactPath = Path.of("/path/to/plugin.jar");

        runtimeService.install(descriptor, artifactPath);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().artifactPath()).isEqualTo(artifactPath);
        assertThat(snapshot.get().source()).isEqualTo(PluginSource.JAR);
    }

    /**
     * 测试安装插件时不传递 artifactPath，source 为 CLASSPATH。
     */
    @Test
    void installWithoutArtifactPathUsesClasspathSource() {
        runtimeService.install(descriptor);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().artifactPath()).isNull();
        assertThat(snapshot.get().source()).isEqualTo(PluginSource.CLASSPATH);
    }

    /**
     * 测试正常启用插件，状态应为 ENABLED。
     */
    @Test
    void enableSetsStatusToEnabled() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);
    }

    /**
     * 测试启用插件时记录启动时间。
     */
    @Test
    void enableRecordsLastStartedAt() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().lastStartedAt()).isEqualTo(FIXED_TIME);
    }

    /**
     * 测试启用插件时加载并启动插件引导类。
     */
    @Test
    void enableLoadsAndStartsBoot() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        verify(bootLoader).load(any(PluginDescriptor.class), nullable(Path.class));
        verify(pluginBoot).boot(any(PluginContext.class));
    }

    /**
     * 测试启用插件时执行迁移。
     */
    @Test
    void enableExecutesMigration() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        verify(migrationExecutor).migrate(descriptor, loadedBoot.classLoader());
    }

    @Test
    void activationGuardRunsBeforeLoadingMigrationAndBoot() {
        PluginActivationGuard guard = ignored -> {
            throw new PluginRuntimeException("License missing");
        };
        DefaultPluginRuntimeService guardedRuntime = new DefaultPluginRuntimeService(
                bootLoader,
                NoopPluginRegistries.hooks(),
                NoopPluginRegistries.routes(),
                NoopPluginRegistries.assets(),
                PluginBeanResolver.empty(),
                contributionRegistries,
                migrationExecutor,
                PluginI18nRegistry.noop(),
                guard,
                FIXED_CLOCK);
        guardedRuntime.install(descriptor);

        assertThatThrownBy(() -> guardedRuntime.enable(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("License missing");
        verify(bootLoader, never()).load(any(PluginDescriptor.class), nullable(Path.class));
        verify(migrationExecutor, never()).migrate(any(), any());
        verify(pluginBoot, never()).boot(any());
    }

    /**
     * 测试启用插件时启用贡献注册表。
     */
    @Test
    void enableEnablesContributionRegistries() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        verify(contributionRegistries).enable(PLUGIN_ID);
    }

    /**
     * 测试启用已启用的插件时直接返回，不重复执行。
     */
    @Test
    void enableAlreadyEnabledPluginDoesNothing() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        runtimeService.enable(PLUGIN_ID);

        verify(bootLoader, org.mockito.Mockito.times(1)).load(any(PluginDescriptor.class), nullable(Path.class));
    }

    /**
     * 测试启用未安装的插件会抛出异常。
     */
    @Test
    void enableThrowsWhenPluginNotInstalled() {
        assertThatThrownBy(() -> runtimeService.enable("non-existent-plugin"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin is not installed");
    }

    /**
     * 测试启用已卸载的插件会抛出异常。
     */
    @Test
    void enableThrowsWhenPluginIsUninstalled() {
        runtimeService.install(descriptor);
        runtimeService.uninstall(PLUGIN_ID);

        assertThatThrownBy(() -> runtimeService.enable(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot enable uninstalled plugin");
    }

    /**
     * 测试启用插件时迁移失败，状态应为 MIGRATION_FAILED。
     */
    @Test
    void enableSetsMigrationFailedWhenMigrationThrows() {
        runtimeService.install(descriptor);
        doThrow(new PluginMigrationException("Migration failed"))
                .when(migrationExecutor).migrate(any(), any(ClassLoader.class));

        assertThatThrownBy(() -> runtimeService.enable(PLUGIN_ID))
                .isInstanceOf(PluginMigrationException.class);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.MIGRATION_FAILED);
        assertThat(snapshot.get().lastError()).contains("Migration failed");
    }

    /**
     * 测试启用插件时启动失败，状态应为 FAILED。
     */
    @Test
    void enableSetsFailedWhenBootThrows() {
        runtimeService.install(descriptor);
        doThrow(new RuntimeException("Boot failed"))
                .when(pluginBoot).boot(any(PluginContext.class));

        assertThatThrownBy(() -> runtimeService.enable(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Boot failed");

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.FAILED);
        assertThat(snapshot.get().lastError()).isEqualTo("Boot failed");
    }

    /**
     * 测试启用插件失败时回滚注册的句柄。
     */
    @Test
    void enableRollbackHandlesOnFailure() {
        RegistrationHandle handle = mock(RegistrationHandle.class);
        doAnswer(invocation -> {
            PluginContext context = invocation.getArgument(0);
            context.registerHandle(handle);
            throw new RuntimeException("Boot failed");
        }).when(pluginBoot).boot(any(PluginContext.class));

        runtimeService.install(descriptor);
        assertThatThrownBy(() -> runtimeService.enable(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class);

        verify(handle).unregister();
    }

    /**
     * 测试启用插件失败时关闭已加载的启动实例。
     */
    @Test
    void enableClosesLoadedBootOnFailure() {
        doThrow(new RuntimeException("Boot failed"))
                .when(pluginBoot).boot(any(PluginContext.class));

        runtimeService.install(descriptor);
        assertThatThrownBy(() -> runtimeService.enable(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class);

        try {
            verify(loadedBoot).close();
        } catch (Exception e) {
            // LoadedPluginBoot.close() declares checked exception, but should not throw in test
        }
    }

    /**
     * 测试正常禁用插件，状态应为 DISABLED。
     */
    @Test
    void disableSetsStatusToDisabled() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.DISABLED);
    }

    /**
     * 测试禁用插件时记录停止时间。
     */
    @Test
    void disableRecordsLastStoppedAt() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().lastStoppedAt()).isEqualTo(FIXED_TIME);
    }

    /**
     * 测试禁用插件时停止插件引导类。
     */
    @Test
    void disableStopsBoot() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        verify(pluginBoot).stop(any(PluginContext.class));
    }

    /**
     * 测试禁用插件时禁用贡献注册表。
     */
    @Test
    void disableDisablesContributionRegistries() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        verify(contributionRegistries).disable(PLUGIN_ID);
    }

    /**
     * 测试禁用插件时注销注册的句柄。
     */
    @Test
    void disableUnregistersHandles() {
        RegistrationHandle handle = mock(RegistrationHandle.class);
        doAnswer(invocation -> {
            PluginContext context = invocation.getArgument(0);
            context.registerHandle(handle);
            return null;
        }).when(pluginBoot).boot(any(PluginContext.class));

        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        verify(handle).unregister();
    }

    @Test
    void pluginMessagesRemainRegisteredWhilePluginIsDisabled() {
        RegistrationHandle i18nHandle = mock(RegistrationHandle.class);
        when(i18nRegistry.register(eq(PLUGIN_ID), eq("i18n.messages"), any(ClassLoader.class)))
                .thenReturn(i18nHandle);
        PluginDescriptor localizedDescriptor = createDescriptor(
                PLUGIN_ID, "Test Plugin", new PluginI18n("i18n.messages"));

        runtimeService.install(localizedDescriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        verify(i18nRegistry).register(eq(PLUGIN_ID), eq("i18n.messages"), any(ClassLoader.class));
        verify(i18nHandle, never()).unregister();

        runtimeService.uninstall(PLUGIN_ID);
        verify(i18nHandle).unregister();
    }

    @Test
    void replacingPluginReplacesInstalledMessageRegistration() {
        RegistrationHandle oldHandle = mock(RegistrationHandle.class);
        RegistrationHandle newHandle = mock(RegistrationHandle.class);
        when(i18nRegistry.register(eq(PLUGIN_ID), any(String.class), any(ClassLoader.class)))
                .thenReturn(oldHandle, newHandle);

        runtimeService.install(createDescriptor(
                PLUGIN_ID, "Test Plugin", new PluginI18n("i18n.messages")));
        runtimeService.replace(createDescriptor(
                PLUGIN_ID, "Updated Plugin", new PluginI18n("i18n.updated-messages")), null);

        verify(oldHandle).unregister();
        verify(newHandle, never()).unregister();
        verify(i18nRegistry, org.mockito.Mockito.times(2))
                .register(eq(PLUGIN_ID), any(String.class), any(ClassLoader.class));
    }

    /**
     * 测试禁用插件时关闭已加载的启动实例。
     */
    @Test
    void disableClosesLoadedBoot() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        try {
            verify(loadedBoot).close();
        } catch (Exception e) {
            // LoadedPluginBoot.close() declares checked exception, but should not throw in test
        }
    }

    /**
     * 测试禁用未启用的插件时直接返回。
     */
    @Test
    void disableNonEnabledPluginDoesNothing() {
        runtimeService.install(descriptor);
        runtimeService.disable(PLUGIN_ID);

        verify(pluginBoot, never()).stop(any(PluginContext.class));
    }

    /**
     * 测试禁用未安装的插件会抛出异常。
     */
    @Test
    void disableThrowsWhenPluginNotInstalled() {
        assertThatThrownBy(() -> runtimeService.disable("non-existent-plugin"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin is not installed");
    }

    /**
     * 测试禁用插件时停止失败，状态应为 FAILED。
     */
    @Test
    void disableSetsFailedWhenStopThrows() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        doThrow(new RuntimeException("Stop failed"))
                .when(pluginBoot).stop(any(PluginContext.class));

        assertThatThrownBy(() -> runtimeService.disable(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Failed to disable plugin");

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.FAILED);
    }

    /**
     * 测试正常卸载插件，状态应为 UNINSTALLED。
     */
    @Test
    void uninstallSetsStatusToUninstalled() {
        runtimeService.install(descriptor);
        runtimeService.uninstall(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.UNINSTALLED);
    }

    /**
     * 测试默认卸载不清理插件数据。
     */
    @Test
    void uninstallDoesNotCleanDataByDefault() {
        runtimeService.install(descriptor);
        runtimeService.uninstall(PLUGIN_ID);

        verify(migrationExecutor, never()).clean(any(PluginDescriptor.class), nullable(Path.class));
    }

    /**
     * 测试勾选清理时执行插件迁移清理。
     */
    @Test
    void uninstallWithCleanDataRunsMigrationCleanup() {
        Path artifactPath = Path.of("/path/to/plugin.jar");
        runtimeService.install(descriptor, artifactPath);
        runtimeService.uninstall(PLUGIN_ID, true);

        verify(migrationExecutor).clean(any(PluginDescriptor.class), eq(artifactPath));
    }

    /**
     * 测试卸载已启用的插件会先禁用再卸载。
     */
    @Test
    void uninstallDisablesEnabledPluginFirst() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.uninstall(PLUGIN_ID);

        verify(pluginBoot).stop(any(PluginContext.class));

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.UNINSTALLED);
    }

    /**
     * 测试卸载插件时清除运行时状态。
     */
    @Test
    void uninstallClearsRuntimeState() {
        RegistrationHandle handle = mock(RegistrationHandle.class);
        doAnswer(invocation -> {
            PluginContext context = invocation.getArgument(0);
            context.registerHandle(handle);
            return null;
        }).when(pluginBoot).boot(any(PluginContext.class));

        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.uninstall(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().registrationCount()).isEqualTo(0);
    }

    /**
     * 测试卸载插件时清除错误信息。
     */
    @Test
    void uninstallClearsLastError() {
        runtimeService.install(descriptor);
        runtimeService.recordRuntimeError(PLUGIN_ID, "some error");
        runtimeService.uninstall(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().lastError()).isNull();
    }

    /**
     * 测试卸载未安装的插件会抛出异常。
     */
    @Test
    void uninstallThrowsWhenPluginNotInstalled() {
        assertThatThrownBy(() -> runtimeService.uninstall("non-existent-plugin"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin is not installed");
    }

    /**
     * 测试替换已安装的插件，状态应为 INSTALLED。
     */
    @Test
    void replaceUpdatesDescriptor() {
        runtimeService.install(descriptor);

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        runtimeService.replace(updatedDescriptor, Path.of("/path/to/updated.jar"));

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().descriptor().name()).isEqualTo("Updated Plugin");
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);
    }

    /**
     * 测试替换已启用的插件会抛出异常。
     */
    @Test
    void replaceThrowsWhenPluginIsEnabled() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        assertThatThrownBy(() -> runtimeService.replace(updatedDescriptor, Path.of("/path/to/updated.jar")))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot replace enabled plugin");
    }

    /**
     * 测试替换已卸载的插件会抛出异常。
     */
    @Test
    void replaceThrowsWhenPluginIsUninstalled() {
        runtimeService.install(descriptor);
        runtimeService.uninstall(PLUGIN_ID);

        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        assertThatThrownBy(() -> runtimeService.replace(updatedDescriptor, Path.of("/path/to/updated.jar")))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot replace uninstalled plugin");
    }

    /**
     * 测试替换未安装的插件会抛出异常。
     */
    @Test
    void replaceThrowsWhenPluginNotInstalled() {
        PluginDescriptor updatedDescriptor = createDescriptor(PLUGIN_ID, "Updated Plugin");
        assertThatThrownBy(() -> runtimeService.replace(updatedDescriptor, Path.of("/path/to/updated.jar")))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin is not installed");
    }

    /**
     * 测试重载已启用的插件。
     */
    @Test
    void reloadEnablesPluginAfterDisable() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);

        runtimeService.reload(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);
        verify(pluginBoot, org.mockito.Mockito.times(2)).boot(any(PluginContext.class));
    }

    /**
     * 测试重载未启用的插件会抛出异常。
     */
    @Test
    void reloadThrowsWhenPluginNotEnabled() {
        runtimeService.install(descriptor);

        assertThatThrownBy(() -> runtimeService.reload(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot reload plugin unless it is enabled");
    }

    /**
     * 测试重载未安装的插件会抛出异常。
     */
    @Test
    void reloadThrowsWhenPluginNotInstalled() {
        assertThatThrownBy(() -> runtimeService.reload("non-existent-plugin"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Plugin is not installed");
    }

    /**
     * 测试记录运行时错误。
     */
    @Test
    void recordRuntimeErrorUpdatesLastError() {
        runtimeService.install(descriptor);
        runtimeService.recordRuntimeError(PLUGIN_ID, "runtime error");

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().lastError()).isEqualTo("runtime error");
    }

    /**
     * 测试记录 null 插件 ID 时直接返回。
     */
    @Test
    void recordRuntimeErrorWithNullPluginIdDoesNothing() {
        runtimeService.recordRuntimeError(null, "error");

        // 不应抛出异常
    }

    /**
     * 测试记录空插件 ID 时直接返回。
     */
    @Test
    void recordRuntimeErrorWithBlankPluginIdDoesNothing() {
        runtimeService.recordRuntimeError("", "error");
        runtimeService.recordRuntimeError("  ", "error");

        // 不应抛出异常
    }

    /**
     * 测试记录不存在的插件 ID 时直接返回。
     */
    @Test
    void recordRuntimeErrorForNonExistentPluginDoesNothing() {
        runtimeService.recordRuntimeError("non-existent-plugin", "error");

        // 不应抛出异常
    }

    /**
     * 测试查找不存在的插件返回空。
     */
    @Test
    void findReturnsEmptyForNonExistentPlugin() {
        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find("non-existent-plugin");

        assertThat(snapshot).isEmpty();
    }

    /**
     * 测试列出所有已安装的插件。
     */
    @Test
    void listReturnsAllInstalledPlugins() {
        PluginDescriptor descriptor2 = createDescriptor("other-plugin", "Other Plugin");
        runtimeService.install(descriptor);
        runtimeService.install(descriptor2);

        List<PluginRuntimeSnapshot> plugins = runtimeService.list();

        assertThat(plugins).hasSize(2);
        assertThat(plugins).extracting(PluginRuntimeSnapshot::pluginId)
                .containsExactlyInAnyOrder(PLUGIN_ID, "other-plugin");
    }

    /**
     * 测试列出插件时按插件 ID 排序。
     */
    @Test
    void listReturnsPluginsSortedByPluginId() {
        PluginDescriptor descriptor1 = createDescriptor("alpha-plugin", "Alpha Plugin");
        PluginDescriptor descriptor2 = createDescriptor("beta-plugin", "Beta Plugin");
        PluginDescriptor descriptor3 = createDescriptor("gamma-plugin", "Gamma Plugin");

        runtimeService.install(descriptor3);
        runtimeService.install(descriptor1);
        runtimeService.install(descriptor2);

        List<PluginRuntimeSnapshot> plugins = runtimeService.list();

        assertThat(plugins).extracting(PluginRuntimeSnapshot::pluginId)
                .containsExactly("alpha-plugin", "beta-plugin", "gamma-plugin");
    }

    /**
     * 测试完整的插件生命周期：安装 -> 启用 -> 禁用 -> 卸载。
     */
    @Test
    void fullLifecycleInstallEnableDisableUninstall() {
        // 安装
        runtimeService.install(descriptor);
        assertThat(runtimeService.find(PLUGIN_ID).get().status()).isEqualTo(PluginStatus.INSTALLED);

        // 启用
        runtimeService.enable(PLUGIN_ID);
        assertThat(runtimeService.find(PLUGIN_ID).get().status()).isEqualTo(PluginStatus.ENABLED);

        // 禁用
        runtimeService.disable(PLUGIN_ID);
        assertThat(runtimeService.find(PLUGIN_ID).get().status()).isEqualTo(PluginStatus.DISABLED);

        // 卸载
        runtimeService.uninstall(PLUGIN_ID);
        assertThat(runtimeService.find(PLUGIN_ID).get().status()).isEqualTo(PluginStatus.UNINSTALLED);
    }

    /**
     * 测试插件生命周期事件序列。
     */
    @Test
    void lifecycleEventsAreRecordedInOrder() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);
        runtimeService.uninstall(PLUGIN_ID);

        Optional<PluginRuntimeSnapshot> snapshot = runtimeService.find(PLUGIN_ID);
        assertThat(snapshot).isPresent();

        List<PluginLifecycleEvent> events = snapshot.get().events();
        assertThat(events).extracting(PluginLifecycleEvent::phase)
                .containsExactly(
                        PluginLifecyclePhase.INSTALLING,
                        PluginLifecyclePhase.INSTALLED,
                        PluginLifecyclePhase.ENABLING,
                        PluginLifecyclePhase.ENABLED,
                        PluginLifecyclePhase.DISABLING,
                        PluginLifecyclePhase.DISABLED,
                        PluginLifecyclePhase.UNINSTALLING,
                        PluginLifecyclePhase.UNINSTALLED);
    }

    /**
     * 测试使用自定义迁移执行器启用插件。
     */
    @Test
    void enableWithCustomMigrationExecutor() {
        PluginMigrationExecutor customExecutor = mock(PluginMigrationExecutor.class);
        runtimeService.install(descriptor);
        runtimeService.enable(descriptor, customExecutor);

        verify(customExecutor).migrate(any(PluginDescriptor.class), any(ClassLoader.class));
    }

    /**
     * 测试 install 时 descriptor 为 null 抛出异常。
     */
    @Test
    void installThrowsWhenDescriptorIsNull() {
        assertThatThrownBy(() -> runtimeService.install(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 测试 replace 时 descriptor 为 null 抛出异常。
     */
    @Test
    void replaceThrowsWhenDescriptorIsNull() {
        runtimeService.install(descriptor);

        assertThatThrownBy(() -> runtimeService.replace(null, Path.of("/path/to/updated.jar")))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 测试 reload 对已禁用（DISABLED）状态的插件抛出异常。
     */
    @Test
    void reloadThrowsWhenPluginIsDisabled() {
        runtimeService.install(descriptor);
        runtimeService.enable(PLUGIN_ID);
        runtimeService.disable(PLUGIN_ID);

        assertThatThrownBy(() -> runtimeService.reload(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot reload plugin unless it is enabled");
    }

    /**
     * 测试 reload 对已卸载（UNINSTALLED）状态的插件抛出异常。
     */
    @Test
    void reloadThrowsWhenPluginIsUninstalled() {
        runtimeService.install(descriptor);
        runtimeService.uninstall(PLUGIN_ID);

        assertThatThrownBy(() -> runtimeService.reload(PLUGIN_ID))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Cannot reload plugin unless it is enabled");
    }

    private PluginDescriptor createDescriptor(String id, String name) {
        return createDescriptor(id, name, null);
    }

    private PluginDescriptor createDescriptor(String id, String name, PluginI18n i18n) {
        return new PluginDescriptor(
                "1.0",
                id,
                name,
                null,
                null,
                null,
                "1.0.0",
                ">=1.0.0",
                null,
                null,
                "com.example.TestBoot",
                i18n,
                List.of(),
                null);
    }
}
