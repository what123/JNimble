package com.jnimble.kernel.resource;

import com.jnimble.kernel.plugin.PluginIds;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.hook.RegistrationType;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of {@link PluginAssetRegistry} that stores asset registrations
 * in a HashMap. Handles asset conflict detection and plugin enable/disable states.
 */
@Component
public class InMemoryPluginAssetRegistry implements PluginAssetRegistry {

    private final Map<String, AssetEntry> assets = new HashMap<>();
    private final Set<String> disabledPlugins = new HashSet<>();

    @Override
    public synchronized AssetRegistry scoped(String pluginId) {
        requirePluginId(pluginId);
        return asset -> register(pluginId, asset);
    }

    @Override
    public synchronized RegistrationHandle register(String pluginId, AssetDefinition asset) {
        requirePluginId(pluginId);
        Objects.requireNonNull(asset, "asset must not be null");
        String fullRequestPath = assetPath(pluginId, asset.requestPath());
        AssetEntry existing = assets.get(fullRequestPath);
        if (existing != null) {
            throw new PluginAssetConflictException(
                    "Asset conflict for " + fullRequestPath
                            + " between plugins " + existing.pluginId + " and " + pluginId);
        }

        String registrationId = "asset:" + UUID.randomUUID();
        assets.put(fullRequestPath, new AssetEntry(registrationId, pluginId, fullRequestPath, asset));
        return new AssetRegistrationHandle(registrationId, fullRequestPath);
    }

    @Override
    public synchronized void enablePlugin(String pluginId) {
        requirePluginId(pluginId);
        disabledPlugins.remove(pluginId);
    }

    @Override
    public synchronized void disablePlugin(String pluginId) {
        requirePluginId(pluginId);
        disabledPlugins.add(pluginId);
    }

    @Override
    public synchronized PluginAssetAvailability availability(String requestPath) {
        AssetEntry asset = findEntry(requestPath).orElse(null);
        if (asset == null) {
            return PluginAssetAvailability.NOT_FOUND;
        }
        if (disabledPlugins.contains(asset.pluginId)) {
            return PluginAssetAvailability.PLUGIN_DISABLED;
        }
        return PluginAssetAvailability.AVAILABLE;
    }

    @Override
    public synchronized Optional<RegisteredPluginAsset> find(String requestPath) {
        return findEntry(requestPath).map(this::snapshot);
    }

    @Override
    public synchronized List<RegisteredPluginAsset> assets() {
        return assets.values().stream()
                .map(this::snapshot)
                .toList();
    }

    @Override
    public synchronized List<RegisteredPluginAsset> assets(String pluginId) {
        requirePluginId(pluginId);
        return assets.values().stream()
                .filter(asset -> asset.pluginId.equals(pluginId))
                .map(this::snapshot)
                .toList();
    }

    private synchronized void unregister(String registrationId, String fullRequestPath) {
        AssetEntry current = assets.get(fullRequestPath);
        if (current != null && current.registrationId.equals(registrationId)) {
            assets.remove(fullRequestPath);
        }
    }

    private RegisteredPluginAsset snapshot(AssetEntry asset) {
        return new RegisteredPluginAsset(
                asset.registrationId,
                asset.pluginId,
                asset.fullRequestPath,
                asset.definition,
                !disabledPlugins.contains(asset.pluginId));
    }

    private Optional<AssetEntry> findEntry(String requestPath) {
        String normalizedPath = normalizeAbsolutePath(requestPath);
        AssetEntry exact = assets.get(normalizedPath);
        if (exact != null) {
            return Optional.of(exact);
        }
        return assets.values().stream()
                .filter(asset -> normalizedPath.startsWith(directoryPrefix(asset.fullRequestPath)))
                .max((left, right) -> Integer.compare(left.fullRequestPath.length(), right.fullRequestPath.length()));
    }

    private static String assetPath(String pluginId, String relativePath) {
        return ASSET_NAMESPACE_PREFIX + "/" + pluginId + normalizeRelativePath(relativePath);
    }

    private static String normalizeAbsolutePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String normalizeRelativePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "/";
        }
        String normalized = path.trim().replace('\\', '/');
        if (hasParentDirectorySegment(normalized)) {
            throw new IllegalArgumentException("path must not contain parent directory segments");
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static boolean hasParentDirectorySegment(String path) {
        String[] segments = path.split("/");
        for (String segment : segments) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static String directoryPrefix(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private static void requirePluginId(String pluginId) {
        PluginIds.requireValid(pluginId);
    }

    private static final class AssetEntry {

        private final String registrationId;
        private final String pluginId;
        private final String fullRequestPath;
        private final AssetDefinition definition;

        private AssetEntry(
                String registrationId,
                String pluginId,
                String fullRequestPath,
                AssetDefinition definition) {
            this.registrationId = registrationId;
            this.pluginId = pluginId;
            this.fullRequestPath = fullRequestPath;
            this.definition = definition;
        }
    }

    /**
     * Registration handle for a plugin asset.
     *
     * <p>Removes the asset from the registry upon unregistration.
     * Safe to call multiple times; subsequent calls are no-ops.</p>
     */
    private final class AssetRegistrationHandle implements RegistrationHandle {

        private final String registrationId;
        private final String fullRequestPath;
        private boolean unregistered;

        private AssetRegistrationHandle(String registrationId, String fullRequestPath) {
            this.registrationId = registrationId;
            this.fullRequestPath = fullRequestPath;
        }

        @Override
        public synchronized void unregister() {
            if (!unregistered) {
                InMemoryPluginAssetRegistry.this.unregister(registrationId, fullRequestPath);
                unregistered = true;
            }
        }

        @Override
        public Optional<String> registrationId() {
            return Optional.of(registrationId);
        }

        @Override
        public RegistrationType type() {
            return RegistrationType.ASSET;
        }
    }
}
