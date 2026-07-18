package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginLifecycleEvent;
import com.jnimble.sdk.plugin.PluginLifecyclePhase;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * PluginRuntimeState 单元测试。
 *
 * <p>验证插件运行时状态的管理、状态转换和快照功能。</p>
 */
class PluginRuntimeStateTest {

    private PluginDescriptor descriptor;
    private PluginRuntimeState state;

    @BeforeEach
    void setUp() {
        descriptor = createDescriptor("test-plugin", "Test Plugin");
        state = new PluginRuntimeState(descriptor);
    }

    /**
     * 测试初始状态应为 DISCOVERED。
     */
    @Test
    void initialStateShouldBeDiscovered() {
        assertThat(state.status()).isEqualTo(PluginStatus.DISCOVERED);
        assertThat(state.pluginId()).isEqualTo("test-plugin");
        assertThat(state.descriptor()).isEqualTo(descriptor);
        assertThat(state.artifactPath()).isNull();
    }

    /**
     * 测试设置和获取插件状态。
     */
    @Test
    void setStatusShouldUpdateStatus() {
        state.setStatus(PluginStatus.INSTALLED);
        assertThat(state.status()).isEqualTo(PluginStatus.INSTALLED);

        state.setStatus(PluginStatus.ENABLED);
        assertThat(state.status()).isEqualTo(PluginStatus.ENABLED);

        state.setStatus(PluginStatus.DISABLED);
        assertThat(state.status()).isEqualTo(PluginStatus.DISABLED);
    }

    /**
     * 测试替换描述符和 artifact 路径。
     */
    @Test
    void replaceDescriptorShouldUpdateDescriptorAndPath() {
        PluginDescriptor newDescriptor = createDescriptor("test-plugin", "Updated Plugin");
        Path artifactPath = Path.of("/path/to/plugin.jar");

        PluginRuntimeState result = state.replaceDescriptor(newDescriptor, artifactPath);

        assertThat(result).isEqualTo(state);
        assertThat(state.descriptor()).isEqualTo(newDescriptor);
        assertThat(state.artifactPath()).isEqualTo(artifactPath);
    }

    /**
     * 测试替换描述符时 artifact 路径为 null，source 应为 CLASSPATH。
     */
    @Test
    void replaceDescriptorWithNullPathShouldUseClasspathSource() {
        PluginDescriptor newDescriptor = createDescriptor("test-plugin", "Updated Plugin");

        state.replaceDescriptor(newDescriptor, null);

        assertThat(state.artifactPath()).isNull();
    }

    /**
     * 测试添加和获取注册句柄。
     */
    @Test
    void addHandleShouldAddToHandlesList() {
        RegistrationHandle handle1 = Mockito.mock(RegistrationHandle.class);
        RegistrationHandle handle2 = Mockito.mock(RegistrationHandle.class);

        state.addHandle(handle1);
        state.addHandle(handle2);

        assertThat(state.handles()).hasSize(2);
        assertThat(state.handles()).containsExactly(handle1, handle2);
    }

    /**
     * 测试添加重复的句柄不会重复添加。
     */
    @Test
    void addDuplicateHandleShouldNotDuplicate() {
        RegistrationHandle handle = Mockito.mock(RegistrationHandle.class);

        state.addHandle(handle);
        state.addHandle(handle);

        assertThat(state.handles()).hasSize(1);
    }

    /**
     * 测试添加 null 句柄不会添加。
     */
    @Test
    void addNullHandleShouldNotAdd() {
        state.addHandle(null);

        assertThat(state.handles()).isEmpty();
    }

    /**
     * 测试设置和获取加载的插件启动实例。
     */
    @Test
    void setLoadedBootShouldUpdateLoadedBoot() {
        LoadedPluginBoot loadedBoot = Mockito.mock(LoadedPluginBoot.class);

        state.setLoadedBoot(loadedBoot);

        assertThat(state.loadedBoot()).isEqualTo(loadedBoot);
    }

    /**
     * 测试设置和获取插件上下文。
     */
    @Test
    void setContextShouldUpdateContext() {
        PluginContext context = Mockito.mock(PluginContext.class);

        state.setContext(context);

        assertThat(state.context()).isEqualTo(context);
    }

    /**
     * 测试设置最后错误信息通过 snapshot 验证。
     */
    @Test
    void setLastErrorShouldUpdateLastError() {
        state.setLastError("Something went wrong");

        PluginRuntimeSnapshot snapshot = state.snapshot();
        assertThat(snapshot.lastError()).isEqualTo("Something went wrong");
    }

    /**
     * 测试设置时间戳通过 snapshot 验证。
     */
    @Test
    void setTimestampsShouldUpdateTimestamps() {
        Instant now = Instant.now();

        state.setInstalledAt(now);
        state.setLastStartedAt(now.plusSeconds(100));
        state.setLastStoppedAt(now.plusSeconds(200));

        PluginRuntimeSnapshot snapshot = state.snapshot();
        assertThat(snapshot.installedAt()).isEqualTo(now);
        assertThat(snapshot.lastStartedAt()).isEqualTo(now.plusSeconds(100));
        assertThat(snapshot.lastStoppedAt()).isEqualTo(now.plusSeconds(200));
    }

