package com.jnimble.admin.plugin;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.PluginConfigurationEntity;
import com.jnimble.platform.persistence.mapper.PluginConfigurationMapper;
import com.jnimble.sdk.plugin.PluginConfiguration;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginConfigurationOption;
import com.jnimble.sdk.plugin.PluginDescriptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing plugin configuration values with optional encryption support.
 *
 * <p>Provides CRUD operations for plugin configuration parameters, supporting
 * transparent encryption of SECRET type fields. Implements the
 * {@link PluginConfiguration} interface for runtime lookups.</p>
 *
 * <p>插件配置管理服务。提供插件配置参数的增删改查操作，支持对机密类型字段进行
 * 透明加密。实现 {@link PluginConfiguration} 接口以供运行时查找。
 * 使用 MyBatis-Plus 和 MapperUtils 进行数据库操作。</p>
 */
@Service
public class PluginConfigurationService implements PluginConfiguration {

    private static final int MAX_VALUE_LENGTH = 10_000;

    private final PluginConfigurationMapper mapper;
    private final PluginConfigurationCrypto crypto;

    /**
     * Constructs a new plugin configuration service.
     *
     * @param mapper the MyBatis mapper for plugin configurations
     * @param crypto the configuration encryption helper
     */
    public PluginConfigurationService(PluginConfigurationMapper mapper, PluginConfigurationCrypto crypto) {
        this.mapper = mapper;
        this.crypto = crypto;
    }

    @Override
    public Optional<String> find(String pluginId, String key) {
        return Optional.ofNullable(values(pluginId).get(key));
    }

    @Override
    public Map<String, String> values(String pluginId) {
        requirePluginId(pluginId);
        String trimmedId = pluginId.trim();
        List<PluginConfigurationEntity> entities = MapperUtils.selectList(mapper,
                PluginConfigurationEntity.class,
                w -> w.eq("plugin_id", trimmedId).orderByAsc("config_key"));
        Map<String, String> values = new LinkedHashMap<>();
        for (PluginConfigurationEntity entity : entities) {
            String storedValue = entity.getConfigValue();
            values.put(entity.getConfigKey(),
                    Boolean.TRUE.equals(entity.getEncrypted())
                            ? crypto.decrypt(trimmedId, entity.getConfigKey(), storedValue)
                            : storedValue);
        }
        return Map.copyOf(values);
    }

    /**
     * Returns the configuration values for display purposes, with secrets hidden.
     *
     * @param descriptor the plugin descriptor
     * @return the configuration values view, with secret fields blanked out
     */
    public ConfigurationValues configurationValues(PluginDescriptor descriptor) {
        PluginConfigurationDescriptor configuration = requireConfiguration(descriptor);
        Map<String, String> stored = values(descriptor.id());
        Map<String, String> displayed = new LinkedHashMap<>();
        for (PluginConfigurationField field : configuration.fields()) {
            if (field.type() == PluginConfigurationFieldType.SECRET) {
                displayed.put(field.key(), "");
            } else {
                displayed.put(field.key(), stored.getOrDefault(
                        field.key(), field.defaultValue() == null ? "" : field.defaultValue()));
            }
        }
        return new ConfigurationValues(Map.copyOf(displayed), Set.copyOf(stored.keySet()));
    }

