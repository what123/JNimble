package com.jnimble.kernel.resource;

import com.jnimble.sdk.resource.AssetDefinition;

/**
 * Record representing a registered plugin asset with its metadata and availability status.
 *
 * @param registrationId unique identifier for this asset registration
 * @param pluginId       the ID of the plugin that owns this asset
 * @param fullRequestPath the full request path including namespace prefix
 * @param definition     the original asset definition
 * @param pluginEnabled  whether the owning plugin is currently enabled
 */
public record RegisteredPluginAsset(
        String registrationId,
        String pluginId,
        String fullRequestPath,
        AssetDefinition definition,
        boolean pluginEnabled
) {
}
