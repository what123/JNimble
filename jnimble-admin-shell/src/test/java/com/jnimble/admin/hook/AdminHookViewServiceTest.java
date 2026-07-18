package com.jnimble.admin.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.jnimble.kernel.hook.HookManager;
import com.jnimble.kernel.hook.InMemoryHookRegistry.HookRegistration;
import com.jnimble.kernel.hook.InMemoryHookRegistry.HookResolution;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookViewContribution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;

/**
 * AdminHookViewService 单元测试。
 *
 * <p>验证 Hook 视图解析服务的核心逻辑，包括 hook 解析、权限过滤、
 * 激活条件判断以及渲染失败处理。</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminHookViewServiceTest {

    @Mock
    private HookManager hookManager;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    private HookProperties properties;

    private AdminHookViewService hookViewService;

    @BeforeEach
    void setUp() {
        properties = new HookProperties();
        hookViewService = new AdminHookViewService(
                hookManager, authorization, templateEngine, properties, pluginRuntimeService);
    }

    /**
     * 测试 resolve() 在 hook 存在 REMOVE 标记时返回空列表。
     */
    @Test
    void resolveReturnsEmptyWhenRemovalPresent() {
        HookRegistration removal = buildRegistration("alpha", HookMode.REMOVE, "plugin/alpha/view", 10);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(), Optional.empty(), Optional.of(removal), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.sidebar");

        assertThat(result).isEmpty();
    }

    /**
     * 测试 resolve() 在 hook 存在 REPLACE 替代时只返回替代视图。
     */
    @Test
    void resolveReturnsReplacementWhenPresent() {
        HookRegistration replacement = buildRegistration("alpha", HookMode.REPLACE, "plugin/alpha/replace", 5);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(), Optional.of(replacement), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.sidebar");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).pluginId()).isEqualTo("alpha");
    }

    /**
     * 测试 resolve() 在 hook 无注册时返回空列表。
     */
    @Test
    void resolveReturnsEmptyForNoRegistrations() {
        HookResolution resolution = new HookResolution(
                "admin.empty", List.of(), List.of(), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.empty")).thenReturn(resolution);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.empty");

        assertThat(result).isEmpty();
    }

    /**
     * 测试 resolve() 按 prepends + appends 顺序返回视图。
     */
    @Test
    void resolveReturnsAppendsInResolutionOrder() {
        HookRegistration reg1 = buildRegistration("alpha", HookMode.APPEND, "plugin/alpha/view", 10);
        HookRegistration reg2 = buildRegistration("beta", HookMode.APPEND, "plugin/beta/view", 20);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(reg1, reg2), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.sidebar");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).pluginId()).isEqualTo("alpha");
        assertThat(result.get(1).pluginId()).isEqualTo("beta");
    }

    /**
     * 测试 resolve() 过滤无权限的 hook 视图（有权限的通过，无权限的被过滤）。
     */
    @Test
    void resolveFiltersOutViewsWithoutPermission() {
        HookRegistration permitted = buildRegistrationWithPermission(
                "alpha", HookMode.APPEND, "plugin/alpha/view", 10, "admin.read");
        HookRegistration noPermission = buildRegistrationWithPermission(
                "beta", HookMode.APPEND, "plugin/beta/view", 20, "admin.write");
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(permitted, noPermission),
                Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);
        when(authorization.hasPermission("admin.read")).thenReturn(true);
        when(authorization.hasPermission("admin.write")).thenReturn(false);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.sidebar");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).pluginId()).isEqualTo("alpha");
    }

    /**
     * 测试 resolve() 中权限为 null 的视图不触发权限检查直接通过。
     */
    @Test
    void resolveAllowsViewWithNullPermission() {
        HookRegistration reg = buildRegistration("alpha", HookMode.APPEND, "plugin/alpha/view", 10);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(reg), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);

        List<AdminHookViewService.AdminHookView> result = hookViewService.resolve("admin.sidebar");

        assertThat(result).hasSize(1);
        verify(authorization, never()).hasPermission(anyString());
    }

    /**
     * 测试 render() 方法正常渲染 hook 视图并返回 HTML 内容。
     */
    @Test
    void renderReturnsHtmlForResolvedViews() {
        HookRegistration reg = buildRegistration("alpha", HookMode.APPEND, "plugin/alpha/view", 10);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(reg), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);
        when(templateEngine.process(any(TemplateSpec.class), any()))
                .thenReturn("<div>hook content</div>");

        Context context = new Context();
        List<AdminHookViewService.RenderedAdminHookView> result =
                hookViewService.render("admin.sidebar", context);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).html()).isEqualTo("<div>hook content</div>");
        assertThat(result.get(0).pluginId()).isEqualTo("alpha");
    }

    /**
     * 测试 render() 在模板渲染失败时优雅降级并记录错误。
     */
    @Test
    void renderHandlesTemplateRenderingFailure() {
        HookRegistration reg = buildRegistration("alpha", HookMode.APPEND, "plugin/alpha/view", 10);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(reg), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);
        when(templateEngine.process(any(TemplateSpec.class), any()))
                .thenThrow(new RuntimeException("Template error"));

        properties.setFailFast(false);
        Context context = new Context();

        List<AdminHookViewService.RenderedAdminHookView> result =
                hookViewService.render("admin.sidebar", context);

        assertThat(result).isEmpty();
        verify(pluginRuntimeService).recordRuntimeError(eq("alpha"), anyString());
    }

    /**
     * 测试 render() 在 failFast 开启时抛出异常。
     */
    @Test
    void renderThrowsWhenFailFastEnabled() {
        HookRegistration reg = buildRegistration("alpha", HookMode.APPEND, "plugin/alpha/view", 10);
        HookResolution resolution = new HookResolution(
                "admin.sidebar", List.of(), List.of(reg), Optional.empty(), Optional.empty(), List.of());

        when(hookManager.resolve("admin.sidebar")).thenReturn(resolution);
        when(templateEngine.process(any(TemplateSpec.class), any()))
                .thenThrow(new RuntimeException("Template error"));

        properties.setFailFast(true);
        Context context = new Context();

        assertThrows(IllegalStateException.class,
                () -> hookViewService.render("admin.sidebar", context));
    }

    private HookRegistration buildRegistration(String pluginId, HookMode mode, String view, int order) {
        return new HookRegistration(
                "reg-" + pluginId, "admin.sidebar", pluginId, mode,
                new HookViewContribution(view, Map.of(), order, null, null), order);
    }

    private HookRegistration buildRegistrationWithPermission(
            String pluginId, HookMode mode, String view, int order, String permission) {
        return new HookRegistration(
                "reg-" + pluginId, "admin.sidebar", pluginId, mode,
                new HookViewContribution(view, Map.of(), order, permission, null), order);
    }
}
