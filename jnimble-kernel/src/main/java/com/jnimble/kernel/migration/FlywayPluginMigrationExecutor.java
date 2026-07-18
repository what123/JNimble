package com.jnimble.kernel.migration;

import com.jnimble.sdk.plugin.PluginDescriptor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Flyway-based implementation of {@link PluginMigrationExecutor} that executes
 * database migrations for plugins using Flyway. Each plugin can define its own
 * migration scripts and history table.
 */
public class FlywayPluginMigrationExecutor implements PluginMigrationExecutor {

    private static final String[] TABLE_TYPES = {"TABLE"};

    private final DataSource dataSource;
    private final ClassLoader classLoader;

    /**
     * Creates a Flyway migration executor with the current thread's context class loader.
     *
     * @param dataSource the data source for database connections
     */
    public FlywayPluginMigrationExecutor(DataSource dataSource) {
        this(dataSource, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a Flyway migration executor with a specific class loader.
     *
     * @param dataSource  the data source for database connections
     * @param classLoader the class loader for resolving migration script resources
     */
    public FlywayPluginMigrationExecutor(DataSource dataSource, ClassLoader classLoader) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource is required");
        this.classLoader = classLoader == null ? FlywayPluginMigrationExecutor.class.getClassLoader() : classLoader;
    }

    @Override
    public void migrate(PluginDescriptor descriptor) {
        migrate(descriptor, classLoader);
    }

    @Override
    public void migrate(PluginDescriptor descriptor, ClassLoader migrationClassLoader) {
        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);
        if (!config.enabled()) {
            return;
        }

        try {
            flyway(config, migrationClassLoader).migrate();
        } catch (FlywayException ex) {
            throw new PluginMigrationException(
                    "Plugin migration failed for plugin " + config.pluginId()
                            + " at " + config.location()
                            + " using history table " + config.historyTable()
                            + " with failOnError=" + config.failOnError(),
                    ex
            );
        }
    }

    @Override
    public void clean(PluginDescriptor descriptor) {
        clean(descriptor, classLoader);
    }

