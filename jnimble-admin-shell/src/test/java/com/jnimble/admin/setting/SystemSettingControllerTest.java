package com.jnimble.admin.setting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.auth.ControllerAuthorization;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Unit tests for {@link SystemSettingController}. */
@ExtendWith(MockitoExtension.class)
class SystemSettingControllerTest {

    @Mock
    private SystemSettingService settingService;

    @Mock
    private LogoStorageService logoStorageService;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private SystemSettingController controller;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("message");
    }

    @Test
    void viewPopulatesModelAndReturnsView() {
        when(authorization.hasPermission(anyString())).thenReturn(true);
        when(settingService.siteBranding())
                .thenReturn(new SiteBranding("MyShop", "POS", "/logo/x.png"));

        String view = controller.view(model);

        assertEquals("page/system-settings", view);
        verify(model).addAttribute("siteName", "MyShop");
        verify(model).addAttribute("siteSubtitle", "POS");
        verify(model).addAttribute("logoUrl", "/logo/x.png");
        verify(model).addAttribute("activeNav", "system-settings");
    }

    @Test
    void savePersistsSettingsAndRedirects() {
        when(settingService.get(SystemSettingService.KEY_SITE_LOGO_URL)).thenReturn("/logo/old.png");

        String result = controller.save("NewName", "NewSubtitle", false, null, "admin", redirectAttributes);

        assertEquals("redirect:/admin/system-settings", result);
        verify(settingService).saveAll(argThat(map ->
                "NewName".equals(map.get("site.name"))
                        && "NewSubtitle".equals(map.get("site.subtitle"))
                        && "/logo/old.png".equals(map.get("site.logoUrl"))
        ), eq("admin"));
        verify(auditRecorder).success(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void saveWithRemoveLogoClearsLogoUrl() {
        when(settingService.get(SystemSettingService.KEY_SITE_LOGO_URL)).thenReturn("/logo/old.png");

        controller.save("Name", "Subtitle", true, null, "admin", redirectAttributes);

        verify(settingService).saveAll(argThat(map -> "".equals(map.get("site.logoUrl"))), eq("admin"));
    }

    @Test
    void saveWithoutLogoKeepsExistingUrl() {
        when(settingService.get(SystemSettingService.KEY_SITE_LOGO_URL)).thenReturn("/logo/old.png");

        controller.save("Name", "Subtitle", false, null, "admin", redirectAttributes);

        verify(settingService).saveAll(argThat(map -> "/logo/old.png".equals(map.get("site.logoUrl"))), eq("admin"));
    }
}
