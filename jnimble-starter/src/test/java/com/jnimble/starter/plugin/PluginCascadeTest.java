package com.jnimble.starter.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class PluginCascadeTest {

    @Test
    void leafPluginCascadesToItselfOnly() {
        List<PluginRuntimeSnapshot> snapshots = List.of(enabled("a", List.of()));

        assertThat(PluginCascade.affectedEnableOrder("a", snapshots)).containsExactly("a");
    }

    @Test
    void includesTransitiveDependentsDependenciesFirst() {
        // a <- b <- c : b depends on a, c depends on b
        List<PluginRuntimeSnapshot> snapshots = List.of(
                enabled("a", List.of()),
                enabled("b", List.of(dep("a"))),
                enabled("c", List.of(dep("b"))));

        assertThat(PluginCascade.affectedEnableOrder("a", snapshots))
                .containsExactly("a", "b", "c");
    }

    @Test
    void disablingOrderIsReverseOfEnableOrder() {
        List<PluginRuntimeSnapshot> snapshots = List.of(
                enabled("a", List.of()),
                enabled("b", List.of(dep("a"))),
                enabled("c", List.of(dep("b"))));

        List<String> enableOrder = PluginCascade.affectedEnableOrder("a", snapshots);
        // dependents must be disabled before their dependency
        assertThat(enableOrder.get(0)).isEqualTo("a");
        assertThat(enableOrder).endsWith("c");
    }

    @Test
    void ignoresDisabledDependents() {
        List<PluginRuntimeSnapshot> snapshots = List.of(
                enabled("a", List.of()),
                disabled("b", List.of(dep("a"))));

        assertThat(PluginCascade.affectedEnableOrder("a", snapshots)).containsExactly("a");
    }

    @Test
    void returnsOnlyRootWhenRootIsNotEnabled() {
        List<PluginRuntimeSnapshot> snapshots = List.of(disabled("a", List.of()));

        assertThat(PluginCascade.affectedEnableOrder("a", snapshots)).containsExactly("a");
    }

    private static PluginDependency dep(String pluginId) {
        return new PluginDependency(pluginId, "*", true);
    }

    private static PluginDescriptor descriptor(String id, List<PluginDependency> dependencies) {
        return new PluginDescriptor(
                "1.0", id, id, null, null, null, "1.0.0", "0.1.x",
                null, null, "example." + id + ".PluginBoot",
                null, null, null, dependencies, List.of(), null);
    }

    private static PluginRuntimeSnapshot enabled(String id, List<PluginDependency> dependencies) {
        return snapshot(id, dependencies, PluginStatus.ENABLED);
    }

    private static PluginRuntimeSnapshot disabled(String id, List<PluginDependency> dependencies) {
        return snapshot(id, dependencies, PluginStatus.DISABLED);
    }

    private static PluginRuntimeSnapshot snapshot(
            String id, List<PluginDependency> dependencies, PluginStatus status) {
        return new PluginRuntimeSnapshot(
                id, descriptor(id, dependencies), PluginSource.CLASSPATH, null, status,
                0, null, null, null, null, List.of());
    }
}