    @Override
    public void clean(PluginDescriptor descriptor, Path artifactPath) {
        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);
        if (!config.enabled()) {
            return;
        }
        if (artifactPath == null) {
            clean(descriptor, classLoader);
            return;
        }
        Path normalizedPath = artifactPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedPath)) {
            throw new PluginMigrationException("Plugin artifact does not exist: " + normalizedPath);
        }
        try (URLClassLoader pluginClassLoader = new URLClassLoader(
                new URL[]{normalizedPath.toUri().toURL()},
                classLoader)) {
            clean(descriptor, pluginClassLoader);
        } catch (MalformedURLException ex) {
            throw new PluginMigrationException("Invalid plugin artifact URL: " + normalizedPath, ex);
        } catch (IOException ex) {
            throw new PluginMigrationException("Failed to close plugin artifact classloader: " + normalizedPath, ex);
        }
    }

    @Override
    public void clean(PluginDescriptor descriptor, ClassLoader migrationClassLoader) {
        PluginMigrationConfig config = PluginMigrationConfig.from(descriptor);
        if (!config.enabled()) {
            return;
        }

        try {
            PluginMigrationCleanupPlan plan = cleanupPlan(config, migrationClassLoader);
            executeCleanup(config, plan);
        } catch (IOException | SQLException ex) {
            throw cleanupException(config, ex);
        } catch (RuntimeException ex) {
            if (ex instanceof PluginMigrationException pluginMigrationException) {
                throw pluginMigrationException;
            }
            throw cleanupException(config, ex);
        }
    }

    protected Flyway flyway(PluginMigrationConfig config) {
        return flyway(config, classLoader);
    }

    protected Flyway flyway(PluginMigrationConfig config, ClassLoader migrationClassLoader) {
        ClassLoader effectiveClassLoader = migrationClassLoader == null ? classLoader : migrationClassLoader;
        return Flyway.configure(effectiveClassLoader)
                .dataSource(dataSource)
                .locations(config.location())
                .table(config.historyTable())
                .baselineOnMigrate(config.baselineOnMigrate())
                .baselineVersion("0")
                .failOnMissingLocations(false)
                .load();
    }

    protected PluginMigrationCleanupPlan cleanupPlan(
            PluginMigrationConfig config,
            ClassLoader migrationClassLoader
    ) throws IOException {
        ClassLoader effectiveClassLoader = migrationClassLoader == null ? classLoader : migrationClassLoader;
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(effectiveClassLoader);
        Resource[] resources = resolver.getResources(resourcePattern(config.location()));
        List<Resource> sortedResources = Arrays.stream(resources)
                .filter(Resource::isReadable)
                .sorted(Comparator.comparing(this::resourceSortKey))
                .toList();
        List<String> scripts = new ArrayList<>();
        for (Resource resource : sortedResources) {
            scripts.add(readResource(resource));
        }
        return PluginMigrationCleanupPlan.fromScripts(scripts);
    }

    protected void executeCleanup(PluginMigrationConfig config, PluginMigrationCleanupPlan plan) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (!plan.isEmpty()) {
                for (PluginMigrationCleanupPlan.Operation operation : plan.operationsInReverseOrder()) {
                    executeCleanupOperation(connection, statement, operation);
                }
            }
            dropTableIfExists(connection, statement,
                    PluginMigrationCleanupPlan.validateIdentifier(config.historyTable()));
        }
    }

    private void executeCleanupOperation(
            Connection connection,
            Statement statement,
            PluginMigrationCleanupPlan.Operation operation
    ) throws SQLException {
        switch (operation.type()) {
            case DROP_TABLE -> dropTableIfExists(connection, statement, operation.table());
            case DROP_COLUMN -> dropColumnIfExists(connection, statement, operation.table(), operation.column());
            default -> throw new IllegalStateException("Unsupported cleanup operation: " + operation.type());
        }
    }

    private void dropTableIfExists(Connection connection, Statement statement, String table) throws SQLException {
        if (tableExists(connection, table)) {
            statement.executeUpdate("DROP TABLE " + table);
        }
    }

    private void dropColumnIfExists(
            Connection connection,
            Statement statement,
            String table,
            String column
    ) throws SQLException {
        if (columnExists(connection, table, column)) {
            statement.executeUpdate("ALTER TABLE " + table + " DROP COLUMN " + column);
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String candidate : identifierCandidates(table)) {
            try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, candidate, TABLE_TYPES)) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String tableCandidate : identifierCandidates(table)) {
            for (String columnCandidate : identifierCandidates(column)) {
                try (ResultSet resultSet = metadata.getColumns(
                        connection.getCatalog(), null, tableCandidate, columnCandidate)) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> identifierCandidates(String identifier) {
        return List.of(identifier, identifier.toUpperCase(Locale.ROOT), identifier.toLowerCase(Locale.ROOT))
                .stream()
                .distinct()
                .toList();
    }

    private String resourcePattern(String location) {
        String normalizedLocation = location.endsWith("/") ? location.substring(0, location.length() - 1) : location;
        if (normalizedLocation.startsWith("classpath*:")) {
            return normalizedLocation + "/*.sql";
        }
        if (normalizedLocation.startsWith("classpath:")) {
            return "classpath*:" + normalizedLocation.substring("classpath:".length()) + "/*.sql";
        }
        if (normalizedLocation.startsWith("filesystem:")) {
            return "file:" + normalizedLocation.substring("filesystem:".length()) + "/*.sql";
        }
        return normalizedLocation + "/*.sql";
    }

    private String readResource(Resource resource) throws IOException {
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String resourceSortKey(Resource resource) {
        String filename = resource.getFilename();
        return (filename == null ? "" : filename) + "|" + resource.getDescription();
    }

    private PluginMigrationException cleanupException(PluginMigrationConfig config, Exception ex) {
        return new PluginMigrationException(
                "Plugin data cleanup failed for plugin " + config.pluginId()
                        + " at " + config.location()
                        + " using history table " + config.historyTable(),
                ex
        );
    }
}
