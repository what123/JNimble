package com.jnimble.admin.plugin;

import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.kernel.route.RegisteredPluginRoute;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.route.RouteMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies permissions declared through {@link com.jnimble.sdk.route.RouteDefinition}
 * to dynamically registered plugin controllers as well as generic plugin views.
 */
@Component
public class PluginRouteAuthorizationInterceptor implements HandlerInterceptor {

    private final PluginRouteRegistry routeRegistry;
    private final ControllerAuthorization authorization;

    public PluginRouteAuthorizationInterceptor(
            PluginRouteRegistry routeRegistry,
            ControllerAuthorization authorization
    ) {
        this.routeRegistry = routeRegistry;
        this.authorization = authorization;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RouteMethod method;
        try {
            method = RouteMethod.valueOf(request.getMethod().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return true;
        }
        RegisteredPluginRoute route = routeRegistry.find(requestPath(request), method).orElse(null);
        if (route == null) {
            return true;
        }
        if (!route.pluginEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plugin route is disabled");
        }
        String permission = route.definition().permission();
        if (permission != null && !permission.isBlank()) {
            authorization.requirePermission(permission.trim());
        }
        return true;
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
