package com.jnimble.kernel.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import org.junit.jupiter.api.Test;

class InMemoryPluginAssetRegistryTest {

    private final InMemoryPluginAssetRegistry registry = new InMemoryPluginAssetRegistry();

    @Test
    void resolvesDirectoryAssetByLongestPrefix() {
        registry.register("crm", new AssetDefinition("/", "classpath:static/plugin/crm/", true));
        registry.register("crm", new AssetDefinition("/theme/", "classpath:static/plugin/crm/theme/", true));

        assertThat(registry.find("/assets/plugins/crm/theme/dark.css"))
                .get()
                .satisfies(asset -> {
                    assertThat(asset.fullRequestPath()).isEqualTo("/assets/plugins/crm/theme/");
                    assertThat(asset.pluginEnabled()).isTrue();
                });
    }

    @Test
    void tracksAssetAvailabilityAndUnregistersHandles() {
        RegistrationHandle handle = registry.register("crm",
                new AssetDefinition("/", "classpath:static/plugin/crm/", true));

        assertThat(registry.availability("/assets/plugins/crm/crm.css"))
                .isEqualTo(PluginAssetAvailability.AVAILABLE);

        registry.disablePlugin("crm");
        assertThat(registry.availability("/assets/plugins/crm/crm.css"))
                .isEqualTo(PluginAssetAvailability.PLUGIN_DISABLED);

        registry.enablePlugin("crm");
        assertThat(registry.availability("/assets/plugins/crm/crm.css"))
                .isEqualTo(PluginAssetAvailability.AVAILABLE);

        handle.unregister();
        assertThat(registry.find("/assets/plugins/crm/crm.css")).isEmpty();
    }

    @Test
    void rejectsDuplicateAssetMounts() {
        registry.register("crm", new AssetDefinition("/", "classpath:static/plugin/crm/", true));

        assertThatThrownBy(() -> registry.register("crm",
                new AssetDefinition("/", "classpath:static/plugin/crm-copy/", true)))
                .isInstanceOf(PluginAssetConflictException.class)
                .hasMessageContaining("Asset conflict");
    }

    @Test
    void allowsSameRelativeAssetMountForDifferentPlugins() {
        registry.register("crm", new AssetDefinition("/", "classpath:static/plugin/crm/", true));
        registry.register("sales", new AssetDefinition("/", "classpath:static/plugin/sales/", true));

        assertThat(registry.find("/assets/plugins/crm/crm.css")).isPresent();
        assertThat(registry.find("/assets/plugins/sales/sales.css")).isPresent();
    }

    @Test
    void rejectsRelativeAssetPathWithParentDirectorySegments() {
        assertThatThrownBy(() -> registry.register("crm",
                new AssetDefinition("../admin", "classpath:static/plugin/crm/", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent directory");

        assertThatThrownBy(() -> registry.register("crm",
                new AssetDefinition("safe\\..\\admin", "classpath:static/plugin/crm/", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent directory");
    }

    /**
     * 测试 scoped() 返回限定插件作用域的 AssetRegistry。
     */
    @Test
    void scopedReturnsAssetRegistryForPlugin() {
        AssetRegistry scoped = registry.scoped("crm");
        scoped.register(new AssetDefinition("/", "classpath:static/plugin/crm/", true));

        assertThat(registry.find("/assets/plugins/crm/crm.css")).isPresent();
        assertThat(registry.find("/assets/plugins/crm/crm.css").get().pluginId()).isEqualTo("crm");
    }

    /**
     * 测试 assets(pluginId) 返回指定插件的资产列表。
     */
    @Test
    void assetsReturnsAssetsForSpecificPlugin() {
        registry.register("crm", new AssetDefinition("/", "classpath:static/plugin/crm/", true));
        registry.register("sales", new AssetDefinition("/", "classpath:static/plugin/sales/", true));

        java.util.List<RegisteredPluginAsset> crmAssets = registry.assets("crm");

        assertThat(crmAssets).hasSize(1);
        assertThat(crmAssets.get(0).pluginId()).isEqualTo("crm");
    }

    /**
     * 测试 availability() 对未注册路径返回 NOT_FOUND。
     */
    @Test
    void availabilityReturnsNotFoundForUnregisteredPath() {
        registry.register("crm", new AssetDefinition("/", "classpath:static/plugin/crm/", true));

        assertThat(registry.availability("/assets/plugins/unknown/file.css"))
                .isEqualTo(PluginAssetAvailability.NOT_FOUND);
    }

    /**
     * 测试注册时 asset 为 null 抛出异常。
     */
    @Test
    void registerRejectsNullAsset() {
        assertThatThrownBy(() -> registry.register("crm", null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 测试 find() 对空白 requestPath 抛出异常。
     */
    @Test
    void findRejectsBlankRequestPath() {
        assertThatThrownBy(() -> registry.find(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must not be blank");

        assertThatThrownBy(() -> registry.find("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must not be blank");
    }

    @Test
    void rejectsInvalidPluginIds() {
        assertThatThrownBy(() -> registry.register("../crm",
                new AssetDefinition("/", "classpath:static/plugin/crm/", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");

        assertThatThrownBy(() -> registry.register("Crm",
                new AssetDefinition("/", "classpath:static/plugin/crm/", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId must match");
    }
}
