package com.jnimble.kernel.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginMigration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link PluginMigrationConfig} 单元测试。
 *
 * <p>覆盖 from() 工厂方法的各种场景、defaults() 默认配置生成、
 * normalizePluginId() 插件 ID 标准化等核心逻辑。</p>
 */
class PluginMigrationConfigTest {

    // ========== 辅助方法 ==========

    private static PluginDescriptor buildDescriptor(String id, PluginMigration migration) {
        return new PluginDescriptor(
                "1.0", id, "Test Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "test.TestPlugin", null,
                List.of(), migration);
    }

    private static PluginDescriptor buildMinimalDescriptor(String id) {
        return buildDescriptor(id, null);
    }

    // ========== from() 方法测试 ==========

    /**
     * 测试从带有完整迁移配置的 descriptor 创建 config。
     */
    @Test
    void fromDescriptorWithFullMigration() {
        PluginMigration migration = new PluginMigration(
                true, "classpath:db/custom", "custom_history", false, false);
        PluginDescriptor descriptor = buildDescriptor("my-plugin", migration);

        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);

        assertThat(config.pluginId()).isEqualTo("my-plugin");
        assertThat(config.enabled()).isTrue();
        assertThat(config.location()).isEqualTo("classpath:db/custom");
        assertThat(config.historyTable()).isEqualTo("custom_history");
        assertThat(config.baselineOnMigrate()).isFalse();
        assertThat(config.failOnError()).isFalse();
    }

    /**
     * 测试从迁移配置为 null 的 descriptor 创建默认 config。
     */
    @Test
    void fromDescriptorWithNullMigrationReturnsDefaults() {
        PluginDescriptor descriptor = buildMinimalDescriptor("test-plugin");

        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);

        assertThat(config.pluginId()).isEqualTo("test-plugin");
        assertThat(config.enabled()).isFalse();
        assertThat(config.location()).isEqualTo("classpath:db/migration/plugin/test-plugin");
        assertThat(config.historyTable()).isEqualTo("flyway_schema_history_test_plugin");
        assertThat(config.baselineOnMigrate()).isTrue();
        assertThat(config.failOnError()).isTrue();
    }

    /**
     * 测试从部分字段为 null 的迁移配置创建 config，验证默认值回退。
     */
    @Test
    void fromDescriptorWithPartialMigrationUsesDefaults() {
        PluginMigration migration = new PluginMigration(true, null, null, null, null);
        PluginDescriptor descriptor = buildDescriptor("partial", migration);

        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);

        assertThat(config.enabled()).isTrue();
        assertThat(config.location()).isEqualTo("classpath:db/migration/plugin/partial");
        assertThat(config.historyTable()).isEqualTo("flyway_schema_history_partial");
        assertThat(config.baselineOnMigrate()).isTrue();
        assertThat(config.failOnError()).isTrue();
    }

    /**
     * 测试 from() 方法传入 null descriptor 抛出 NullPointerException。
     */
    @Test
    void fromNullDescriptorThrowsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> PluginMigrationConfig.from(null))
                .withMessageContaining("descriptor is required");
    }

    /**
     * 测试从空白 pluginId 的 descriptor 创建 config 抛出异常。
     */
    @Test
    void fromDescriptorWithBlankIdThrowsException() {
        PluginDescriptor descriptor = buildDescriptor("  ", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationConfig.from(descriptor))
                .withMessageContaining("descriptor.id");
    }

    // ========== defaults() 方法测试 ==========

    /**
     * 测试 defaults() 返回默认禁用的迁移配置。
     */
    @Test
    void defaultsReturnsDisabledMigration() {
        PluginMigrationConfig config = PluginMigrationConfig.defaults("my-plugin");

        assertThat(config.pluginId()).isEqualTo("my-plugin");
        assertThat(config.enabled()).isFalse();
        assertThat(config.baselineOnMigrate()).isTrue();
        assertThat(config.failOnError()).isTrue();
    }

    /**
     * 测试 defaults() 传入 null pluginId 抛出异常。
     */
    @Test
    void defaultsWithNullPluginIdThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationConfig.defaults(null))
                .withMessageContaining("pluginId");
    }

    /**
     * 测试 defaults() 传入空白 pluginId 抛出异常。
     */
    @Test
    void defaultsWithBlankPluginIdThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationConfig.defaults("  "))
                .withMessageContaining("pluginId");
    }

    // ========== normalizePluginId() 方法测试 ==========

    /**
     * 测试 normalizePluginId() 将连字符替换为下划线。
     */
    @Test
    void normalizePluginIdReplacesHyphensWithUnderscores() {
        assertThat(PluginMigrationConfig.normalizePluginId("my-plugin"))
                .isEqualTo("my_plugin");
    }

    /**
     * 测试 normalizePluginId() 无连字符时返回原值。
     */
    @Test
    void normalizePluginIdNoHyphensUnchanged() {
        assertThat(PluginMigrationConfig.normalizePluginId("myplugin"))
                .isEqualTo("myplugin");
    }

    /**
     * 测试 normalizePluginId() 多个连字符全部替换。
     */
    @Test
    void normalizePluginIdMultipleHyphens() {
        assertThat(PluginMigrationConfig.normalizePluginId("a-b-c-d"))
                .isEqualTo("a_b_c_d");
    }

    /**
     * 测试 normalizePluginId() 传入 null 抛出异常。
     */
    @Test
    void normalizePluginIdNullThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationConfig.normalizePluginId(null))
                .withMessageContaining("pluginId");
    }

    // ========== defaultLocation() / defaultHistoryTable() 测试 ==========

    /**
     * 测试默认 location 路径格式正确。
     */
    @Test
    void defaultLocationHasCorrectPrefix() {
        assertThat(PluginMigrationConfig.defaultLocation("hello"))
                .isEqualTo("classpath:db/migration/plugin/hello");
    }

    /**
     * 测试默认 history table 名称格式正确。
     */
    @Test
    void defaultHistoryTableHasCorrectPrefix() {
        assertThat(PluginMigrationConfig.defaultHistoryTable("hello-world"))
                .isEqualTo("flyway_schema_history_hello_world");
    }

    // ========== record 不可变性测试 ==========

    /**
     * 测试 record 的 accessor 方法返回正确值。
     */
    @Test
    void recordAccessorMethodsReturnCorrectValues() {
        PluginMigrationConfig config = new PluginMigrationConfig(
                "test-plugin", true, "location", "history_table", false, true);

        assertThat(config.pluginId()).isEqualTo("test-plugin");
        assertThat(config.enabled()).isTrue();
        assertThat(config.location()).isEqualTo("location");
        assertThat(config.historyTable()).isEqualTo("history_table");
        assertThat(config.baselineOnMigrate()).isFalse();
        assertThat(config.failOnError()).isTrue();
    }

    /**
     * 测试 compact constructor 对空 pluginId 抛出异常。
     */
    @Test
    void compactConstructorRejectsBlankPluginId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PluginMigrationConfig(
                        "  ", true, "loc", "tbl", true, true))
                .withMessageContaining("pluginId");
    }

    /**
     * 测试 compact constructor 对空 location 抛出异常。
     */
    @Test
    void compactConstructorRejectsBlankLocation() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PluginMigrationConfig(
                        "id", true, "  ", "tbl", true, true))
                .withMessageContaining("location");
    }

    /**
     * 测试 compact constructor 对空 historyTable 抛出异常。
     */
    @Test
    void compactConstructorRejectsBlankHistoryTable() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PluginMigrationConfig(
                        "id", true, "loc", "  ", true, true))
                .withMessageContaining("historyTable");
    }
}
