package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_permission} table.
 *
 * <p>Provides CRUD operations for permission definitions via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_permission} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供权限定义的 CRUD 操作。</p>
 */
@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
}
