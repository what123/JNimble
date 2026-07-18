package com.jnimble.admin.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves and persists storage directory configuration at runtime.
 *
 * <p>存储目录配置服务，负责在运行时解析和持久化插件目录与 Logo 目录。
 * 目录解析优先级：数据库配置 > 启动属性/环境变量 > 硬编码默认值。</p>
 */
@Service
public class StorageConfigService {

    /** Setting key for the plugin JAR storage directory. */
    public static final String KEY_PLUGIN_DIR = "storage.plugin-dir";

    /** Setting key for the logo image storage directory. */
    public static final String KEY_LOGO_DIR = "storage.logo-dir";

    private final SystemSettingService settingService;
    private final String defaultPluginDir;
    private final String defaultLogoDir;

    /**
     * Constructs a new storage config service.
     *
     * @param settingService   the system setting service for DB persistence
     * @param defaultPluginDir the fallback plugin directory from properties
     * @param defaultLogoDir   the fallback logo directory from properties
     */
    public StorageConfigService(
            SystemSettingService settingService,
            @Value("${jnimble.plugins.dir:./plugins}") String defaultPluginDir,
            @Value("${jnimble.branding.logo-storage-dir:./data/branding}") String defaultLogoDir
    ) {
        this.settingService = settingService;
        this.defaultPluginDir = defaultPluginDir;
        this.defaultLogoDir = defaultLogoDir;
    }

    /**
     * Resolves the effective plugin storage directory.
     *
     * @return the plugin directory path
     */
    public String resolvePluginDir() {
        String dbValue = settingService.get(KEY_PLUGIN_DIR);
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue.trim();
        }
        return defaultPluginDir;
    }

    /**
     * Resolves the effective logo storage directory.
     *
     * @return the logo directory path
     */
    public String resolveLogoDir() {
        String dbValue = settingService.get(KEY_LOGO_DIR);
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue.trim();
        }
        return defaultLogoDir;
    }

    /**
     * Returns the current storage configuration snapshot.
     *
     * @return the current storage config
     */
    public StorageConfig currentConfig() {
        return new StorageConfig(resolvePluginDir(), resolveLogoDir());
    }

    /**
     * Persists storage directory settings.
     *
     * @param config   the storage configuration to save
     * @param operator the operator identifier
     */
    @Transactional
    public void save(StorageConfig config, String operator) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put(KEY_PLUGIN_DIR, config.pluginDir() == null ? "" : config.pluginDir().trim());
        settings.put(KEY_LOGO_DIR, config.logoDir() == null ? "" : config.logoDir().trim());
        settingService.saveAll(settings, operator);
    }
}
