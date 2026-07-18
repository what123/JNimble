package com.jnimble.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnimble.platform.audit.AuditOutcome;
import com.jnimble.platform.audit.AuditRecord;
import com.jnimble.platform.audit.AuditService;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 审计日志集成测试。
 *
 * <p>测试审计日志的记录、查询和不同操作结果的记录功能。</p>
 * <p>使用 H2 内存数据库进行隔离测试。</p>
 */
@SpringBootTest(
        properties = {
                "jnimble.plugins.auto-enable=false",
                "jnimble.plugins.restore-enabled=false",
                "jnimble.plugins.directory-scan-enabled=false"
        })
@ActiveProfiles("test")
class AuditIntegrationTest {

    @Autowired
    private AuditService auditService;

    /**
     * 测试成功操作的审计日志记录
     */
    @Test
    @DisplayName("成功操作审计日志记录测试")
    void shouldRecordSuccessfulAuditEvent() {
        // 1. 记录成功操作
        AuditRecord record = auditService.recordSuccess(
                "plugin.install",
                "plugin",
                "test-plugin",
                "Test plugin installed"
        );

        // 2. 验证审计记录
        assertThat(record).isNotNull();
        assertThat(record.id()).isNotBlank();
        assertThat(record.action()).isEqualTo("plugin.install");
        assertThat(record.targetType()).isEqualTo("plugin");
        assertThat(record.targetId()).isEqualTo("test-plugin");
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(record.message()).isEqualTo("Test plugin installed");
        assertThat(record.occurredAt()).isNotNull();
    }

    /**
     * 测试失败操作的审计日志记录
     */
    @Test
    @DisplayName("失败操作审计日志记录测试")
    void shouldRecordFailedAuditEvent() {
        // 1. 记录失败操作
        AuditRecord record = auditService.recordFailure(
                "plugin.enable",
                "plugin",
                "invalid-plugin",
                "插件启用失败：找不到插件描述符"
        );

        // 2. 验证审计记录
        assertThat(record).isNotNull();
        assertThat(record.id()).isNotBlank();
        assertThat(record.action()).isEqualTo("plugin.enable");
        assertThat(record.targetType()).isEqualTo("plugin");
        assertThat(record.targetId()).isEqualTo("invalid-plugin");
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.message()).isEqualTo("插件启用失败：找不到插件描述符");
    }

    /**
     * 测试审计日志查询功能：记录多条日志后，应能按时间倒序查询
     */
    @Test
    @DisplayName("审计日志查询功能测试")
    void shouldQueryRecentAuditRecords() {
        // 1. 记录多条审计日志
        auditService.recordSuccess("user.login", "user", "admin", "管理员登录成功");
        auditService.recordSuccess("plugin.install", "plugin", "test-plugin", "Test plugin installed");
        auditService.recordFailure("permission.grant", "role", "editor", "权限授予失败：角色不存在");

        // 2. 查询最近的审计记录
        Collection<AuditRecord> recentRecords = auditService.listRecent(10);

        // 3. 验证查询结果
        assertThat(recentRecords).isNotEmpty();
        assertThat(recentRecords.size()).isGreaterThanOrEqualTo(3);

        // 4. 验证记录按时间倒序排列（最新的在前）
        AuditRecord[] records = recentRecords.toArray(new AuditRecord[0]);
        for (int i = 0; i < records.length - 1; i++) {
            assertThat(records[i].occurredAt()).isAfterOrEqualTo(records[i + 1].occurredAt());
        }
    }

    /**
     * 测试不同目标类型的审计日志记录
     */
    @Test
    @DisplayName("不同目标类型审计日志记录测试")
    void shouldRecordAuditEventsForDifferentTargetTypes() {
        // 1. 记录不同目标类型的审计日志
        auditService.recordSuccess("role.create", "role", "ADMIN", "创建管理员角色");
        auditService.recordSuccess("user.create", "user", "testuser", "创建测试用户");
        auditService.recordSuccess("permission.register", "permission", "crm.view", "注册CRM查看权限");

        // 2. 查询所有记录
        Collection<AuditRecord> records = auditService.listRecent(100);

        // 3. 验证不同目标类型的记录都存在
        assertThat(records).filteredOn(r -> "role".equals(r.targetType())).isNotEmpty();
        assertThat(records).filteredOn(r -> "user".equals(r.targetType())).isNotEmpty();
        assertThat(records).filteredOn(r -> "permission".equals(r.targetType())).isNotEmpty();
    }

    /**
     * 测试自定义操作者的审计日志记录
     */
    @Test
    @DisplayName("自定义操作者审计日志记录测试")
    void shouldRecordAuditEventWithCustomActor() {
        // 1. 使用自定义操作者记录审计日志
        AuditRecord record = auditService.record(
                "system-admin",
                "system.maintenance",
                "system",
                null,
                AuditOutcome.SUCCESS,
                "系统维护完成"
        );

        // 2. 验证操作者信息
        assertThat(record).isNotNull();
        assertThat(record.actor()).isEqualTo("system-admin");
        assertThat(record.action()).isEqualTo("system.maintenance");
        assertThat(record.targetType()).isEqualTo("system");
        assertThat(record.targetId()).isNull();
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
    }
}
