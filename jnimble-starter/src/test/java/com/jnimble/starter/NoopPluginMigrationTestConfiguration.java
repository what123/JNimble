package com.jnimble.starter;

import com.jnimble.kernel.migration.PluginMigrationExecutor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class NoopPluginMigrationTestConfiguration {

    @Bean
    PluginMigrationExecutor testPluginMigrationExecutor() {
        return PluginMigrationExecutor.noop();
    }
}
