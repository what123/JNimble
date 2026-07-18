package com.jnimble.starter.plugin.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * Delegates plugin-controller exceptions to an exception resolver that is
 * initialized against the owning plugin child context. This makes plugin-local
 * {@code @ControllerAdvice} participate without leaking advice beans into the
 * platform context.
 */
@Component
public class PluginHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private final Map<Object, ExceptionHandlerExceptionResolver> resolvers =
            java.util.Collections.synchronizedMap(new IdentityHashMap<>());

    void register(ConfigurableApplicationContext context, Collection<?> handlers) {
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setApplicationContext(context);
        resolver.afterPropertiesSet();
        synchronized (resolvers) {
            handlers.forEach(handler -> resolvers.put(handler, resolver));
        }
    }

    void unregister(Collection<?> handlers) {
        synchronized (resolvers) {
            handlers.forEach(resolvers::remove);
        }
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Object bean = handler instanceof HandlerMethod handlerMethod
                ? handlerMethod.getBean()
                : handler;
        ExceptionHandlerExceptionResolver resolver;
        synchronized (resolvers) {
            resolver = resolvers.get(bean);
        }
        return resolver == null ? null : resolver.resolveException(request, response, handler, ex);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
