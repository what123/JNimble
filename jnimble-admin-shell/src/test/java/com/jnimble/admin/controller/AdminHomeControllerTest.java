package com.jnimble.admin.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.audit.AuditRecord;
import com.jnimble.platform.audit.AuditService;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.sdk.plugin.PluginStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * AdminHomeController 单元测试。
 *
 * <p>使用 MockMvc 独立模式测试管理后台首页和登录页面的端点行为，
 * 包括视图解析、Model 属性填充等。</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminHomeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        AdminHomeController controller = new AdminHomeController(
                pluginRuntimeService, roleService, userAccountService, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 测试访问根路径 "/" 时返回首页视图，并验证 Model 属性正确填充。
     */
    @Test
    void homeReturnsHomeViewWithModelAttributes() throws Exception {
        PluginRuntimeSnapshot enabledPlugin = mock(PluginRuntimeSnapshot.class);
        when(enabledPlugin.status()).thenReturn(PluginStatus.ENABLED);
        PluginRuntimeSnapshot disabledPlugin = mock(PluginRuntimeSnapshot.class);
        when(disabledPlugin.status()).thenReturn(PluginStatus.DISABLED);
        when(pluginRuntimeService.list()).thenReturn(List.of(enabledPlugin, disabledPlugin));

        UserRecord user1 = new UserRecord("id1", "user1", "hash", "User One",
                com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now());
        UserRecord user2 = new UserRecord("id2", "user2", "hash", "User Two",
                com.jnimble.platform.auth.UserStatus.ACTIVE, Instant.now(), Instant.now());
        when(userAccountService.listUsers()).thenReturn(List.of(user1, user2));

        RoleRecord role = new RoleRecord("r1", "admin", "Administrator",
                com.jnimble.platform.permission.RoleStatus.ACTIVE, Instant.now(), Instant.now());
        when(roleService.listRoles()).thenReturn(List.of(role));

        AuditRecord audit = AuditRecord.create("system", "test.action", "test", "1",
                com.jnimble.platform.audit.AuditOutcome.SUCCESS, "test message");
        when(auditService.listRecent(5)).thenReturn(List.of(audit));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/home"))
                .andExpect(model().attribute("activeNav", "home"))
                .andExpect(model().attribute("pluginTotal", 2L))
                .andExpect(model().attribute("pluginEnabled", 1L))
                .andExpect(model().attribute("roleTotal", 1))
                .andExpect(model().attribute("userTotal", 2));
    }

    /**
     * 测试访问 "/admin" 路径时同样返回首页视图（与 "/" 等效）。
     */
    @Test
    void homeAliasReturnsHomeView() throws Exception {
        when(pluginRuntimeService.list()).thenReturn(List.of());
        when(roleService.listRoles()).thenReturn(List.of());
        when(userAccountService.listUsers()).thenReturn(List.of());
        when(auditService.listRecent(5)).thenReturn(List.of());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/home"))
                .andExpect(model().attribute("pluginTotal", 0L))
                .andExpect(model().attribute("pluginEnabled", 0L));
    }

    /**
     * 测试访问 "/login" 路径返回登录页面视图。
     */
    @Test
    void loginReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    /**
     * 测试首页在无插件、无用户、无角色时仍然正常渲染。
     */
    @Test
    void homeWithEmptyDataReturnsZeroCounts() throws Exception {
        when(pluginRuntimeService.list()).thenReturn(List.of());
        when(roleService.listRoles()).thenReturn(List.of());
        when(userAccountService.listUsers()).thenReturn(List.of());
        when(auditService.listRecent(5)).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pluginTotal", 0L))
                .andExpect(model().attribute("pluginEnabled", 0L))
                .andExpect(model().attribute("roleTotal", 0))
                .andExpect(model().attribute("userTotal", 0))
                .andExpect(model().attribute("recentAudit", org.hamcrest.Matchers.empty()));
    }
}