    /**
     * 测试添加生命周期事件通过 snapshot 验证。
     */
    @Test
    void addEventShouldAddToEventsList() {
        PluginLifecycleEvent event1 = new PluginLifecycleEvent(
                "test-plugin", PluginLifecyclePhase.INSTALLED, Instant.now(), null);
        PluginLifecycleEvent event2 = new PluginLifecycleEvent(
                "test-plugin", PluginLifecyclePhase.ENABLED, Instant.now(), null);

        state.addEvent(event1);
        state.addEvent(event2);

        PluginRuntimeSnapshot snapshot = state.snapshot();
        assertThat(snapshot.events()).hasSize(2);
        assertThat(snapshot.events()).containsExactly(event1, event2);
    }

    /**
     * 测试清除运行时状态。
     */
    @Test
    void clearRuntimeShouldResetHandlesBootAndContext() {
        RegistrationHandle handle = Mockito.mock(RegistrationHandle.class);
        LoadedPluginBoot loadedBoot = Mockito.mock(LoadedPluginBoot.class);
        PluginContext context = Mockito.mock(PluginContext.class);

        state.addHandle(handle);
        state.setLoadedBoot(loadedBoot);
        state.setContext(context);

        state.clearRuntime();

        assertThat(state.handles()).isEmpty();
        assertThat(state.loadedBoot()).isNull();
        assertThat(state.context()).isNull();
    }

    /**
     * 测试创建状态快照。
     */
    @Test
    void snapshotShouldCreateImmutableSnapshot() {
        state.setStatus(PluginStatus.INSTALLED);
        state.setInstalledAt(Instant.now());

        PluginRuntimeSnapshot snapshot = state.snapshot();

        assertThat(snapshot.pluginId()).isEqualTo("test-plugin");
        assertThat(snapshot.descriptor()).isEqualTo(descriptor);
        assertThat(snapshot.status()).isEqualTo(PluginStatus.INSTALLED);
        assertThat(snapshot.registrationCount()).isEqualTo(0);
    }

    /**
     * 测试快照包含事件副本。
     */
    @Test
    void snapshotShouldIncludeEventsCopy() {
        PluginLifecycleEvent event = new PluginLifecycleEvent(
                "test-plugin", PluginLifecyclePhase.INSTALLED, Instant.now(), null);
        state.addEvent(event);

        PluginRuntimeSnapshot snapshot = state.snapshot();

        assertThat(snapshot.events()).hasSize(1);
        assertThat(snapshot.events().get(0).phase()).isEqualTo(PluginLifecyclePhase.INSTALLED);
    }

    /**
     * 测试状态转换序列。
     */
    @Test
    void stateTransitionsShouldWorkCorrectly() {
        // 初始状态
        assertThat(state.status()).isEqualTo(PluginStatus.DISCOVERED);

        // 安装
        state.setStatus(PluginStatus.INSTALLED);
        assertThat(state.status()).isEqualTo(PluginStatus.INSTALLED);

        // 启用
        state.setStatus(PluginStatus.ENABLED);
        assertThat(state.status()).isEqualTo(PluginStatus.ENABLED);

        // 禁用
        state.setStatus(PluginStatus.DISABLED);
        assertThat(state.status()).isEqualTo(PluginStatus.DISABLED);

        // 卸载
        state.setStatus(PluginStatus.UNINSTALLED);
        assertThat(state.status()).isEqualTo(PluginStatus.UNINSTALLED);
    }

    /**
     * 测试替换描述符后 source 变为 JAR。
     */
    @Test
    void replaceDescriptorWithJarPathShouldSetSourceToJAR() {
        PluginDescriptor newDescriptor = createDescriptor("test-plugin", "Updated Plugin");
        Path jarPath = Path.of("/path/to/plugin.jar");

        state.replaceDescriptor(newDescriptor, jarPath);

        PluginRuntimeSnapshot snapshot = state.snapshot();
        assertThat(snapshot.source()).isEqualTo(PluginSource.JAR);
        assertThat(snapshot.artifactPath()).isEqualTo(jarPath);
    }

    /**
     * 测试替换描述符后 source 保持 CLASSPATH。
     */
    @Test
    void replaceDescriptorWithNullPathShouldKeepSourceAsClasspath() {
        PluginDescriptor newDescriptor = createDescriptor("test-plugin", "Updated Plugin");

        state.replaceDescriptor(newDescriptor, null);

        PluginRuntimeSnapshot snapshot = state.snapshot();
        assertThat(snapshot.source()).isEqualTo(PluginSource.CLASSPATH);
        assertThat(snapshot.artifactPath()).isNull();
    }

    private PluginDescriptor createDescriptor(String id, String name) {
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
                null,
                List.of(),
                null);
    }
}