package com.jnimble.kernel.hook;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookViewContribution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HookManager 单元测试。
 *
 * <p>验证 hook 查询与解析功能，包括 list、list(hookName)、resolve 方法。</p>
 */
class HookManagerTest {

    private final InMemoryHookRegistry registry = new InMemoryHookRegistry();
    private final HookManager manager = new HookManager(registry);

    /**
     * 测试 list() 返回所有已注册的 hook 条目。
     */
    @Test
    void listReturnsAllRegistrations() {
        registry.register("admin.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.sidebar", "beta", HookMode.APPEND,
                contribution("plugin/beta/sidebar", 20));
        registry.register("admin.toolbar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/toolbar", 5));

        List<InMemoryHookRegistry.HookRegistration> all = manager.list();

        assertThat(all).hasSize(3);
    }

    /**
     * 测试 list(hookName) 按 hook 名称过滤注册条目。
     */
    @Test
    void listByHookNameFiltersRegistrations() {
        registry.register("admin.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.sidebar", "beta", HookMode.APPEND,
                contribution("plugin/beta/sidebar", 20));
        registry.register("admin.toolbar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/toolbar", 5));

        List<InMemoryHookRegistry.HookRegistration> sidebarRegistrations = manager.list("admin.sidebar");

        assertThat(sidebarRegistrations).hasSize(2);
        assertThat(sidebarRegistrations)
                .allMatch(r -> r.hookName().equals("admin.sidebar"));
    }

    /**
     * 测试 list(hookName) 在无匹配时返回空列表。
     */
    @Test
    void listByHookNameReturnsEmptyForNoMatch() {
        registry.register("admin.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10));

        List<InMemoryHookRegistry.HookRegistration> result = manager.list("nonexistent.hook");

        assertThat(result).isEmpty();
    }

    /**
     * 测试 resolve() 正确解析 APPEND 与 PREPEND 贡献的排序。
     */
    @Test
    void resolveOrdersPrependAndAppendByOrderThenPluginId() {
        registry.register("admin.sidebar", "zeta", HookMode.APPEND,
                contribution("plugin/zeta/sidebar", 20));
        registry.register("admin.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 20));
        registry.register("admin.sidebar", "crm", HookMode.PREPEND,
                contribution("plugin/crm/sidebar", 10));

        InMemoryHookRegistry.HookResolution resolution = manager.resolve("admin.sidebar");

        assertThat(resolution.prepends())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("crm");
        assertThat(resolution.appends())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("alpha", "zeta");
    }

    /**
     * 测试 resolve() 处理 REPLACE 模式：替代贡献被正确识别，同时抑制后续覆盖。
     */
    @Test
    void resolveHandlesReplaceMode() {
        registry.register("admin.sidebar", "alpha", HookMode.REPLACE,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.sidebar", "zeta", HookMode.REPLACE,
                contribution("plugin/zeta/sidebar", 20));
        registry.register("admin.sidebar", "crm", HookMode.APPEND,
                contribution("plugin/crm/sidebar", 1));

        InMemoryHookRegistry.HookResolution resolution = manager.resolve("admin.sidebar");

        assertThat(resolution.replacement()).isPresent();
        assertThat(resolution.replacement().get().pluginId()).isEqualTo("alpha");
        assertThat(resolution.suppressedOverrides()).hasSize(1);
        assertThat(resolution.suppressedOverrides().get(0).pluginId()).isEqualTo("zeta");
        assertThat(resolution.appends()).hasSize(1);
    }

    /**
     * 测试 resolve() 处理 REMOVE 模式：移除贡献被正确识别。
     */
    @Test
    void resolveHandlesRemoveMode() {
        registry.register("admin.sidebar", "alpha", HookMode.REMOVE,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.sidebar", "crm", HookMode.APPEND,
                contribution("plugin/crm/sidebar", 1));

        InMemoryHookRegistry.HookResolution resolution = manager.resolve("admin.sidebar");

        assertThat(resolution.removal()).isPresent();
        assertThat(resolution.removal().get().pluginId()).isEqualTo("alpha");
        assertThat(resolution.override()).isPresent();
    }

    /**
     * 测试 resolve() 在无注册时返回空的解析结果。
     */
    @Test
    void resolveReturnsEmptyForUnknownHook() {
        InMemoryHookRegistry.HookResolution resolution = manager.resolve("nonexistent.hook");

        assertThat(resolution.prepends()).isEmpty();
        assertThat(resolution.appends()).isEmpty();
        assertThat(resolution.replacement()).isEmpty();
        assertThat(resolution.removal()).isEmpty();
        assertThat(resolution.suppressedOverrides()).isEmpty();
    }

    private HookViewContribution contribution(String view, int order) {
        return new HookViewContribution(view, null, order, null, null);
    }
}
