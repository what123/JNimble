package com.jnimble.admin.setting;

/** Snapshot of storage directory configuration. */
public record StorageConfig(
        String pluginDir,
        String logoDir
) {
}
