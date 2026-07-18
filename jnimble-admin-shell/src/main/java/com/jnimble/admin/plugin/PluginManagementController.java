package com.jnimble.admin.plugin;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.permission.SystemPermissions;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginPermission;
import com.jnimble.sdk.plugin.PluginStatus;
import com.jnimble.sdk.route.RouteMethod;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for managing plugin lifecycle (install, enable, disable, uninstall, reload).
 *
 * <p>Provides the main plugin management page and endpoints for all plugin
 * lifecycle operations in the admin console.</p>
 *
 * <p>插件生命周期管理控制器。提供插件管理主页面以及所有插件生命周期操作
 * （安装、启用、禁用、卸载、重载）的端点。</p>
 */
@Controller
@RequestMapping("/admin/plugins")
public class PluginManagementController {

    private final PluginRuntimeService pluginRuntimeService;
    private final PluginJarInstallService pluginJarInstallService;
    private final PluginRouteRegistry pluginRouteRegistry;
    private final AdminAuditRecorder auditRecorder;
    private final ControllerAuthorization authorization;
    private final MessageSource messageSource;

    /**
     * Constructs a new plugin management controller.
     *
     * @param pluginRuntimeService     the plugin runtime service
     * @param pluginJarInstallService  the plugin JAR install service
     * @param pluginRouteRegistry      the plugin route registry
     * @param auditRecorder            the audit recorder
     * @param authorization            the authorization service
     * @param messageSource            the message source for i18n
     */
    public PluginManagementController(
            PluginRuntimeService pluginRuntimeService,
            PluginJarInstallService pluginJarInstallService,
            PluginRouteRegistry pluginRouteRegistry,
            AdminAuditRecorder auditRecorder,
            ControllerAuthorization authorization,
            MessageSource messageSource
    ) {
        this.pluginRuntimeService = pluginRuntimeService;
        this.pluginJarInstallService = pluginJarInstallService;
        this.pluginRouteRegistry = pluginRouteRegistry;
        this.auditRecorder = auditRecorder;
        this.authorization = authorization;
        this.messageSource = messageSource;
    }

    /**
     * Displays the plugin list page.
     *
     * @param model the view model
     * @return the plugin list template name
     */
    @GetMapping
    public String list(Model model) {
        authorization.requirePermission(SystemPermissions.PLUGIN_VIEW);
        addPluginList(model);
        return "page/plugin/list";
    }

    /**
     * Displays the plugin detail page.
     *
     * @param pluginId the plugin ID
     * @param model    the view model
     * @return the plugin detail template name
     */
    @GetMapping("/{pluginId}")
    public String detail(@PathVariable String pluginId, Model model) {
        authorization.requirePermission(SystemPermissions.PLUGIN_VIEW);
        addPluginList(model);
        model.addAttribute("plugin", pluginRuntimeService.find(pluginId).orElse(null));
        model.addAttribute("selectedPluginId", pluginId);
        return "page/plugin/detail";
    }

