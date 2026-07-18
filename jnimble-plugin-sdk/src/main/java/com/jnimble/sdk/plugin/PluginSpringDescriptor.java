package com.jnimble.sdk.plugin;

/**
 * Optional Spring container entry point for a plugin.
 *
 * @param configurationClass fully qualified configuration class loaded in the plugin child context
 */
public record PluginSpringDescriptor(String configurationClass) {
}
