package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_user} table.
 *
 * <p>Stores user account information including credentials, display name,
 * and account status for authentication purposes.</p>
 *
 * <p>{@code jnimble_user} 表的 MyBatis-Plus 实体。
 * 存储用户账户信息，包括凭证、显示名和账户状态，用于认证。</p>
 */
@TableName("jnimble_user")
public class UserEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Gets the user unique identifier.
     *
     * @return the user ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the user unique identifier.
     *
     * @param id the user ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password hash.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the password hash.
     *
     * @param passwordHash the password hash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the account status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the account status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
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
