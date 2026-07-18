package com.jnimble.starter.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorLoader;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorValidator;
import com.jnimble.sdk.plugin.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Application runner that scans a plugin directory for JAR files and installs them.
 * Supports automatic enabling of discovered plugins based on configuration.
 */
@Component
@Order(15)
@EnableConfigurationProperties(PluginDiscoveryProperties.class)
public class PluginDirectoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PluginDirectoryInitializer.class);

    private final PluginDiscoveryProperties properties;
    private final PluginRuntimeService pluginRuntimeService;

    public PluginDirectoryInitializer(
            PluginDiscoveryProperties properties,
            PluginRuntimeService pluginRuntimeService
    ) {
        this.properties = properties;
        this.pluginRuntimeService = pluginRuntimeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isDirectoryScanEnabled()) {
            return;
        }
        Path pluginDirectory = Path.of(properties.getDir()).toAbsolutePath().normalize();
        if (!Files.isDirectory(pluginDirectory)) {
            log.debug("Plugin directory does not exist, skipped: {}", pluginDirectory);
            return;
        }
        try (Stream<Path> jars = Files.list(pluginDirectory)) {
            List<Path> jarPaths = jars.filter(this::isJar)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            Map<String, PluginDescriptor> installed = new LinkedHashMap<>();
            for (Path jarPath : jarPaths) {
                PluginDescriptor descriptor = installJar(jarPath);
                if (descriptor != null) {
                    installed.put(descriptor.id(), descriptor);
                }
            }
            if (properties.isAutoEnable()) {
                PluginDependencyOrder.sort(installed.values())
                        .forEach(this::enableJar);
            }
        } catch (IOException ex) {
            handleFailure("Failed to scan plugin directory " + pluginDirectory, ex);
        }
    }

    private PluginDescriptor installJar(Path jarPath) {
        Path normalizedPath = jarPath.toAbsolutePath().normalize();
        try {
            PluginDescriptor descriptor = descriptorLoader().loadJar(normalizedPath);
            if (pluginRuntimeService.find(descriptor.id()).isPresent()) {
                log.debug("Plugin already restored or installed, directory jar skipped: {}", descriptor.id());
                return null;
            }
            pluginRuntimeService.install(descriptor, normalizedPath);
            return descriptor;
        } catch (RuntimeException ex) {
            handleFailure("Failed to install plugin jar " + normalizedPath, ex);
            return null;
        }
    }

    private void enableJar(PluginDescriptor descriptor) {
        try {
            pluginRuntimeService.enable(descriptor.id());
        } catch (RuntimeException ex) {
            handleFailure("Failed to enable plugin jar " + descriptor.id(), ex);
        }
    }

    private boolean isJar(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar");
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
