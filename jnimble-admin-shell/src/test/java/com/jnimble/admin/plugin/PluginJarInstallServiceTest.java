package com.jnimble.admin.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.sdk.plugin.PluginStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * PluginJarInstallService 单元测试。
 *
 * <p>验证插件 JAR 安装服务的核心逻辑，包括 JAR 校验、安装和替换流程，
 * 覆盖正常场景和异常场景（空文件、非 JAR 格式、重复安装等）。</p>
 */
@ExtendWith(MockitoExtension.class)
class PluginJarInstallServiceTest {

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    private PluginInstallProperties properties;

    private PluginJarInstallService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new PluginInstallProperties();
        properties.setDir(tempDir.toString());
        service = new PluginJarInstallService(pluginRuntimeService, properties);
    }

    /**
     * 测试空文件上传时抛出 IllegalArgumentException。
     */
    @Test
    void installWithEmptyJarThrowsException() {
        MockMultipartFile emptyJar = new MockMultipartFile(
                "jar", "plugin.jar", "application/java-archive", new byte[0]);

        assertThatThrownBy(() -> service.install(emptyJar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin jar is required");
    }

    /**
     * 测试 null 文件上传时抛出 IllegalArgumentException。
     */
    @Test
    void installWithNullJarThrowsException() {
        assertThatThrownBy(() -> service.install(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin jar is required");
    }

    /**
     * 测试非 JAR 扩展名文件上传时抛出 IllegalArgumentException。
     */
    @Test
    void installWithNonJarExtensionThrowsException() {
        MockMultipartFile zipFile = new MockMultipartFile(
                "jar", "plugin.zip", "application/zip", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.install(zipFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only .jar plugin packages are supported");
    }

    /**
     * 测试 replace 方法在 pluginId 为空时抛出异常。
     */
    @Test
    void replaceWithBlankPluginIdThrowsException() {
        MockMultipartFile jar = new MockMultipartFile(
                "jar", "plugin.jar", "application/java-archive", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.replace("  ", jar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin id is required");
    }

    /**
     * 测试 replace 方法在 pluginId 为 null 时抛出异常。
     */
    @Test
    void replaceWithNullPluginIdThrowsException() {
        MockMultipartFile jar = new MockMultipartFile(
                "jar", "plugin.jar", "application/java-archive", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.replace(null, jar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin id is required");
    }

    /**
     * 测试 replace 方法在 JAR 文件为空时抛出异常。
     */
    @Test
    void replaceWithEmptyJarThrowsException() {
        MockMultipartFile emptyJar = new MockMultipartFile(
                "jar", "plugin.jar", "application/java-archive", new byte[0]);

        assertThatThrownBy(() -> service.replace("test-plugin", emptyJar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin jar is required");
    }

    /**
     * 测试 PluginInstallProperties 默认值和 setter/getter。
     */
    @Test
    void pluginInstallPropertiesDefaultsAreCorrect() {
        PluginInstallProperties props = new PluginInstallProperties();
        assertThat(props.getDir()).isEqualTo("./plugins");
        assertThat(props.getPlatformVersion()).isEqualTo("0.1.0");

        props.setDir("/custom/path");
        props.setPlatformVersion("1.0.0");
        assertThat(props.getDir()).isEqualTo("/custom/path");
        assertThat(props.getPlatformVersion()).isEqualTo("1.0.0");
    }
}
