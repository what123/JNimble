package com.jnimble.sdk.plugin;

import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;

import java.util.Optional;

/**
 * Plugin-scoped access point supplied by the platform during lifecycle calls.
 */
public interface PluginContext {

    /**
     * Descriptor parsed from {@code META-INF/jnimble-plugin.json}.
     */
    PluginDescriptor descriptor();

    /**
     * Hook registry for runtime contributions.
     */
    HookRegistry hooks();

    /**
     * Route registry for plugin routes.
     */
    RouteRegistry routes();

    /**
     * Asset registry for plugin static resources.
     */
    AssetRegistry assets();

    /**
     * Returns a platform or application bean exposed to plugins.
     *
     * @throws RuntimeException when the bean is not available
     */
    <T> T bean(Class<T> type);

    /**
     * Attempts to return a platform or application bean exposed to plugins.
     */
    default <T> Optional<T> findBean(Class<T> type) {
        try {
            return Optional.ofNullable(bean(type));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * Allows plugin code to attach handles created outside the standard
     * registries so the runtime can remove them during hot unplug.
     */
    default RegistrationHandle registerHandle(RegistrationHandle handle) {
        return handle;
    }
}
