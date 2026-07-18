package com.jnimble.platform.i18n;

import com.jnimble.kernel.plugin.PluginI18nRegistry;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the platform's internationalization (i18n) infrastructure.
 * Sets up the dynamic plugin message source and plugin i18n registry.
 */
@Configuration
public class PlatformI18nConfiguration {

    /**
     * Creates the dynamic plugin message source bean.
     *
     * @return the dynamic plugin message source
     */
    @Bean
    DynamicPluginMessageSource dynamicPluginMessageSource() {
        return new DynamicPluginMessageSource(List.of("i18n/messages"));
    }

    /**
     * Creates the primary message source bean backed by the dynamic plugin message source.
     *
     * @param dynamicPluginMessageSource the dynamic plugin message source
     * @return the primary message source
     */
    @Bean
    @Primary
    MessageSource messageSource(DynamicPluginMessageSource dynamicPluginMessageSource) {
        return dynamicPluginMessageSource;
    }

    /**
     * Creates the plugin i18n registry bean for plugin message bundle registration.
     *
     * @param dynamicPluginMessageSource the dynamic plugin message source
     * @return the plugin i18n registry
     */
    @Bean
    PluginI18nRegistry pluginI18nRegistry(DynamicPluginMessageSource dynamicPluginMessageSource) {
        return (pluginId, basename, classLoader) -> {
            dynamicPluginMessageSource.registerPluginMessages(pluginId, basename, classLoader);
            return () -> dynamicPluginMessageSource.unregisterPluginMessages(pluginId);
        };
    }
}
