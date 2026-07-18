package com.jnimble.admin.setting;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.SystemPermissions;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin UI for configuring storage directories (plugin dir, logo dir). */
@Controller
@RequestMapping("/admin/storage-config")
public class StorageConfigController {

    private static final String TARGET_TYPE = "storage-config";
    private static final String TARGET_ID = "storage";

    private final StorageConfigService storageConfigService;
    private final ControllerAuthorization authorization;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new storage config controller.
     *
     * @param storageConfigService the storage config service
     * @param authorization        the authorization service
     * @param auditRecorder        the audit recorder
     * @param messageSource        the message source for i18n
     */
    public StorageConfigController(
            StorageConfigService storageConfigService,
            ControllerAuthorization authorization,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.storageConfigService = storageConfigService;
        this.authorization = authorization;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Displays the storage config page.
     *
     * @param model the view model
     * @return the storage config template name
     */
    @GetMapping
    public String view(Model model) {
        authorization.requirePermission(SystemPermissions.SETTING_VIEW);
        StorageConfig config = storageConfigService.currentConfig();
        model.addAttribute("pluginDir", config.pluginDir());
        model.addAttribute("logoDir", config.logoDir());
        model.addAttribute("canManage", authorization.hasPermission(SystemPermissions.SETTING_MANAGE));
        model.addAttribute("activeNav", "storage-config");
        return "page/storage-config";
    }

    /**
     * Saves storage directory settings.
     *
     * @param pluginDir           the plugin storage directory
     * @param logoDir             the logo storage directory
     * @param operator            the operator identifier
     * @param redirectAttributes  redirect attributes for flash messages
     * @return redirect to the storage config page
     */
    @PostMapping
    public String save(
            @RequestParam String pluginDir,
            @RequestParam String logoDir,
            @RequestParam String operator,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.SETTING_MANAGE);
        try {
            storageConfigService.save(new StorageConfig(pluginDir, logoDir), operator);
            auditRecorder.success(AuditActions.SYSTEM_SETTING_UPDATE, TARGET_TYPE, TARGET_ID, "updated");
            redirectAttributes.addFlashAttribute("successMessage",
                    message("admin.storage.message.saved"));
        } catch (RuntimeException ex) {
            String detail = ex.getMessage() == null
                    ? ex.getClass().getSimpleName() : ex.getMessage();
            auditRecorder.failure(AuditActions.SYSTEM_SETTING_UPDATE, TARGET_TYPE, TARGET_ID, detail);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("admin.storage.message.failed", new Object[]{detail},
                            LocaleContextHolder.getLocale()));
        }
        return "redirect:/admin/storage-config";
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
