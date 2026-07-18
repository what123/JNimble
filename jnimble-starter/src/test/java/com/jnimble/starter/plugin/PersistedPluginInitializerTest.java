package com.jnimble.starter.plugin;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.platform.plugin.PluginStateRecord;
import com.jnimble.platform.plugin.PluginStateStore;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

/**
 * {@link PersistedPluginInitializer} 单元测试。
 *
 * <p>覆盖禁用恢复、跳过已卸载插件、CLASSPATH 插件恢复、DIRECTORY 插件恢复、
 * 自动启用、fail-fast 等核心逻辑。</p>
 */
class PersistedPluginInitializerTest {

    private static final String VALID_DESCRIPTOR_JSON = """
            {
                "schemaVersion": "1.0",
                "id": "test-plugin",
                "name": "Test Plugin",
                "version": "1.0.0",
                "platformVersion": "0.1.0",
                "bootClass": "test.TestPlugin"
            }
            """;

    private PluginDiscoveryProperties properties;
    private PluginStateStore pluginStateStore;
    private PluginRuntimeService pluginRuntimeService;
    private PersistedPluginInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new PluginDiscoveryProperties();
        pluginStateStore = org.mockito.Mockito.mock(PluginStateStore.class);
        pluginRuntimeService = org.mockito.Mockito.mock(PluginRuntimeService.class);
        initializer = new PersistedPluginInitializer(properties, pluginStateStore, pluginRuntimeService);
    }

    // ========== restoreEnabled 开关测试 ==========

    /**
     * 测试 restoreEnabled 为 false 时，run 方法直接返回不恢复任何插件。
     */
    @Test
    void runWhenRestoreDisabledShouldReturnEarly() {
        properties.setRestoreEnabled(false);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginStateStore, never()).list();
    }

    // ========== UNINSTALLED 状态跳过测试 ==========

    /**
     * 测试状态为 UNINSTALLED 的插件应被跳过，不执行恢复操作。
     */
    @Test
    void runWhenPluginUninstalledShouldSkip() {
        properties.setRestoreEnabled(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.UNINSTALLED, PluginSource.CLASSPATH, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any());
    }

    // ========== CLASSPATH 插件恢复测试 ==========

    /**
     * 测试恢复 CLASSPATH 来源的插件，调用不带 artifactPath 的 install 方法。
     */
    @Test
    void runWithClasspathPluginShouldRestoreWithoutArtifactPath() {
        properties.setRestoreEnabled(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.INSTALLED, PluginSource.CLASSPATH, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService).install(any());
    }

    // ========== DIRECTORY 插件恢复测试 ==========

    /**
     * 测试恢复 DIRECTORY 来源的插件，调用不带 artifactPath 的 install 方法。
     */
    @Test
    void runWithDirectoryPluginShouldRestoreWithoutArtifactPath() {
        properties.setRestoreEnabled(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.INSTALLED, PluginSource.DIRECTORY, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService).install(any());
    }

    // ========== 启用已启用插件测试 ==========

    /**
     * 测试插件状态为 ENABLED 时，恢复后应自动启用。
     */
    @Test
    void runWithEnabledPluginShouldEnableAfterRestore() {
        properties.setRestoreEnabled(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.ENABLED, PluginSource.CLASSPATH, true, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService).enable(anyString());
    }

    /**
     * 测试插件状态为 DISABLED 且 enabled 为 false 时，恢复后不应启用。
     */
    @Test
    void runWithDisabledPluginShouldNotEnableAfterRestore() {
        properties.setRestoreEnabled(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.DISABLED, PluginSource.CLASSPATH, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).enable(anyString());
    }

    // ========== fail-fast 测试 ==========

    /**
     * 测试 failFast 为 true 时，恢复失败抛出 IllegalStateException。
     */
    @Test
    void runWhenFailFastEnabledAndRestoreFailsShouldThrow() {
        properties.setRestoreEnabled(true);
        properties.setFailFast(true);
        PluginStateRecord record = buildRecord(
                PluginStatus.INSTALLED, PluginSource.CLASSPATH, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        doThrow(new RuntimeException("restore failed"))
                .when(pluginRuntimeService).install(any());
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        assertThatThrownBy(() -> initializer.run(args))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 测试 failFast 为 false 时，恢复失败仅记录日志不抛出异常。
     */
    @Test
    void runWhenFailFastDisabledAndRestoreFailsShouldNotThrow() {
        properties.setRestoreEnabled(true);
        properties.setFailFast(false);
        PluginStateRecord record = buildRecord(
                PluginStatus.INSTALLED, PluginSource.CLASSPATH, false, null, VALID_DESCRIPTOR_JSON);
        when(pluginStateStore.list()).thenReturn(List.of(record));
        doThrow(new RuntimeException("restore failed"))
                .when(pluginRuntimeService).install(any());
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        assertThatNoException().isThrownBy(() -> initializer.run(args));
    }

    // ========== 空列表测试 ==========

    /**
     * 测试状态存储返回空列表时不执行任何恢复操作。
     */
    @Test
    void runWhenStateStoreEmptyShouldDoNothing() {
        properties.setRestoreEnabled(true);
        when(pluginStateStore.list()).thenReturn(List.of());
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any());
    }

    // ========== 辅助方法 ==========

    private PluginStateRecord buildRecord(
            PluginStatus status,
            PluginSource source,
            boolean enabled,
            String artifactPath,
            String descriptorJson) {
        return new PluginStateRecord(
                "test-plugin",
                "Test Plugin",
                "1.0.0",
                source,
                artifactPath,
                enabled,
                status,
                descriptorJson,
                "hash",
                null,
                Instant.now(),
                null,
                null,
                Instant.now(),
                Instant.now());
    }
}
