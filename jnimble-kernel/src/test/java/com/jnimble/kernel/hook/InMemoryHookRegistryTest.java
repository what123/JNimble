package com.jnimble.kernel.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import org.junit.jupiter.api.Test;

class InMemoryHookRegistryTest {

    private final InMemoryHookRegistry registry = new InMemoryHookRegistry();

    @Test
    void ordersAppendAndPrependContributionsByOrderThenPluginId() {
        registry.register("admin.layout.sidebar", "zeta", HookMode.APPEND,
                contribution("plugin/zeta/sidebar", 20));
        registry.register("admin.layout.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 20));
        registry.register("admin.layout.sidebar", "crm", HookMode.PREPEND,
                contribution("plugin/crm/sidebar", 10));

        InMemoryHookRegistry.HookResolution resolution = registry.resolve("admin.layout.sidebar");

        assertThat(resolution.prepends())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("crm");
        assertThat(resolution.appends())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("alpha", "zeta");
    }

    @Test
    void firstOverrideWinsAndSuppressesLaterOverrides() {
        registry.register("admin.layout.sidebar", "alpha", HookMode.REPLACE,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.layout.sidebar", "zeta", HookMode.REMOVE,
                contribution("plugin/zeta/sidebar", 20));
        registry.register("admin.layout.sidebar", "crm", HookMode.APPEND,
                contribution("plugin/crm/sidebar", 1));

        InMemoryHookRegistry.HookResolution resolution = registry.resolve("admin.layout.sidebar");

        assertThat(resolution.replacement()).get()
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .isEqualTo("alpha");
        assertThat(resolution.removal()).isEmpty();
        assertThat(resolution.suppressedOverrides())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("zeta");
    }

    @Test
    void rejectsInvalidPluginIds() {
        assertThatThrownBy(() -> registry.register("admin.layout.sidebar", "../crm", HookMode.APPEND,
                contribution("plugin/crm/sidebar", 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");

        assertThatThrownBy(() -> registry.register("admin.layout.sidebar", "Crm", HookMode.APPEND,
                contribution("plugin/crm/sidebar", 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");
    }

    /**
     * 测试 hookName 为 null 时抛出异常。
     */
    @Test
    void rejectsNullHookName() {
        assertThatThrownBy(() -> registry.register(null, "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hookName must not be blank");
    }

    /**
     * 测试 hookName 为空白字符串时抛出异常。
     */
    @Test
    void rejectsBlankHookName() {
        assertThatThrownBy(() -> registry.register("  ", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hookName must not be blank");
    }

    /**
     * 测试 contribution 为 null 时抛出异常。
     */
    @Test
    void rejectsNullContribution() {
        assertThatThrownBy(() -> registry.register("admin.layout.sidebar", "alpha", HookMode.APPEND,
                null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 测试 handle.unregister() 后注册被移除，resolve 不再返回该贡献。
     */
    @Test
    void handleUnregisterRemovesRegistration() {
        RegistrationHandle handle = registry.register("admin.layout.sidebar", "alpha", HookMode.APPEND,
                contribution("plugin/alpha/sidebar", 10));
        registry.register("admin.layout.sidebar", "zeta", HookMode.APPEND,
                contribution("plugin/zeta/sidebar", 20));

        handle.unregister();

        InMemoryHookRegistry.HookResolution resolution = registry.resolve("admin.layout.sidebar");
        assertThat(resolution.appends())
                .extracting(InMemoryHookRegistry.HookRegistration::pluginId)
                .containsExactly("zeta");
    }

    /**
     * 测试 contributions() 返回按 order、pluginId、sequence 排序的结果。
     */
    @Test
    void contributionsAreSortedByOrderThenPluginIdThenSequence() {
        registry.register("hook.b", "bravo", HookMode.APPEND, contribution("view/b", 20));
        registry.register("hook.a", "alpha", HookMode.APPEND, contribution("view/a", 10));
        registry.register("hook.b", "alpha", HookMode.APPEND, contribution("view/b2", 20));

        java.util.List<HookViewContribution> result = registry.contributions();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).view()).isEqualTo("view/a");
        assertThat(result.get(1).view()).isEqualTo("view/b2");
        assertThat(result.get(2).view()).isEqualTo("view/b");
    }

    /**
     * 测试 mode 为 null 时默认使用 APPEND。
     */
    @Test
    void nullModeDefaultsToAppend() {
        registry.register("admin.layout.sidebar", "alpha", null,
                contribution("plugin/alpha/sidebar", 10));

        InMemoryHookRegistry.HookResolution resolution = registry.resolve("admin.layout.sidebar");
        assertThat(resolution.appends()).hasSize(1);
        assertThat(resolution.prepends()).isEmpty();
    }

    private HookViewContribution contribution(String view, int order) {
        return new HookViewContribution(view, null, order, null, null);
    }
}
