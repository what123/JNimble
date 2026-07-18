package com.jnimble.kernel.plugin;

import com.jnimble.kernel.hook.InMemoryHookRegistry;
import com.jnimble.kernel.resource.PluginAssetRegistry;
import com.jnimble.kernel.route.PluginRouteRegistry;
import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Manages plugin contributions to hooks, routes, and assets.
 *
 * <p>Provides scoped registries for each plugin and handles enabling/disabling
 * of plugin contributions.</p>
 */
@Component
public class PluginContributionRegistries {

    private final InMemoryHookRegistry hookRegistry;
    private final PluginRouteRegistry routeRegistry;
    private final PluginAssetRegistry assetRegistry;

    /**
     * Creates a new plugin contribution registries.
     *
     * @param hookRegistry  the hook registry
     * @param routeRegistry the route registry
     * @param assetRegistry the asset registry
     */
    public PluginContributionRegistries(
            InMemoryHookRegistry hookRegistry,
            PluginRouteRegistry routeRegistry,
            PluginAssetRegistry assetRegistry
    ) {
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry");
        this.routeRegistry = Objects.requireNonNull(routeRegistry, "routeRegistry");
        this.assetRegistry = Objects.requireNonNull(assetRegistry, "assetRegistry");
    }

    /**
     * Returns a scoped hook registry for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return a scoped hook registry
     */
    public HookRegistry hooks(String pluginId) {
        String scopedPluginId = requirePluginId(pluginId);
        return new HookRegistry() {
            @Override
            public RegistrationHandle register(String hookName, HookViewContribution contribution) {
                return hookRegistry.register(hookName, scopedPluginId, HookMode.APPEND, contribution);
            }

            @Override
            public RegistrationHandle register(String hookName, HookMode mode, HookViewContribution contribution) {
                return hookRegistry.register(hookName, scopedPluginId, mode, contribution);
            }
        };
    }

    /**
     * Returns a scoped route registry for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return a scoped route registry
     */
    public RouteRegistry routes(String pluginId) {
        return routeRegistry.scoped(requirePluginId(pluginId));
    }

    /**
     * Returns a scoped asset registry for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return a scoped asset registry
     */
    public AssetRegistry assets(String pluginId) {
        return assetRegistry.scoped(requirePluginId(pluginId));
    }

    /**
     * Enables all contributions for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    public void enable(String pluginId) {
        String scopedPluginId = requirePluginId(pluginId);
        routeRegistry.enablePlugin(scopedPluginId);
        assetRegistry.enablePlugin(scopedPluginId);
    }

    /**
     * Disables all contributions for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    public void disable(String pluginId) {
        String scopedPluginId = requirePluginId(pluginId);
        routeRegistry.disablePlugin(scopedPluginId);
        assetRegistry.disablePlugin(scopedPluginId);
    }

    private static String requirePluginId(String pluginId) {
        return PluginIds.requireValid(pluginId);
    }
}
