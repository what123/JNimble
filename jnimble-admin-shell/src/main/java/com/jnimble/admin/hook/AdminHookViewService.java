package com.jnimble.admin.hook;

import com.jnimble.kernel.hook.HookManager;
import com.jnimble.kernel.hook.InMemoryHookRegistry.HookRegistration;
import com.jnimble.kernel.hook.InMemoryHookRegistry.HookResolution;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.hook.HookViewContribution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Service responsible for resolving and rendering admin hook views contributed by plugins.
 * Handles hook resolution, permission checks, and template rendering with error handling.
 */
@Component("adminHookViews")
@EnableConfigurationProperties(HookProperties.class)
public class AdminHookViewService {

    private static final Logger log = LoggerFactory.getLogger(AdminHookViewService.class);

    private final HookManager hookManager;
    private final ControllerAuthorization authorization;
    private final TemplateEngine templateEngine;
    private final HookProperties properties;
    private final PluginRuntimeService pluginRuntimeService;
    private final Set<String> warnedHookConflicts = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new admin hook view service.
     *
     * @param hookManager           the hook manager for resolving hook registrations
     * @param authorization         the authorization service for permission checks
     * @param templateEngine        the Thymeleaf template engine for rendering
     * @param properties            the hook configuration properties
     * @param pluginRuntimeService  the plugin runtime service for error recording
     */
    public AdminHookViewService(
            HookManager hookManager,
            ControllerAuthorization authorization,
            TemplateEngine templateEngine,
            HookProperties properties,
            PluginRuntimeService pluginRuntimeService
    ) {
        this.hookManager = hookManager;
        this.authorization = authorization;
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    /**
     * Resolves admin hook views for the given hook name.
     *
     * @param hookName the name of the hook to resolve
     * @return a list of resolved hook views
     */
    public List<AdminHookView> resolve(String hookName) {
        HookResolution resolution = hookManager.resolve(hookName);
        logSuppressedOverrides(resolution);
        if (resolution.removal().isPresent()) {
            return List.of();
        }

        Stream<HookRegistration> registrations = resolution.replacement()
                .map(Stream::of)
                .orElseGet(() -> Stream.concat(resolution.prepends().stream(), resolution.appends().stream()));

        return registrations
                .filter(this::canRender)
                .map(this::toView)
                .toList();
    }

    private void logSuppressedOverrides(HookResolution resolution) {
        if (resolution.suppressedOverrides().isEmpty()) {
            return;
        }

        String selectedPlugin = resolution.override()
                .map(HookRegistration::pluginId)
                .orElse("-");
        for (HookRegistration suppressed : resolution.suppressedOverrides()) {
            String key = resolution.hookName() + ":" + selectedPlugin + ":" + suppressed.pluginId();
            if (warnedHookConflicts.add(key)) {
                log.warn(
                        "Hook override conflict: hook={}, selectedPlugin={}, suppressedPlugin={}",
                        resolution.hookName(),
                        selectedPlugin,
                        suppressed.pluginId());
            }
        }
    }

    /**
     * Renders admin hook views for the given hook name and template context.
     *
     * @param hookName        the name of the hook to render
     * @param templateContext the Thymeleaf template context
     * @return a list of rendered hook views with HTML content
     */
    public List<RenderedAdminHookView> render(String hookName, IContext templateContext) {
        return resolve(hookName).stream()
                .map(view -> renderHook(hookName, view, templateContext))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<RenderedAdminHookView> renderHook(String hookName, AdminHookView view, IContext templateContext) {
        try {
            return Optional.of(new RenderedAdminHookView(
                    view.pluginId(),
                    templateEngine.process(templateSpec(view.view()), hookContext(templateContext, view)),
                    view.order()));
        } catch (RuntimeException ex) {
            handleHookFailure(hookName, view, ex);
            return Optional.empty();
        }
    }

    private IContext hookContext(IContext inheritedContext, AdminHookView view) {
        Map<String, Object> variables = inheritedVariables(inheritedContext);
        variables.putAll(view.model());
        variables.put("hookModel", view.model());
        variables.put("hookPluginId", view.pluginId());
        variables.put("pluginId", view.modelPluginId());

        if (inheritedContext instanceof IWebContext webContext) {
            return new WebContext(webContext.getExchange(), inheritedContext.getLocale(), variables);
        }
        return new Context(inheritedContext == null ? null : inheritedContext.getLocale(), variables);
    }

    private Map<String, Object> inheritedVariables(IContext context) {
        Map<String, Object> variables = new HashMap<>();
        if (context == null) {
            return variables;
        }
        for (String name : context.getVariableNames()) {
            variables.put(name, context.getVariable(name));
        }
        return variables;
    }

    private TemplateSpec templateSpec(String view) {
        String expression = stripFragmentExpression(view);
        int selectorIndex = expression.indexOf("::");
        if (selectorIndex < 0) {
            return new TemplateSpec(expression, TemplateMode.HTML);
        }

        String template = expression.substring(0, selectorIndex).trim();
        String selector = expression.substring(selectorIndex + 2).trim();
        if (selector.isBlank()) {
            return new TemplateSpec(template, TemplateMode.HTML);
        }
        return new TemplateSpec(template, Set.of(selector), TemplateMode.HTML, Map.of());
    }

    private String stripFragmentExpression(String view) {
        String expression = view.trim();
        if (expression.startsWith("~{") && expression.endsWith("}")) {
            return expression.substring(2, expression.length() - 1).trim();
        }
        return expression;
    }

    private void handleHookFailure(String hookName, AdminHookView view, RuntimeException failure) {
        String message = hookFailureMessage(hookName, view, failure);
        recordHookFailure(view.pluginId(), message);
        if (properties.isFailFast()) {
            throw new IllegalStateException(message, failure);
        }
        log.warn(message, failure);
    }

    private void recordHookFailure(String pluginId, String message) {
        try {
            pluginRuntimeService.recordRuntimeError(pluginId, message);
        } catch (RuntimeException ex) {
            log.warn("Failed to record hook runtime error for plugin {}", pluginId, ex);
        }
    }

    private String hookFailureMessage(String hookName, AdminHookView view, RuntimeException failure) {
        String failureMessage = failure.getMessage();
        String suffix = failureMessage == null || failureMessage.isBlank()
                ? failure.getClass().getName()
                : failureMessage;
        return "Hook rendering failed: hook=" + hookName
                + ", plugin=" + view.pluginId()
                + ", view=" + view.view()
                + ", error=" + suffix;
    }

    private boolean canRender(HookRegistration registration) {
        HookViewContribution contribution = registration.contribution();
        return hasView(contribution)
                && hasPermission(contribution.permission())
                && isActive(contribution.activeWhen());
    }

    private boolean hasView(HookViewContribution contribution) {
        return contribution.view() != null && !contribution.view().isBlank();
    }

    private boolean hasPermission(String permission) {
        return permission == null || permission.isBlank() || authorization.hasPermission(permission.trim());
    }

    private boolean isActive(String activeWhen) {
        if (activeWhen == null || activeWhen.isBlank()) {
            return true;
        }
        String normalized = activeWhen.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        return !"false".equalsIgnoreCase(normalized);
    }

    private AdminHookView toView(HookRegistration registration) {
        HookViewContribution contribution = registration.contribution();
        Map<String, Object> model = contribution.model() == null ? Map.of() : contribution.model();
        Object modelPluginId = model.getOrDefault("pluginId", registration.pluginId());
        return new AdminHookView(
                registration.pluginId(),
                contribution.view().trim(),
                model,
                modelPluginId,
                contribution.order()
        );
    }

    /**
     * Represents a resolved hook view contributed by a plugin.
     *
     * @param pluginId    the ID of the contributing plugin
     * @param view        the Thymeleaf view fragment expression
     * @param model       the model variables to pass to the view
     * @param modelPluginId the plugin ID exposed in the model
     * @param order       the rendering order among hook views
     */
    public record AdminHookView(
            String pluginId,
            String view,
            Map<String, Object> model,
            Object modelPluginId,
            int order
    ) {
    }

    /**
     * Represents a rendered hook view with its HTML output.
     *
     * @param pluginId the ID of the contributing plugin
     * @param html     the rendered HTML content
     * @param order    the rendering order among hook views
     */
    public record RenderedAdminHookView(
            String pluginId,
            String html,
            int order
    ) {
    }
}
