package com.jnimble.kernel.plugin;

import com.jnimble.sdk.hook.RegistrationHandle;

/**
 * Registry for plugin internationalization (i18n) resources.
 *
 * <p>Allows plugins to register message bundles for localization.</p>
 */
public interface PluginI18nRegistry {

    /**
     * Registers an i18n message bundle for a plugin.
     *
     * @param pluginId   the plugin identifier
     * @param basename   the message bundle basename
     * @param classLoader the class loader to load the bundle
     * @return a registration handle for cleanup
     */
    RegistrationHandle register(String pluginId, String basename, ClassLoader classLoader);

    /**
     * Returns a no-op implementation that discards registrations.
     *
     * @return a no-op registry
     */
    static PluginI18nRegistry noop() {
        return (pluginId, basename, classLoader) -> () -> {
        };
    }
}
