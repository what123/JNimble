package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.PluginStateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_plugin_state} table.
 *
 * <p>Provides CRUD operations for plugin state persistence via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_plugin_state} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供插件状态持久化的 CRUD 操作。</p>
 */
@Mapper
public interface PluginStateMapper extends BaseMapper<PluginStateEntity> {
}
