package com.jnimble.kernel.plugin.descriptor;

import com.jnimble.kernel.plugin.PluginIds;
import com.jnimble.sdk.plugin.PluginAdminDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginConfigurationOption;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginPermission;
import com.jnimble.sdk.plugin.PluginSpringDescriptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates plugin descriptors for structural integrity and platform compatibility.
 *
 * <p>Checks required fields, identifier formats, dependency declarations,
 * permission codes, configuration definitions, and platform version matching.</p>
 *
 * 校验插件描述符的结构完整性和平台兼容性。
 * 检查必填字段、标识符格式、依赖声明、权限代码、配置定义和平台版本匹配。
 */
public class PluginDescriptorValidator {

    /** The only schema version supported by this validator. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1.0";

    private final String currentPlatformVersion;

    /**
     * Creates a validator with the current platform version for compatibility checks.
     *
     * @param currentPlatformVersion the current JNimble platform version
     */
    public PluginDescriptorValidator(String currentPlatformVersion) {
        this.currentPlatformVersion = currentPlatformVersion;
    }

    /**
     * Validates a plugin descriptor, throwing an exception if any violations are found.
     *
     * @param descriptor the plugin descriptor to validate
     * @throws PluginDescriptorValidationException if validation fails
     */
    public void validate(PluginDescriptor descriptor) {
        List<String> violations = new ArrayList<>();

        if (descriptor == null) {
            violations.add("descriptor is required");
            throw new PluginDescriptorValidationException(violations);
        }

        requireNonBlank(descriptor.schemaVersion(), "schemaVersion", violations);
        requireNonBlank(descriptor.id(), "id", violations);
        requireNonBlank(descriptor.name(), "name", violations);
        requireNonBlank(descriptor.version(), "version", violations);
        requireNonBlank(descriptor.platformVersion(), "platformVersion", violations);
        requireNonBlank(descriptor.bootClass(), "bootClass", violations);

        if (!isBlank(descriptor.schemaVersion()) && !SUPPORTED_SCHEMA_VERSION.equals(descriptor.schemaVersion().trim())) {
            violations.add("schemaVersion must be " + SUPPORTED_SCHEMA_VERSION);
        }

        if (!isBlank(descriptor.id()) && !PluginIds.isValid(descriptor.id())) {
            violations.add("id must match " + PluginIds.PATTERN.pattern());
        }

        if (!isBlank(descriptor.id())) {
            validatePermissionCodes(descriptor.id(), descriptor.permissions(), violations);
            validateAdmin(descriptor, violations);
            validateSpring(descriptor.spring(), violations);
            validateDependencies(descriptor, violations);
        }
        validateConfiguration(descriptor.configuration(), violations);

        if (!isBlank(descriptor.platformVersion())
                && !PluginPlatformVersionMatcher.matches(descriptor.platformVersion(), currentPlatformVersion)) {
            violations.add("platformVersion " + descriptor.platformVersion()
                    + " is not compatible with current platform " + currentPlatformVersion);
        }

        if (!violations.isEmpty()) {
            throw new PluginDescriptorValidationException(violations);
        }
    }

