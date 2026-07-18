package com.jnimble.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.admin.plugin.PluginJarInstallResult;
import com.jnimble.admin.plugin.PluginJarInstallService;
import com.jnimble.admin.plugin.PluginManagementController;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.license.core.PluginLicenseService;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * PluginManagementController 单元测试。
 *
 * <p>使用 MockMvc 独立模式测试插件管理端点的 CRUD 操作、
 * 权限检查和异常处理行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class PluginManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    @Mock
    private PluginJarInstallService pluginJarInstallService;

    @Mock
    private PluginRouteRegistry pluginRouteRegistry;

    @Mock
    private PluginLicenseService pluginLicenseService;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        PluginManagementController controller = new PluginManagementController(
                pluginRuntimeService, pluginJarInstallService, pluginRouteRegistry,
                pluginLicenseService, auditRecorder, authorization, messageSource);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 测试获取插件列表页面，验证返回正确视图和 Model 属性。
     */
    @Test
    void listReturnsPluginListView() throws Exception {
        PluginRuntimeSnapshot plugin = mock(PluginRuntimeSnapshot.class);
        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        when(plugin.descriptor()).thenReturn(descriptor);
        when(plugin.status()).thenReturn(PluginStatus.INSTALLED);
        when(pluginRuntimeService.list()).thenReturn(List.of(plugin));

        mockMvc.perform(get("/admin/plugins"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/plugin/list"))
                .andExpect(model().attribute("activeNav", "plugins"))
                .andExpect(model().attributeExists("plugins"))
                .andExpect(model().attributeExists("pluginAdminEntries"))
                .andExpect(model().attributeExists("pluginMessages"));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试获取插件详情页面，验证返回正确视图和插件信息。
     */
    @Test
    void detailReturnsPluginDetailView() throws Exception {
        PluginRuntimeSnapshot plugin = mock(PluginRuntimeSnapshot.class);
        when(pluginRuntimeService.find("test-plugin")).thenReturn(Optional.of(plugin));
        when(pluginRuntimeService.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/plugins/{pluginId}", "test-plugin"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/plugin/detail"))
                .andExpect(model().attribute("selectedPluginId", "test-plugin"))
                .andExpect(model().attribute("plugin", plugin));

        verify(authorization).requirePermission(any());
    }

    /**
     * 测试安装插件成功场景，验证重定向和审计记录。
     */
    @Test
    void installSuccessRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/install")
                        .param("pluginId", "new-plugin")
                        .param("bootClass", "com.example.Boot")
                        .param("name", "New Plugin")
                        .param("version", "1.0.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin installed."));

        verify(authorization).requirePermission(any());
        verify(pluginRuntimeService).install(any(PluginDescriptor.class));
        verify(auditRecorder).success(any(), any(), any(), any());
    }

    /**
     * 测试安装插件缺少必要参数时返回错误信息。
     */
    @Test
    void installWithMissingPluginIdRedirectsWithError() throws Exception {
        mockMvc.perform(post("/admin/plugins/install")
                        .param("bootClass", "com.example.Boot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("error", "Plugin id is required."));

        verify(auditRecorder).failure(any(), any(), any(), any());
    }

    /**
     * 测试启用插件成功场景。
     */
    @Test
    void enableSuccessRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/{pluginId}/enable", "test-plugin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin enabled."));

        verify(pluginRuntimeService).enable("test-plugin");
        verify(auditRecorder).success(any(), any(), eq("test-plugin"), any());
    }

    /**
     * 测试禁用插件成功场景。
     */
    @Test
    void disableSuccessRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/{pluginId}/disable", "test-plugin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin disabled."));

        verify(pluginRuntimeService).disable("test-plugin");
        verify(auditRecorder).success(any(), any(), eq("test-plugin"), any());
    }

    /**
     * 测试卸载插件成功场景。
     */
    @Test
    void uninstallSuccessRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/{pluginId}/uninstall", "test-plugin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin uninstalled."));

        verify(pluginRuntimeService).uninstall("test-plugin", false);
        verify(auditRecorder).success(any(), any(), eq("test-plugin"), any());
    }

    /**
     * 测试卸载插件并清理插件数据。
     */
    @Test
    void uninstallWithCleanDataRedirectsWithCleanupMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/{pluginId}/uninstall", "test-plugin")
                        .param("cleanPluginData", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin uninstalled. Plugin data cleanup completed."));

        verify(pluginRuntimeService).uninstall("test-plugin", true);
        verify(auditRecorder).success(any(), any(), eq("test-plugin"), any());
    }

    /**
     * 测试上传插件 JAR 包成功场景。
     */
    @Test
    void uploadJarSuccessRedirectsWithMessage() throws Exception {
        PluginJarInstallResult result = mock(PluginJarInstallResult.class);
        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        when(descriptor.id()).thenReturn("uploaded-plugin");
        when(result.descriptor()).thenReturn(descriptor);
        when(pluginJarInstallService.install(any())).thenReturn(result);

        MockMultipartFile jarFile = new MockMultipartFile(
                "jar", "plugin.jar", "application/java-archive", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/admin/plugins/upload").file(jarFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin jar installed."));

        verify(pluginJarInstallService).install(argThat(file ->
                file != null && "plugin.jar".equals(file.getOriginalFilename())));
        verify(auditRecorder).success(any(), any(), any(), any());
    }

    /**
     * 测试上传插件 JAR 包失败时将安装错误反馈到插件管理页面。
     */
    @Test
    void uploadJarFailureRedirectsWithError() throws Exception {
        when(pluginJarInstallService.install(any()))
                .thenThrow(new IllegalArgumentException("Only .jar plugin packages are supported."));
        MockMultipartFile jarFile = new MockMultipartFile(
                "jar", "plugin.zip", "application/zip", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/admin/plugins/upload").file(jarFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("error", "Only .jar plugin packages are supported."));

        verify(auditRecorder).failure(any(), any(), any(), any());
    }

    /**
     * 测试重新加载插件成功场景。
     */
    @Test
    void reloadSuccessRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/admin/plugins/{pluginId}/reload", "test-plugin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/plugins"))
                .andExpect(flash().attribute("message", "Plugin reloaded."));

        verify(pluginRuntimeService).reload("test-plugin");
        verify(auditRecorder).success(any(), any(), eq("test-plugin"), any());
    }
}
