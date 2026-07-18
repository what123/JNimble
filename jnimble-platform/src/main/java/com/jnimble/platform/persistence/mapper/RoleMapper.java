package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_role} table.
 *
 * <p>Provides CRUD operations for role definitions via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_role} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供角色定义的 CRUD 操作。</p>
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