    /**
     * Installs a plugin by its descriptor parameters.
     *
     * @param pluginId           the plugin ID
     * @param name               the plugin name
     * @param version            the plugin version
     * @param platformVersion    the required platform version
     * @param bootClass          the plugin boot class
     * @param description        the plugin description
     * @param permissionCode     the plugin permission code
     * @param permissionName     the plugin permission name
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/install")
    public String install(
            @RequestParam(required = false) String pluginId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String platformVersion,
            @RequestParam(required = false) String bootClass,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String permissionCode,
            @RequestParam(required = false) String permissionName,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_DESCRIPTOR_INSTALL, "Plugin installed.", redirectAttributes, () -> {
            String normalizedPluginId = requiredValue(pluginId, "Plugin id is required.");
            PluginDescriptor descriptor = new PluginDescriptor(
                    "1.0",
                    normalizedPluginId,
                    defaultValue(name, normalizedPluginId),
                    null,
                    blankToNull(description),
                    null,
                    defaultValue(version, "0.0.0"),
                    defaultValue(platformVersion, "0.1.x"),
                    null,
                    null,
                    requiredValue(bootClass, "Boot class is required."),
                    null,
                    pluginPermissions(permissionCode, permissionName),
                    null
            );
            pluginRuntimeService.install(descriptor);
            return normalizedPluginId;
        });
    }

    /**
     * Uploads and installs a plugin JAR file.
     *
     * @param jar                the uploaded JAR file
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/upload")
    public String uploadJar(@RequestParam(required = false) MultipartFile jar, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_JAR_INSTALL, "Plugin jar installed.", redirectAttributes, () -> {
            PluginJarInstallResult result = pluginJarInstallService.install(jar);
            return result.descriptor().id();
        });
    }

    /**
     * Replaces a plugin JAR file for the specified plugin.
     *
     * @param pluginId           the plugin ID
     * @param jar                the uploaded replacement JAR file
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/replace")
    public String replaceJar(
            @PathVariable String pluginId,
            @RequestParam(required = false) MultipartFile jar,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_JAR_REPLACE, "Plugin jar replaced.", redirectAttributes, () -> {
            PluginJarInstallResult result = pluginJarInstallService.replace(pluginId, jar);
            return result.descriptor().id();
        });
    }

    /**
     * Replaces a plugin JAR file using request parameters.
     *
     * @param pluginId           the plugin ID
     * @param jar                the uploaded replacement JAR file
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/replace")
    public String replaceJarByParam(
            @RequestParam(required = false) String pluginId,
            @RequestParam(required = false) MultipartFile jar,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_JAR_REPLACE, "Plugin jar replaced.", redirectAttributes, () -> {
            PluginJarInstallResult result = pluginJarInstallService.replace(pluginId, jar);
            return result.descriptor().id();
        });
    }

    /**
     * Enables a plugin.
     *
     * @param pluginId           the plugin ID
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/enable")
    public String enable(@PathVariable String pluginId, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_ENABLE, "Plugin enabled.", redirectAttributes, () -> {
            pluginRuntimeService.enable(pluginId);
            return pluginId;
        });
    }

    /**
     * Disables a plugin.
     *
     * @param pluginId           the plugin ID
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/disable")
    public String disable(@PathVariable String pluginId, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_DISABLE, "Plugin disabled.", redirectAttributes, () -> {
            pluginRuntimeService.disable(pluginId);
            return pluginId;
        });
    }

    /**
     * Installs a previously uninstalled plugin.
     *
     * @param pluginId           the plugin ID
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/install")
    public String installExisting(@PathVariable String pluginId, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_DESCRIPTOR_INSTALL, "Plugin installed.", redirectAttributes, () -> {
            PluginRuntimeSnapshot snapshot = pluginRuntimeService.find(pluginId)
                    .filter(plugin -> plugin.status() == PluginStatus.UNINSTALLED)
                    .orElseThrow(() -> new IllegalArgumentException("Plugin is not uninstalled: " + pluginId));
            pluginRuntimeService.install(snapshot.descriptor(), snapshot.artifactPath());
            return pluginId;
        });
    }

    /**
     * Uninstalls a plugin, optionally cleaning up plugin data.
     *
     * @param pluginId           the plugin ID
     * @param cleanPluginData    whether to clean up plugin data during uninstall
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/uninstall")
    public String uninstall(
            @PathVariable String pluginId,
            @RequestParam(defaultValue = "false") boolean cleanPluginData,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        String successMessage = cleanPluginData
                ? "Plugin uninstalled. Plugin data cleanup completed."
                : "Plugin uninstalled.";
        return execute(AuditActions.PLUGIN_UNINSTALL, successMessage, redirectAttributes, () -> {
            pluginRuntimeService.uninstall(pluginId, cleanPluginData);
            return pluginId;
        });
    }

    /**
     * Reloads a plugin.
     *
     * @param pluginId           the plugin ID
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin list
     */
    @PostMapping("/{pluginId}/reload")
    public String reload(@PathVariable String pluginId, RedirectAttributes redirectAttributes) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        return execute(AuditActions.PLUGIN_RELOAD, "Plugin reloaded.", redirectAttributes, () -> {
            pluginRuntimeService.reload(pluginId);
            return pluginId;
        });
    }

    private void addPluginList(Model model) {
        List<PluginRuntimeSnapshot> plugins = pluginRuntimeService.list();
        model.addAttribute("plugins", plugins);
        model.addAttribute("pluginAdminEntries", pluginAdminEntries(plugins));
        model.addAttribute("pluginMessages", new PluginMessages(messageSource));
        model.addAttribute("activeNav", "plugins");
    }

    private Map<String, String> pluginAdminEntries(List<PluginRuntimeSnapshot> plugins) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (PluginRuntimeSnapshot plugin : plugins) {
            PluginDescriptor descriptor = plugin.descriptor();
            if (plugin.status() != PluginStatus.ENABLED || descriptor.admin() == null) {
                continue;
            }
            String permission = descriptor.admin().permission();
            if (permission == null || permission.isBlank() || !authorization.hasPermission(permission.trim())) {
                continue;
            }
            String entryUrl = pluginAdminUrl(descriptor);
            boolean routeAvailable = pluginRouteRegistry.find(entryUrl, RouteMethod.GET)
                    .filter(route -> route.pluginId().equals(plugin.pluginId()) && route.pluginEnabled())
                    .isPresent();
            if (routeAvailable) {
                entries.put(plugin.pluginId(), entryUrl);
            }
        }
        return entries;
    }

    private String pluginAdminUrl(PluginDescriptor descriptor) {
        String entry = descriptor.admin().entry().trim();
        return "/admin/plugins/" + descriptor.id() + entry;
    }

    private String execute(
            String action,
            String successMessage,
            RedirectAttributes redirectAttributes,
            PluginOperation operation
    ) {
        String targetId = null;
        try {
            targetId = operation.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
            auditRecorder.success(action, "plugin", targetId, successMessage);
        } catch (RuntimeException ex) {
            String errorMessage = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            auditRecorder.failure(action, "plugin", targetId, errorMessage);
        }
        return "redirect:/admin/plugins";
    }

    private String failureMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private String requiredValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<PluginPermission> pluginPermissions(String code, String name) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        String permissionCode = code.trim();
        return List.of(new PluginPermission(permissionCode, defaultValue(name, permissionCode), null));
    }

    @FunctionalInterface
    private interface PluginOperation {
        String run();
    }

    /**
     * Provides localized message resolution for plugin display properties.
     *
     * @param messageSource the message source for i18n
     */
    public record PluginMessages(MessageSource messageSource) {

        /**
         * Returns the localized plugin name.
         *
         * @param descriptor the plugin descriptor
         * @return the localized name
         */
        public String name(PluginDescriptor descriptor) {
            return resolve(descriptor.nameKey(), descriptor.name());
        }

        /**
         * Returns the localized plugin description.
         *
         * @param descriptor the plugin descriptor
         * @return the localized description
         */
        public String description(PluginDescriptor descriptor) {
            return resolve(descriptor.descriptionKey(), descriptor.description());
        }

        /**
         * Returns the localized plugin status label.
         *
         * @param status the plugin status
         * @return the localized status label
         */
        public String status(PluginStatus status) {
            if (status == null) {
                return "-";
            }
            return resolve("admin.plugin.status." + status.name(), status.name());
        }

        /**
         * Returns the localized admin entry label for a plugin.
         *
         * @param descriptor the plugin descriptor
         * @return the localized admin label
         */
        public String adminLabel(PluginDescriptor descriptor) {
            if (descriptor.admin() == null) {
                return resolve("admin.plugin.action.configure", "Configure");
            }
            return resolve(
                    descriptor.admin().labelKey(),
                    resolve("admin.plugin.action.configure", "Configure")
            );
        }

        private String resolve(String key, String fallback) {
            if (key != null && !key.isBlank()) {
                try {
                    return messageSource.getMessage(key.trim(), null, LocaleContextHolder.getLocale());
                } catch (NoSuchMessageException ignored) {
                    // Use descriptor fallback below.
                }
            }
            return fallback == null || fallback.isBlank() ? "-" : fallback;
        }
    }
}
