package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteRegistry;
import org.junit.jupiter.api.Test;

/**
 * NoopPluginRegistries 单元测试。
 *
 * <p>验证空操作插件注册表的正确行为，包括 Hook、路由和资源注册表。</p>
 */
class NoopPluginRegistriesTest {

    /**
     * 测试获取空操作 Hook 注册表不为 null。
     */
    @Test
    void hooksReturnsNonNullRegistry() {
        HookRegistry registry = NoopPluginRegistries.hooks();

        assertThat(registry).isNotNull();
    }

    /**
     * 测试空操作 Hook 注册表的注册方法返回有效的处理句柄。
     */
    @Test
    void hooksRegisterReturnsValidHandle() {
        HookRegistry registry = NoopPluginRegistries.hooks();
        HookViewContribution contribution = new HookViewContribution("test-hook", null, 0, null, null);

        RegistrationHandle handle = registry.register("test-hook", contribution);

        assertThat(handle).isNotNull();
    }

    /**
     * 测试空操作 Hook 注册表的带模式注册方法返回有效的处理句柄。
     */
    @Test
    void hooksRegisterWithModeReturnsValidHandle() {
        HookRegistry registry = NoopPluginRegistries.hooks();
        HookViewContribution contribution = new HookViewContribution("test-hook", null, 0, null, null);

        RegistrationHandle handle = registry.register("test-hook", HookMode.APPEND, contribution);

        assertThat(handle).isNotNull();
    }

    /**
     * 测试空操作 Hook 注册表的处理句柄可以安全调用 unregister 方法。
     */
    @Test
    void hooksHandleUnregisterDoesNotThrow() {
        HookRegistry registry = NoopPluginRegistries.hooks();
        HookViewContribution contribution = new HookViewContribution("test-hook", null, 0, null, null);

        RegistrationHandle handle = registry.register("test-hook", contribution);

        assertThatCode(handle::unregister).doesNotThrowAnyException();
    }

    /**
     * 测试获取空操作路由注册表不为 null。
     */
    @Test
    void routesReturnsNonNullRegistry() {
        RouteRegistry registry = NoopPluginRegistries.routes();

        assertThat(registry).isNotNull();
    }

    /**
     * 测试空操作路由注册表的注册方法返回有效的处理句柄。
     */
    @Test
    void routesRegisterReturnsValidHandle() {
        RouteRegistry registry = NoopPluginRegistries.routes();
        RouteDefinition route = mock(RouteDefinition.class);

        RegistrationHandle handle = registry.register(route);

        assertThat(handle).isNotNull();
    }

    /**
     * 测试空操作路由注册表的处理句柄可以安全调用 unregister 方法。
     */
    @Test
    void routesHandleUnregisterDoesNotThrow() {
        RouteRegistry registry = NoopPluginRegistries.routes();
        RouteDefinition route = mock(RouteDefinition.class);

        RegistrationHandle handle = registry.register(route);

        assertThatCode(handle::unregister).doesNotThrowAnyException();
    }

    /**
     * 测试获取空操作资源注册表不为 null。
     */
    @Test
    void assetsReturnsNonNullRegistry() {
        AssetRegistry registry = NoopPluginRegistries.assets();

        assertThat(registry).isNotNull();
    }

    /**
     * 测试空操作资源注册表的注册方法返回有效的处理句柄。
     */
    @Test
    void assetsRegisterReturnsValidHandle() {
        AssetRegistry registry = NoopPluginRegistries.assets();
        AssetDefinition asset = mock(AssetDefinition.class);

        RegistrationHandle handle = registry.register(asset);

        assertThat(handle).isNotNull();
    }

    /**
     * 测试空操作资源注册表的处理句柄可以安全调用 unregister 方法。
     */
    @Test
    void assetsHandleUnregisterDoesNotThrow() {
        AssetRegistry registry = NoopPluginRegistries.assets();
        AssetDefinition asset = mock(AssetDefinition.class);

        RegistrationHandle handle = registry.register(asset);

        assertThatCode(handle::unregister).doesNotThrowAnyException();
    }
}
