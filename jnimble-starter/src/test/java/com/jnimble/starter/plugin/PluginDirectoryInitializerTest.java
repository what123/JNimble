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
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;

/**
 * {@link PluginDirectoryInitializer} 单元测试。
 *
 * <p>覆盖禁用扫描、目录不存在、JAR 安装、已安装跳过、自动启用、
 * 非 JAR 文件过滤、fail-fast 等核心逻辑。</p>
 */
class PluginDirectoryInitializerTest {

    private PluginDiscoveryProperties properties;
    private PluginRuntimeService pluginRuntimeService;
    private PluginDirectoryInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new PluginDiscoveryProperties();
        pluginRuntimeService = org.mockito.Mockito.mock(PluginRuntimeService.class);
        initializer = new PluginDirectoryInitializer(properties, pluginRuntimeService);
    }

    // ========== directoryScanEnabled 开关测试 ==========

    /**
     * 测试 directoryScanEnabled 为 false 时，run 方法直接返回不扫描目录。
     */
    @Test
    void runWhenDirectoryScanDisabledShouldReturnEarly() {
        properties.setDirectoryScanEnabled(false);
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class), any(Path.class));
    }

    // ========== 目录不存在测试 ==========

    /**
     * 测试插件目录不存在时，仅记录日志不抛出异常。
     */
    @Test
    void runWhenDirectoryDoesNotExistShouldSkipGracefully() {
        properties.setDirectoryScanEnabled(true);
        properties.setDir("/nonexistent/path");
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        assertThatNoException().isThrownBy(() -> initializer.run(args));
    }

    // ========== JAR 安装测试 ==========

    /**
     * 测试扫描目录中存在 JAR 文件时，加载描述符并安装插件。
     */
    @Test
    void runWithJarFilesShouldInstallPlugins(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService).install(any(PluginDescriptor.class), any(Path.class));
    }

    // ========== 已安装插件跳过测试 ==========

    /**
     * 测试插件已安装时跳过重复安装。
     */
    @Test
    void runWhenPluginAlreadyInstalledShouldSkip(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString()))
                .thenReturn(Optional.of(buildSnapshot("test-plugin", PluginStatus.INSTALLED)));

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class), any(Path.class));
    }

    // ========== 自动启用测试 ==========

    /**
     * 测试 autoEnable 为 true 时，安装后自动启用插件。
     */
    @Test
    void runWhenAutoEnableEnabledShouldEnablePlugin(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        properties.setAutoEnable(true);
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService).enable(anyString());
    }

    /**
     * 测试 autoEnable 为 false 时，安装后不自动启用插件。
     */
    @Test
    void runWhenAutoEnableDisabledShouldNotEnablePlugin(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        properties.setAutoEnable(false);
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService, never()).enable(anyString());
    }

    // ========== 非 JAR 文件过滤测试 ==========

    /**
     * 测试非 JAR 文件被过滤，不尝试安装。
     */
    @Test
    void runWithNonJarFilesShouldFilterThemOut(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        Files.writeString(tempDir.resolve("readme.txt"), "not a jar");
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class), any(Path.class));
    }

    /**
     * 测试大小写混合的 .JAR 扩展名也能正确识别。
     */
    @Test
    void runWithUpperCaseJarExtensionShouldBeRecognized(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        createTestJar(tempDir.resolve("test-plugin.JAR"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());

        initializer.run(args);

        verify(pluginRuntimeService).install(any(PluginDescriptor.class), any(Path.class));
    }

    // ========== fail-fast 测试 ==========

    /**
     * 测试 failFast 为 true 时，安装失败抛出 IllegalStateException。
     */
    @Test
    void runWhenFailFastEnabledAndInstallFailsShouldThrow(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        properties.setFailFast(true);
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("install failed"))
                .when(pluginRuntimeService).install(any(PluginDescriptor.class), any(Path.class));

        assertThatThrownBy(() -> initializer.run(args))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 测试 failFast 为 false 时，安装失败仅记录日志不抛出异常。
     */
    @Test
    void runWhenFailFastDisabledAndInstallFailsShouldNotThrow(@TempDir Path tempDir) throws Exception {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        properties.setFailFast(false);
        createTestJar(tempDir.resolve("test-plugin.jar"));
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        when(pluginRuntimeService.find(anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("install failed"))
                .when(pluginRuntimeService).install(any(PluginDescriptor.class), any(Path.class));

        assertThatNoException().isThrownBy(() -> initializer.run(args));
    }

    // ========== 空目录测试 ==========

    /**
     * 测试空插件目录时不执行任何安装操作。
     */
    @Test
    void runWithEmptyDirectoryShouldDoNothing(@TempDir Path tempDir) {
        properties.setDirectoryScanEnabled(true);
        properties.setDir(tempDir.toString());
        ApplicationArguments args = org.mockito.Mockito.mock(ApplicationArguments.class);

        initializer.run(args);

        verify(pluginRuntimeService, never()).install(any(PluginDescriptor.class), any(Path.class));
    }

    // ========== 辅助方法 ==========

    private void createTestJar(Path jarPath) throws IOException {
        try (var jos = new java.util.jar.JarOutputStream(Files.newOutputStream(jarPath))) {
            var entry = new java.util.jar.JarEntry("META-INF/jnimble-plugin.json");
            jos.putNextEntry(entry);
            jos.write("""
                    {
                        "schemaVersion": "1.0",
                        "id": "test-plugin",
                        "name": "Test Plugin",
                        "version": "1.0.0",
                        "platformVersion": "0.1.0",
                        "bootClass": "test.TestPlugin"
                    }
                    """.getBytes());
            jos.closeEntry();
        }
    }

    private static PluginRuntimeSnapshot buildSnapshot(String pluginId, PluginStatus status) {
        return new PluginRuntimeSnapshot(
                pluginId,
                null,
                PluginSource.JAR,
                null,
                status,
                0,
                null,
                Instant.now(),
                null,
                null,
                List.of());
    }
}
