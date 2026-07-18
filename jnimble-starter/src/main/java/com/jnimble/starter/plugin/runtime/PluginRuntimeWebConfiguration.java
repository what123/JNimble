package com.jnimble.starter.plugin.runtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.util.List;

@Configuration
public class PluginRuntimeWebConfiguration implements WebMvcConfigurer {

    private final PluginRequestDrainInterceptor interceptor;
    private final PluginHandlerExceptionResolver exceptionResolver;

    public PluginRuntimeWebConfiguration(
            PluginRequestDrainInterceptor interceptor,
            PluginHandlerExceptionResolver exceptionResolver
    ) {
        this.interceptor = interceptor;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(0, exceptionResolver);
    }
}
