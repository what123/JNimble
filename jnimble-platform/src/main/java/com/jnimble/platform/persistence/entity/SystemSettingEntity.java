package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_system_setting} table.
 *
 * <p>Stores framework-wide key-value settings such as site name, subtitle, and logo URL.</p>
 *
 * <p>{@code jnimble_system_setting} 表的 MyBatis-Plus 实体。
 * 存储框架级键值配置，如站点名称、副标题和 Logo 地址。</p>
 */
@TableName("jnimble_system_setting")
public class SystemSettingEntity {

    @TableId(type = IdType.INPUT)
    private String settingKey;
    private String settingValue;
    private String updatedBy;
    private Instant updatedAt;

    /**
     * Gets the setting key (primary key).
     *
     * @return the setting key
     */
    public String getSettingKey() {
        return settingKey;
    }

    /**
     * Sets the setting key (primary key).
     *
     * @param settingKey the setting key to set
     */
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    /**
     * Gets the setting value.
     *
     * @return the setting value
     */
    public String getSettingValue() {
        return settingValue;
    }

    /**
     * Sets the setting value.
     *
     * @param settingValue the setting value to set
     */
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    /**
     * Gets the operator who last updated this setting.
     *
     * @return the operator identifier
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the operator who last updated this setting.
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
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt the update time to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
