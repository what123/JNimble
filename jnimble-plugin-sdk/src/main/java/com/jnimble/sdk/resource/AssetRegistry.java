package com.jnimble.sdk.resource;

import com.jnimble.sdk.hook.RegistrationHandle;

/**
 * Registry used by plugins to expose static assets while the plugin is enabled.
 */
public interface AssetRegistry {

    /**
     * Registers a plugin asset mapping.
     *
     * @param asset asset mapping under the plugin asset namespace
     * @return handle used by the runtime to remove the mapping
     */
    RegistrationHandle register(AssetDefinition asset);
}
