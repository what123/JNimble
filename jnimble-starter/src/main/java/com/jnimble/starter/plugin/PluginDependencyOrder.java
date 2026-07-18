package com.jnimble.starter.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeException;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces a stable dependency-first order for a batch of plugin descriptors.
 */
final class PluginDependencyOrder {

    private PluginDependencyOrder() {
    }

    static List<PluginDescriptor> sort(Collection<PluginDescriptor> descriptors) {
        Map<String, PluginDescriptor> byId = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(java.util.Comparator.comparing(PluginDescriptor::id))
                .forEach(descriptor -> byId.put(descriptor.id(), descriptor));
        Map<String, VisitState> states = new LinkedHashMap<>();
        List<PluginDescriptor> ordered = new ArrayList<>();
        for (PluginDescriptor descriptor : byId.values()) {
            visit(descriptor, byId, states, ordered, new ArrayList<>());
        }
        return List.copyOf(ordered);
    }

    private static void visit(
            PluginDescriptor descriptor,
            Map<String, PluginDescriptor> byId,
            Map<String, VisitState> states,
            List<PluginDescriptor> ordered,
            List<String> path
    ) {
        VisitState state = states.get(descriptor.id());
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            path.add(descriptor.id());
            throw new PluginRuntimeException("Circular plugin dependency: " + String.join(" -> ", path));
        }
        states.put(descriptor.id(), VisitState.VISITING);
        path.add(descriptor.id());
        if (descriptor.dependencies() != null) {
            for (PluginDependency dependency : descriptor.dependencies()) {
                if (dependency == null) {
                    continue;
                }
                PluginDescriptor dependencyDescriptor = byId.get(dependency.pluginId());
                if (dependencyDescriptor != null) {
                    visit(dependencyDescriptor, byId, states, ordered, new ArrayList<>(path));
                }
            }
        }
        states.put(descriptor.id(), VisitState.VISITED);
        ordered.add(descriptor);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
