package com.jnimble.admin.hook;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.dialect.IDialect;

/**
 * Spring configuration for the admin hook Thymeleaf dialect.
 * Creates and registers the {@link AdminHookDialect} bean.
 */
@Configuration
public class AdminHookDialectConfiguration {

    /**
     * Creates the admin hook dialect bean.
     *
     * @param hookViewServiceProvider the provider for hook view service
     * @return the admin hook dialect
     */
    @Bean
    IDialect adminHookDialect(ObjectProvider<AdminHookViewService> hookViewServiceProvider) {
        return new AdminHookDialect(hookViewServiceProvider);
    }
}
