package com.jnimble.starter.plugin.runtime;

import com.jnimble.kernel.plugin.PluginBeanContainer;
import com.jnimble.kernel.plugin.PluginBeanContainerFactory;
import com.jnimble.kernel.plugin.PluginBeanResolver;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.jnimble.kernel.plugin.PluginRuntimeException;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSpringDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.aop.support.AopUtils;

/**
 * Creates one Spring child context per enabled plugin.
 */
@Component
public class SpringPluginBeanContainerFactory implements PluginBeanContainerFactory {

    private static final String DEPENDENCY_PREFIX = "pluginDependency$";

    private final ApplicationContext platformContext;
    private final PluginMvcEndpointRegistrar mvcRegistrar;
    private final DataSource dataSource;
    private final SqlSessionFactory platformSqlSessionFactory;

    public SpringPluginBeanContainerFactory(
            ApplicationContext platformContext,
            PluginMvcEndpointRegistrar mvcRegistrar
    ) {
        this.platformContext = platformContext;
        this.mvcRegistrar = mvcRegistrar;
        this.dataSource = platformContext.getBeanProvider(DataSource.class).getIfAvailable();
        this.platformSqlSessionFactory =
                platformContext.getBeanProvider(SqlSessionFactory.class).getIfAvailable();
    }

    @Override
    public PluginBeanContainer create(
            PluginDescriptor descriptor,
            ClassLoader classLoader,
            Map<String, PluginBeanContainer> dependencies,
            PluginBeanResolver platformResolver
    ) {
        PluginSpringDescriptor spring = descriptor.spring();
        if (spring == null || spring.configurationClass() == null || spring.configurationClass().isBlank()) {
            return new DependencyOnlyBeanContainer(dependencies, platformResolver);
        }

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setParent(platformContext);
        context.setClassLoader(classLoader);
        context.getBeanFactory().setBeanClassLoader(classLoader);
        registerDependencyBeans(context, dependencies);
        if (dataSource != null) {
            // Isolate plugin MyBatis state per class loader: mapper/entity classes from a previous
            // incarnation must not leak across hot reloads. Register both a plugin-scoped
            // SqlSessionFactory and SqlSessionTemplate so mapper beans use them instead of the
            // shared (platform) ones.
            context.registerBean("sqlSessionFactory", SqlSessionFactory.class,
                    this::createPluginSqlSessionFactory,
                    beanDefinition -> beanDefinition.setPrimary(true));
            context.registerBean("sqlSessionTemplate", SqlSessionTemplate.class,
                    () -> new SqlSessionTemplate(context.getBean(SqlSessionFactory.class)),
                    beanDefinition -> beanDefinition.setPrimary(true));
        }
        try {
            Class<?> configurationClass = Class.forName(
                    spring.configurationClass().trim(),
                    true,
                    classLoader);
            context.register(configurationClass);
            context.refresh();
            return new SpringPluginBeanContainer(descriptor.id(), context, mvcRegistrar);
        } catch (RuntimeException | ClassNotFoundException ex) {
            context.close();
            throw new PluginRuntimeException(
                    "Failed to create Spring container for plugin " + descriptor.id(),
                    ex);
        }
    }

    private SqlSessionFactory createPluginSqlSessionFactory() {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        if (platformSqlSessionFactory != null) {
            org.apache.ibatis.session.Configuration platform =
                    platformSqlSessionFactory.getConfiguration();
            configuration.setMapUnderscoreToCamelCase(platform.isMapUnderscoreToCamelCase());
            if (!platform.getInterceptors().isEmpty()) {
                factoryBean.setPlugins(platform.getInterceptors().toArray(new Interceptor[0]));
            }
        }
        factoryBean.setConfiguration(configuration);
        try {
            return factoryBean.getObject();
        } catch (Exception ex) {
            throw new PluginRuntimeException(
                    "Failed to create plugin MyBatis SqlSessionFactory", ex);
        }
    }

    private void registerDependencyBeans(
            AnnotationConfigApplicationContext context,
            Map<String, PluginBeanContainer> dependencies
    ) {
        dependencies.forEach((pluginId, container) -> container.exportedBeans().forEach((beanName, bean) -> {
            String externalName = DEPENDENCY_PREFIX
                    + pluginId.replace('-', '_')
                    + "$"
                    + beanName;
            context.getBeanFactory().registerSingleton(externalName, bean);
        }));
    }

    private static final class SpringPluginBeanContainer implements PluginBeanContainer {

        private final String pluginId;
        private final AnnotationConfigApplicationContext context;
        private final PluginMvcEndpointRegistrar mvcRegistrar;
        private PluginMvcEndpointRegistrar.PluginMvcRegistration mvcRegistration;

        private SpringPluginBeanContainer(
                String pluginId,
                AnnotationConfigApplicationContext context,
                PluginMvcEndpointRegistrar mvcRegistrar
        ) {
            this.pluginId = pluginId;
            this.context = context;
            this.mvcRegistrar = mvcRegistrar;
        }

        @Override
        public <T> T resolve(Class<T> type) {
            return context.getBean(type);
        }

        @Override
        public Map<String, Object> exportedBeans() {
            Map<String, Object> exports = new LinkedHashMap<>();
            for (String beanName : context.getBeanFactory().getSingletonNames()) {
                if (beanName.startsWith(DEPENDENCY_PREFIX)
                        || beanName.startsWith("org.springframework.")) {
                    continue;
                }
                Object bean = context.getBeanFactory().getSingleton(beanName);
                if (bean == null) {
                    continue;
                }
                Class<?> beanType = AopUtils.getTargetClass(bean);
                if (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class)
                        || AnnotatedElementUtils.hasAnnotation(beanType, ControllerAdvice.class)
                        || AnnotatedElementUtils.hasAnnotation(beanType, Configuration.class)) {
                    continue;
                }
                exports.put(beanName, bean);
            }
            return Map.copyOf(exports);
        }

        @Override
        public void activate() {
            mvcRegistration = mvcRegistrar.register(pluginId, context);
        }

        @Override
        public void deactivate() {
            if (mvcRegistration != null) {
                mvcRegistration.deactivate();
                mvcRegistration = null;
            }
        }

        @Override
        public void close() {
            context.close();
        }
    }

    private static final class DependencyOnlyBeanContainer implements PluginBeanContainer {

        private final Map<String, PluginBeanContainer> dependencies;
        private final PluginBeanResolver platformResolver;

        private DependencyOnlyBeanContainer(
                Map<String, PluginBeanContainer> dependencies,
                PluginBeanResolver platformResolver
        ) {
            this.dependencies = Map.copyOf(dependencies);
            this.platformResolver = Objects.requireNonNull(platformResolver, "platformResolver");
        }

        @Override
        public <T> T resolve(Class<T> type) {
            Object resolved = null;
            for (PluginBeanContainer dependency : dependencies.values()) {
                for (Object candidate : dependency.exportedBeans().values()) {
                    if (type.isInstance(candidate)) {
                        if (resolved != null && resolved != candidate) {
                            throw new PluginRuntimeException("Multiple plugin dependency beans match " + type.getName());
                        }
                        resolved = candidate;
                    }
                }
            }
            return resolved == null ? platformResolver.resolve(type) : type.cast(resolved);
        }
    }
}
