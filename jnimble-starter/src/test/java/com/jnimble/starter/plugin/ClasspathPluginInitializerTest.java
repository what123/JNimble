package com.jnimble.starter.plugin;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

/**
 * {@link ClasspathPluginInitializer} 单元测试。
 *
 * <p>覆盖禁用发现、已安装插件跳过、自动启用、fail-fast 等核心逻辑。</p>
 */
class ClasspathPluginInitializerTest {

    private PluginDiscoveryProperties properties;
    private PluginRuntimeService pluginRuntimeService;
    private ClasspathPluginInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new PluginDiscoveryProperties();
        pluginRuntimeService = org.mockito.Mockito.mock(PluginRuntimeService.class);
        initializer = new ClasspathPluginInitializer(properties, pluginRuntimeService);
    }

    // ========== devClasspathEnabled 开关测试 ==========

    /**
     * 测试 devClasspathEnabled 为 false 时，run 方法直接返回不执行任何操作。
     */
    @Test
    void runWhenDevClasspathDisabledShouldReturnEarly() {
        properties.setDevClasspathEnabled(false);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class));
    }

    // ========== 已安装插件跳过测试 ==========

    /**
     * 测试插件已安装时跳过重复安装。
     */
    @Test
    void runWhenClasspathDescriptorChangedShouldRefreshWithoutInstalling() {
        properties.setDevClasspathEnabled(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.of(buildSnapshot("test-plugin", PluginStatus.INSTALLED)));

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class));
        verify(pluginRuntimeService, atLeastOnce()).replace(any(PluginDescriptor.class), eq(null));
    }

    @Test
    void runWhenEnabledClasspathDescriptorChangedShouldRestoreEnabledState() {
        properties.setDevClasspathEnabled(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);
        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.of(buildSnapshot("test-plugin", PluginSource.CLASSPATH, null, PluginStatus.ENABLED)));

        initializer.run(args);

        verify(pluginRuntimeService, atLeastOnce()).disable(any(String.class));
        verify(pluginRuntimeService, atLeastOnce()).replace(any(PluginDescriptor.class), eq(null));
        verify(pluginRuntimeService, atLeastOnce()).enable(any(String.class));
    }

    @Test
    void runWhenExternalArtifactIsMissingShouldReplaceWithClasspathPlugin() {
        properties.setDevClasspathEnabled(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);
        Path missingArtifact = Path.of("target", "missing-plugin.jar");
        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.of(buildSnapshot(
                        "test-plugin",
                        PluginSource.JAR,
                        missingArtifact,
                        PluginStatus.FAILED)));

        initializer.run(args);

        verify(pluginRuntimeService, atLeastOnce()).replace(any(PluginDescriptor.class), eq(null));
    }

    @Test
    void runWhenExternalArtifactExistsShouldReplaceWithClasspathPlugin() throws Exception {
        properties.setDevClasspathEnabled(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);
        Path artifact = Files.createTempFile("jnimble-plugin-", ".jar");
        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.of(buildSnapshot(
                        "test-plugin",
                        PluginSource.JAR,
                        artifact,
                        PluginStatus.INSTALLED)));

        try {
            initializer.run(args);
            verify(pluginRuntimeService, atLeastOnce()).replace(any(PluginDescriptor.class), eq(null));
        } finally {
            Files.deleteIfExists(artifact);
        }
    }

    // ========== 自动启用测试 ==========

    /**
     * 测试 autoEnable 为 true 时，安装后自动启用插件。
     */
    @Test
    void runWhenAutoEnableEnabledShouldEnablePlugin() {
        properties.setDevClasspathEnabled(true);
        properties.setAutoEnable(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService, atLeastOnce()).enable(any(String.class));
    }

    /**
     * 测试 autoEnable 为 false 时，安装后不自动启用插件。
     */
    @Test
    void runWhenAutoEnableDisabledShouldNotEnablePlugin() {
        properties.setDevClasspathEnabled(true);
        properties.setAutoEnable(false);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService, never()).enable(any(String.class));
    }

    // ========== fail-fast 测试 ==========

    /**
     * 测试 failFast 为 true 时，安装失败抛出 IllegalStateException。
     */
    @Test
    void runWhenFailFastEnabledAndInstallFailsShouldThrow() {
        properties.setDevClasspathEnabled(true);
        properties.setFailFast(true);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("install failed"))
                .when(pluginRuntimeService).install(any(PluginDescriptor.class));

        assertThatThrownBy(() -> initializer.run(args))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 测试 failFast 为 false 时，安装失败仅记录日志不抛出异常。
     */
    @Test
    void runWhenFailFastDisabledAndInstallFailsShouldNotThrow() {
        properties.setDevClasspathEnabled(true);
        properties.setFailFast(false);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(any(String.class)))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("install failed"))
                .when(pluginRuntimeService).install(any(PluginDescriptor.class));

        assertThatNoException().isThrownBy(() -> initializer.run(args));
    }

    // ========== 辅助方法 ==========

    private static PluginRuntimeSnapshot buildSnapshot(String pluginId, PluginStatus status) {
        return buildSnapshot(pluginId, PluginSource.CLASSPATH, null, status);
    }

    private static PluginRuntimeSnapshot buildSnapshot(
            String pluginId,
            PluginSource source,
            Path artifactPath,
            PluginStatus status
    ) {
        return new PluginRuntimeSnapshot(
                pluginId,
                null,
                source,
                artifactPath,
                status,
                0,
                null,
                Instant.now(),
                null,
                null,
                List.of());
    }
}
