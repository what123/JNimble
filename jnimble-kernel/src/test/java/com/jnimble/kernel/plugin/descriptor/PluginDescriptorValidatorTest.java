package com.jnimble.kernel.plugin.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.jnimble.sdk.plugin.PluginAdminDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginConfigurationOption;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginPermission;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link PluginDescriptorValidator} 单元测试。
 *
 * <p>覆盖 schema version 校验、插件 ID 格式校验、权限 code 前缀校验等核心逻辑。</p>
 */
class PluginDescriptorValidatorTest {

    private final PluginDescriptorValidator validator =
            new PluginDescriptorValidator("1.0");

    // ========== 辅助方法 ==========

    private static PluginDescriptor buildValidDescriptor() {
        return new PluginDescriptor(
                "1.0",
                "example-hello",
                "Hello Plugin",
                null,
                null,
                null,
                "1.0.0",
                "1.0",
                null,
                null,
                "example.HelloPlugin",
                null,
                List.of(),
                null);
    }

    private static PluginDescriptorValidationException assertValidationFails(Runnable action) {
        try {
            action.run();
            org.junit.jupiter.api.Assertions.fail("Expected PluginDescriptorValidationException");
            return null;
        } catch (PluginDescriptorValidationException ex) {
            return ex;
        }
    }

    // ========== Null descriptor 测试 ==========

