package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_plugin_state} table.
 *
 * <p>Stores the persisted state of plugins including their status, version,
 * descriptor metadata, and timestamps for lifecycle events.</p>
 *
 * <p>{@code jnimble_plugin_state} 表的 MyBatis-Plus 实体。
 * 存储插件的持久化状态，包括状态、版本、描述符元数据和生命周期事件时间戳。</p>
 */
@TableName("jnimble_plugin_state")
public class PluginStateEntity {

    @TableId(type = IdType.INPUT)
    private String pluginId;
    private String name;
    private String version;
    private String source;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String artifactPath;
    private Boolean enabled;
    private String status;
    private Instant installedAt;
    private Instant lastStartedAt;
    private Instant lastStoppedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastError;
    private String descriptorJson;
    private String descriptorHash;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Gets the unique plugin identifier.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Sets the unique plugin identifier.
     *
     * @param pluginId the plugin ID to set
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * Gets the plugin display name.
     *
     * @return the plugin name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the plugin display name.
     *
     * @param name the plugin name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the plugin version string.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the plugin version string.
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the plugin source identifier.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the plugin source identifier.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the artifact path on the filesystem.
     *
     * @return the artifact path
     */
    public String getArtifactPath() {
        return artifactPath;
    }

    /**
     * Sets the artifact path on the filesystem.
     *
     * @param artifactPath the artifact path to set
     */
    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    /**
     * Checks whether the plugin is enabled.
     *
     * @return {@code true} if enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether the plugin is enabled.
     *
     * @param enabled {@code true} to enable
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the plugin status string.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the plugin status string.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the installation timestamp.
     *
     * @return the installation time
     */
    public Instant getInstalledAt() {
        return installedAt;
    }

    /**
     * Sets the installation timestamp.
     *
     * @param installedAt the installation time to set
     */
    public void setInstalledAt(Instant installedAt) {
        this.installedAt = installedAt;
    }

    /**
     * Gets the last start timestamp.
     *
     * @return the last start time
     */
    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    /**
     * Sets the last start timestamp.
     *
     * @param lastStartedAt the last start time to set
     */
    public void setLastStartedAt(Instant lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    /**
     * Gets the last stop timestamp.
     *
     * @return the last stop time
     */
    public Instant getLastStoppedAt() {
        return lastStoppedAt;
    }

    /**
     * Sets the last stop timestamp.
     *
     * @param lastStoppedAt the last stop time to set
     */
    public void setLastStoppedAt(Instant lastStoppedAt) {
        this.lastStoppedAt = lastStoppedAt;
    }

    /**
     * Gets the last error message.
     *
     * @return the last error, or null if none
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Sets the last error message.
     *
     * @param lastError the last error to set
     */
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    /**
     * Gets the plugin descriptor as a JSON string.
     *
     * @return the descriptor JSON
     */
    public String getDescriptorJson() {
        return descriptorJson;
    }

    /**
     * Sets the plugin descriptor as a JSON string.
     *
     * @param descriptorJson the descriptor JSON to set
     */
    public void setDescriptorJson(String descriptorJson) {
        this.descriptorJson = descriptorJson;
    }

    /**
     * Gets the SHA-256 hash of the descriptor JSON.
     *
     * @return the descriptor hash
     */
    public String getDescriptorHash() {
        return descriptorHash;
    }

    /**
     * Sets the SHA-256 hash of the descriptor JSON.
     *
     * @param descriptorHash the descriptor hash to set
     */
    public void setDescriptorHash(String descriptorHash) {
        this.descriptorHash = descriptorHash;
    }

    /**
     * Gets the record creation timestamp.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the record creation timestamp.
     *
     * @param createdAt the creation time to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
