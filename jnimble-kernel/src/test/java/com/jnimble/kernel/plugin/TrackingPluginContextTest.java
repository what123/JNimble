package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TrackingPluginContext 单元测试。
 *
 * <p>验证注册句柄追踪、资源注册代理及 bean 解析功能。</p>
 */
class TrackingPluginContextTest {

    private PluginDescriptor descriptor;
    private HookRegistry hookRegistry;
    private RouteRegistry routeRegistry;
    private AssetRegistry assetRegistry;
    private PluginBeanResolver beanResolver;
    private List<RegistrationHandle> trackedHandles;
    private TrackingPluginContext context;

    @BeforeEach
    void setUp() {
        descriptor = new PluginDescriptor(
                "1", "test-plugin", "Test Plugin", null,
                "A test plugin", null, "1.0.0", ">=1.0.0",
                "Tester", null, "com.example.TestBoot",
                null, List.of(), null);
        hookRegistry = mock(HookRegistry.class);
        routeRegistry = mock(RouteRegistry.class);
        assetRegistry = mock(AssetRegistry.class);
        beanResolver = mock(PluginBeanResolver.class);
        trackedHandles = new ArrayList<>();
        context = new TrackingPluginContext(
                descriptor, hookRegistry, routeRegistry, assetRegistry,
                beanResolver, trackedHandles::add);
    }

    /**
     * 测试 descriptor() 返回构造时传入的插件描述符。
     */
    @Test
    void descriptorReturnsProvidedDescriptor() {
        assertThat(context.descriptor()).isEqualTo(descriptor);
        assertThat(context.descriptor().id()).isEqualTo("test-plugin");
    }

    /**
     * 测试 registerHandle() 将外部句柄加入追踪列表并原样返回。
     */
    @Test
    void registerHandleTracksAndReturnsHandle() {
        RegistrationHandle handle = mock(RegistrationHandle.class);

        RegistrationHandle result = context.registerHandle(handle);

        assertThat(result).isSameAs(handle);
        assertThat(trackedHandles).containsExactly(handle);
    }

    /**
     * 测试 hooks() 注册贡献时自动追踪返回的句柄。
     */
    @Test
    void hooksRegistryTracksRegistrationHandles() {
        RegistrationHandle expectedHandle = mock(RegistrationHandle.class);
        HookViewContribution contribution = new HookViewContribution("view.html", null, 10, null, null);
        when(hookRegistry.register("admin.sidebar", contribution)).thenReturn(expectedHandle);

        RegistrationHandle result = context.hooks().register("admin.sidebar", contribution);

        assertThat(result).isSameAs(expectedHandle);
        assertThat(trackedHandles).containsExactly(expectedHandle);
    }

    /**
     * 测试 hooks() 带 HookMode 的注册方法也自动追踪句柄。
     */
    @Test
    void hooksRegistryTracksHandlesWithMode() {
        RegistrationHandle expectedHandle = mock(RegistrationHandle.class);
        HookViewContribution contribution = new HookViewContribution("view.html", Map.of("key", "val"), 5, null, null);
        when(hookRegistry.register("admin.toolbar", HookMode.PREPEND, contribution)).thenReturn(expectedHandle);

        RegistrationHandle result = context.hooks().register("admin.toolbar", HookMode.PREPEND, contribution);

        assertThat(result).isSameAs(expectedHandle);
        assertThat(trackedHandles).containsExactly(expectedHandle);
    }

    /**
     * 测试 routes() 注册路由时自动追踪句柄。
     */
    @Test
    void routesRegistryTracksRegistrationHandles() {
        RegistrationHandle expectedHandle = mock(RegistrationHandle.class);
        RouteDefinition route = new RouteDefinition("/list", "plugin/views/list.html");
        when(routeRegistry.register(route)).thenReturn(expectedHandle);

        RegistrationHandle result = context.routes().register(route);

        assertThat(result).isSameAs(expectedHandle);
        assertThat(trackedHandles).containsExactly(expectedHandle);
    }

    /**
     * 测试 assets() 注册资源时自动追踪句柄。
     */
    @Test
    void assetsRegistryTracksRegistrationHandles() {
        RegistrationHandle expectedHandle = mock(RegistrationHandle.class);
        AssetDefinition asset = new AssetDefinition("/logo.png", "classpath:logo.png");
        when(assetRegistry.register(asset)).thenReturn(expectedHandle);

        RegistrationHandle result = context.assets().register(asset);

        assertThat(result).isSameAs(expectedHandle);
        assertThat(trackedHandles).containsExactly(expectedHandle);
    }

    /**
     * 测试 bean() 委托给 PluginBeanResolver 解析。
     */
    @Test
    void beanDelegatesToBeanResolver() {
        String expectedBean = "resolved-bean";
        when(beanResolver.resolve(String.class)).thenReturn(expectedBean);

        String result = context.bean(String.class);

        assertThat(result).isSameAs(expectedBean);
        verify(beanResolver).resolve(String.class);
    }

    /**
     * 测试注册表返回 null 句柄时抛出 PluginRuntimeException。
     */
    @Test
    void throwsWhenRegistryReturnsNullHandle() {
        HookViewContribution contribution = new HookViewContribution("view.html", null, 10, null, null);
        when(hookRegistry.register("admin.sidebar", contribution)).thenReturn(null);

        assertThatThrownBy(() -> context.hooks().register("admin.sidebar", contribution))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("null RegistrationHandle")
                .hasMessageContaining("test-plugin");
    }

    /**
     * 测试多个注册句柄按顺序被追踪。
     */
    @Test
    void tracksMultipleHandlesInOrder() {
        RegistrationHandle handle1 = mock(RegistrationHandle.class);
        RegistrationHandle handle2 = mock(RegistrationHandle.class);
        RegistrationHandle handle3 = mock(RegistrationHandle.class);

        HookViewContribution contribution = new HookViewContribution("view.html", null, 10, null, null);
        when(hookRegistry.register("hook1", contribution)).thenReturn(handle1);
        when(hookRegistry.register("hook2", contribution)).thenReturn(handle2);
        when(hookRegistry.register("hook3", contribution)).thenReturn(handle3);

        context.hooks().register("hook1", contribution);
        context.hooks().register("hook2", contribution);
        context.registerHandle(handle3);

        assertThat(trackedHandles).containsExactly(handle1, handle2, handle3);
    }
}
