package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_role_permission} table.
 *
 * <p>Associates permissions with roles for role-based access control.
 * Each record represents a permission grant to a role, with status tracking
 * for availability management.</p>
 *
 * <p>{@code jnimble_role_permission} 表的 MyBatis-Plus 实体。
 * 将权限与角色关联以实现基于角色的访问控制。每条记录代表一个角色被授予的权限，
 * 并包含状态跟踪以管理可用性。</p>
 */
@TableName("jnimble_role_permission")
public class RolePermissionEntity {

    private String roleId;
    private String permissionCode;
    private String status;
    private Instant grantedAt;
    private Instant updatedAt;

    /**
     * Gets the role identifier.
     *
     * @return the role ID
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Sets the role identifier.
     *
     * @param roleId the role ID to set
     */
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    /**
     * Gets the permission code.
     *
     * @return the permission code
     */
    public String getPermissionCode() {
        return permissionCode;
    }

    /**
     * Sets the permission code.
     *
     * @param permissionCode the permission code to set
     */
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    /**
     * Gets the association status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the association status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the timestamp when the permission was granted.
     *
     * @return the grant time
     */
    public Instant getGrantedAt() {
        return grantedAt;
    }

    /**
     * Sets the timestamp when the permission was granted.
     *
     * @param grantedAt the grant time to set
     */
    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
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
