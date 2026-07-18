package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for the {@code jnimble_role_permission} table.
 *
 * <p>Provides CRUD operations for role-permission associations and a custom
 * query to count active permissions for a specific role.</p>
 *
 * <p>{@code jnimble_role_permission} 表的 MyBatis-Plus 映射器。
 * 提供角色-权限关联的 CRUD 操作以及统计指定角色活跃权限数量的自定义查询。</p>
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {

    /**
     * Counts active role-permission associations for a given role and permission.
     *
     * <p>Checks that the role is active, the permission status matches, and
     * the permission entity itself is available.</p>
     *
     * @param roleId           the role ID
     * @param permissionCode   the permission code
     * @param roleStatus       the expected role status (e.g., "ACTIVE")
     * @param permissionStatus the expected permission status (e.g., "AVAILABLE")
     * @return the count of matching active associations (0 or 1)
     */
    @Select("""
            select count(*)
              from jnimble_role r
              join jnimble_role_permission rp on rp.role_id = r.id
              join jnimble_permission p on p.code = rp.permission_code
             where r.id = #{roleId}
               and rp.permission_code = #{permissionCode}
               and r.status = #{roleStatus}
               and rp.status = #{permissionStatus}
               and p.status = #{permissionStatus}
            """)
    int countActiveRolePermission(
            @Param("roleId") String roleId,
            @Param("permissionCode") String permissionCode,
            @Param("roleStatus") String roleStatus,
            @Param("permissionStatus") String permissionStatus
    );
}
