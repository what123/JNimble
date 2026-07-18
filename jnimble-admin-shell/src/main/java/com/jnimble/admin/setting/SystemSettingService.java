package com.jnimble.admin.setting;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.SystemSettingEntity;
import com.jnimble.platform.persistence.mapper.SystemSettingMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and retrieves framework-wide system settings (site name, logo, etc.).
 *
 * <p>系统设置服务，负责存储和读取框架级配置（站点名称、Logo 等）。
 * 使用 MyBatis-Plus 和 MapperUtils 进行数据库操作。</p>
 */
@Service
public class SystemSettingService {

    /** Setting key for the site display name. */
    public static final String KEY_SITE_NAME = "site.name";

    /** Setting key for the site subtitle/tagline. */
    public static final String KEY_SITE_SUBTITLE = "site.subtitle";

    /** Setting key for the site logo URL. */
    public static final String KEY_SITE_LOGO_URL = "site.logoUrl";

    private final SystemSettingMapper mapper;
    private volatile Map<String, String> cache;

    /**
     * Constructs a new system setting service.
     *
     * @param mapper the MyBatis mapper for system settings
     */
    public SystemSettingService(SystemSettingMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns all system setting records ordered by key.
     *
     * @return a list of all setting records
     */
    public List<SystemSettingRecord> listAll() {
        return MapperUtils.selectList(mapper, SystemSettingEntity.class, w -> w
                .orderByAsc("setting_key"))
                .stream()
                .map(SystemSettingService::toRecord)
                .toList();
    }

    /**
     * Returns a defensive copy of all settings as a key-value map (cached).
     *
     * @return an unmodifiable map of all setting key-value pairs
     */
    public Map<String, String> findAll() {
        Map<String, String> cached = cache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cache == null) {
                cache = loadAll();
            }
            return cache;
        }
    }

    /**
     * Returns the value for a single key, or null if absent.
     *
     * @param key the setting key
     * @return the setting value, or null if not found
     */
    public String get(String key) {
        return findAll().get(key);
    }

    /**
     * Returns the composed site branding snapshot (name, subtitle, logo URL).
     *
     * @return the site branding configuration
     */
    public SiteBranding siteBranding() {
        Map<String, String> values = findAll();
        return new SiteBranding(
                values.get(KEY_SITE_NAME),
                values.get(KEY_SITE_SUBTITLE),
                values.get(KEY_SITE_LOGO_URL)
        );
    }

    /**
     * Upserts a setting value and invalidates the cache.
     *
     * @param key      the setting key
     * @param value    the setting value
     * @param operator the operator identifier
     * @return the updated setting record
     */
    @Transactional
    public SystemSettingRecord save(String key, String value, String operator) {
        String normalizedKey = normalizeKey(key);
        String normalizedValue = value == null ? "" : value.trim();
        String normalizedOperator = operator == null || operator.isBlank() ? "system" : operator.trim();
        Instant now = Instant.now();
        upsert(normalizedKey, normalizedValue, normalizedOperator, now);
        invalidateCache();
        return find(normalizedKey);
    }

    /**
     * Saves multiple settings in one transaction.
     *
     * @param settings a map of setting key-value pairs
     * @param operator the operator identifier
     */
    @Transactional
    public void saveAll(Map<String, String> settings, String operator) {
        if (settings == null || settings.isEmpty()) {
            return;
        }
        String normalizedOperator = operator == null || operator.isBlank() ? "system" : operator.trim();
        Instant now = Instant.now();
        settings.forEach((key, value) -> {
            String normalizedKey = normalizeKey(key);
            String normalizedValue = value == null ? "" : value.trim();
            upsert(normalizedKey, normalizedValue, normalizedOperator, now);
        });
        invalidateCache();
    }

    /**
     * Returns a single setting record by key, throwing an exception if not found.
     *
     * @param key the setting key
     * @return the setting record
     * @throws IllegalArgumentException if the key does not exist
     */
    public SystemSettingRecord find(String key) {
        String normalizedKey = normalizeKey(key);
        SystemSettingEntity entity = MapperUtils.selectOne(mapper, SystemSettingEntity.class,
                w -> w.eq("setting_key", normalizedKey));
        if (entity == null) {
            throw new IllegalArgumentException("设置不存在：" + normalizedKey);
        }
        return toRecord(entity);
    }

    private void upsert(String key, String value, String operator, Instant now) {
        SystemSettingEntity existing = MapperUtils.selectOne(mapper, SystemSettingEntity.class,
                w -> w.eq("setting_key", key));
        if (existing != null) {
            existing.setSettingValue(value);
            existing.setUpdatedBy(operator);
            existing.setUpdatedAt(now);
            MapperUtils.updateById(mapper, existing);
        } else {
            SystemSettingEntity entity = new SystemSettingEntity();
            entity.setSettingKey(key);
            entity.setSettingValue(value);
            entity.setUpdatedBy(operator);
            entity.setUpdatedAt(now);
            MapperUtils.insert(mapper, entity);
        }
    }

    private Map<String, String> loadAll() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSettingRecord record : listAll()) {
            map.put(record.settingKey(), record.settingValue());
        }
        return Map.copyOf(map);
    }

    private void invalidateCache() {
        cache = null;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("设置键不能为空");
        }
        return key.trim();
    }

    private static SystemSettingRecord toRecord(SystemSettingEntity entity) {
        return new SystemSettingRecord(
                entity.getSettingKey(),
                entity.getSettingValue(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