    /**
     * Saves or updates configuration parameters for a plugin.
     *
     * <p>Existing parameters are deleted and re-inserted. Secret-type values
     * are encrypted before storage.</p>
     *
     * @param descriptor the plugin descriptor
     * @param submitted  the submitted configuration parameter key-value pairs
     * @param actor      the actor performing the save
     */
    @Transactional
    public void save(PluginDescriptor descriptor, Map<String, String> submitted, String actor) {
        PluginConfigurationDescriptor configuration = requireConfiguration(descriptor);
        Map<String, String> existing = values(descriptor.id());
        for (PluginConfigurationField field : configuration.fields()) {
            String rawValue = submitted == null ? null : submitted.get(field.key());
            if (field.type() == PluginConfigurationFieldType.SECRET
                    && (rawValue == null || rawValue.isBlank())
                    && existing.containsKey(field.key())) {
                continue;
            }
            String normalized = normalize(field, rawValue);
            if (normalized == null) {
                if (field.required()) {
                    throw new IllegalArgumentException(fieldName(field) + " is required");
                }
                delete(descriptor.id(), field.key());
                continue;
            }
            if (normalized.length() > MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(fieldName(field) + " must not exceed "
                        + MAX_VALUE_LENGTH + " characters");
            }
            boolean encrypted = field.type() == PluginConfigurationFieldType.SECRET;
            String storedValue = encrypted
                    ? crypto.encrypt(descriptor.id(), field.key(), normalized)
                    : normalized;
            upsert(descriptor.id(), field.key(), storedValue, encrypted, actor);
        }
    }

    private String normalize(PluginConfigurationField field, String rawValue) {
        if (field.type() == PluginConfigurationFieldType.BOOLEAN) {
            return Boolean.toString(Boolean.parseBoolean(rawValue));
        }
        String value = rawValue == null ? null : rawValue.trim();
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (field.type() == PluginConfigurationFieldType.NUMBER) {
            try {
                return new BigDecimal(value).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(fieldName(field) + " must be a valid number");
            }
        }
        if (field.type() == PluginConfigurationFieldType.SELECT) {
            List<PluginConfigurationOption> options = field.options() == null ? List.of() : field.options();
            Set<String> allowed = options.stream()
                    .map(PluginConfigurationOption::value)
                    .collect(Collectors.toSet());
            if (!allowed.contains(value)) {
                throw new IllegalArgumentException(fieldName(field) + " contains an unsupported option");
            }
        }
        return value;
    }

    private void upsert(
            String pluginId,
            String key,
            String value,
            boolean encrypted,
            String actor
    ) {
        PluginConfigurationEntity existing = MapperUtils.selectOne(mapper,
                PluginConfigurationEntity.class,
                w -> w.eq("plugin_id", pluginId).eq("config_key", key));
        if (existing != null) {
            existing.setConfigValue(value);
            existing.setEncrypted(encrypted);
            existing.setUpdatedBy(normalizeActor(actor));
            existing.setUpdatedAt(LocalDateTime.now());
            MapperUtils.updateOne(mapper, existing, PluginConfigurationEntity.class,
                    w -> w.eq("plugin_id", pluginId).eq("config_key", key));
        } else {
            PluginConfigurationEntity entity = new PluginConfigurationEntity();
            entity.setPluginId(pluginId);
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            entity.setEncrypted(encrypted);
            entity.setUpdatedBy(normalizeActor(actor));
            entity.setUpdatedAt(LocalDateTime.now());
            MapperUtils.insert(mapper, entity);
        }
    }

    private void delete(String pluginId, String key) {
        MapperUtils.deleteByCondition(mapper, PluginConfigurationEntity.class,
                w -> w.eq("plugin_id", pluginId).eq("config_key", key));
    }

    private PluginConfigurationDescriptor requireConfiguration(PluginDescriptor descriptor) {
        if (descriptor == null || descriptor.configuration() == null
                || descriptor.configuration().fields() == null
                || descriptor.configuration().fields().isEmpty()) {
            throw new IllegalArgumentException("Plugin does not declare configurable parameters");
        }
        return descriptor.configuration();
    }

    private void requirePluginId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id is required");
        }
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private String fieldName(PluginConfigurationField field) {
        return field.label() == null || field.label().isBlank() ? field.key() : field.label();
    }

    /**
     * View model for plugin configuration values displayed in the admin UI.
     *
     * @param displayedValues the configuration values to display (secrets omitted)
     * @param configuredKeys  the set of keys that have been explicitly configured
     */
    public record ConfigurationValues(Map<String, String> displayedValues, Set<String> configuredKeys) {
    }
}