    private void validateSpring(PluginSpringDescriptor spring, List<String> violations) {
        if (spring == null) {
            return;
        }
        requireNonBlank(spring.configurationClass(), "spring configurationClass", violations);
        if (!isBlank(spring.configurationClass())
                && !spring.configurationClass().trim().matches(
                "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            violations.add("spring configurationClass is invalid");
        }
    }

    private void validateDependencies(PluginDescriptor descriptor, List<String> violations) {
        if (descriptor.dependencies() == null || descriptor.dependencies().isEmpty()) {
            return;
        }
        Set<String> dependencyIds = new HashSet<>();
        for (PluginDependency dependency : descriptor.dependencies()) {
            if (dependency == null) {
                violations.add("dependencies must not contain null entries");
                continue;
            }
            if (isBlank(dependency.pluginId())) {
                violations.add("dependency pluginId is required");
                continue;
            }
            String pluginId = dependency.pluginId().trim();
            if (!PluginIds.isValid(pluginId)) {
                violations.add("dependency pluginId " + pluginId + " must match " + PluginIds.PATTERN.pattern());
            }
            if (pluginId.equals(descriptor.id())) {
                violations.add("plugin must not depend on itself");
            }
            if (!dependencyIds.add(pluginId)) {
                violations.add("duplicate dependency " + pluginId);
            }
            if (dependency.required() && isBlank(dependency.version())) {
                violations.add("dependency version is required for " + pluginId);
            }
        }
    }

    private void validateConfiguration(
            PluginConfigurationDescriptor configuration,
            List<String> violations
    ) {
        if (configuration == null) {
            return;
        }
        List<PluginConfigurationField> fields = configuration.fields();
        if (fields == null || fields.isEmpty()) {
            violations.add("configuration fields must not be empty");
            return;
        }
        if (fields.size() > 100) {
            violations.add("configuration fields must not exceed 100 entries");
        }
        Set<String> keys = new HashSet<>();
        for (PluginConfigurationField field : fields) {
            if (field == null) {
                violations.add("configuration fields must not contain null entries");
                continue;
            }
            if (isBlank(field.key())) {
                violations.add("configuration field key is required");
                continue;
            }
            String key = field.key().trim();
            if (!key.matches("[a-z][A-Za-z0-9._-]{0,127}")) {
                violations.add("configuration field key " + key + " is invalid");
            }
            if (!keys.add(key)) {
                violations.add("duplicate configuration field key " + key);
            }
            if (isBlank(field.label()) && isBlank(field.labelKey())) {
                violations.add("configuration field " + key + " label or labelKey is required");
            }
            if (field.type() == null) {
                violations.add("configuration field " + key + " type is required");
                continue;
            }
            validateConfigurationDefault(field, key, violations);
            validateConfigurationOptions(field, key, violations);
        }
    }

    private void validateConfigurationDefault(
            PluginConfigurationField field,
            String key,
            List<String> violations
    ) {
        String defaultValue = field.defaultValue();
        if (defaultValue == null) {
            return;
        }
        if (field.type() == PluginConfigurationFieldType.SECRET) {
            violations.add("configuration secret field " + key + " must not declare a default value");
            return;
        }
        if (field.type() == PluginConfigurationFieldType.BOOLEAN
                && !List.of("true", "false").contains(defaultValue.toLowerCase())) {
            violations.add("configuration boolean field " + key + " defaultValue must be true or false");
        }
        if (field.type() == PluginConfigurationFieldType.NUMBER) {
            try {
                new BigDecimal(defaultValue);
            } catch (NumberFormatException ex) {
                violations.add("configuration number field " + key + " defaultValue is invalid");
            }
        }
    }

    private void validateConfigurationOptions(
            PluginConfigurationField field,
            String key,
            List<String> violations
    ) {
        List<PluginConfigurationOption> options = field.options();
        if (field.type() != PluginConfigurationFieldType.SELECT) {
            return;
        }
        if (options == null || options.isEmpty()) {
            violations.add("configuration select field " + key + " options must not be empty");
            return;
        }
        Set<String> values = new HashSet<>();
        for (PluginConfigurationOption option : options) {
            if (option == null || isBlank(option.value())) {
                violations.add("configuration select field " + key + " option value is required");
                continue;
            }
            String value = option.value().trim();
            if (!values.add(value)) {
                violations.add("configuration select field " + key + " has duplicate option " + value);
            }
            if (isBlank(option.label()) && isBlank(option.labelKey())) {
                violations.add("configuration select field " + key + " option " + value
                        + " label or labelKey is required");
            }
        }
        if (field.defaultValue() != null && !values.contains(field.defaultValue())) {
            violations.add("configuration select field " + key + " defaultValue must match an option");
        }
    }

    private void validateAdmin(PluginDescriptor descriptor, List<String> violations) {
        PluginAdminDescriptor admin = descriptor.admin();
        if (admin == null) {
            return;
        }

        if (isBlank(admin.entry())) {
            violations.add("admin entry is required");
        } else {
            String entry = admin.entry().trim();
            if (!entry.startsWith("/")) {
                violations.add("admin entry must start with /");
            }
            if (entry.startsWith("//") || entry.contains("\\") || entry.contains("?") || entry.contains("#")
                    || List.of(entry.split("/")).contains("..")) {
                violations.add("admin entry must be a safe plugin-relative path");
            }
        }

        if (isBlank(admin.permission())) {
            violations.add("admin permission is required");
            return;
        }

        String permission = admin.permission().trim();
        boolean declared = descriptor.permissions() != null && descriptor.permissions().stream()
                .filter(java.util.Objects::nonNull)
                .map(PluginPermission::code)
                .filter(code -> !isBlank(code))
                .map(String::trim)
                .anyMatch(permission::equals);
        if (!declared) {
            violations.add("admin permission " + permission + " must be declared in permissions");
        }
    }

    private void validatePermissionCodes(
            String pluginId,
            List<PluginPermission> permissions,
            List<String> violations
    ) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        String requiredPrefix = pluginId + ".";
        Set<String> permissionCodes = new HashSet<>();
        for (PluginPermission permission : permissions) {
            if (permission == null) {
                violations.add("permissions must not contain null entries");
                continue;
            }
            String code = permission.code();
            if (isBlank(code)) {
                violations.add("permission code is required");
                continue;
            }
            String normalizedCode = code.trim();
            if (!normalizedCode.startsWith(requiredPrefix)) {
                violations.add("permission code " + normalizedCode + " must start with " + requiredPrefix);
            }
            if (!permissionCodes.add(normalizedCode)) {
                violations.add("duplicate permission code " + normalizedCode);
            }
        }
    }

    private static void requireNonBlank(String value, String field, List<String> violations) {
        if (isBlank(value)) {
            violations.add(field + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
