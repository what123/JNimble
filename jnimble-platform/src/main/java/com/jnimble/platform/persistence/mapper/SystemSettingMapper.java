package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.SystemSettingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_system_setting} table.
 *
 * <p>Provides CRUD operations for system settings via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_system_setting} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供系统设置的 CRUD 操作。</p>
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSettingEntity> {
}
