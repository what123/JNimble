package com.jnimble.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnimble.platform.persistence.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the {@code jnimble_audit_log} table.
 *
 * <p>Provides CRUD operations for audit log entries via the
 * {@link BaseMapper} interface.</p>
 *
 * <p>{@code jnimble_audit_log} 表的 MyBatis-Plus 映射器。
 * 通过 BaseMapper 接口提供审计日志条目的 CRUD 操作。</p>
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
