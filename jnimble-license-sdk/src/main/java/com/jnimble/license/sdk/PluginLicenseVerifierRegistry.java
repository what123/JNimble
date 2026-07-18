package com.jnimble.license.sdk;

import com.jnimble.sdk.hook.RegistrationHandle;

import java.util.Optional;

public interface PluginLicenseVerifierRegistry {

    RegistrationHandle register(String pluginId, PluginLicenseVerifier verifier);

    Optional<PluginLicenseVerifier> find(String pluginId);
}
