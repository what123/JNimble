package com.jnimble.starter.plugin.runtime;

import com.jnimble.kernel.plugin.PluginClassLoaderRegistry;
import com.jnimble.kernel.plugin.PluginIds;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ClassLoaderTemplateResource;

/**
 * Resolves {@code plugin/{pluginId}/...} templates from the enabled plugin's
 * own class loader. Results are deliberately not cached across plugin replacement.
 */
@Component
public class PluginTemplateResolver implements ITemplateResolver {

    private static final String PREFIX = "plugin/";
    private final PluginClassLoaderRegistry classLoaderRegistry;

    public PluginTemplateResolver(PluginClassLoaderRegistry classLoaderRegistry) {
        this.classLoaderRegistry = classLoaderRegistry;
    }

    @Override
    public String getName() {
        return "jnimblePluginTemplateResolver";
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public TemplateResolution resolveTemplate(
            IEngineConfiguration configuration,
            String ownerTemplate,
            String template,
            Map<String, Object> templateResolutionAttributes
    ) {
        if (template == null || !template.startsWith(PREFIX)) {
            return null;
        }
        String remaining = template.substring(PREFIX.length());
        int separator = remaining.indexOf('/');
        if (separator <= 0) {
            return null;
        }
        String pluginId = remaining.substring(0, separator);
        try {
            PluginIds.requireValid(pluginId, "Plugin id");
        } catch (IllegalArgumentException ex) {
            return null;
        }
        ClassLoader classLoader = classLoaderRegistry.find(pluginId).orElse(null);
        if (classLoader == null) {
            return null;
        }
        String resourceName = "templates/" + template + (template.endsWith(".html") ? "" : ".html");
        ClassLoaderTemplateResource resource = new ClassLoaderTemplateResource(
                classLoader,
                resourceName,
                StandardCharsets.UTF_8.name());
        if (!resource.exists()) {
            return null;
        }
        return new TemplateResolution(
                resource,
                TemplateMode.HTML,
                NonCacheableCacheEntryValidity.INSTANCE);
    }
}
