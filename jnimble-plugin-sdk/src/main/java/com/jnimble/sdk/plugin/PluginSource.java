package com.jnimble.sdk.plugin;

/**
 * Source from which a plugin was discovered or installed.
 */
public enum PluginSource {
    /** Discovered from the application classpath. */
    CLASSPATH,
    /** Installed from an external JAR file. */
    JAR,
    /** Installed from an external directory. */
    DIRECTORY
}
