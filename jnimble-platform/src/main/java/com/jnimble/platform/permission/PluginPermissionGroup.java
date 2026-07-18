package com.jnimble.platform.permission;

import java.util.List;

/**
 * Groups permissions by plugin for display purposes.
 *
 * @param pluginId    the plugin identifier
 * @param permissions the list of permissions registered by the plugin
 */
public record PluginPermissionGroup(String pluginId, List<PermissionRecord> permissions) {
}
