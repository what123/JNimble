package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_permission} table.
 *
 * <p>Stores registered permissions in the system. Permissions are declared by
 * plugins or the system itself, and can be granted to roles for access control.</p>
 *
 * <p>{@code jnimble_permission} 表的 MyBatis-Plus 实体。
 * 存储系统中注册的权限。权限由插件或系统自身声明，可授予角色以实现访问控制。</p>
 */
@TableName("jnimble_permission")
public class PermissionEntity {

    @TableId(type = IdType.INPUT)
    private String code;
    private String pluginId;
    private String name;
    private String nameKey;
    private String description;
    private String descriptionKey;
    private String status;
    private Instant updatedAt;

    /**
     * Gets the permission code (unique identifier).
     *
     * @return the permission code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the permission code (unique identifier).
     *
     * @param code the permission code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the plugin ID that registered this permission.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Sets the plugin ID that registered this permission.
     *
     * @param pluginId the plugin ID to set
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * Gets the display name of the permission.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the permission.
     *
     * @param name the display name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the i18n key for the permission name.
     *
     * @return the name key
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * Sets the i18n key for the permission name.
     *
     * @param nameKey the name key to set
     */
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    /**
     * Gets the description of the permission.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the permission.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the i18n key for the permission description.
     *
     * @return the description key
     */
    public String getDescriptionKey() {
        return descriptionKey;
    }

    /**
     * Sets the i18n key for the permission description.
     *
     * @param descriptionKey the description key to set
     */
    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    /**
     * Gets the permission status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the permission status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
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
