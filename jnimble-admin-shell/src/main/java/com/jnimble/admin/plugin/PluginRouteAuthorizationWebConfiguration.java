package com.jnimble.admin.plugin;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration that registers the plugin route authorization interceptor.
 *
 * <p>Adds the {@link PluginRouteAuthorizationInterceptor} to the interceptor
 * registry, protecting plugin routes from unauthorized access based on
 * plugin-level permission declarations.</p>
 *
 * <p>插件路由授权 Web 配置。注册 {@link PluginRouteAuthorizationInterceptor}
 * 到拦截器注册表中，基于插件声明的权限保护插件路由免受未授权访问。</p>
 */
@Configuration
public class PluginRouteAuthorizationWebConfiguration implements WebMvcConfigurer {

    private final PluginRouteAuthorizationInterceptor interceptor;

    public PluginRouteAuthorizationWebConfiguration(PluginRouteAuthorizationInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/admin/plugins/**");
    }
}
