package com.jnimble.kernel.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import com.jnimble.sdk.route.RouteRegistry;
import org.junit.jupiter.api.Test;

class InMemoryPluginRouteRegistryTest {

    private final InMemoryPluginRouteRegistry registry = new InMemoryPluginRouteRegistry();

    @Test
    void registersPluginScopedRouteAndTracksAvailability() {
        RegistrationHandle handle = registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", "crm.customer.view"));

        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET))
                .get()
                .satisfies(route -> {
                    assertThat(route.pluginId()).isEqualTo("crm");
                    assertThat(route.fullPath()).isEqualTo("/admin/plugins/crm/customers");
                    assertThat(route.pluginEnabled()).isTrue();
                });
        assertThat(registry.availability("/admin/plugins/crm/customers", RouteMethod.GET))
                .isEqualTo(PluginRouteAvailability.AVAILABLE);

        registry.disablePlugin("crm");
        assertThat(registry.availability("/admin/plugins/crm/customers", RouteMethod.GET))
                .isEqualTo(PluginRouteAvailability.PLUGIN_DISABLED);

        registry.enablePlugin("crm");
        assertThat(registry.availability("/admin/plugins/crm/customers", RouteMethod.GET))
                .isEqualTo(PluginRouteAvailability.AVAILABLE);

        handle.unregister();
        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET)).isEmpty();
    }

    @Test
    void rejectsDuplicateRouteMethodAndPath() {
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));

        assertThatThrownBy(() -> registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers-copy", null)))
                .isInstanceOf(PluginRouteConflictException.class)
                .hasMessageContaining("Route conflict");
    }

    @Test
    void allowsSameRelativeRoutePathForDifferentPlugins() {
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));
        registry.register("sales",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/sales/page/customers", null));

        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET)).isPresent();
        assertThat(registry.find("/admin/plugins/sales/customers", RouteMethod.GET)).isPresent();
    }

    @Test
    void allowsSameRoutePathWithDifferentHttpMethod() {
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.POST, "plugin/crm/page/customers-post", null));

        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET)).isPresent();
        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.POST)).isPresent();
    }

    @Test
    void rejectsRelativeRoutePathWithParentDirectorySegments() {
        assertThatThrownBy(() -> registry.register("crm",
                new RouteDefinition("../admin", RouteMethod.GET, "plugin/crm/page/admin", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent directory");

        assertThatThrownBy(() -> registry.register("crm",
                new RouteDefinition("safe\\..\\admin", RouteMethod.GET, "plugin/crm/page/admin", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent directory");
    }

    /**
     * 测试 scoped() 返回限定插件作用域的 RouteRegistry。
     */
    @Test
    void scopedReturnsRouteRegistryForPlugin() {
        RouteRegistry scoped = registry.scoped("crm");
        scoped.register(new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));

        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET)).isPresent();
        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET).get().pluginId()).isEqualTo("crm");
    }

    /**
     * 测试 routes(pluginId) 返回指定插件的路由列表。
     */
    @Test
    void routesReturnsRoutesForSpecificPlugin() {
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));
        registry.register("sales",
                new RouteDefinition("/leads", RouteMethod.GET, "plugin/sales/page/leads", null));

        java.util.List<RegisteredPluginRoute> crmRoutes = registry.routes("crm");

        assertThat(crmRoutes).hasSize(1);
        assertThat(crmRoutes.get(0).pluginId()).isEqualTo("crm");
    }

    /**
     * 测试 availability() 对未注册路径返回 NOT_FOUND。
     */
    @Test
    void availabilityReturnsNotFoundForUnregisteredPath() {
        registry.register("crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null));

        assertThat(registry.availability("/admin/plugins/unknown/path", RouteMethod.GET))
                .isEqualTo(PluginRouteAvailability.NOT_FOUND);
    }

    /**
     * 测试注册时 route 为 null 抛出异常。
     */
    @Test
    void registerRejectsNullRoute() {
        assertThatThrownBy(() -> registry.register("crm", null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 测试 route 的 method 为 null 时默认使用 GET。
     */
    @Test
    void nullMethodDefaultsToGet() {
        registry.register("crm",
                new RouteDefinition("/customers", null, "plugin/crm/page/customers", null));

        assertThat(registry.find("/admin/plugins/crm/customers", RouteMethod.GET)).isPresent();
        assertThat(registry.availability("/admin/plugins/crm/customers", RouteMethod.GET))
                .isEqualTo(PluginRouteAvailability.AVAILABLE);
    }

    /**
     * 测试 find() 对空白 requestPath 抛出异常。
     */
    @Test
    void findRejectsBlankRequestPath() {
        assertThatThrownBy(() -> registry.find("", RouteMethod.GET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must not be blank");

        assertThatThrownBy(() -> registry.find("  ", RouteMethod.GET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must not be blank");
    }

    @Test
    void rejectsInvalidPluginIds() {
        assertThatThrownBy(() -> registry.register("../crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");

        assertThatThrownBy(() -> registry.register("Crm",
                new RouteDefinition("/customers", RouteMethod.GET, "plugin/crm/page/customers", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");
    }
}
