package com.jnimble.platform.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnimble.platform.persistence.mapper.PluginStateMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for plugin state persistence.
 *
 * <p>Creates the {@link PluginStateStore} bean backed by MyBatis-Plus,
 * using Jackson {@link ObjectMapper} for plugin descriptor serialization.</p>
 *
 * <p>插件状态持久化的 Spring 配置。创建基于 MyBatis-Plus 的 PluginStateStore Bean，
 * 使用 Jackson ObjectMapper 进行插件描述符序列化。</p>
 */
@Configuration
public class PluginStateConfiguration {

    /**
     * Creates the plugin state store bean.
     *
     * @param pluginStateMapper   the MyBatis mapper for plugin state table
     * @param objectMapperProvider the ObjectMapper provider (falls back to default if unavailable)
     * @return a new MybatisPluginStateStore instance
     */
    @Bean
    PluginStateStore jnimblePluginStateStore(
            PluginStateMapper pluginStateMapper,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {
        return new MybatisPluginStateStore(pluginStateMapper, objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }
}
