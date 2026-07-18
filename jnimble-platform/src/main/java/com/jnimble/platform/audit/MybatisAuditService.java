package com.jnimble.platform.audit;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.AuditLogEntity;
import com.jnimble.platform.persistence.mapper.AuditLogMapper;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatis-backed implementation of {@link AuditService}.
 *
 * <p>Persists audit log entries to the database via MyBatis-Plus mappers.
 * Failed writes are logged as warnings rather than thrown, ensuring audit
 * failures do not disrupt the main operation flow.</p>
 *
 * <p>基于 MyBatis 的 AuditService 实现。通过 MyBatis-Plus 映射器将审计日志
 * 条目持久化到数据库。写入失败时记录警告而非抛出异常，确保审计失败不影响主操作流程。</p>
 */
public class MybatisAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(MybatisAuditService.class);

    private final AuditLogMapper auditLogMapper;

    /**
     * Creates a new MyBatis-backed audit service.
     *
     * @param auditLogMapper the MyBatis mapper for audit log table
     */
    public MybatisAuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuditRecord record(
            String actor,
            String action,
            String targetType,
            String targetId,
            AuditOutcome outcome,
            String message
    ) {
        AuditRecord record = AuditRecord.create(actor, action, targetType, targetId, outcome, message);
        try {
            MapperUtils.insert(auditLogMapper, toEntity(record));
        } catch (RuntimeException ex) {
            log.warn("Failed to write audit record: action={}, targetType={}, targetId={}",
                    record.action(), record.targetType(), record.targetId(), ex);
        }
        return record;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AuditRecord> listRecent(int limit) {
        try {
            return MapperUtils.selectList(auditLogMapper, AuditLogEntity.class,
                            wrapper -> wrapper.orderByDesc("occurred_at").orderByDesc("id")
                                    .last("limit " + normalizeLimit(limit)))
                    .stream()
                    .map(this::toRecord)
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Failed to read recent audit records.", ex);
            return List.of();
        }
    }

    private AuditLogEntity toEntity(AuditRecord record) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(record.id());
        entity.setActor(record.actor());
        entity.setAction(record.action());
        entity.setTargetType(record.targetType());
        entity.setTargetId(record.targetId());
        entity.setOutcome(record.outcome().name());
        entity.setMessage(record.message());
        entity.setOccurredAt(record.occurredAt());
        return entity;
    }

    private AuditRecord toRecord(AuditLogEntity entity) {
        return new AuditRecord(
                entity.getId(),
                entity.getActor(),
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                AuditOutcome.valueOf(entity.getOutcome()),
                entity.getMessage(),
                entity.getOccurredAt()
        );
    }

    /**
     * Normalizes the limit value to a safe range (1-500).
     *
     * @param limit the requested limit
     * @return a clamped value between 1 and 500
     */
    private static int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
