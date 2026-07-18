package com.jnimble.admin.plugin;

import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.kernel.route.RegisteredPluginRoute;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Spring MVC controller that handles plugin route requests under the
 * {@code /admin/plugins/{pluginId}/**} path pattern. Resolves routes
 * and delegates rendering to plugin-provided views.
 */
@Controller
public class PluginRouteController {

    private final PluginRouteRegistry routeRegistry;
    private final ControllerAuthorization authorization;

    public PluginRouteController(PluginRouteRegistry routeRegistry, ControllerAuthorization authorization) {
        this.routeRegistry = routeRegistry;
        this.authorization = authorization;
    }

    /**
     * Handles plugin route requests and renders the plugin's view.
     *
     * @param pluginId the plugin ID from the URL path
     * @param request  the HTTP request containing the route path
     * @param model    the Spring MVC model for view rendering
     * @return the view name to render
     * @throws ResponseStatusException if the route is not found or inaccessible
     */
    @RequestMapping("/admin/plugins/{pluginId}/**")
    public String handlePluginRoute(
            @PathVariable String pluginId,
            HttpServletRequest request,
            Model model
    ) {
        RouteMethod method = routeMethod(request.getMethod());
        String requestPath = requestPath(request);
        RegisteredPluginRoute route = routeRegistry.find(requestPath, method)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin route not found: " + requestPath));

        if (!route.pluginId().equals(pluginId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin route not found: " + requestPath);
        }
        if (!route.pluginEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Plugin route is disabled: " + requestPath);
        }

        RouteDefinition definition = route.definition();
        requirePermission(definition.permission());
        model.addAttribute("activeNav", pluginActiveNav(route, definition));
        model.addAttribute("pluginId", route.pluginId());
        model.addAttribute("pluginRoute", route);
        model.addAttribute("pluginRouteDefinition", definition);
        return viewName(definition);
    }

    private RouteMethod routeMethod(String method) {
        try {
            return RouteMethod.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,
                    "Unsupported plugin route method: " + method, ex);
        }
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void requirePermission(String permission) {
        if (permission != null && !permission.isBlank()) {
            authorization.requirePermission(permission.trim());
        }
    }

    private String viewName(RouteDefinition definition) {
        if (definition.view() == null || definition.view().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Plugin route view is blank");
        }
        return definition.view().trim();
    }

    private String pluginActiveNav(RegisteredPluginRoute route, RouteDefinition definition) {
        String routePath = definition.path() == null ? "" : definition.path().trim();
        String routeKey = routePath.replaceAll("^/+|/+$", "").replace('/', '.');
        if (routeKey.isBlank()) {
            routeKey = route.definition().view() == null ? "index" : route.definition().view();
        }
        return "plugin:" + route.pluginId() + ":" + routeKey;
    }
}
