package com.jnimble.starter.plugin.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Acquires a request lease for plugin-owned controller methods.
 */
@Component
public class PluginRequestDrainInterceptor implements HandlerInterceptor {

    private static final String LEASE_ATTRIBUTE = PluginRequestDrainInterceptor.class.getName() + ".lease";
    private final PluginRequestDrainRegistry registry;

    public PluginRequestDrainInterceptor(PluginRequestDrainRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        try {
            registry.acquire(handlerMethod.getBean())
                    .ifPresent(lease -> request.setAttribute(LEASE_ATTRIBUTE, lease));
            return true;
        } catch (PluginRequestDrainRegistry.PluginRequestDrainingException ex) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Object lease = request.getAttribute(LEASE_ATTRIBUTE);
        if (lease instanceof PluginRequestDrainRegistry.RequestLease requestLease) {
            requestLease.close();
        }
    }
}
