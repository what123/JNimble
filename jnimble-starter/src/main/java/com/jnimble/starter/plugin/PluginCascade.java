package com.jnimble.starter.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes which plugins are affected when hot-replacing a plugin: the target plus all
 * (transitively) enabled plugins that depend on it, ordered so that a plugin always appears
 * after its dependencies. Disabling in the reverse of this order is therefore safe, and
 * re-enabling in this order restores the dependency stack.
 */
final class PluginCascade {

    private PluginCascade() {
    }

    /**
     * Returns the target plugin and its enabled transitive dependents, dependencies first.
     *
     * @param rootId    the plugin being replaced
     * @param snapshots the current runtime snapshots
     * @return plugin ids in enable order (target included); just the target if it is not enabled
     */
    static List<String> affectedEnableOrder(String rootId, Collection<PluginRuntimeSnapshot> snapshots) {
        Map<String, PluginDescriptor> enabled = new LinkedHashMap<>();
        for (PluginRuntimeSnapshot snapshot : snapshots) {
            if (snapshot.status() == PluginStatus.ENABLED && snapshot.descriptor() != null) {
                enabled.put(snapshot.pluginId(), snapshot.descriptor());
            }
        }
        if (!enabled.containsKey(rootId)) {
            return List.of(rootId);
        }
        Set<String> members = new LinkedHashSet<>();
        members.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, PluginDescriptor> entry : enabled.entrySet()) {
                if (members.contains(entry.getKey())) {
                    continue;
                }
                List<PluginDependency> dependencies = entry.getValue().dependencies();
                if (dependencies == null) {
                    continue;
                }
                boolean dependsOnMember = dependencies.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(dependency -> members.contains(dependency.pluginId()));
                if (dependsOnMember) {
                    members.add(entry.getKey());
                    changed = true;
                }
            }
        }
        List<PluginDescriptor> descriptors = new ArrayList<>();
        for (String pluginId : members) {
            PluginDescriptor descriptor = enabled.get(pluginId);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
        return PluginDependencyOrder.sort(descriptors).stream()
                .map(PluginDescriptor::id)
                .toList();
    }
}
