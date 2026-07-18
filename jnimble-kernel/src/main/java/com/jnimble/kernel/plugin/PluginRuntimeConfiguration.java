package com.jnimble.kernel.plugin;

import com.jnimble.kernel.migration.FlywayPluginMigrationExecutor;
import com.jnimble.kernel.migration.PluginMigrationExecutor;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring configuration for the plugin runtime subsystem.
 *
 * <p>Provides beans for plugin boot loading, bean resolution, and runtime management.
 * If a {@link DataSource} is available, Flyway-based migrations are enabled.</p>
 */
@Configuration
public class PluginRuntimeConfiguration {

    /**
     * Creates the plugin boot loader using reflection.
     *
     * @return the plugin boot loader
     */
    @Bean
    PluginBootLoader pluginBootLoader() {
        return new ReflectivePluginBootLoader();
    }

    /**
     * Creates the plugin bean resolver backed by the Spring bean factory.
     *
     * @param beanFactory the Spring bean factory
     * @return the plugin bean resolver
     */
    @Bean
    PluginBeanResolver pluginBeanResolver(BeanFactory beanFactory) {
        return new PluginBeanResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                return beanFactory.getBean(type);
            }
        };
    }

    /**
     * Creates the plugin class loader registry.
     *
     * @return an in-memory class loader registry
     */
    @Bean
    PluginClassLoaderRegistry pluginClassLoaderRegistry() {
        return PluginClassLoaderRegistry.inMemory();
    }

    /**
     * Creates the plugin runtime service.
     *
     * @param pluginBootLoader      the boot loader for plugin classes
     * @param contributionRegistries the contribution registries
     * @param pluginBeanResolver    the bean resolver for plugin dependencies
     * @param i18nRegistryProvider  the i18n registry provider (optional)
     * @param dataSourceProvider    the data source provider (optional, for migrations)
     * @return the plugin runtime service
     */
    @Bean
    PluginRuntimeService pluginRuntimeService(
            PluginBootLoader pluginBootLoader,
            PluginContributionRegistries contributionRegistries,
            PluginBeanResolver pluginBeanResolver,
            PluginClassLoaderRegistry pluginClassLoaderRegistry,
            ObjectProvider<PluginBeanContainerFactory> beanContainerFactoryProvider,
            ObjectProvider<PluginActivationGuard> activationGuardProvider,
            ObjectProvider<PluginI18nRegistry> i18nRegistryProvider,
            ObjectProvider<PluginMigrationExecutor> migrationExecutorProvider,
            ObjectProvider<DataSource> dataSourceProvider
    ) {
        PluginMigrationExecutor migrationExecutor = migrationExecutorProvider.getIfAvailable();
        if (migrationExecutor == null) {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            migrationExecutor = dataSource == null
                    ? PluginMigrationExecutor.noop()
                    : new FlywayPluginMigrationExecutor(dataSource);
        }
        PluginActivationGuard activationGuard = descriptor -> activationGuardProvider.orderedStream()
                .forEach(guard -> guard.requireCanActivate(descriptor));
        PluginBeanContainerFactory beanContainerFactory = (descriptor, classLoader, dependencies, resolver) ->
                beanContainerFactoryProvider
                        .getIfAvailable(PluginBeanContainerFactory::resolverBacked)
                        .create(descriptor, classLoader, dependencies, resolver);
        return new DefaultPluginRuntimeService(
                pluginBootLoader,
                NoopPluginRegistries.hooks(),
                NoopPluginRegistries.routes(),
                NoopPluginRegistries.assets(),
                pluginBeanResolver,
                contributionRegistries,
                migrationExecutor,
                i18nRegistryProvider.getIfAvailable(PluginI18nRegistry::noop),
                activationGuard,
                beanContainerFactory,
                pluginClassLoaderRegistry,
                Clock.systemUTC());
    }
}
