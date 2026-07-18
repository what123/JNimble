package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.kernel.hook.InMemoryHookRegistry;
import com.jnimble.kernel.resource.PluginAssetRegistry;
import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PluginContributionRegistries 单元测试。
 *
 * <p>验证插件贡献注册表的插件作用域管理、启用和禁用功能。</p>
 */
class PluginContributionRegistriesTest {

    private InMemoryHookRegistry hookRegistry;
    private PluginRouteRegistry routeRegistry;
    private PluginAssetRegistry assetRegistry;
    private PluginContributionRegistries contributionRegistries;

    @BeforeEach
    void setUp() {
        hookRegistry = mock(InMemoryHookRegistry.class);
        routeRegistry = mock(PluginRouteRegistry.class);
        assetRegistry = mock(PluginAssetRegistry.class);
        contributionRegistries = new PluginContributionRegistries(hookRegistry, routeRegistry, assetRegistry);
    }

    /**
     * 测试获取插件作用域的 Hook 注册表。
     */
    @Test
    void hooksReturnsScopedHookRegistry() {
        HookRegistry hooks = contributionRegistries.hooks("test-plugin");

        assertThat(hooks).isNotNull();

        RegistrationHandle handle = mock(RegistrationHandle.class);
        when(hookRegistry.register("test-hook", "test-plugin", HookMode.APPEND, 
                new HookViewContribution("view", null, 0, null, null)))
                .thenReturn(handle);

        HookViewContribution contribution = new HookViewContribution("view", null, 0, null, null);
        RegistrationHandle result = hooks.register("test-hook", contribution);

        verify(hookRegistry).register("test-hook", "test-plugin", HookMode.APPEND, contribution);
        assertThat(result).isEqualTo(handle);
    }

    /**
     * 测试获取插件作用域的 Hook 注册表并指定 HookMode。
     */
    @Test
    void hooksReturnsScopedHookRegistryWithMode() {
        HookRegistry hooks = contributionRegistries.hooks("test-plugin");

        RegistrationHandle handle = mock(RegistrationHandle.class);
        when(hookRegistry.register("test-hook", "test-plugin", HookMode.REPLACE, 
                new HookViewContribution("view", null, 0, null, null)))
                .thenReturn(handle);

        HookViewContribution contribution = new HookViewContribution("view", null, 0, null, null);
        RegistrationHandle result = hooks.register("test-hook", HookMode.REPLACE, contribution);

        verify(hookRegistry).register("test-hook", "test-plugin", HookMode.REPLACE, contribution);
        assertThat(result).isEqualTo(handle);
    }

    /**
     * 测试获取插件作用域的路由注册表。
     */
    @Test
    void routesReturnsScopedRouteRegistry() {
        RouteRegistry scopedRegistry = mock(RouteRegistry.class);
        when(routeRegistry.scoped("test-plugin")).thenReturn(scopedRegistry);

        RouteRegistry routes = contributionRegistries.routes("test-plugin");

        assertThat(routes).isEqualTo(scopedRegistry);
        verify(routeRegistry).scoped("test-plugin");
    }

    /**
     * 测试获取插件作用域的资源注册表。
     */
    @Test
    void assetsReturnsScopedAssetRegistry() {
        AssetRegistry scopedRegistry = mock(AssetRegistry.class);
        when(assetRegistry.scoped("test-plugin")).thenReturn(scopedRegistry);

        AssetRegistry assets = contributionRegistries.assets("test-plugin");

        assertThat(assets).isEqualTo(scopedRegistry);
        verify(assetRegistry).scoped("test-plugin");
    }

    /**
     * 测试启用插件贡献。
     */
    @Test
    void enableEnablesAllContributions() {
        contributionRegistries.enable("test-plugin");

        verify(routeRegistry).enablePlugin("test-plugin");
        verify(assetRegistry).enablePlugin("test-plugin");
    }

    /**
     * 测试禁用插件贡献。
     */
    @Test
    void disableDisablesAllContributions() {
        contributionRegistries.disable("test-plugin");

        verify(routeRegistry).disablePlugin("test-plugin");
        verify(assetRegistry).disablePlugin("test-plugin");
    }

    /**
     * 测试使用无效的插件 ID 时抛出异常。
     */
    @Test
    void rejectsInvalidPluginIds() {
        assertThatThrownBy(() -> contributionRegistries.hooks("../invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");

        assertThatThrownBy(() -> contributionRegistries.routes("Invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");

        assertThatThrownBy(() -> contributionRegistries.assets(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must not be blank");

        assertThatThrownBy(() -> contributionRegistries.enable(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must not be blank");

        assertThatThrownBy(() -> contributionRegistries.disable(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must not be blank");
    }

    /**
     * 测试构造函数中传入 null 参数时抛出异常。
     */
    @Test
    void constructorRejectsNullParameters() {
        assertThatThrownBy(() -> new PluginContributionRegistries(null, routeRegistry, assetRegistry))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PluginContributionRegistries(hookRegistry, null, assetRegistry))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PluginContributionRegistries(hookRegistry, routeRegistry, null))
                .isInstanceOf(NullPointerException.class);
    }
}