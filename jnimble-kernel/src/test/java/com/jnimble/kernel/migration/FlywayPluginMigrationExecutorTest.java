package com.jnimble.kernel.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginMigration;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link FlywayPluginMigrationExecutor} 的单元测试。
 *
 * <p>验证 migrate 方法的参数验证、ClassLoader 切换逻辑、
 * 迁移禁用时的跳过行为以及 Flyway 异常的包装处理。</p>
 */
@ExtendWith(MockitoExtension.class)
class FlywayPluginMigrationExecutorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Flyway flyway;

    private FlywayPluginMigrationExecutor executor;

    private static final String PLUGIN_ID = "test-plugin";

    @BeforeEach
    void setUp() {
        executor = new FlywayPluginMigrationExecutor(dataSource) {
            @Override
            protected Flyway flyway(PluginMigrationConfig config, ClassLoader migrationClassLoader) {
                return FlywayPluginMigrationExecutorTest.this.flyway;
            }
        };
    }

    @Test
    @DisplayName("构造函数拒绝 null DataSource")
    void constructorRejectsNullDataSource() {
        assertThatThrownBy(() -> new FlywayPluginMigrationExecutor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dataSource is required");
    }

    @Test
    @DisplayName("迁移禁用时应跳过 Flyway 调用")
    void migrateSkipsWhenMigrationDisabled() {
        PluginDescriptor descriptor = createDescriptor(false);

        executor.migrate(descriptor);

        verifyNoInteractions(flyway);
    }

    @Test
    @DisplayName("迁移启用时应调用 Flyway migrate")
    void migrateCallsFlywayWhenEnabled() {
        PluginDescriptor descriptor = createDescriptor(true);

        executor.migrate(descriptor);

        verify(flyway).migrate();
    }

    @Test
    @DisplayName("Flyway 抛出异常时应包装为 PluginMigrationException")
    void migrateWrapsFlywayExceptionIntoPluginMigrationException() {
        PluginDescriptor descriptor = createDescriptor(true);
        when(flyway.migrate()).thenThrow(new FlywayException("db error"));

        assertThatThrownBy(() -> executor.migrate(descriptor))
                .isInstanceOf(PluginMigrationException.class)
                .hasMessageContaining(PLUGIN_ID)
                .hasCauseInstanceOf(FlywayException.class);
    }

    @Test
    @DisplayName("清理禁用时应跳过 DataSource 调用")
    void cleanSkipsWhenMigrationDisabled() {
        PluginDescriptor descriptor = createDescriptor(false);

        executor.clean(descriptor);

        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("清理禁用时应跳过缺失插件包")
    void cleanSkipsMissingArtifactWhenMigrationDisabled() {
        PluginDescriptor descriptor = createDescriptor(false);

        executor.clean(descriptor, Path.of("/missing/plugin.jar"));

        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("clean 应按迁移脚本反向清理字段、表和历史表")
    void cleanDropsParsedColumnsTablesAndHistoryTable() throws Exception {
        PluginDescriptor descriptor = createDescriptor(true);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables(any(), any(), any(), any())).thenAnswer(invocation -> resultSet(true));
        when(metadata.getColumns(any(), any(), any(), any())).thenAnswer(invocation -> resultSet(true));

        executor.clean(descriptor, getClass().getClassLoader());

        InOrder order = org.mockito.Mockito.inOrder(statement);
        order.verify(statement).executeUpdate("ALTER TABLE shared_cleanup_table DROP COLUMN second_flag");
        order.verify(statement).executeUpdate("ALTER TABLE shared_cleanup_table DROP COLUMN first_flag");
        order.verify(statement).executeUpdate("DROP TABLE cleanup_owned_table");
        order.verify(statement).executeUpdate("DROP TABLE flyway_schema_history_test_plugin");
    }

    @Test
    @DisplayName("migrate(descriptor) 应使用默认 ClassLoader")
    void migrateWithDefaultClassLoader() {
        PluginDescriptor descriptor = createDescriptor(true);
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        executor.migrate(descriptor);

        verify(flyway).migrate();
    }

    @Test
    @DisplayName("migrate(descriptor, classLoader) 应传入指定 ClassLoader")
    void migrateWithSpecificClassLoader() {
        PluginDescriptor descriptor = createDescriptor(true);
        ClassLoader customClassLoader = mock(ClassLoader.class);

        executor.migrate(descriptor, customClassLoader);

        verify(flyway).migrate();
    }

    @Test
    @DisplayName("默认构造函数应使用上下文 ClassLoader 回退到自身 ClassLoader")
    void defaultConstructorUsesContextClassLoaderFallback() {
        FlywayPluginMigrationExecutor defaultExecutor = new FlywayPluginMigrationExecutor(dataSource);

        assertThat(defaultExecutor).isNotNull();
    }

    @Test
    @DisplayName("带 ClassLoader 构造函数 null 时应回退到自身 ClassLoader")
    void constructorWithNullClassLoaderFallsBack() {
        FlywayPluginMigrationExecutor fallbackExecutor =
                new FlywayPluginMigrationExecutor(dataSource, null);

        assertThat(fallbackExecutor).isNotNull();
    }

    private PluginDescriptor createDescriptor(boolean migrationEnabled) {
        PluginMigration migration = new PluginMigration(
                migrationEnabled,
                null,
                null,
                null,
                null
        );
        return new PluginDescriptor(
                "1.0",
                PLUGIN_ID,
                "Test Plugin",
                null,
                null,
                null,
                "1.0.0",
                "0.1.x",
                null,
                null,
                "com.example.TestPlugin",
                null,
                List.of(),
                migration
        );
    }

    private ResultSet resultSet(boolean exists) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(exists);
        return resultSet;
    }
}
