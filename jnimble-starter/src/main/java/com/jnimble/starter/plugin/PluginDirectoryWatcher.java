package com.jnimble.starter.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.kernel.plugin.PluginOperationLocks;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorLoader;
import com.jnimble.kernel.plugin.descriptor.PluginDescriptorValidator;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Watches the external plugin directory and applies new or replaced JARs
 * without restarting the application.
 */
@Component
public class PluginDirectoryWatcher implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PluginDirectoryWatcher.class);
    private static final String CACHE_DIRECTORY = ".runtime-cache";

    private final PluginDiscoveryProperties properties;
    private final PluginRuntimeService runtimeService;
    private final Map<Path, String> fingerprints = new ConcurrentHashMap<>();
    private final Map<Path, Path> cachedArtifacts = new ConcurrentHashMap<>();
    private final Map<Path, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService processor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "jnimble-plugin-watcher-processor");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;
    private volatile WatchService watchService;
    private Thread watcherThread;

    public PluginDirectoryWatcher(
            PluginDiscoveryProperties properties,
            PluginRuntimeService runtimeService
    ) {
        this.properties = properties;
        this.runtimeService = runtimeService;
    }

    @Override
    public synchronized void start() {
        if (running || !properties.isDirectoryWatchEnabled()) {
            return;
        }
        Path directory = pluginDirectory();
        try {
            Files.createDirectories(directory);
            Files.createDirectories(cacheDirectory());
            initializeKnownArtifacts(directory);
            watchService = directory.getFileSystem().newWatchService();
            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            running = true;
            watcherThread = new Thread(this::watchLoop, "jnimble-plugin-directory-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
            log.info("Watching plugin directory for hot deployment: {}", directory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to watch plugin directory " + directory, ex);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        pending.values().forEach(future -> future.cancel(false));
        pending.clear();
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ex) {
                log.debug("Failed to close plugin watch service", ex);
            }
        }
        processor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    handleEvent(event);
                }
                if (!key.reset()) {
                    log.warn("Plugin directory watch key is no longer valid: {}", pluginDirectory());
                    running = false;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (java.nio.file.ClosedWatchServiceException ex) {
                return;
            } catch (RuntimeException ex) {
                log.warn("Plugin directory watcher loop failed", ex);
            }
        }
    }

    private void handleEvent(WatchEvent<?> event) {
        if (!(event.context() instanceof Path relativePath)) {
            return;
        }
        Path artifact = pluginDirectory().resolve(relativePath).toAbsolutePath().normalize();
        if (!isJar(artifact)) {
            return;
        }
        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            log.warn("Plugin artifact was deleted; running plugin is kept until explicitly disabled: {}", artifact);
            fingerprints.remove(artifact);
            return;
        }
        ScheduledFuture<?> previous = pending.remove(artifact);
        if (previous != null) {
            previous.cancel(false);
        }
        ScheduledFuture<?> scheduled = processor.schedule(
                () -> processStableArtifact(artifact),
                properties.getWatchSettleDelayMillis(),
                TimeUnit.MILLISECONDS);
        pending.put(artifact, scheduled);
    }

    private void processStableArtifact(Path artifact) {
        pending.remove(artifact);
        try {
            if (!Files.isRegularFile(artifact)) {
                return;
            }
            String fingerprint = fingerprint(artifact);
            String previousFingerprint = fingerprints.get(artifact);
            if (fingerprint.equals(previousFingerprint)) {
                return;
            }
            PluginDescriptor descriptor = descriptorLoader().loadJar(artifact);
            synchronized (PluginOperationLocks.lockFor(descriptor.id())) {
                PluginRuntimeSnapshot existing = runtimeService.find(descriptor.id()).orElse(null);
                if (existing == null || existing.status() == PluginStatus.UNINSTALLED) {
                    Path staged = stage(artifact, fingerprint);
                    runtimeService.install(descriptor, staged);
                    if (properties.isAutoEnable()) {
                        runtimeService.enable(descriptor.id());
                    }
                    cachedArtifacts.put(artifact, staged);
                    fingerprints.put(artifact, fingerprint);
                    log.info("Hot-installed plugin {} from {}", descriptor.id(), artifact);
                    return;
                }

                Path existingArtifact = existing.artifactPath() == null
                        ? null
                        : existing.artifactPath().toAbsolutePath().normalize();
                if (previousFingerprint == null && artifact.equals(existingArtifact)) {
                    cachedArtifacts.put(artifact, stage(artifact, fingerprint));
                    fingerprints.put(artifact, fingerprint);
                    return;
                }
                replaceWithRollback(artifact, descriptor, fingerprint, existing);
            }
        } catch (RuntimeException | IOException ex) {
            log.warn("Failed to hot-deploy plugin artifact {}", artifact, ex);
        }
    }

    private void replaceWithRollback(
            Path sourceArtifact,
            PluginDescriptor replacement,
            String replacementFingerprint,
            PluginRuntimeSnapshot previous
    ) throws IOException {
        Path replacementArtifact = stage(sourceArtifact, replacementFingerprint);
        Path rollbackArtifact = cachedArtifacts.get(sourceArtifact);
        if (rollbackArtifact == null && previous.artifactPath() != null) {
            rollbackArtifact = previous.artifactPath();
        }
        boolean wasEnabled = previous.status() == PluginStatus.ENABLED;
        try {
            if (wasEnabled) {
                runtimeService.disable(previous.pluginId());
            }
            runtimeService.replace(replacement, replacementArtifact);
            if (wasEnabled || properties.isAutoEnable()) {
                runtimeService.enable(replacement.id());
            }
            cachedArtifacts.put(sourceArtifact, replacementArtifact);
            fingerprints.put(sourceArtifact, replacementFingerprint);
            log.info("Hot-replaced plugin {} from {}", replacement.id(), sourceArtifact);
        } catch (RuntimeException replacementFailure) {
            rollback(previous, rollbackArtifact, wasEnabled, replacementFailure);
            throw replacementFailure;
        }
    }

    private void rollback(
            PluginRuntimeSnapshot previous,
            Path rollbackArtifact,
            boolean wasEnabled,
            RuntimeException replacementFailure
    ) {
        try {
            PluginRuntimeSnapshot current = runtimeService.find(previous.pluginId()).orElse(null);
            if (current != null && current.status() == PluginStatus.ENABLED) {
                runtimeService.disable(previous.pluginId());
            }
            runtimeService.replace(previous.descriptor(), rollbackArtifact);
            if (wasEnabled) {
                runtimeService.enable(previous.pluginId());
            }
            log.warn("Rolled back failed plugin replacement: {}", previous.pluginId());
        } catch (RuntimeException rollbackFailure) {
            replacementFailure.addSuppressed(rollbackFailure);
        }
    }

    private void initializeKnownArtifacts(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            for (Path artifact : paths.filter(this::isJar).toList()) {
                String fingerprint = fingerprint(artifact);
                fingerprints.put(artifact.toAbsolutePath().normalize(), fingerprint);
                cachedArtifacts.put(artifact.toAbsolutePath().normalize(), stage(artifact, fingerprint));
            }
        }
    }

    private Path stage(Path source, String fingerprint) throws IOException {
        Path sourceName = source.getFileName();
        String fileName = sourceName != null ? sourceName.toString() : "plugin";
        int extension = fileName.toLowerCase(java.util.Locale.ROOT).lastIndexOf(".jar");
        String baseName = extension < 0 ? fileName : fileName.substring(0, extension);
        Path staged = cacheDirectory().resolve(baseName + "-" + fingerprint.substring(0, 16) + ".jar");
        if (!Files.isRegularFile(staged)) {
            Path temporary = Files.createTempFile(cacheDirectory(), baseName + "-", ".part");
            try {
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporary, staged, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return staged.toAbsolutePath().normalize();
    }

    private String fingerprint(Path artifact) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(artifact)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private PluginDescriptorLoader descriptorLoader() {
        return new PluginDescriptorLoader(new PluginDescriptorValidator(properties.getPlatformVersion()));
    }

    private Path pluginDirectory() {
        return Path.of(properties.getDir()).toAbsolutePath().normalize();
    }

    private Path cacheDirectory() {
        return pluginDirectory().resolve(CACHE_DIRECTORY);
    }

    private boolean isJar(Path path) {
        if (path == null) {
            return false;
        }
        Path fileName = path.getFileName();
        return fileName != null
                && fileName.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar");
    }
}
