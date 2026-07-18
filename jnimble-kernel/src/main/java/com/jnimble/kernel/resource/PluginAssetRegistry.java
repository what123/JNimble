package com.jnimble.kernel.resource;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import java.util.List;
import java.util.Optional;

/**
 * Registry for plugin static assets.
 *
 * <p>Manages asset registrations from plugins, including scoped registries
 * for individual plugins and availability checking.</p>
 */
public interface PluginAssetRegistry {

    /** The namespace prefix for plugin assets. */
    String ASSET_NAMESPACE_PREFIX = "/assets/plugins";

    /**
     * Returns a scoped asset registry for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return a scoped registry
     */
    AssetRegistry scoped(String pluginId);

    /**
     * Registers an asset for a plugin.
     *
     * @param pluginId the plugin identifier
     * @param asset    the asset definition
     * @return a registration handle
     */
    RegistrationHandle register(String pluginId, AssetDefinition asset);

    /**
     * Enables all assets for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    void enablePlugin(String pluginId);

    /**
     * Disables all assets for a plugin.
     *
     * @param pluginId the plugin identifier
     */
    void disablePlugin(String pluginId);

    /**
     * Checks asset availability for a request path.
     *
     * @param requestPath the request path
     * @return the availability result
     */
    PluginAssetAvailability availability(String requestPath);

    /**
     * Finds an asset matching the request path.
     *
     * @param requestPath the request path
     * @return an optional containing the registered asset if found
     */
    Optional<RegisteredPluginAsset> find(String requestPath);

    /**
     * Lists all registered assets.
     *
     * @return list of all assets
     */
    List<RegisteredPluginAsset> assets();

    /**
     * Lists all assets for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return list of assets
     */
    List<RegisteredPluginAsset> assets(String pluginId);
}
