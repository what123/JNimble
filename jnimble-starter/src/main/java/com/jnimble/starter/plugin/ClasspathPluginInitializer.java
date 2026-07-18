package com.jnimble.starter.plugin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorLoader;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorValidator;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Application runner that discovers and installs plugins from the classpath.
 * Scans for plugin descriptor files in {@code META-INF/} directories and
 * installs them as classpath-based plugins.
 */
@Component
@Order(20)
@EnableConfigurationProperties(PluginDiscoveryProperties.class)
public class ClasspathPluginInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClasspathPluginInitializer.class);
    private static final String DESCRIPTOR_PATTERN = "classpath*:" + PluginDescriptorLoader.META_INF_DESCRIPTOR_PATH;

    private final PluginDiscoveryProperties properties;
    private final PluginRuntimeService pluginRuntimeService;
    private final ObjectMapper objectMapper;
    private final Set<String> enableAfterInstall = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> classpathCandidates = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ClasspathPluginInitializer(
            PluginDiscoveryProperties properties,
            PluginRuntimeService pluginRuntimeService
    ) {
        this.properties = properties;
        this.pluginRuntimeService = pluginRuntimeService;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isDevClasspathEnabled()) {
            return;
        }
        Map<String, PluginDescriptor> descriptors = discoverClasspathPlugins();
        if (properties.isAutoEnable()) {
            enableAfterInstall.addAll(descriptors.keySet());
        }
        descriptors.values().forEach(this::installDiscoveredPlugin);
        PluginDependencyOrder.sort(descriptors.values()).stream()
                .filter(descriptor -> enableAfterInstall.contains(descriptor.id()))
                .forEach(this::enableDiscoveredPlugin);
    }

    private Map<String, PluginDescriptor> discoverClasspathPlugins() {
        Map<String, PluginDescriptor> descriptors = new LinkedHashMap<>();
        for (Resource resource : classpathPluginDescriptors()) {
            try {
                PluginDescriptor descriptor = loadDescriptor(resource);
                if (descriptors.containsKey(descriptor.id())) {
                    handleFailure("Duplicate classpath plugin descriptor: " + descriptor.id(), null);
                    continue;
                }
                descriptors.put(descriptor.id(), descriptor);
            } catch (RuntimeException ex) {
                handleFailure("Skipped classpath plugin descriptor " + resource.getDescription(), ex);
            }
        }
        return descriptors;
    }

    private Resource[] classpathPluginDescriptors() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(DESCRIPTOR_PATTERN);
        } catch (IOException ex) {
            handleFailure("Failed to scan classpath plugin descriptors", ex);
            return new Resource[0];
        }
    }

    private PluginDescriptor loadDescriptor(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            PluginDescriptor descriptor = objectMapper.readValue(inputStream, PluginDescriptor.class);
            new PluginDescriptorValidator(properties.getPlatformVersion()).validate(descriptor);
            return descriptor;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load plugin descriptor from " + resource.getDescription(), ex);
        }
    }

    private void installDiscoveredPlugin(PluginDescriptor descriptor) {
        try {
            PluginRuntimeSnapshot existing = pluginRuntimeService.find(descriptor.id()).orElse(null);
            if (existing != null) {
                refreshClasspathPlugin(existing, descriptor);
                return;
            }
            pluginRuntimeService.install(descriptor);
            classpathCandidates.add(descriptor.id());
        } catch (RuntimeException ex) {
            handleFailure("Failed to initialize plugin " + descriptor.id(), ex);
        }
    }

    private void enableDiscoveredPlugin(PluginDescriptor descriptor) {
        try {
            if (classpathCandidates.contains(descriptor.id())) {
                pluginRuntimeService.enable(descriptor.id());
            }
        } catch (RuntimeException ex) {
            handleFailure("Failed to enable plugin " + descriptor.id(), ex);
        }
    }

    private void refreshClasspathPlugin(PluginRuntimeSnapshot existing, PluginDescriptor discovered) {
        classpathCandidates.add(discovered.id());
        boolean wasEnabled = existing.status() == PluginStatus.ENABLED;
        if (wasEnabled) {
            pluginRuntimeService.disable(discovered.id());
            enableAfterInstall.add(discovered.id());
        }
        if (existing.source() != PluginSource.CLASSPATH) {
            pluginRuntimeService.replace(discovered, null);
            log.info("Replaced plugin with development classpath plugin: {}", discovered.id());
            return;
        }
        if (discovered.equals(existing.descriptor())) {
            log.debug("Classpath plugin descriptor is unchanged: {}", discovered.id());
            return;
        }
        pluginRuntimeService.replace(discovered, null);
        log.info("Refreshed changed classpath plugin descriptor: {}", discovered.id());
    }

    private void handleFailure(String message, Exception failure) {
        if (properties.isFailFast()) {
            if (failure == null) {
                throw new IllegalStateException(message);
            }
            throw new IllegalStateException(message, failure);
        }
        if (failure == null) {
            log.warn(message);
        } else {
            log.warn(message, failure);
        }
    }
}
