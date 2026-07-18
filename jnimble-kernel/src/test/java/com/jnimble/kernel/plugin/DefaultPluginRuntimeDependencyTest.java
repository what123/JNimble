package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPluginRuntimeDependencyTest {

    private PluginRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        PluginBootLoader bootLoader = descriptor -> new LoadedPluginBoot() {
            private final PluginBoot boot = context -> { };

            @Override
            public PluginBoot boot() {
                return boot;
            }
        };
        runtimeService = new DefaultPluginRuntimeService(
                bootLoader,
                NoopPluginRegistries.hooks(),
                NoopPluginRegistries.routes(),
                NoopPluginRegistries.assets(),
                PluginBeanResolver.empty(),
                Clock.systemUTC());
    }

    @Test
    void requiredDependencyMustBeEnabledBeforeDependentPlugin() {
        runtimeService.install(descriptor("provider", "1.2.0", List.of()));
        runtimeService.install(descriptor(
                "consumer",
                "1.0.0",
                List.of(new PluginDependency("provider", "1.x", true))));

        assertThatThrownBy(() -> runtimeService.enable("consumer"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("required dependency provider is not enabled");

        runtimeService.enable("provider");
        runtimeService.enable("consumer");

        assertThat(runtimeService.find("consumer").orElseThrow().status())
                .isEqualTo(PluginStatus.ENABLED);
    }

    @Test
    void enabledDependentProtectsProviderFromDisable() {
        runtimeService.install(descriptor("provider", "1.2.0", List.of()));
        runtimeService.install(descriptor(
                "consumer",
                "1.0.0",
                List.of(new PluginDependency("provider", ">=1.0.0", true))));
        runtimeService.enable("provider");
        runtimeService.enable("consumer");

        assertThatThrownBy(() -> runtimeService.disable("provider"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("enabled dependents: consumer");

        runtimeService.disable("consumer");
        runtimeService.disable("provider");
        assertThat(runtimeService.find("provider").orElseThrow().status())
                .isEqualTo(PluginStatus.DISABLED);
    }

    @Test
    void incompatibleRequiredDependencyVersionIsRejected() {
        runtimeService.install(descriptor("provider", "2.0.0", List.of()));
        runtimeService.install(descriptor(
                "consumer",
                "1.0.0",
                List.of(new PluginDependency("provider", "1.x", true))));
        runtimeService.enable("provider");

        assertThatThrownBy(() -> runtimeService.enable("consumer"))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("incompatible version 2.0.0");
    }

    private PluginDescriptor descriptor(
            String id,
            String version,
            List<PluginDependency> dependencies
    ) {
        return new PluginDescriptor(
                "1.0",
                id,
                id,
                null,
                null,
                null,
                version,
                "0.1.x",
                null,
                null,
                "example." + id + ".PluginBoot",
                null,
                null,
                null,
                null,
                dependencies,
                List.of(),
                null);
    }
}
