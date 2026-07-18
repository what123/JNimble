package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_user} table.
 *
 * <p>Provides CRUD operations for user accounts via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_user} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供用户账户的 CRUD 操作。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
