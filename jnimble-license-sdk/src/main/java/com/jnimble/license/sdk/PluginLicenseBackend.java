package com.jnimble.license.sdk;

import java.util.Optional;

public interface PluginLicenseBackend {

    Optional<String> loadToken(String pluginId);

    String machineCode(String pluginId, String productCode);

    void report(String pluginId, PluginLicenseResult result);
}
