package com.jnimble.sdk.plugin;

/**
 * Entry point implemented by every JNimble plugin.
 */
public interface PluginBoot {

    /**
     * Called when the platform enables the plugin and accepts runtime
     * registrations.
     *
     * @param context plugin-scoped context supplied by the platform
     * @throws PluginException when the plugin cannot be started
     */
    void boot(PluginContext context);

    /**
     * Called before the platform disables or reloads the plugin.
     *
     * <p>The platform owns registration rollback through returned handles, so
     * implementations should only release plugin-owned resources here.</p>
     *
     * @param context plugin-scoped context supplied by the platform
     * @throws PluginException when plugin-owned resources cannot be released
     */
    default void stop(PluginContext context) {
    }
}
