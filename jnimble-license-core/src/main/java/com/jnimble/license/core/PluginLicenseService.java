package com.jnimble.license.core;

import com.jnimble.license.sdk.LicenseStatus;
import com.jnimble.sdk.plugin.PluginDescriptor;

public interface PluginLicenseService {

    PluginLicenseView view(PluginDescriptor descriptor);

    PluginLicenseView activate(PluginDescriptor descriptor, String token, String operator);

    PluginLicenseView revalidate(PluginDescriptor descriptor, boolean force);

    void remove(PluginDescriptor descriptor, String operator);

    LicenseStatus status(String pluginId);
}
