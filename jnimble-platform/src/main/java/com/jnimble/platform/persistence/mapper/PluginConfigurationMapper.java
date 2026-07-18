package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.PluginConfigurationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_plugin_configuration} table.
 *
 * <p>Provides CRUD operations for plugin configurations via the
 * {@link BaseMapper} interface. Note that the table uses a composite
 * primary key (plugin_id, config_key), so conditional queries should
 * always include both columns.</p>
 *
 * <p>{@code jnimble_plugin_configuration} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供插件配置的 CRUD 操作。注意该表使用复合主键
 * （plugin_id, config_key），条件查询应始终同时包含这两个列。</p>
 */
@Mapper
public interface PluginConfigurationMapper extends BaseMapper<PluginConfigurationEntity> {
}
