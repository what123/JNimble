package com.jnimble.admin.i18n;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.SystemPermissions;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin UI for extending, enabling and selecting the system languages. */
@Controller
@RequestMapping("/admin/languages")
public class LanguageManagementController {

    private static final String TARGET_TYPE = "language";

    private final LanguageService languageService;
    private final ControllerAuthorization authorization;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new language management controller.
     *
     * @param languageService the language service
     * @param authorization   the authorization service
     * @param auditRecorder   the audit recorder
     * @param messageSource   the message source for i18n
     */
    public LanguageManagementController(
            LanguageService languageService,
            ControllerAuthorization authorization,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.languageService = languageService;
        this.authorization = authorization;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Lists all languages with their current settings.
     *
     * @param model the view model
     * @return the language list template name
     */
    @GetMapping
    public String list(Model model) {
        authorization.requirePermission(SystemPermissions.LANGUAGE_VIEW);
        model.addAttribute("languages", languageService.listAll());
        model.addAttribute("canManageLanguages",
                authorization.hasPermission(SystemPermissions.LANGUAGE_MANAGE));
        model.addAttribute("activeNav", "languages");
        return "page/language/list";
    }

    /**
     * Creates a new language entry.
     *
     * @param code               the language code
     * @param localeTag          the BCP 47 locale tag
     * @param name               the language name
     * @param nativeName         the native language name
     * @param sortOrder          the display sort order
     * @param enabled            whether the language is enabled
     * @param defaultLanguage    whether this is the default language
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the language list
     */
    @PostMapping
    public String create(
            @RequestParam String code,
            @RequestParam String localeTag,
            @RequestParam String name,
            @RequestParam String nativeName,
            @RequestParam(defaultValue = "0") int sortOrder,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "false") boolean defaultLanguage,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.LANGUAGE_MANAGE);
        try {
            LanguageRecord language = languageService.create(
                    code, localeTag, name, nativeName, sortOrder, enabled, defaultLanguage);
            auditRecorder.success(AuditActions.LANGUAGE_CREATE, TARGET_TYPE, language.code(), "created");
            success(redirectAttributes, "admin.languages.message.created");
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.LANGUAGE_CREATE, TARGET_TYPE, code, ex.getMessage());
            failure(redirectAttributes, ex);
        }
        return "redirect:/admin/languages";
    }

    /**
     * Updates an existing language entry.
     *
     * @param code               the language code to update
     * @param localeTag          the BCP 47 locale tag
     * @param name               the language name
     * @param nativeName         the native language name
     * @param sortOrder          the display sort order
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the language list
     */
    @PostMapping("/{code}")
    public String update(
            @PathVariable String code,
            @RequestParam String localeTag,
            @RequestParam String name,
            @RequestParam String nativeName,
            @RequestParam(defaultValue = "0") int sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.LANGUAGE_MANAGE);
        try {
            languageService.update(code, localeTag, name, nativeName, sortOrder);
            auditRecorder.success(AuditActions.LANGUAGE_UPDATE, TARGET_TYPE, code, "updated");
            success(redirectAttributes, "admin.languages.message.updated");
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.LANGUAGE_UPDATE, TARGET_TYPE, code, ex.getMessage());
            failure(redirectAttributes, ex);
        }
        return "redirect:/admin/languages";
    }

    /**
     * Enables or disables a language.
     *
     * @param code               the language code
     * @param enabled            true to enable, false to disable
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the language list
     */
    @PostMapping("/{code}/enabled")
    public String setEnabled(
            @PathVariable String code,
            @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.LANGUAGE_MANAGE);
        String action = enabled ? AuditActions.LANGUAGE_ENABLE : AuditActions.LANGUAGE_DISABLE;
        try {
            languageService.setEnabled(code, enabled);
            auditRecorder.success(action, TARGET_TYPE, code, enabled ? "enabled" : "disabled");
            success(redirectAttributes, enabled
                    ? "admin.languages.message.enabled" : "admin.languages.message.disabled");
        } catch (RuntimeException ex) {
            auditRecorder.failure(action, TARGET_TYPE, code, ex.getMessage());
            failure(redirectAttributes, ex);
        }
        return "redirect:/admin/languages";
    }

    /**
     * Sets a language as the default language.
     *
     * @param code               the language code to set as default
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the language list
     */
    @PostMapping("/{code}/default")
    public String setDefault(@PathVariable String code, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.LANGUAGE_MANAGE);
        try {
            languageService.setDefault(code);
            auditRecorder.success(AuditActions.LANGUAGE_DEFAULT, TARGET_TYPE, code, "set default");
            success(redirectAttributes, "admin.languages.message.defaultChanged");
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.LANGUAGE_DEFAULT, TARGET_TYPE, code, ex.getMessage());
            failure(redirectAttributes, ex);
        }
        return "redirect:/admin/languages";
    }

    private void success(RedirectAttributes redirectAttributes, String key) {
        redirectAttributes.addFlashAttribute("successMessage", message(key));
    }

    private void failure(RedirectAttributes redirectAttributes, RuntimeException exception) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("admin.languages.message.failed", new Object[]{detail},
                        LocaleContextHolder.getLocale()));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
