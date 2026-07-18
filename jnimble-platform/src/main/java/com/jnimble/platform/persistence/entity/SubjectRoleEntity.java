package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_subject_role} table.
 *
 * <p>Associates subjects (users) with roles for role-based access control.
 * Each record represents a role assignment to a user.</p>
 *
 * <p>{@code jnimble_subject_role} 表的 MyBatis-Plus 实体。
 * 将主体（用户）与角色关联以实现基于角色的访问控制。每条记录代表一个用户的角色分配。</p>
 */
@TableName("jnimble_subject_role")
public class SubjectRoleEntity {

    private String subjectId;
    private String roleId;
    private Instant grantedAt;

    /**
     * Gets the subject identifier (typically username).
     *
     * @return the subject ID
     */
    public String getSubjectId() {
        return subjectId;
    }

    /**
     * Sets the subject identifier (typically username).
     *
     * @param subjectId the subject ID to set
     */
    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

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
     * Gets the timestamp when the role was granted.
     *
     * @return the grant time
     */
    public Instant getGrantedAt() {
        return grantedAt;
    }

    /**
     * Sets the timestamp when the role was granted.
     *
     * @param grantedAt the grant time to set
     */
    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }
}