    /**
     * 测试空 descriptor 直接抛出校验异常。
     */
    @Test
    void rejectNullDescriptor() {
        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(null));
        assertThat(ex.violations()).contains("descriptor is required");
    }

    // ========== Schema version 校验 ==========

    /**
     * 测试支持的 schema version 1.0 校验通过。
     */
    @Test
    void acceptSupportedSchemaVersion() {
        assertThatNoException().isThrownBy(() -> validator.validate(buildValidDescriptor()));
    }

    /**
     * 测试不支持的 schema version 抛出校验异常。
     */
    @Test
    void rejectUnsupportedSchemaVersion() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "2.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("schemaVersion must be 1.0"));
    }

    /**
     * 测试 schema version 为空时报必填错误。
     */
    @Test
    void schemaVersionBlankReportsRequired() {
        PluginDescriptor descriptor = new PluginDescriptor(
                null, "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("schemaVersion is required"));
    }

    // ========== Plugin ID 格式校验 ==========

    /**
     * 测试合法的插件 ID（小写字母、数字、连字符）校验通过。
     */
    @Test
    void acceptValidPluginId() {
        assertThatNoException().isThrownBy(() -> validator.validate(buildValidDescriptor()));
    }

    /**
     * 测试包含大写字母的插件 ID 被拒绝。
     */
    @Test
    void rejectPluginIdWithUppercase() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "Example-Hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("id must match"));
    }

    /**
     * 测试以数字开头的插件 ID 被拒绝。
     */
    @Test
    void rejectPluginIdStartingWithDigit() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "1hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("id must match"));
    }

    /**
     * 测试包含特殊字符的插件 ID 被拒绝。
     */
    @Test
    void rejectPluginIdWithSpecialCharacters() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example@hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("id must match"));
    }

    // ========== Permission code 前缀校验 ==========

    /**
     * 测试权限 code 带正确插件 ID 前缀校验通过。
     */
    @Test
    void acceptPermissionCodeWithCorrectPrefix() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                List.of(new PluginPermission("example-hello.greet", "Greet", "greet")),
                null);

        assertThatNoException().isThrownBy(() -> validator.validate(descriptor));
    }

    /**
     * 测试权限 code 缺少插件 ID 前缀被拒绝。
     */
    @Test
    void rejectPermissionCodeWithoutPluginIdPrefix() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                List.of(new PluginPermission("other.greet", "Greet", "greet")),
                null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("must start with example-hello."));
    }

    /**
     * 测试重复的权限 code 被拒绝。
     */
    @Test
    void rejectDuplicatePermissionCodes() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                List.of(
                        new PluginPermission("example-hello.greet", "Greet", "greet"),
                        new PluginPermission("example-hello.greet", "Greet Again", "greet2")),
                null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("duplicate permission code"));
    }

    /**
     * 测试空权限 code 被拒绝。
     */
    @Test
    void rejectBlankPermissionCode() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                List.of(new PluginPermission("  ", "No Code", "no-code")),
                null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("permission code is required"));
    }

    /**
     * 测试 null 权限条目被拒绝。
     */
    @Test
    void rejectNullPermissionEntry() {
        List<PluginPermission> permissions = new ArrayList<>();
        permissions.add(null);
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                permissions,
                null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("must not contain null"));
    }

    // ========== Admin entry 校验 ==========

    @Test
    void acceptDeclaredAdminEntry() {
        PluginDescriptor descriptor = descriptorWithAdmin(
                new PluginAdminDescriptor("/settings", "plugin.settings", "example-hello.settings"),
                List.of(new PluginPermission("example-hello.settings", "Settings", null))
        );

        assertThatNoException().isThrownBy(() -> validator.validate(descriptor));
    }

    @Test
    void rejectUnsafeAdminEntry() {
        PluginDescriptor descriptor = descriptorWithAdmin(
                new PluginAdminDescriptor("../settings", "plugin.settings", "example-hello.settings"),
                List.of(new PluginPermission("example-hello.settings", "Settings", null))
        );

        PluginDescriptorValidationException ex = assertValidationFails(() -> validator.validate(descriptor));

        assertThat(ex.violations()).contains("admin entry must start with /");
        assertThat(ex.violations()).contains("admin entry must be a safe plugin-relative path");
    }

    @Test
    void rejectUndeclaredAdminPermission() {
        PluginDescriptor descriptor = descriptorWithAdmin(
                new PluginAdminDescriptor("/settings", "plugin.settings", "example-hello.settings"),
                List.of()
        );

        PluginDescriptorValidationException ex = assertValidationFails(() -> validator.validate(descriptor));

        assertThat(ex.violations())
                .contains("admin permission example-hello.settings must be declared in permissions");
    }

    @Test
    void acceptDeclarativePluginConfiguration() {
        PluginConfigurationField field = new PluginConfigurationField(
                "merchantId", "Merchant ID", null, null, null, null, null,
                PluginConfigurationFieldType.TEXT, true, null, null
        );

        assertThatNoException().isThrownBy(() -> validator.validate(
                descriptorWithConfiguration(new PluginConfigurationDescriptor(
                        "Settings", null, null, null, List.of(field)))));
    }

    @Test
    void rejectSecretDefaultAndDuplicateConfigurationKeys() {
        PluginConfigurationField first = new PluginConfigurationField(
                "apiKey", "API Key", null, null, null, null, null,
                PluginConfigurationFieldType.SECRET, false, "do-not-store-in-descriptor", null
        );
        PluginConfigurationField duplicate = new PluginConfigurationField(
                "apiKey", "API Key Again", null, null, null, null, null,
                PluginConfigurationFieldType.TEXT, false, null, null
        );
        PluginDescriptor descriptor = descriptorWithConfiguration(new PluginConfigurationDescriptor(
                "Settings", null, null, null, List.of(first, duplicate)));

        PluginDescriptorValidationException ex = assertValidationFails(() -> validator.validate(descriptor));

        assertThat(ex.violations()).contains(
                "configuration secret field apiKey must not declare a default value",
                "duplicate configuration field key apiKey"
        );
    }

    @Test
    void rejectSelectConfigurationWithoutOptions() {
        PluginConfigurationField field = new PluginConfigurationField(
                "environment", "Environment", null, null, null, null, null,
                PluginConfigurationFieldType.SELECT, false, "live", List.<PluginConfigurationOption>of()
        );

        PluginDescriptorValidationException ex = assertValidationFails(() -> validator.validate(
                descriptorWithConfiguration(new PluginConfigurationDescriptor(
                        "Settings", null, null, null, List.of(field)))));

        assertThat(ex.violations()).contains(
                "configuration select field environment options must not be empty");
    }

    private static PluginDescriptor descriptorWithConfiguration(
            PluginConfigurationDescriptor configuration
    ) {
        return new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                null, null, null, configuration, List.of(), null
        );
    }

    private static PluginDescriptor descriptorWithAdmin(
            PluginAdminDescriptor admin,
            List<PluginPermission> permissions
    ) {
        return new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "1.0", null, null, "example.HelloPlugin", null,
                admin, permissions, null
        );
    }

    // ========== Platform version 兼容性校验 ==========

    /**
     * 测试兼容的 platform version 校验通过。
     */
    @Test
    void acceptCompatiblePlatformVersion() {
        assertThatNoException().isThrownBy(() -> validator.validate(buildValidDescriptor()));
    }

    /**
     * 测试不兼容的 platform version 被拒绝。
     */
    @Test
    void rejectIncompatiblePlatformVersion() {
        PluginDescriptor descriptor = new PluginDescriptor(
                "1.0", "example-hello", "Hello Plugin", null, null, null,
                "1.0.0", "2.0", null, null, "example.HelloPlugin", null, List.of(), null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("is not compatible with current platform"));
    }

    // ========== 必填字段缺失 ==========

    /**
     * 测试多个必填字段缺失时一次性报告所有违规。
     */
    @Test
    void reportAllMissingRequiredFields() {
        PluginDescriptor descriptor = new PluginDescriptor(
                null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null);

        PluginDescriptorValidationException ex =
                assertValidationFails(() -> validator.validate(descriptor));
        assertThat(ex.violations()).anyMatch(v -> v.contains("schemaVersion is required"));
        assertThat(ex.violations()).anyMatch(v -> v.contains("id is required"));
        assertThat(ex.violations()).anyMatch(v -> v.contains("name is required"));
        assertThat(ex.violations()).anyMatch(v -> v.contains("version is required"));
        assertThat(ex.violations()).anyMatch(v -> v.contains("platformVersion is required"));
        assertThat(ex.violations()).anyMatch(v -> v.contains("bootClass is required"));
    }
}
