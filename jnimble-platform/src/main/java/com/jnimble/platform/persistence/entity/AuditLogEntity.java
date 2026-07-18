package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_audit_log} table.
 *
 * <p>Stores audit log entries recording system operations such as plugin
 * management, user management, and permission changes.</p>
 *
 * <p>{@code jnimble_audit_log} 表的 MyBatis-Plus 实体。
 * 存储审计日志条目，记录系统操作如插件管理、用户管理和权限变更。</p>
 */
@TableName("jnimble_audit_log")
public class AuditLogEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String actor;
    private String action;
    private String targetType;
    private String targetId;
    private String outcome;
    private String message;
    private Instant occurredAt;

    /**
     * Gets the audit log entry unique identifier.
     *
     * @return the entry ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the audit log entry unique identifier.
     *
     * @param id the entry ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the actor who performed the operation.
     *
     * @return the actor identifier
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets the actor who performed the operation.
     *
     * @param actor the actor identifier to set
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Gets the action that was performed.
     *
     * @return the action code
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action that was performed.
     *
     * @param action the action code to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets the type of the target entity.
     *
     * @return the target type
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Sets the type of the target entity.
     *
     * @param targetType the target type to set
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * Gets the identifier of the target entity.
     *
     * @return the target ID
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Sets the identifier of the target entity.
     *
     * @param targetId the target ID to set
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Gets the outcome of the operation.
     *
     * @return the outcome string (e.g., "SUCCESS", "FAILURE")
     */
    public String getOutcome() {
        return outcome;
    }

    /**
     * Sets the outcome of the operation.
     *
     * @param outcome the outcome string to set
     */
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    /**
     * Gets the detail message of the audit entry.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the detail message of the audit entry.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the timestamp when the operation occurred.
     *
     * @return the occurrence time
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Sets the timestamp when the operation occurred.
     *
     * @param occurredAt the occurrence time to set
     */
    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
