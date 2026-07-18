package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus entity for the {@code jnimble_plugin_configuration} table.
 *
 * <p>Stores plugin configuration key-value pairs with optional encryption.
 * The table has a composite primary key (plugin_id, config_key), so conditional
 * operations should always include both columns.</p>
 *
 * <p>{@code jnimble_plugin_configuration} 表的 MyBatis-Plus 实体。
 * 存储插件配置键值对，支持可选加密。该表具有复合主键（plugin_id, config_key），
 * 因此条件操作应始终同时包含这两个列。</p>
 */
@TableName("jnimble_plugin_configuration")
public class PluginConfigurationEntity {

    @TableId(type = IdType.INPUT)
    private String pluginId;
    private String configKey;
    private String configValue;
    private Boolean encrypted;
    private String updatedBy;
    private LocalDateTime updatedAt;

    /**
     * Gets the plugin identifier (part of composite primary key).
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Sets the plugin identifier (part of composite primary key).
     *
     * @param pluginId the plugin ID to set
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * Gets the configuration key (part of composite primary key).
     *
     * @return the config key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Sets the configuration key (part of composite primary key).
     *
     * @param configKey the config key to set
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * Gets the configuration value (may be encrypted).
     *
     * @return the config value
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * Sets the configuration value.
     *
     * @param configValue the config value to set
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    /**
     * Returns whether the value is encrypted.
     *
     * @return {@code true} if encrypted
     */
    public Boolean getEncrypted() {
        return encrypted;
    }

    /**
     * Sets whether the value is encrypted.
     *
     * @param encrypted the encrypted flag to set
     */
    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * Gets the operator who last updated this configuration.
     *
     * @return the operator identifier
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the operator who last updated this configuration.
     *
     * @param updatedBy the operator identifier to set
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the update time
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt the update time to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
