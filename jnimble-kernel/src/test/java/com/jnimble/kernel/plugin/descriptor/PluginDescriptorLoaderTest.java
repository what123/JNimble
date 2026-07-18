package com.jnimble.kernel.plugin.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnimble.sdk.plugin.PluginDescriptor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link PluginDescriptorLoader} 单元测试。
 *
 * <p>覆盖 load(Path)、loadJar()、load(InputStream)、loadJson() 等核心加载方法，
 * 包括正常路径和异常路径。</p>
 */
class PluginDescriptorLoaderTest {

    private static final String VALID_JSON = """
            {
                "schemaVersion": "1.0",
                "id": "test-plugin",
                "name": "Test Plugin",
                "version": "1.0.0",
                "platformVersion": "1.0",
                "bootClass": "test.TestPlugin"
            }
            """;

    private PluginDescriptorValidator validator;
    private PluginDescriptorLoader loader;

    @BeforeEach
    void setUp() {
        validator = new PluginDescriptorValidator("1.0");
        loader = new PluginDescriptorLoader(validator);
    }

    // ========== loadJson() 测试 ==========

    /**
     * 测试从有效 JSON 字符串加载 descriptor。
     */
    @Test
    void loadJsonValidDescriptor() {
        PluginDescriptor descriptor = loader.loadJson(VALID_JSON);

        assertThat(descriptor.id()).isEqualTo("test-plugin");
        assertThat(descriptor.name()).isEqualTo("Test Plugin");
        assertThat(descriptor.version()).isEqualTo("1.0.0");
        assertThat(descriptor.schemaVersion()).isEqualTo("1.0");
    }

    @Test
    void loadJsonWithAdminEntry() {
        String json = """
                {
                    "schemaVersion": "1.0",
                    "id": "test-plugin",
                    "name": "Test Plugin",
                    "version": "1.0.0",
                    "platformVersion": "1.0",
                    "bootClass": "test.TestPlugin",
                    "admin": {
                        "entry": "/settings",
                        "labelKey": "test.settings",
                        "permission": "test-plugin.settings"
                    },
                    "permissions": [
                        {"code": "test-plugin.settings", "name": "Settings"}
                    ]
                }
                """;

        PluginDescriptor descriptor = loader.loadJson(json);

        assertThat(descriptor.admin()).isNotNull();
        assertThat(descriptor.admin().entry()).isEqualTo("/settings");
        assertThat(descriptor.admin().labelKey()).isEqualTo("test.settings");
        assertThat(descriptor.admin().permission()).isEqualTo("test-plugin.settings");
    }

    @Test
    void loadJsonWithDeclarativeConfiguration() {
        String json = """
                {
                    "schemaVersion": "1.0",
                    "id": "test-plugin",
                    "name": "Test Plugin",
                    "version": "1.0.0",
                    "platformVersion": "1.0",
                    "bootClass": "test.TestPlugin",
                    "configuration": {
                        "title": "Settings",
                        "fields": [
                            {
                                "key": "merchantId",
                                "label": "Merchant ID",
                                "type": "TEXT",
                                "required": true
                            },
                            {
                                "key": "apiKey",
                                "label": "API Key",
                                "type": "SECRET",
                                "required": false
                            }
                        ]
                    }
                }
                """;

        PluginDescriptor descriptor = loader.loadJson(json);

        assertThat(descriptor.configuration()).isNotNull();
        assertThat(descriptor.configuration().fields()).hasSize(2);
        assertThat(descriptor.configuration().fields().get(1).type().name()).isEqualTo("SECRET");
    }

