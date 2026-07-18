package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.plugin.PluginDescriptor;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReflectivePluginBootLoaderTest {

    /** 示例 PluginBoot 实现，用于正常加载测试 */
    public static class ValidPluginBoot implements PluginBoot {
        @Override
        public void boot(PluginContext context) {
            // no-op
        }
    }

    /** 未实现 PluginBoot 接口的普通类，用于验证加载失败场景 */
    public static class NotAPluginBoot {
        // 故意不实现 PluginBoot 接口
    }

    private static PluginDescriptor descriptor(String bootClass) {
        return new PluginDescriptor(
                "1.0",
                "test-plugin",
                "Test",
                null,
                null,
                null,
                "1.0.0",
                "0.1.x",
                null,
                null,
                bootClass,
                null,
                List.of(),
                null);
    }

    /** 测试正常加载 PluginBoot 实例 */
    @Test
    void loadsValidPluginBootInstance() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc);

        assertThat(loaded).isNotNull();
        assertThat(loaded.boot()).isInstanceOf(ValidPluginBoot.class);
        assertThat(loaded.classLoader()).isNotNull();
    }

    /** 测试 bootClass 为 null 时抛出 PluginRuntimeException */
    @Test
    void rejectsNullBootClass() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(null);

        assertThatThrownBy(() -> loader.load(desc))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("bootClass is required");
    }

    /** 测试 bootClass 为空字符串时抛出 PluginRuntimeException */
    @Test
    void rejectsBlankBootClass() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor("   ");

        assertThatThrownBy(() -> loader.load(desc))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("bootClass is required");
    }

    /** 测试指定的类不存在时抛出 PluginRuntimeException */
    @Test
    void throwsWhenClassNotFound() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor("com.example.NonexistentClass");

        assertThatThrownBy(() -> loader.load(desc))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("Failed to instantiate");
    }

    /** 测试类未实现 PluginBoot 接口时抛出 PluginRuntimeException */
    @Test
    void throwsWhenClassDoesNotImplementPluginBoot() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(NotAPluginBoot.class.getName());

        assertThatThrownBy(() -> loader.load(desc))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("does not implement PluginBoot");
    }

    /** 测试通过自定义 ClassLoader 加载插件，验证 ClassLoader 隔离 */
    @Test
    void usesProvidedClassLoaderForLoading() {
        ClassLoader parent = ReflectivePluginBootLoaderTest.class.getClassLoader();
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader(parent);
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc);

        assertThat(loaded.classLoader()).isEqualTo(parent);
    }

    /** 测试 null ClassLoader 时回退到当前类的 ClassLoader */
    @Test
    void fallsBackToOwnClassLoaderWhenContextClassLoaderIsNull() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader((ClassLoader) null);
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc);

        assertThat(loaded).isNotNull();
        assertThat(loaded.boot()).isInstanceOf(ValidPluginBoot.class);
    }

    /** 测试 artifactPath 为 null 时回退到默认类路径加载 */
    @Test
    void fallsBackToClasspathLoadWhenArtifactPathIsNull() {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc, (Path) null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.boot()).isInstanceOf(ValidPluginBoot.class);
    }

    /** 测试 artifactPath 指向不存在的文件时抛出异常 */
    @Test
    void throwsWhenArtifactPathDoesNotExist(@TempDir Path tempDir) {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());
        Path missingPath = tempDir.resolve("nonexistent.jar");

        assertThatThrownBy(() -> loader.load(desc, missingPath))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("does not exist");
    }

    /** 测试 artifactPath 指向目录（而非文件）时抛出异常 */
    @Test
    void throwsWhenArtifactPathIsDirectory(@TempDir Path tempDir) {
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());
        Path dirPath = tempDir.resolve("not-a-jar");

        assertThatThrownBy(() -> loader.load(desc, dirPath))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("does not exist");
    }

    /** 测试通过独立 ClassLoader 加载时，插件 ClassLoader 与应用 ClassLoader 不同 */
    @Test
    void createsIsolatedClassLoaderForArtifact(@TempDir Path tempDir) throws Exception {
        Path jarPath = createMinimalPluginJar(tempDir, ValidPluginBoot.class);
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc, jarPath);

        assertThat(loaded).isNotNull();
        assertThat(loaded.boot()).isInstanceOf(ValidPluginBoot.class);
        assertThat(loaded.classLoader()).isInstanceOf(URLClassLoader.class);
        assertThat(loaded.classLoader())
                .isNotEqualTo(ReflectivePluginBootLoaderTest.class.getClassLoader());
    }

    /** 测试关闭 LoadedPluginBoot 时 ClassLoader 被正确释放 */
    @Test
    void closeReleasesIsolatedClassLoader(@TempDir Path tempDir) throws Exception {
        Path jarPath = createMinimalPluginJar(tempDir, ValidPluginBoot.class);
        ReflectivePluginBootLoader loader = new ReflectivePluginBootLoader();
        PluginDescriptor desc = descriptor(ValidPluginBoot.class.getName());

        LoadedPluginBoot loaded = loader.load(desc, jarPath);
        assertThat(loaded.classLoader()).isInstanceOf(URLClassLoader.class);
        loaded.close();
    }

    /**
     * 创建一个最小化的 JAR 文件，仅包含给定的类。
     * 用于模拟插件 artifact 加载场景。
     */
    private Path createMinimalPluginJar(Path tempDir, Class<?> clazz) throws Exception {
        Path jarDir = tempDir.resolve("jar-content");
        Path classDir = jarDir.resolve(clazz.getPackageName().replace('.', File.separatorChar));
        Files.createDirectories(classDir);
        Path classFile = classDir.resolve(clazz.getSimpleName() + ".class");
        byte[] bytecode = clazz.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class")
                .readAllBytes();
        Files.write(classFile, bytecode);

        Path jarPath = tempDir.resolve("test-plugin.jar");
        java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                Files.newOutputStream(jarPath));
        java.util.jar.JarEntry entry = new java.util.jar.JarEntry(
                clazz.getName().replace('.', '/') + ".class");
        jos.putNextEntry(entry);
        jos.write(bytecode);
        jos.closeEntry();
        jos.close();

        return jarPath;
    }
}
