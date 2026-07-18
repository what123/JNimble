package com.jnimble.admin.plugin;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.permission.SystemPermissions;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginConfigurationOption;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for viewing and saving plugin configuration parameters.
 *
 * <p>Provides endpoints for displaying configuration forms and persisting
 * configuration values for configurable plugins.</p>
 *
 * <p>插件配置控制器。提供查看和保存插件配置参数的端点，
 * 支持展示配置表单和持久化配置值。</p>
 */
@Controller
@RequestMapping("/admin/plugin-configurations")
public class PluginConfigurationController {

    private final PluginRuntimeService pluginRuntimeService;
    private final PluginConfigurationService configurationService;
    private final ControllerAuthorization authorization;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new plugin configuration controller.
     *
     * @param pluginRuntimeService   the plugin runtime service
     * @param configurationService   the plugin configuration service
     * @param authorization          the authorization service
     * @param auditRecorder          the audit recorder
     * @param messageSource          the message source for i18n
     */
    public PluginConfigurationController(
            PluginRuntimeService pluginRuntimeService,
            PluginConfigurationService configurationService,
            ControllerAuthorization authorization,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.pluginRuntimeService = pluginRuntimeService;
        this.configurationService = configurationService;
        this.authorization = authorization;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Displays the configuration form for a plugin.
     *
     * @param pluginId the plugin ID
     * @param model    the view model
     * @return the plugin configuration template name
     */
    @GetMapping("/{pluginId}")
    public String configuration(@PathVariable String pluginId, Model model) {
        authorization.requirePermission(SystemPermissions.PLUGIN_VIEW);
        PluginDescriptor descriptor = configurablePlugin(pluginId).descriptor();
        PluginConfigurationService.ConfigurationValues values =
                configurationService.configurationValues(descriptor);
        PluginConfigurationDescriptor configuration = descriptor.configuration();

        model.addAttribute("pluginId", descriptor.id());
        model.addAttribute("configPluginId", descriptor.id());
        model.addAttribute("pluginName", resolve(descriptor.nameKey(), descriptor.name()));
        model.addAttribute("configurationTitle", resolve(
                configuration.titleKey(),
                defaultValue(configuration.title(), resolve("admin.plugin.configuration.title", "Plugin configuration"))
        ));
        model.addAttribute("configurationDescription", resolve(
                configuration.descriptionKey(), configuration.description()
        ));
        model.addAttribute("configurationFields", configuration.fields().stream()
                .map(field -> fieldView(field, values))
                .toList());
        model.addAttribute("canManagePlugins",
                authorization.hasPermission(SystemPermissions.PLUGIN_MANAGE));
        model.addAttribute("activeNav", "plugins");
        return "page/plugin/configuration";
    }

    /**
     * Saves the configuration values submitted for a plugin.
     *
     * @param pluginId           the plugin ID
     * @param parameters         the submitted form parameters
     * @param principal          the authenticated user principal
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the plugin configuration page
     */
    @PostMapping("/{pluginId}")
    public String save(
            @PathVariable String pluginId,
            @RequestParam MultiValueMap<String, String> parameters,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.PLUGIN_MANAGE);
        PluginDescriptor descriptor = configurablePlugin(pluginId).descriptor();
        Map<String, String> submitted = new LinkedHashMap<>();
        for (PluginConfigurationField field : descriptor.configuration().fields()) {
            submitted.put(field.key(), lastValue(parameters.get("field." + field.key())));
        }
        String actor = principal == null ? "system" : principal.getName();
        try {
            configurationService.save(descriptor, submitted, actor);
            String message = resolve("admin.plugin.configuration.saved", "Plugin configuration saved.");
            redirectAttributes.addFlashAttribute("message", message);
            auditRecorder.success(
                    AuditActions.PLUGIN_CONFIGURATION_UPDATE, "plugin", descriptor.id(), message);
        } catch (RuntimeException ex) {
            String message = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", message);
            auditRecorder.failure(
                    AuditActions.PLUGIN_CONFIGURATION_UPDATE, "plugin", descriptor.id(), message);
        }
        return "redirect:/admin/plugin-configurations/" + descriptor.id();
    }

    private PluginRuntimeSnapshot configurablePlugin(String pluginId) {
        PluginRuntimeSnapshot snapshot = pluginRuntimeService.find(pluginId)
                .filter(plugin -> plugin.status() != PluginStatus.UNINSTALLED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found"));
        if (snapshot.descriptor().configuration() == null
                || snapshot.descriptor().configuration().fields() == null
                || snapshot.descriptor().configuration().fields().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin has no configurable parameters");
        }
        return snapshot;
    }

    private ConfigurationFieldView fieldView(
            PluginConfigurationField field,
            PluginConfigurationService.ConfigurationValues values
    ) {
        boolean configured = values.configuredKeys().contains(field.key());
        String placeholder = resolve(field.placeholderKey(), field.placeholder());
        if (field.type() == PluginConfigurationFieldType.SECRET && configured) {
            placeholder = resolve(
                    "admin.plugin.configuration.secretConfigured",
                    "Configured; leave blank to keep the existing secret"
            );
        }
        List<ConfigurationOptionView> options = field.options() == null ? List.of() : field.options().stream()
                .map(option -> new ConfigurationOptionView(
                        option.value(), resolve(option.labelKey(), option.label())))
                .toList();
        return new ConfigurationFieldView(
                field.key(),
                resolve(field.labelKey(), defaultValue(field.label(), field.key())),
                resolve(field.descriptionKey(), field.description()),
                placeholder,
                field.type(),
                field.required(),
                values.displayedValues().getOrDefault(field.key(), ""),
                configured,
                options
        );
    }

    private String resolve(String key, String fallback) {
        if (key != null && !key.isBlank()) {
            try {
                return messageSource.getMessage(key.trim(), null, LocaleContextHolder.getLocale());
            } catch (RuntimeException ignored) {
                // Use the descriptor fallback when the current locale has no plugin message.
            }
        }
        return fallback;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String lastValue(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getLast();
    }

    private String failureMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }

    /**
     * View model for a single configuration field on the edit page.
     *
     * @param key         the field key
     * @param label       the localized field label
     * @param description the localized field description
     * @param placeholder the localized placeholder text
     * @param type        the field type (text, number, boolean, select, secret)
     * @param required    whether the field is required
     * @param value       the current field value
     * @param configured  whether the field has a stored value
     * @param options     the selectable options (for SELECT type fields)
     */
    public record ConfigurationFieldView(
            String key,
            String label,
            String description,
            String placeholder,
            PluginConfigurationFieldType type,
            boolean required,
            String value,
            boolean configured,
            List<ConfigurationOptionView> options
    ) {
    }

    /**
     * View model for a select option in a configuration field.
     *
     * @param value the option value
     * @param label the localized option label
     */
    public record ConfigurationOptionView(String value, String label) {
    }
}
