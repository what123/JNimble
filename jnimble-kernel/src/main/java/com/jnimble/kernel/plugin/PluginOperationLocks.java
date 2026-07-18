package com.jnimble.kernel.plugin;

/**
 * Shared per-plugin monitor used to coordinate multi-step lifecycle operations
 * performed by the runtime, upload service, and directory watcher.
 */
public final class PluginOperationLocks {

    private PluginOperationLocks() {
    }

    /**
     * Returns a shared monitor object for the given plugin.
     *
     * <p>All callers synchronizing on the same plugin ID will share the same monitor,
     * enabling coordinated multi-step lifecycle operations.</p>
     *
     * @param pluginId the plugin identifier
     * @return the monitor object for synchronization
     */
    public static Object lockFor(String pluginId) {
        return PluginIds.requireValid(pluginId, "Plugin id").intern();
    }
}
