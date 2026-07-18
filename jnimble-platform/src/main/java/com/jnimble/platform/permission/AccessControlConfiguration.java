package com.jnimble.platform.permission;

import com.jnimble.platform.persistence.mapper.PermissionMapper;
import com.jnimble.platform.persistence.mapper.RoleMapper;
import com.jnimble.platform.persistence.mapper.RolePermissionMapper;
import com.jnimble.platform.persistence.mapper.SubjectRoleMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the access control (RBAC) system.
 *
 * <p>Creates the {@link MybatisAccessControlService} bean which implements
 * {@link RoleService}, {@link PermissionService}, and {@link AuthorizationService}
 * interfaces using MyBatis-Plus mappers.</p>
 *
 * <p>访问控制（RBAC）系统的 Spring 配置。创建 MybatisAccessControlService Bean，
 * 它使用 MyBatis-Plus 映射器实现 RoleService、PermissionService 和 AuthorizationService 接口。</p>
 */
@Configuration
@EnableConfigurationProperties(AccessControlProperties.class)
public class AccessControlConfiguration {

    /**
     * Creates the access control service bean.
     *
     * @param roleMapper           the MyBatis mapper for role table
     * @param permissionMapper     the MyBatis mapper for permission table
     * @param rolePermissionMapper the MyBatis mapper for role-permission association table
     * @param subjectRoleMapper    the MyBatis mapper for subject-role association table
     * @return a new MybatisAccessControlService instance
     */
    @Bean
    MybatisAccessControlService mybatisAccessControlService(
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            SubjectRoleMapper subjectRoleMapper
    ) {
        return new MybatisAccessControlService(roleMapper, permissionMapper, rolePermissionMapper, subjectRoleMapper);
    }
}
