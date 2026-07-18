package com.jnimble.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.plugin.PluginRuntimeException;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 插件生命周期集成测试。
 *
 * <p>测试插件的安装、启用、禁用、卸载等完整生命周期管理。</p>
 * <p>使用 H2 内存数据库进行隔离测试。</p>
 */
@SpringBootTest(
        properties = {
                "jnimble.plugins.auto-enable=false",
                "jnimble.plugins.restore-enabled=false",
                "jnimble.plugins.directory-scan-enabled=false"
        })
@ActiveProfiles("test")
class PluginLifecycleIntegrationTest {

    @Autowired
    private PluginRuntimeService pluginRuntimeService;

    /**
     * 测试插件完整生命周期：安装 -> 启用 -> 禁用 -> 卸载
     */
    @Test
    @DisplayName("插件完整生命周期测试")
    void shouldCompleteFullPluginLifecycle() {
        // 1. 创建插件描述符
        PluginDescriptor descriptor = createTestPluginDescriptor("lifecycle-test-plugin", "生命周期测试插件");

        // 2. 安装插件
        pluginRuntimeService.install(descriptor);

        // 3. 验证插件已安装
        Optional<PluginRuntimeSnapshot> snapshot = pluginRuntimeService.find("lifecycle-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);

        // 4. 启用插件
        pluginRuntimeService.enable("lifecycle-test-plugin");

        // 5. 验证插件已启用
        snapshot = pluginRuntimeService.find("lifecycle-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);

        // 6. 禁用插件
        pluginRuntimeService.disable("lifecycle-test-plugin");

        // 7. 验证插件已禁用
        snapshot = pluginRuntimeService.find("lifecycle-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.DISABLED);

        // 8. 卸载插件
        pluginRuntimeService.uninstall("lifecycle-test-plugin");

        // 9. 验证插件已卸载（状态为 UNINSTALLED）
        snapshot = pluginRuntimeService.find("lifecycle-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.UNINSTALLED);
    }

    /**
     * 测试插件重新加载：重新加载插件应先禁用再启用
     */
    @Test
    @DisplayName("插件重新加载测试")
    void shouldReloadPluginSuccessfully() {
        // 1. 安装并启用插件
        PluginDescriptor descriptor = createTestPluginDescriptor("reload-test-plugin", "重新加载测试插件");
        pluginRuntimeService.install(descriptor);
        pluginRuntimeService.enable("reload-test-plugin");

        // 2. 验证插件已启用
        Optional<PluginRuntimeSnapshot> snapshot = pluginRuntimeService.find("reload-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);
        assertThat(snapshot.get().lastStartedAt()).isNotNull();

        // 3. 重新加载插件
        pluginRuntimeService.reload("reload-test-plugin");

        // 4. 验证插件重新加载后仍为启用状态
        snapshot = pluginRuntimeService.find("reload-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);
    }

    /**
     * 测试插件列表功能：安装多个插件后，应能正确列出所有插件
     */
    @Test
    @DisplayName("插件列表功能测试")
    void shouldListAllInstalledPlugins() {
        // 1. 安装多个插件
        PluginDescriptor plugin1 = createTestPluginDescriptor("list-test-plugin-1", "列表测试插件1");
        PluginDescriptor plugin2 = createTestPluginDescriptor("list-test-plugin-2", "列表测试插件2");
        PluginDescriptor plugin3 = createTestPluginDescriptor("list-test-plugin-3", "列表测试插件3");

        pluginRuntimeService.install(plugin1);
        pluginRuntimeService.install(plugin2);
        pluginRuntimeService.install(plugin3);

        // 2. 启用部分插件
        pluginRuntimeService.enable("list-test-plugin-1");
        pluginRuntimeService.enable("list-test-plugin-3");

        // 3. 获取插件列表
        List<PluginRuntimeSnapshot> plugins = pluginRuntimeService.list();

        // 4. 验证插件数量和状态
        assertThat(plugins).hasSizeGreaterThanOrEqualTo(3);

        Optional<PluginRuntimeSnapshot> plugin1Snapshot = plugins.stream()
                .filter(p -> "list-test-plugin-1".equals(p.pluginId()))
                .findFirst();
        assertThat(plugin1Snapshot).isPresent();
        assertThat(plugin1Snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);

        Optional<PluginRuntimeSnapshot> plugin2Snapshot = plugins.stream()
                .filter(p -> "list-test-plugin-2".equals(p.pluginId()))
                .findFirst();
        assertThat(plugin2Snapshot).isPresent();
        assertThat(plugin2Snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);

        Optional<PluginRuntimeSnapshot> plugin3Snapshot = plugins.stream()
                .filter(p -> "list-test-plugin-3".equals(p.pluginId()))
                .findFirst();
        assertThat(plugin3Snapshot).isPresent();
        assertThat(plugin3Snapshot.get().status()).isEqualTo(PluginStatus.ENABLED);
    }

    /**
     * 测试重复安装插件：对已安装的插件再次安装应更新插件描述符
     */
    @Test
    @DisplayName("重复安装插件更新测试")
    void shouldUpdatePluginWhenInstallingDuplicatePlugin() {
        // 1. 安装插件
        PluginDescriptor descriptor = createTestPluginDescriptor("duplicate-test-plugin", "重复安装测试插件");
        pluginRuntimeService.install(descriptor);

        // 2. 验证插件已安装
        Optional<PluginRuntimeSnapshot> snapshot = pluginRuntimeService.find("duplicate-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().status()).isEqualTo(PluginStatus.INSTALLED);
        assertThat(snapshot.get().descriptor().name()).isEqualTo("重复安装测试插件");

        // 3. 使用新描述符重复安装插件
        PluginDescriptor updatedDescriptor = createTestPluginDescriptor("duplicate-test-plugin", "更新后的插件名称");
        pluginRuntimeService.install(updatedDescriptor);

        // 4. 验证插件描述符已更新
        snapshot = pluginRuntimeService.find("duplicate-test-plugin");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().descriptor().name()).isEqualTo("更新后的插件名称");
    }

    /**
     * 创建测试用插件描述符
     *
     * @param id   插件ID
     * @param name 插件名称
     * @return 插件描述符
     */
    private PluginDescriptor createTestPluginDescriptor(String id, String name) {
        return new PluginDescriptor(
                "1.0",
                id,
                name,
                null,
                "测试插件描述",
                null,
                "1.0.0",
                "0.1.0",
                "Test Author",
                null,
                TestPluginBoot.class.getName(),
                null,
                List.of(),
                null
        );
    }
}