    /**
     * 测试从 null JSON 字符串加载抛出异常。
     */
    @Test
    void loadJsonNullThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJson(null))
                .withMessageContaining("required");
    }

    /**
     * 测试从空白 JSON 字符串加载抛出异常。
     */
    @Test
    void loadJsonBlankThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJson("   "))
                .withMessageContaining("required");
    }

    /**
     * 测试从格式错误的 JSON 字符串加载抛出异常。
     */
    @Test
    void loadJsonInvalidJsonThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJson("{invalid json}"))
                .withMessageContaining("Failed to read plugin descriptor json");
    }

    /**
     * 测试从缺少必填字段的 JSON 加载抛出校验异常。
     */
    @Test
    void loadJsonMissingRequiredFieldsThrowsValidationException() {
        String incompleteJson = """
                {
                    "schemaVersion": "1.0"
                }
                """;

        assertThatExceptionOfType(PluginDescriptorValidationException.class)
                .isThrownBy(() -> loader.loadJson(incompleteJson));
    }

    /**
     * 测试 JSON 中包含未知属性时不抛出异常（FAIL_ON_UNKNOWN_PROPERTIES 已禁用）。
     */
    @Test
    void loadJsonWithUnknownPropertiesSucceeds() {
        String jsonWithExtra = """
                {
                    "schemaVersion": "1.0",
                    "id": "test-plugin",
                    "name": "Test Plugin",
                    "version": "1.0.0",
                    "platformVersion": "1.0",
                    "bootClass": "test.TestPlugin",
                    "unknownField": "value"
                }
                """;

        assertThatNoException().isThrownBy(() -> loader.loadJson(jsonWithExtra));
    }

    // ========== load(InputStream) 测试 ==========

    /**
     * 测试从有效 InputStream 加载 descriptor。
     */
    @Test
    void loadFromValidInputStream() {
        InputStream inputStream = new ByteArrayInputStream(
                VALID_JSON.getBytes(StandardCharsets.UTF_8));

        PluginDescriptor descriptor = loader.load(inputStream);

        assertThat(descriptor.id()).isEqualTo("test-plugin");
    }

    /**
     * 测试从 null InputStream 加载抛出异常。
     */
    @Test
    void loadFromNullInputStreamThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.load((InputStream) null))
                .withMessageContaining("required");
    }

    /**
     * 测试从包含无效数据的 InputStream 加载抛出异常。
     */
    @Test
    void loadFromInvalidInputStreamThrowsException() {
        InputStream inputStream = new ByteArrayInputStream(
                "not json".getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.load(inputStream));
    }

    // ========== load(Path) 测试 ==========

    /**
     * 测试从有效文件路径加载 descriptor（使用 META-INF 目录结构）。
     */
    @Test
    void loadFromValidPathWithMetaInf(@TempDir Path tempDir) throws Exception {
        Path metaInfDir = tempDir.resolve("META-INF");
        Files.createDirectories(metaInfDir);
        Files.writeString(metaInfDir.resolve("jnimble-plugin.json"), VALID_JSON);

        PluginDescriptor descriptor = loader.load(tempDir);

        assertThat(descriptor.id()).isEqualTo("test-plugin");
    }

    /**
     * 测试从有效文件路径加载 descriptor（使用根目录结构）。
     */
    @Test
    void loadFromValidPathAtRoot(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("jnimble-plugin.json"), VALID_JSON);

        PluginDescriptor descriptor = loader.load(tempDir);

        assertThat(descriptor.id()).isEqualTo("test-plugin");
    }

    /**
     * 测试 META-INF 路径优先于根目录路径。
     */
    @Test
    void loadPrefersMetaInfOverRoot(@TempDir Path tempDir) throws Exception {
        String metaInfJson = """
                {
                    "schemaVersion": "1.0",
                    "id": "meta-plugin",
                    "name": "Meta Plugin",
                    "version": "1.0.0",
                    "platformVersion": "1.0",
                    "bootClass": "test.MetaPlugin"
                }
                """;
        Path metaInfDir = tempDir.resolve("META-INF");
        Files.createDirectories(metaInfDir);
        Files.writeString(metaInfDir.resolve("jnimble-plugin.json"), metaInfJson);
        Files.writeString(tempDir.resolve("jnimble-plugin.json"), VALID_JSON);

        PluginDescriptor descriptor = loader.load(tempDir);

        assertThat(descriptor.id()).isEqualTo("meta-plugin");
    }

    /**
     * 测试从 null 路径加载抛出异常。
     */
    @Test
    void loadFromNullPathThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.load((java.nio.file.Path) null))
                .withMessageContaining("required");
    }

    /**
     * 测试从不存在的路径加载抛出异常。
     */
    @Test
    void loadFromNonExistentPathThrowsException(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("nonexistent");

        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.load(nonExistent));
    }

    // ========== loadJar() 测试 ==========

    /**
     * 测试从包含有效 descriptor 的 JAR 文件加载。
     */
    @Test
    void loadFromValidJar(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("plugin.jar");
        createTestJar(jarPath, VALID_JSON);

        PluginDescriptor descriptor = loader.loadJar(jarPath);

        assertThat(descriptor.id()).isEqualTo("test-plugin");
    }

    /**
     * 测试从 null JAR 路径加载抛出异常。
     */
    @Test
    void loadFromNullJarPathThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJar(null))
                .withMessageContaining("required");
    }

    /**
     * 测试从不存在的 JAR 文件加载抛出异常。
     */
    @Test
    void loadFromNonExistentJarThrowsException(@TempDir Path tempDir) {
        Path nonExistentJar = tempDir.resolve("nonexistent.jar");

        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJar(nonExistentJar));
    }

    /**
     * 测试从不包含 descriptor 文件的 JAR 加载抛出异常。
     */
    @Test
    void loadFromJarWithoutDescriptorThrowsException(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("empty.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("other-file.txt"));
            jos.write("content".getBytes());
            jos.closeEntry();
        }

        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadJar(jarPath))
                .withMessageContaining("does not contain");
    }

    // ========== loadFile() 测试 ==========

    /**
     * 测试从 null 文件路径加载抛出异常。
     */
    @Test
    void loadFileFromNullPathThrowsException() {
        assertThatExceptionOfType(PluginDescriptorLoadException.class)
                .isThrownBy(() -> loader.loadFile(null))
                .withMessageContaining("required");
    }

    // ========== 构造函数测试 ==========

    /**
     * 测试使用自定义 ObjectMapper 和 validator 构造。
     */
    @Test
    void constructWithCustomObjectMapperAndValidator() {
        ObjectMapper customMapper = new ObjectMapper();
        PluginDescriptorValidator customValidator = new PluginDescriptorValidator("1.0");
        PluginDescriptorLoader customLoader = new PluginDescriptorLoader(customMapper, customValidator);

        PluginDescriptor descriptor = customLoader.loadJson(VALID_JSON);
        assertThat(descriptor.id()).isEqualTo("test-plugin");
    }

    // ========== 辅助方法 ==========

    private static void createTestJar(Path jarPath, String descriptorContent) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            JarEntry entry = new JarEntry("META-INF/jnimble-plugin.json");
            jos.putNextEntry(entry);
            jos.write(descriptorContent.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }
}
