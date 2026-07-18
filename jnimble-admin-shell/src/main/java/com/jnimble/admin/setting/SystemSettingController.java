package com.jnimble.admin.setting;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.SystemPermissions;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin UI for configuring framework-wide system settings (site name, logo, etc.). */
@Controller
@RequestMapping("/admin/system-settings")
public class SystemSettingController {

    private static final String TARGET_TYPE = "system-setting";
    private static final String LOGO_BASE_PATH = "/admin/system-settings/logo/";

    private final SystemSettingService settingService;
    private final LogoStorageService logoStorageService;
    private final ControllerAuthorization authorization;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new system setting controller.
     *
     * @param settingService     the system setting service
     * @param logoStorageService the logo storage service
     * @param authorization      the authorization service
     * @param auditRecorder      the audit recorder
     * @param messageSource      the message source for i18n
     */
    public SystemSettingController(
            SystemSettingService settingService,
            LogoStorageService logoStorageService,
            ControllerAuthorization authorization,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.settingService = settingService;
        this.logoStorageService = logoStorageService;
        this.authorization = authorization;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Displays the system settings page.
     *
     * @param model the view model
     * @return the system settings template name
     */
    @GetMapping
    public String view(Model model) {
        authorization.requirePermission(SystemPermissions.SETTING_VIEW);
        SiteBranding branding = settingService.siteBranding();
        model.addAttribute("siteName", branding.name());
        model.addAttribute("siteSubtitle", branding.subtitle());
        model.addAttribute("logoUrl", branding.logoUrl());
        model.addAttribute("canManage", authorization.hasPermission(SystemPermissions.SETTING_MANAGE));
        model.addAttribute("activeNav", "system-settings");
        return "page/system-settings";
    }

    /**
     * Saves system settings (site name, subtitle, logo configuration).
     *
     * @param siteName           the site name
     * @param siteSubtitle       the site subtitle
     * @param removeLogo         whether to remove the current logo
     * @param logo               the uploaded logo file (optional)
     * @param operator           the operator identifier
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the system settings page
     */
    @PostMapping
    public String save(
            @RequestParam String siteName,
            @RequestParam String siteSubtitle,
            @RequestParam(required = false, defaultValue = "false") boolean removeLogo,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam String operator,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.SETTING_MANAGE);
        try {
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put(SystemSettingService.KEY_SITE_NAME, siteName == null ? "" : siteName.trim());
            settings.put(SystemSettingService.KEY_SITE_SUBTITLE, siteSubtitle == null ? "" : siteSubtitle.trim());

            String logoUrl = settingService.get(SystemSettingService.KEY_SITE_LOGO_URL);
            if (removeLogo) {
                settings.put(SystemSettingService.KEY_SITE_LOGO_URL, "");
            } else if (logo != null && !logo.isEmpty()) {
                String fileName = logoStorageService.store(logo);
                settings.put(SystemSettingService.KEY_SITE_LOGO_URL, LOGO_BASE_PATH + fileName);
            } else {
                settings.put(SystemSettingService.KEY_SITE_LOGO_URL, logoUrl == null ? "" : logoUrl);
            }

            settingService.saveAll(settings, operator);
            auditRecorder.success(AuditActions.SYSTEM_SETTING_UPDATE, TARGET_TYPE, "site", "updated");
            success(redirectAttributes, "admin.settings.message.saved");
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.SYSTEM_SETTING_UPDATE, TARGET_TYPE, "site", ex.getMessage());
            failure(redirectAttributes, ex);
        }
        return "redirect:/admin/system-settings";
    }

    /**
     * Uploads a logo image via AJAX and returns its URL.
     *
     * @param file the uploaded logo file
     * @return a JSON response with the logo URL or error message
     */
    @PostMapping("/logo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadLogo(@RequestParam("file") MultipartFile file) {
        authorization.requirePermission(SystemPermissions.SETTING_MANAGE);
        try {
            String fileName = logoStorageService.store(file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "logoUrl", LOGO_BASE_PATH + fileName
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Serves a stored logo image by file name.
     *
     * @param fileName the stored logo file name
     * @return the logo image resource with caching headers
     */
    @GetMapping("/logo/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> logo(@PathVariable String fileName) {
        try {
            Resource resource = logoStorageService.load(fileName);
            return ResponseEntity.ok()
                    .contentType(logoStorageService.mediaType(fileName))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo 不存在", ex);
        }
    }

    private void success(RedirectAttributes redirectAttributes, String key) {
        redirectAttributes.addFlashAttribute("successMessage", message(key));
    }

    private void failure(RedirectAttributes redirectAttributes, RuntimeException exception) {
        String detail = exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();
        redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("admin.settings.message.failed", new Object[]{detail},
                        LocaleContextHolder.getLocale()));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
