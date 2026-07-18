package com.jnimble.starter.plugin.runtime;

import com.jnimble.kernel.plugin.PluginRuntimeException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registers controller methods from a plugin child context in the platform's
 * primary {@link RequestMappingHandlerMapping}.
 */
@Component
public class PluginMvcEndpointRegistrar {

    private static final Duration DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(30);

    private final RequestMappingHandlerMapping handlerMapping;
    private final PluginRequestDrainRegistry drainRegistry;
    private final PluginHandlerExceptionResolver exceptionResolver;
    private final MappingInspector mappingInspector;

    public PluginMvcEndpointRegistrar(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            PluginRequestDrainRegistry drainRegistry,
            PluginHandlerExceptionResolver exceptionResolver
    ) {
        this.handlerMapping = handlerMapping;
        this.drainRegistry = drainRegistry;
        this.exceptionResolver = exceptionResolver;
        this.mappingInspector = new MappingInspector();
        this.mappingInspector.setPatternParser(handlerMapping.getPatternParser());
    }

    PluginMvcRegistration register(String pluginId, ConfigurableApplicationContext pluginContext) {
        Map<String, Object> controllers = pluginContext.getBeansWithAnnotation(Controller.class).entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("pluginDependency$"))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new));
        List<EndpointRegistration> endpoints = new ArrayList<>();
        Set<Object> handlers = new LinkedHashSet<>(controllers.values());
        try {
            for (Object controller : handlers) {
                Class<?> controllerType = AopUtils.getTargetClass(controller);
                Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(
                        controllerType,
                        (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method ->
                                mappingInspector.mappingFor(method, controllerType));
                methods.forEach((method, mapping) -> {
                    handlerMapping.registerMapping(mapping, controller, method);
                    endpoints.add(new EndpointRegistration(mapping, controller, method));
                });
            }
            drainRegistry.registerHandlers(pluginId, handlers);
            exceptionResolver.register(pluginContext, handlers);
            return new PluginMvcRegistration(
                    pluginId,
                    handlerMapping,
                    drainRegistry,
                    exceptionResolver,
                    List.copyOf(endpoints),
                    Set.copyOf(handlers));
        } catch (RuntimeException ex) {
            unregister(endpoints);
            drainRegistry.unregisterHandlers(pluginId, handlers);
            exceptionResolver.unregister(handlers);
            throw new PluginRuntimeException("Failed to register HTTP endpoints for plugin " + pluginId, ex);
        }
    }

    private void unregister(List<EndpointRegistration> endpoints) {
        for (int i = endpoints.size() - 1; i >= 0; i--) {
            handlerMapping.unregisterMapping(endpoints.get(i).mapping());
        }
    }

    static final class PluginMvcRegistration {

        private final String pluginId;
        private final RequestMappingHandlerMapping handlerMapping;
        private final PluginRequestDrainRegistry drainRegistry;
        private final PluginHandlerExceptionResolver exceptionResolver;
        private final List<EndpointRegistration> endpoints;
        private final Collection<Object> handlers;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private PluginMvcRegistration(
                String pluginId,
                RequestMappingHandlerMapping handlerMapping,
                PluginRequestDrainRegistry drainRegistry,
                PluginHandlerExceptionResolver exceptionResolver,
                List<EndpointRegistration> endpoints,
                Collection<Object> handlers
        ) {
            this.pluginId = pluginId;
            this.handlerMapping = handlerMapping;
            this.drainRegistry = drainRegistry;
            this.exceptionResolver = exceptionResolver;
            this.endpoints = endpoints;
            this.handlers = handlers;
        }

        void deactivate() {
            if (!active.compareAndSet(true, false)) {
                return;
            }
            drainRegistry.beginDrain(pluginId);
            endpoints.forEach(endpoint -> handlerMapping.unregisterMapping(endpoint.mapping()));
            try {
                drainRegistry.awaitDrained(pluginId, DEFAULT_DRAIN_TIMEOUT);
                exceptionResolver.unregister(handlers);
                drainRegistry.unregisterHandlers(pluginId, handlers);
            } catch (RuntimeException ex) {
                rollbackMappings();
                drainRegistry.resume(pluginId);
                active.set(true);
                throw ex;
            }
        }

        private void rollbackMappings() {
            List<EndpointRegistration> restored = new ArrayList<>();
            try {
                for (EndpointRegistration endpoint : endpoints) {
                    handlerMapping.registerMapping(endpoint.mapping(), endpoint.handler(), endpoint.method());
                    restored.add(endpoint);
                }
            } catch (RuntimeException rollbackFailure) {
                restored.forEach(endpoint -> handlerMapping.unregisterMapping(endpoint.mapping()));
                throw new PluginRuntimeException(
                        "Failed to restore HTTP endpoints after drain timeout for plugin " + pluginId,
                        rollbackFailure);
            }
        }
    }

    private record EndpointRegistration(
            RequestMappingInfo mapping,
            Object handler,
            Method method
    ) {
    }

    private static final class MappingInspector extends RequestMappingHandlerMapping {
        private RequestMappingInfo mappingFor(Method method, Class<?> handlerType) {
            return getMappingForMethod(method, handlerType);
        }
    }
}
