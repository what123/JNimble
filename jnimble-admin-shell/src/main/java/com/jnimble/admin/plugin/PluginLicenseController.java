package com.jnimble.admin.plugin;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.license.core.PluginLicenseService;
import com.jnimble.license.core.PluginLicenseView;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.SystemPermissions;
import com.jnimble.sdk.plugin.PluginDescriptor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Controller for managing plugin licenses in the admin console.
 *
 * <p>Provides endpoints for viewing license information, installing license keys,
 * and revoking licenses for plugins.</p>
 *
 * <p>插件许可证管理控制器。提供查看许可证信息、安装许可证密钥以及撤销插件许可证的端点。</p>
 */
@Controller
@RequestMapping("/admin/plugin-licenses")
public class PluginLicenseController {

    private final PluginRuntimeService pluginRuntimeService;
    private final PluginLicenseService licenseService;
    private final ControllerAuthorization authorization;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new plugin license controller.
     *
     * @param pluginRuntimeService the plugin runtime service
     * @param licenseService       the plugin license service
     * @param authorization        the authorization service
     * @param auditRecorder        the audit recorder
     * @param messageSource        the message source for i18n
     */
    public PluginLicenseController(
            PluginRuntimeService pluginRuntimeService,
            PluginLicenseService licenseService,
            ControllerAuthorization authorization,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.pluginRuntimeService = pluginRuntimeService;
        this.licenseService = licenseService;
        this.authorization = authorization;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Activates a license for the given plugin and redirects to the plugin list.
     *
     * @param pluginId           the plugin ID
     * @param token              the license activation token (optional)
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list page
     */
    @PostMapping("/{pluginId}/activate")
    public String activate(
            @PathVariable String pluginId,
            @RequestParam(required = false) String token,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        try {
            PluginDescriptor descriptor = descriptor(pluginId);
            PluginLicenseView view = licenseService.activate(descriptor, token, operator());
            String message = "Plugin license activated. Status: " + view.status();
            redirectAttributes.addFlashAttribute("message", message);
            auditRecorder.success("PLUGIN_LICENSE_ACTIVATE", "plugin", pluginId, message);
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", message);
            auditRecorder.failure("PLUGIN_LICENSE_ACTIVATE", "plugin", pluginId, message);
        }
        return "redirect:/admin/plugins";
    }

    /**
     * Activates a license and returns the result as JSON for AJAX requests.
     *
     * @param pluginId the plugin ID
     * @param token    the license activation token (optional)
     * @return the activation result with status and message
     */
    @PostMapping("/{pluginId}/activate-result")
    @ResponseBody
    public ResponseEntity<PluginLicenseActionResult> activateResult(
            @PathVariable String pluginId,
            @RequestParam(required = false) String token
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        try {
            PluginLicenseView view = licenseService.activate(descriptor(pluginId), token, operator());
            PluginLicenseActionResult result = result(view);
            if (result.success()) {
                auditRecorder.success("PLUGIN_LICENSE_ACTIVATE", "plugin", pluginId, result.message());
                return ResponseEntity.ok(result);
            }
            auditRecorder.failure("PLUGIN_LICENSE_ACTIVATE", "plugin", pluginId, result.message());
            return ResponseEntity.unprocessableEntity().body(result);
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            auditRecorder.failure("PLUGIN_LICENSE_ACTIVATE", "plugin", pluginId, message);
            return ResponseEntity.badRequest().body(PluginLicenseActionResult.failure(message));
        }
    }

    /**
     * Revalidates a plugin license and returns the result as JSON for AJAX requests.
     *
     * @param pluginId the plugin ID
     * @return the revalidation result with status and message
     */
    @PostMapping("/{pluginId}/revalidate-result")
    @ResponseBody
    public ResponseEntity<PluginLicenseActionResult> revalidateResult(@PathVariable String pluginId) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        try {
            PluginLicenseView view = licenseService.revalidate(descriptor(pluginId), true);
            PluginLicenseActionResult result = result(view);
            if (result.success()) {
                auditRecorder.success("PLUGIN_LICENSE_REVALIDATE", "plugin", pluginId, result.message());
                return ResponseEntity.ok(result);
            }
            auditRecorder.failure("PLUGIN_LICENSE_REVALIDATE", "plugin", pluginId, result.message());
            return ResponseEntity.unprocessableEntity().body(result);
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            auditRecorder.failure("PLUGIN_LICENSE_REVALIDATE", "plugin", pluginId, message);
            return ResponseEntity.badRequest().body(PluginLicenseActionResult.failure(message));
        }
    }

    /**
     * Revalidates a plugin license and redirects to the plugin list.
     *
     * @param pluginId           the plugin ID
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list page
     */
    @PostMapping("/{pluginId}/revalidate")
    public String revalidate(@PathVariable String pluginId, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        try {
            PluginLicenseView view = licenseService.revalidate(descriptor(pluginId), true);
            String message = "Plugin license checked. Status: " + view.status();
            redirectAttributes.addFlashAttribute("message", message);
            auditRecorder.success("PLUGIN_LICENSE_REVALIDATE", "plugin", pluginId, message);
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", message);
            auditRecorder.failure("PLUGIN_LICENSE_REVALIDATE", "plugin", pluginId, message);
        }
        return "redirect:/admin/plugins";
    }

    private PluginLicenseActionResult result(PluginLicenseView view) {
        String statusLabel = messageSource.getMessage(
                "admin.plugin.license.status." + view.status().name(),
                null,
                view.status().name(),
                LocaleContextHolder.getLocale());
        String expiresAt = view.expiresAt() == null
                ? "-"
                : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(view.expiresAt());
        String message = view.usable()
                ? messageSource.getMessage(
                "admin.plugin.license.activationSuccess",
                null,
                "License activated successfully",
                LocaleContextHolder.getLocale())
                : messageSource.getMessage(
                "admin.plugin.license.activationFailure",
                new Object[]{statusLabel},
                "License activation failed: " + statusLabel,
                LocaleContextHolder.getLocale());
        return new PluginLicenseActionResult(
                view.usable(),
                view.status().name(),
                statusLabel,
                expiresAt,
                message);
    }

    private PluginDescriptor descriptor(String pluginId) {
        return pluginRuntimeService.find(pluginId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginId))
                .descriptor();
    }

    private String operator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "unknown" : authentication.getName();
    }

    private String failureMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }

    /**
     * Result of a plugin license activation or revalidation, used for JSON responses.
     *
     * @param success     whether the operation succeeded
     * @param status      the license status code
     * @param statusLabel the localized license status label
     * @param expiresAt   the license expiration date string
     * @param message     the localized result message
     */
    public record PluginLicenseActionResult(
            boolean success,
            String status,
            String statusLabel,
            String expiresAt,
            String message
    ) {
        static PluginLicenseActionResult failure(String message) {
            return new PluginLicenseActionResult(false, null, null, null, message);
        }
    }
}
