package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_role} table.
 *
 * <p>Stores role definitions used in the RBAC system. Each role has a unique
 * code and a human-readable name, and can be assigned to subjects (users)
 * via the subject-role association table.</p>
 *
 * <p>{@code jnimble_role} 表的 MyBatis-Plus 实体。
 * 存储 RBAC 系统中使用的角色定义。每个角色有唯一编码和可读名称，
 * 可通过主体-角色关联表分配给用户。</p>
 */
@TableName("jnimble_role")
public class RoleEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String code;
    private String name;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Gets the role unique identifier.
     *
     * @return the role ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the role unique identifier.
     *
     * @param id the role ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the role code (unique business key).
     *
     * @return the role code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the role code (unique business key).
     *
     * @param code the role code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the role display name.
     *
     * @return the role name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the role display name.
     *
     * @param name the role name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the role status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the role status.
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
