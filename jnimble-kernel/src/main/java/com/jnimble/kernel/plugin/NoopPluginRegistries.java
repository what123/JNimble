package com.jnimble.kernel.plugin;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteRegistry;

/**
 * Provides no-op implementations of plugin registries (hooks, routes, assets).
 * Used as default fallbacks when no real registries are available.
 */
final class NoopPluginRegistries {

    private NoopPluginRegistries() {
    }

    /**
     * Returns a no-op hook registry.
     *
     * @return a hook registry that discards all registrations
     */
    static HookRegistry hooks() {
        return new HookRegistry() {
            @Override
            public RegistrationHandle register(String hookName, HookViewContribution contribution) {
                return handle();
            }

            @Override
            public RegistrationHandle register(String hookName, HookMode mode, HookViewContribution contribution) {
                return handle();
            }
        };
    }

    /**
     * Returns a no-op route registry.
     *
     * @return a route registry that discards all registrations
     */
    static RouteRegistry routes() {
        return new RouteRegistry() {
            @Override
            public RegistrationHandle register(RouteDefinition route) {
                return handle();
            }
        };
    }

    /**
     * Returns a no-op asset registry.
     *
     * @return an asset registry that discards all registrations
     */
    static AssetRegistry assets() {
        return new AssetRegistry() {
            @Override
            public RegistrationHandle register(AssetDefinition asset) {
                return handle();
            }
        };
    }

    private static RegistrationHandle handle() {
        return () -> {
        };
    }
}
