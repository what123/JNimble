package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.LanguageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_language} table.
 *
 * <p>Provides CRUD operations for managed languages via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_language} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供受管理语言的 CRUD 操作。</p>
 */
@Mapper
public interface LanguageMapper extends BaseMapper<LanguageEntity> {
}
