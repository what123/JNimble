package com.jnimble.sdk.plugin;

/**
 * Optional commercial license policy declared by a plugin.
 *
 * @param required whether the plugin requires a valid license
 * @param issuer trusted license issuer code
 * @param productCode stable commercial product code used in machine codes
 * @param policy enforcement policy name
 */
public record PluginLicenseDescriptor(
        boolean required,
        String issuer,
        String productCode,
        String policy
) {
}
