package com.jnimble.starter.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorLoader;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorValidator;
import com.jnimble.platform.plugin.PluginStateRecord;
import com.jnimble.platform.plugin.PluginStateStore;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;

/**
 * Application runner that restores previously persisted plugin states on startup.
 * Loads plugin descriptors from the state store and re-installs/enables plugins.
 */
@Component
@Order(10)
@EnableConfigurationProperties(PluginDiscoveryProperties.class)
public class PersistedPluginInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PersistedPluginInitializer.class);

    private final PluginDiscoveryProperties properties;
    private final PluginStateStore pluginStateStore;
    private final PluginRuntimeService pluginRuntimeService;

    public PersistedPluginInitializer(
            PluginDiscoveryProperties properties,
            PluginStateStore pluginStateStore,
            PluginRuntimeService pluginRuntimeService
    ) {
        this.properties = properties;
        this.pluginStateStore = pluginStateStore;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isRestoreEnabled()) {
            return;
        }
        Collection<PluginStateRecord> records = pluginStateStore.list();
        Map<String, PluginDescriptor> enableAfterRestore = new LinkedHashMap<>();
        for (PluginStateRecord record : records) {
            PluginDescriptor descriptor = restorePlugin(record);
            if (descriptor != null && record.enabled()) {
                enableAfterRestore.put(descriptor.id(), descriptor);
            }
        }
        PluginDependencyOrder.sort(enableAfterRestore.values()).forEach(this::enableRestoredPlugin);
    }

    private PluginDescriptor restorePlugin(PluginStateRecord record) {
        if (record.status() == PluginStatus.UNINSTALLED) {
            return null;
        }
        try {
            PluginDescriptor descriptor;
            if (record.source() == PluginSource.JAR) {
                descriptor = restoreJarPlugin(record);
            } else {
                descriptor = restoreNonJarPlugin(record);
            }
            return descriptor;
        } catch (RuntimeException ex) {
            handleFailure("Failed to restore plugin " + record.pluginId(), ex);
            return null;
        }
    }

    private PluginDescriptor restoreJarPlugin(PluginStateRecord record) {
        if (record.artifactPath() == null || record.artifactPath().isBlank()) {
            throw new IllegalStateException("Persisted jar plugin has no artifact path: " + record.pluginId());
        }
        Path artifactPath = Path.of(record.artifactPath()).toAbsolutePath().normalize();
        PluginDescriptor descriptor = descriptorLoader().loadJar(artifactPath);
        pluginRuntimeService.install(descriptor, artifactPath);
        return descriptor;
    }

    private PluginDescriptor restoreNonJarPlugin(PluginStateRecord record) {
        PluginDescriptor descriptor = descriptorLoader().loadJson(record.descriptorJson());
        if (!properties.isDevClasspathEnabled() && !classpathBootAvailable(descriptor)) {
            log.info(
                    "Classpath plugin {} is not present in this distribution; persisted state skipped",
                    descriptor.id());
            return null;
        }
        pluginRuntimeService.install(descriptor);
        return descriptor;
    }

    private boolean classpathBootAvailable(PluginDescriptor descriptor) {
        if (descriptor.bootClass() == null || descriptor.bootClass().isBlank()) {
            return false;
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(descriptor.bootClass(), false, classLoader);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private void enableRestoredPlugin(PluginDescriptor descriptor) {
        try {
            pluginRuntimeService.enable(descriptor.id());
        } catch (RuntimeException ex) {
            handleFailure("Failed to enable restored plugin " + descriptor.id(), ex);
        }
    }

    private PluginDescriptorLoader descriptorLoader() {
        return new PluginDescriptorLoader(new PluginDescriptorValidator(properties.getPlatformVersion()));
    }

    private void handleFailure(String message, Exception failure) {
        if (properties.isFailFast()) {
            throw new IllegalStateException(message, failure);
        }
        log.warn(message, failure);
    }
}
