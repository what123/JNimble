package com.jnimble.kernel.route;

import com.jnimble.kernel.plugin.PluginIds;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.hook.RegistrationType;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import com.jnimble.sdk.route.RouteRegistry;
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
 * In-memory implementation of {@link PluginRouteRegistry} that stores route registrations
 * in a HashMap. Handles route conflict detection and plugin enable/disable states.
 */
@Component
public class InMemoryPluginRouteRegistry implements PluginRouteRegistry {

    private final Map<RouteKey, RouteEntry> routes = new HashMap<>();
    private final Set<String> disabledPlugins = new HashSet<>();

    @Override
    public synchronized RouteRegistry scoped(String pluginId) {
        requirePluginId(pluginId);
        return route -> register(pluginId, route);
    }

    @Override
    public synchronized RegistrationHandle register(String pluginId, RouteDefinition route) {
        requirePluginId(pluginId);
        Objects.requireNonNull(route, "route must not be null");
        RouteMethod method = route.method() == null ? RouteMethod.GET : route.method();
        String fullPath = routePath(pluginId, route.path());
        RouteKey key = new RouteKey(fullPath, method);
        RouteEntry existing = routes.get(key);
        if (existing != null) {
            throw new PluginRouteConflictException(
                    "Route conflict for " + method + " " + fullPath
                            + " between plugins " + existing.pluginId + " and " + pluginId);
        }

        String registrationId = "route:" + UUID.randomUUID();
        RouteEntry entry = new RouteEntry(registrationId, pluginId, fullPath, method, route);
        routes.put(key, entry);
        return new RouteRegistrationHandle(registrationId, key);
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
    public synchronized PluginRouteAvailability availability(String requestPath, RouteMethod method) {
        RouteEntry route = routes.get(new RouteKey(normalizeAbsolutePath(requestPath), methodOrDefault(method)));
        if (route == null) {
            return PluginRouteAvailability.NOT_FOUND;
        }
        if (disabledPlugins.contains(route.pluginId)) {
            return PluginRouteAvailability.PLUGIN_DISABLED;
        }
        return PluginRouteAvailability.AVAILABLE;
    }

    @Override
    public synchronized Optional<RegisteredPluginRoute> find(String requestPath, RouteMethod method) {
        RouteEntry route = routes.get(new RouteKey(normalizeAbsolutePath(requestPath), methodOrDefault(method)));
        return Optional.ofNullable(route).map(this::snapshot);
    }

    @Override
    public synchronized List<RegisteredPluginRoute> routes() {
        return routes.values().stream()
                .map(this::snapshot)
                .toList();
    }

    @Override
    public synchronized List<RegisteredPluginRoute> routes(String pluginId) {
        requirePluginId(pluginId);
        return routes.values().stream()
                .filter(route -> route.pluginId.equals(pluginId))
                .map(this::snapshot)
                .toList();
    }

    private synchronized void unregister(String registrationId, RouteKey key) {
        RouteEntry current = routes.get(key);
        if (current != null && current.registrationId.equals(registrationId)) {
            routes.remove(key);
        }
    }

    private RegisteredPluginRoute snapshot(RouteEntry route) {
        return new RegisteredPluginRoute(
                route.registrationId,
                route.pluginId,
                route.fullPath,
                route.method,
                route.definition,
                !disabledPlugins.contains(route.pluginId));
    }

    private static String routePath(String pluginId, String relativePath) {
        return ROUTE_NAMESPACE_PREFIX + "/" + pluginId + normalizeRelativePath(relativePath);
    }

    private static RouteMethod methodOrDefault(RouteMethod method) {
        return method == null ? RouteMethod.GET : method;
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

    private static void requirePluginId(String pluginId) {
        PluginIds.requireValid(pluginId);
    }

    /**
     * Composite key for route lookups by path and HTTP method.
     */
    private record RouteKey(String fullPath, RouteMethod method) {
    }

    /**
     * Internal entry holding route registration data.
     */
    private static final class RouteEntry {

        private final String registrationId;
        private final String pluginId;
        private final String fullPath;
        private final RouteMethod method;
        private final RouteDefinition definition;

        private RouteEntry(
                String registrationId,
                String pluginId,
                String fullPath,
                RouteMethod method,
                RouteDefinition definition) {
            this.registrationId = registrationId;
            this.pluginId = pluginId;
            this.fullPath = fullPath;
            this.method = method;
            this.definition = definition;
        }
    }

    /**
     * Registration handle for a plugin route.
     *
     * <p>Removes the route from the registry upon unregistration.
     * Safe to call multiple times; subsequent calls are no-ops.</p>
     */
    private final class RouteRegistrationHandle implements RegistrationHandle {

        private final String registrationId;
        private final RouteKey key;
        private boolean unregistered;

        private RouteRegistrationHandle(String registrationId, RouteKey key) {
            this.registrationId = registrationId;
            this.key = key;
        }

        @Override
        public synchronized void unregister() {
            if (!unregistered) {
                InMemoryPluginRouteRegistry.this.unregister(registrationId, key);
                unregistered = true;
            }
        }

        @Override
        public Optional<String> registrationId() {
            return Optional.of(registrationId);
        }

        @Override
        public RegistrationType type() {
            return RegistrationType.ROUTE;
        }
    }
}
