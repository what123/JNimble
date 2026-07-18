package com.jnimble.license.sdk;

@FunctionalInterface
public interface PluginLicenseVerifier {

    PluginLicenseResult verify(boolean force);
}
