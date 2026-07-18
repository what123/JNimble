package com.jnimble.platform.audit;

import com.jnimble.platform.persistence.mapper.AuditLogMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the audit logging system.
 *
 * <p>Creates the {@link AuditService} bean backed by MyBatis-Plus for
 * persistent audit log storage.</p>
 *
 * <p>审计日志系统的 Spring 配置。创建基于 MyBatis-Plus 的 AuditService Bean，
 * 实现审计日志的持久化存储。</p>
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditConfiguration {

    /**
     * Creates the audit service bean.
     *
     * @param auditLogMapper the MyBatis mapper for audit log table
     * @return a new MybatisAuditService instance
     */
    @Bean
    AuditService jnimbleAuditService(AuditLogMapper auditLogMapper) {
        return new MybatisAuditService(auditLogMapper);
    }
}
