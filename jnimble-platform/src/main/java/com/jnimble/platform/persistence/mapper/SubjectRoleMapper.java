package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.SubjectRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for the {@code jnimble_subject_role} table.
 *
 * <p>Provides CRUD operations for subject-role assignments and a custom
 * query to check if a subject has a specific permission through their roles.</p>
 *
 * <p>{@code jnimble_subject_role} 表的 MyBatis-Plus 映射器。
 * 提供主体-角色分配的 CRUD 操作以及检查主体是否通过其角色拥有特定权限的自定义查询。</p>
 */
@Mapper
public interface SubjectRoleMapper extends BaseMapper<SubjectRoleEntity> {

    /**
     * Counts whether a subject has a specific permission through their assigned roles.
     *
     * <p>Joins across subject-role, role, role-permission, and permission tables
     * to verify the permission chain is active.</p>
     *
     * @param subjectId        the subject identifier (typically username)
     * @param permissionCode   the permission code to check
     * @param roleStatus       the expected role status (e.g., "ACTIVE")
     * @param permissionStatus the expected permission status (e.g., "AVAILABLE")
     * @return the count of matching permission paths (0 or more)
     */
    @Select("""
            select count(*)
              from jnimble_subject_role sr
              join jnimble_role r on r.id = sr.role_id
              join jnimble_role_permission rp on rp.role_id = r.id
              join jnimble_permission p on p.code = rp.permission_code
             where sr.subject_id = #{subjectId}
               and rp.permission_code = #{permissionCode}
               and r.status = #{roleStatus}
               and rp.status = #{permissionStatus}
               and p.status = #{permissionStatus}
            """)
    int countSubjectPermission(
            @Param("subjectId") String subjectId,
            @Param("permissionCode") String permissionCode,
            @Param("roleStatus") String roleStatus,
            @Param("permissionStatus") String permissionStatus
    );
}
