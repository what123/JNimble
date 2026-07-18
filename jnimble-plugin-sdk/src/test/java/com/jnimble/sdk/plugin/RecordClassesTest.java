package com.jnimble.sdk.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * SDK 中 record 类的单元测试。
 * 覆盖 PluginDescriptor、PluginI18n、PluginPermission、PluginMetadata、
 * PluginLifecycleEvent、PluginMigration、AssetDefinition、RouteDefinition 和 HookViewContribution。
 */
class RecordClassesTest {

    /**
     * 测试 PluginDescriptor record 的构造和访问器。
     */
    @Test
    void pluginDescriptorConstructionAndAccessors() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "com.example.plugin", "Example Plugin", "plugin.name",
                "An example plugin", "plugin.desc", "2.0.0", ">=2.0",
                "Author", "https://example.com", "com.example.Boot",
                new PluginI18n("messages"),
                List.of(new PluginPermission("perm.code", "Permission", "perm.name")),
                new PluginMigration(true, "/db/migration", "flyway_schema_history", false, true)
        );

        assertThat(descriptor.schemaVersion()).isEqualTo("1.0");
        assertThat(descriptor.id()).isEqualTo("com.example.plugin");
        assertThat(descriptor.name()).isEqualTo("Example Plugin");
        assertThat(descriptor.nameKey()).isEqualTo("plugin.name");
        assertThat(descriptor.description()).isEqualTo("An example plugin");
        assertThat(descriptor.descriptionKey()).isEqualTo("plugin.desc");
        assertThat(descriptor.version()).isEqualTo("2.0.0");
        assertThat(descriptor.platformVersion()).isEqualTo(">=2.0");
        assertThat(descriptor.author()).isEqualTo("Author");
        assertThat(descriptor.website()).isEqualTo("https://example.com");
        assertThat(descriptor.bootClass()).isEqualTo("com.example.Boot");
        assertThat(descriptor.i18n()).isNotNull();
        assertThat(descriptor.admin()).isNull();
        assertThat(descriptor.spring()).isNull();
        assertThat(descriptor.dependencies()).isNull();
        assertThat(descriptor.configuration()).isNull();
        assertThat(descriptor.permissions()).hasSize(1);
        assertThat(descriptor.migration()).isNotNull();
    }

    @Test
    void pluginConfigurationDescriptorsExposeGeneratedFormFields() {
        PluginConfigurationOption option = new PluginConfigurationOption("live", "Live", "config.live");
        PluginConfigurationField field = new PluginConfigurationField(
                "environment", "Environment", "config.environment",
                "Provider environment", "config.environment.help",
                "Select environment", "config.environment.placeholder",
                PluginConfigurationFieldType.SELECT, true, "live", List.of(option)
        );
        PluginConfigurationDescriptor configuration = new PluginConfigurationDescriptor(
                "Settings", "config.title", "Plugin settings", "config.description", List.of(field)
        );

        assertThat(configuration.fields()).containsExactly(field);
        assertThat(field.type()).isEqualTo(PluginConfigurationFieldType.SELECT);
        assertThat(field.options()).containsExactly(option);
    }

    @Test
    void pluginRuntimeDescriptorsExposeSpringAndDependencies() {
        PluginSpringDescriptor spring = new PluginSpringDescriptor("com.example.PluginConfiguration");
        PluginDependency dependency = new PluginDependency("order-core", "1.x", true);

        assertThat(spring.configurationClass()).isEqualTo("com.example.PluginConfiguration");
        assertThat(dependency.pluginId()).isEqualTo("order-core");
        assertThat(dependency.version()).isEqualTo("1.x");
        assertThat(dependency.required()).isTrue();
    }

    @Test
    void pluginAdminDescriptorConstructionAndAccessors() {
        PluginAdminDescriptor admin = new PluginAdminDescriptor(
                "/settings", "plugin.settings", "com.example.plugin.settings"
        );

        assertThat(admin.entry()).isEqualTo("/settings");
        assertThat(admin.labelKey()).isEqualTo("plugin.settings");
        assertThat(admin.permission()).isEqualTo("com.example.plugin.settings");
    }

    /**
     * 测试 PluginI18n record 的构造和访问器。
     */
    @Test
    void pluginI18nConstructionAndAccessors() {
        PluginI18n i18n = new PluginI18n("com.example.messages");

        assertThat(i18n.basename()).isEqualTo("com.example.messages");
    }

    /**
     * 测试 PluginPermission record 的构造和访问器。
     */
    @Test
    void pluginPermissionConstructionAndAccessors() {
        PluginPermission permission = new PluginPermission(
                "plugin.read", "Read Access", "perm.read", "Allows reading", "perm.read.desc"
        );

        assertThat(permission.code()).isEqualTo("plugin.read");
        assertThat(permission.name()).isEqualTo("Read Access");
        assertThat(permission.nameKey()).isEqualTo("perm.read");
        assertThat(permission.description()).isEqualTo("Allows reading");
        assertThat(permission.descriptionKey()).isEqualTo("perm.read.desc");
    }

    /**
     * 测试 PluginPermission record 的简化构造函数。
     */
    @Test
    void pluginPermissionSimplifiedConstructor() {
        PluginPermission permission = new PluginPermission("plugin.write", "Write Access", "perm.write");

        assertThat(permission.code()).isEqualTo("plugin.write");
        assertThat(permission.name()).isEqualTo("Write Access");
        assertThat(permission.nameKey()).isEqualTo("perm.write");
        assertThat(permission.description()).isNull();
        assertThat(permission.descriptionKey()).isNull();
    }

    /**
     * 测试 PluginMetadata record 的构造和访问器。
     */
    @Test
    void pluginMetadataConstructionAndAccessors() {
        Instant now = Instant.now();
        PluginMetadata metadata = new PluginMetadata(
                "com.example.plugin", "Example Plugin", "1.0.0",
                PluginSource.JAR, PluginStatus.ENABLED,
                now, now, now, null
        );

        assertThat(metadata.pluginId()).isEqualTo("com.example.plugin");
        assertThat(metadata.name()).isEqualTo("Example Plugin");
        assertThat(metadata.version()).isEqualTo("1.0.0");
        assertThat(metadata.source()).isEqualTo(PluginSource.JAR);
        assertThat(metadata.status()).isEqualTo(PluginStatus.ENABLED);
        assertThat(metadata.installedAt()).isEqualTo(now);
        assertThat(metadata.lastStartedAt()).isEqualTo(now);
        assertThat(metadata.lastStoppedAt()).isEqualTo(now);
        assertThat(metadata.lastError()).isNull();
    }

    /**
     * 测试 PluginLifecycleEvent record 的构造和访问器。
     */
    @Test
    void pluginLifecycleEventConstructionAndAccessors() {
        Instant now = Instant.now();
        PluginLifecycleEvent event = new PluginLifecycleEvent(
                "com.example.plugin", PluginLifecyclePhase.ENABLED, now, "Plugin started"
        );

        assertThat(event.pluginId()).isEqualTo("com.example.plugin");
        assertThat(event.phase()).isEqualTo(PluginLifecyclePhase.ENABLED);
        assertThat(event.occurredAt()).isEqualTo(now);
        assertThat(event.reason()).isEqualTo("Plugin started");
    }

    /**
     * 测试 PluginMigration record 的构造和访问器。
     */
    @Test
    void pluginMigrationConstructionAndAccessors() {
        PluginMigration migration = new PluginMigration(
                true, "/db/migration", "flyway_schema_history", false, true
        );

        assertThat(migration.enabled()).isTrue();
        assertThat(migration.location()).isEqualTo("/db/migration");
        assertThat(migration.table()).isEqualTo("flyway_schema_history");
        assertThat(migration.baselineOnMigrate()).isFalse();
        assertThat(migration.failOnError()).isTrue();
    }

    /**
     * 测试 AssetDefinition record 的构造和访问器。
     */
    @Test
    void assetDefinitionConstructionAndAccessors() {
        AssetDefinition asset = new AssetDefinition("/css/style.css", "classpath:static/css/style.css", false);

        assertThat(asset.requestPath()).isEqualTo("/css/style.css");
        assertThat(asset.resourceLocation()).isEqualTo("classpath:static/css/style.css");
        assertThat(asset.cacheable()).isFalse();
    }

    /**
     * 测试 AssetDefinition record 的简化构造函数默认 cacheable 为 true。
     */
    @Test
    void assetDefinitionSimplifiedConstructorDefaultsCacheableTrue() {
        AssetDefinition asset = new AssetDefinition("/js/app.js", "classpath:static/js/app.js");

        assertThat(asset.requestPath()).isEqualTo("/js/app.js");
        assertThat(asset.resourceLocation()).isEqualTo("classpath:static/js/app.js");
        assertThat(asset.cacheable()).isTrue();
    }

    /**
     * 测试 RouteDefinition record 的构造和访问器。
     */
    @Test
    void routeDefinitionConstructionAndAccessors() {
        RouteDefinition route = new RouteDefinition(
                "/api/data", RouteMethod.POST, "handler::process", "admin.write"
        );

        assertThat(route.path()).isEqualTo("/api/data");
        assertThat(route.method()).isEqualTo(RouteMethod.POST);
        assertThat(route.view()).isEqualTo("handler::process");
        assertThat(route.permission()).isEqualTo("admin.write");
    }

    /**
     * 测试 RouteDefinition record 的简化构造函数默认 GET 方法和 null 权限。
     */
    @Test
    void routeDefinitionSimplifiedConstructorDefaultsGetAndNoPermission() {
        RouteDefinition route = new RouteDefinition("/dashboard", "views::dashboard");

        assertThat(route.path()).isEqualTo("/dashboard");
        assertThat(route.method()).isEqualTo(RouteMethod.GET);
        assertThat(route.view()).isEqualTo("views::dashboard");
        assertThat(route.permission()).isNull();
    }

    /**
     * 测试 HookViewContribution record 的构造和访问器。
     */
    @Test
    void hookViewContributionConstructionAndAccessors() {
        Map<String, Object> model = Map.of("key", "value", "count", 42);
        HookViewContribution contribution = new HookViewContribution(
                "fragments::myFragment", model, 10, "admin.read", "active"
        );

        assertThat(contribution.view()).isEqualTo("fragments::myFragment");
        assertThat(contribution.model()).isEqualTo(model);
        assertThat(contribution.order()).isEqualTo(10);
        assertThat(contribution.permission()).isEqualTo("admin.read");
        assertThat(contribution.activeWhen()).isEqualTo("active");
    }

    /**
     * 测试 HookViewContribution record 允许 null 值。
     */
    @Test
    void hookViewContributionAllowsNullValues() {
        HookViewContribution contribution = new HookViewContribution(
                "fragments::simple", null, 0, null, null
        );

        assertThat(contribution.view()).isEqualTo("fragments::simple");
        assertThat(contribution.model()).isNull();
        assertThat(contribution.order()).isEqualTo(0);
        assertThat(contribution.permission()).isNull();
        assertThat(contribution.activeWhen()).isNull();
    }
}
