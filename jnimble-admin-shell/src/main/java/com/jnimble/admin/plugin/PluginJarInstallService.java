package com.jnimble.admin.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.plugin.PluginOperationLocks;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorLoader;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorValidator;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service for installing and replacing plugins via JAR file uploads.
 * Handles JAR validation, storage, and plugin lifecycle management.
 */
@Service
@EnableConfigurationProperties(PluginInstallProperties.class)
public class PluginJarInstallService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/java-archive",
            "application/x-java-archive",
            "application/octet-stream"
    );

    private final PluginRuntimeService pluginRuntimeService;
    private final PluginInstallProperties properties;

    public PluginJarInstallService(
            PluginRuntimeService pluginRuntimeService,
            PluginInstallProperties properties
    ) {
        this.pluginRuntimeService = pluginRuntimeService;
        this.properties = properties;
    }

    /**
     * Installs a new plugin from a JAR file upload.
     *
     * @param jar the uploaded JAR file containing the plugin
     * @return the installation result with descriptor and stored path
     * @throws IllegalArgumentException if the JAR is invalid or plugin is already installed
     */
    public PluginJarInstallResult install(MultipartFile jar) {
        requireJar(jar);
        Path stagedPath = stageJar(jar);
        PluginDescriptor descriptor = loadDescriptor(stagedPath);
        synchronized (PluginOperationLocks.lockFor(descriptor.id())) {
            requirePluginNotInstalled(stagedPath, descriptor);
            Path storedPath = moveIntoPluginDirectory(stagedPath, descriptor);
            try {
                pluginRuntimeService.install(descriptor, storedPath);
                return new PluginJarInstallResult(descriptor, storedPath, false);
            } catch (RuntimeException ex) {
                deleteQuietly(storedPath);
                throw ex;
            }
        }
    }

    /**
     * Replaces an existing plugin with a new JAR file upload.
     *
     * @param pluginId the ID of the plugin to replace
     * @param jar      the uploaded JAR file containing the new plugin version
     * @return the installation result with descriptor and stored path
     * @throws IllegalArgumentException if the JAR is invalid, plugin not found, or plugin is enabled
     */
    public PluginJarInstallResult replace(String pluginId, MultipartFile jar) {
        String normalizedPluginId = requirePluginId(pluginId);
        requireJar(jar);
        Path stagedPath = stageJar(jar);
        PluginDescriptor descriptor = loadDescriptor(stagedPath);
        requireMatchingPluginId(stagedPath, normalizedPluginId, descriptor);
        synchronized (PluginOperationLocks.lockFor(descriptor.id())) {
            PluginRuntimeSnapshot existing = requireReplaceablePlugin(stagedPath, descriptor);
            Path storedPath = moveIntoPluginDirectory(stagedPath, descriptor);
            try {
                pluginRuntimeService.replace(descriptor, storedPath);
                deletePreviousArtifact(existing.artifactPath(), storedPath);
                return new PluginJarInstallResult(descriptor, storedPath, true);
            } catch (RuntimeException ex) {
                deleteQuietly(storedPath);
                throw ex;
            }
        }
    }

    private void requireJar(MultipartFile jar) {
        if (jar == null || jar.isEmpty()) {
            throw new IllegalArgumentException("Plugin jar is required.");
        }
        String filename = StringUtils.cleanPath(defaultString(jar.getOriginalFilename()));
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IllegalArgumentException("Only .jar plugin packages are supported.");
        }
        String contentType = jar.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported plugin package content type: " + contentType);
        }
    }

    private Path stageJar(MultipartFile jar) {
        try {
            Path tempPath = Files.createTempFile("jnimble-plugin-", ".jar");
            try (InputStream inputStream = jar.getInputStream()) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempPath;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to stage plugin jar.", ex);
        }
    }

    private Path moveIntoPluginDirectory(Path stagedPath, PluginDescriptor descriptor) {
        Path pluginDirectory = Path.of(properties.getDir()).toAbsolutePath().normalize();
        Path storedPath = uniqueStoredPath(pluginDirectory, descriptor);
        if (!storedPath.startsWith(pluginDirectory)) {
            deleteQuietly(stagedPath);
            throw new IllegalArgumentException("Invalid plugin package target path.");
        }

        try {
            Files.createDirectories(pluginDirectory);
            Files.move(stagedPath, storedPath);
            return storedPath;
        } catch (IOException ex) {
            deleteQuietly(stagedPath);
            throw new IllegalStateException("Failed to store plugin jar.", ex);
        }
    }

    private PluginDescriptor loadDescriptor(Path stagedPath) {
        try {
            return descriptorLoader().loadJar(stagedPath);
        } catch (RuntimeException ex) {
            deleteQuietly(stagedPath);
            throw ex;
        }
    }

    private void requirePluginNotInstalled(Path stagedPath, PluginDescriptor descriptor) {
        boolean installed = pluginRuntimeService.find(descriptor.id())
                .filter(snapshot -> snapshot.status() != PluginStatus.UNINSTALLED)
                .isPresent();
        if (installed) {
            deleteQuietly(stagedPath);
            throw new IllegalArgumentException("Plugin is already installed: " + descriptor.id());
        }
    }

    private PluginRuntimeSnapshot requireReplaceablePlugin(Path stagedPath, PluginDescriptor descriptor) {
        PluginRuntimeSnapshot existing = pluginRuntimeService.find(descriptor.id())
                .filter(snapshot -> snapshot.status() != PluginStatus.UNINSTALLED)
                .orElse(null);
        if (existing == null) {
            deleteQuietly(stagedPath);
            throw new IllegalArgumentException("Plugin is not installed: " + descriptor.id());
        }
        if (existing.status() == PluginStatus.ENABLED) {
            deleteQuietly(stagedPath);
            throw new IllegalArgumentException("Disable plugin before replacing package: " + descriptor.id());
        }
        return existing;
    }

    private void requireMatchingPluginId(Path stagedPath, String pluginId, PluginDescriptor descriptor) {
        if (!pluginId.equals(descriptor.id())) {
            deleteQuietly(stagedPath);
            throw new IllegalArgumentException("Plugin package id does not match target plugin: " + descriptor.id());
        }
    }

    private String requirePluginId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id is required.");
        }
        return pluginId.trim();
    }

    private Path uniqueStoredPath(Path pluginDirectory, PluginDescriptor descriptor) {
        Path preferredPath = pluginDirectory.resolve(descriptor.id() + "-" + descriptor.version() + ".jar").normalize();
        if (!Files.exists(preferredPath)) {
            return preferredPath;
        }
        return pluginDirectory.resolve(descriptor.id() + "-" + descriptor.version() + "-" + UUID.randomUUID() + ".jar")
                .normalize();
    }

    private void deletePreviousArtifact(Path previousPath, Path currentPath) {
        if (previousPath == null || currentPath == null) {
            return;
        }
        Path normalizedPrevious = previousPath.toAbsolutePath().normalize();
        Path normalizedCurrent = currentPath.toAbsolutePath().normalize();
        if (normalizedPrevious.equals(normalizedCurrent)) {
            return;
        }
        deleteQuietly(normalizedPrevious);
    }

    private PluginDescriptorLoader descriptorLoader() {
        return new PluginDescriptorLoader(new PluginDescriptorValidator(properties.getPlatformVersion()));
    }

    private String defaultString(String value) {
        return value == null ? UUID.randomUUID() + ".jar" : value;
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
