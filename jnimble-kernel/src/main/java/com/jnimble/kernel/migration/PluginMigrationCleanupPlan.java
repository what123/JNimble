package com.jnimble.kernel.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed destructive cleanup operations from plugin migration scripts.
 */
final class PluginMigrationCleanupPlan {

    private static final String IDENTIFIER = "(?:`[^`]+`|\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_]*)";
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "(?is)\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(" + IDENTIFIER + ")");
    private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile(
            "(?is)\\bALTER\\s+TABLE\\s+(" + IDENTIFIER + ")\\s+(.*)");
    private static final Pattern ADD_COLUMN_PATTERN = Pattern.compile(
            "(?is)\\bADD\\s+COLUMN\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(" + IDENTIFIER + ")");

    private final List<Operation> operations;

    private PluginMigrationCleanupPlan(List<Operation> operations) {
        this.operations = List.copyOf(operations);
    }

    static PluginMigrationCleanupPlan empty() {
        return new PluginMigrationCleanupPlan(List.of());
    }

    static PluginMigrationCleanupPlan fromScripts(List<String> scripts) {
        List<Operation> operations = new ArrayList<>();
        for (String script : scripts) {
            operations.addAll(fromSql(script).operations);
        }
        return new PluginMigrationCleanupPlan(operations);
    }

    static PluginMigrationCleanupPlan fromSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return empty();
        }
        List<Operation> operations = new ArrayList<>();
        for (String statement : splitStatements(stripComments(sql))) {
            addCreateTableOperation(statement, operations);
            addColumnOperations(statement, operations);
        }
        return new PluginMigrationCleanupPlan(operations);
    }

    List<Operation> operationsInReverseOrder() {
        List<Operation> reversed = new ArrayList<>(operations);
        Collections.reverse(reversed);
        return reversed;
    }

    boolean isEmpty() {
        return operations.isEmpty();
    }

    static String validateIdentifier(String identifier) {
        String normalized = normalizeIdentifier(identifier);
        if (!SIMPLE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Unsupported database identifier: " + identifier);
        }
        return normalized;
    }

    private static void addCreateTableOperation(String statement, List<Operation> operations) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(statement);
        if (matcher.find()) {
            operations.add(Operation.dropTable(validateIdentifier(matcher.group(1))));
        }
    }

    private static void addColumnOperations(String statement, List<Operation> operations) {
        Matcher alterMatcher = ALTER_TABLE_PATTERN.matcher(statement);
        if (!alterMatcher.find()) {
            return;
        }
        String table = validateIdentifier(alterMatcher.group(1));
        Matcher columnMatcher = ADD_COLUMN_PATTERN.matcher(alterMatcher.group(2));
        while (columnMatcher.find()) {
            operations.add(Operation.dropColumn(table, validateIdentifier(columnMatcher.group(1))));
        }
    }

    private static String stripComments(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)^\\s*--.*$", "");
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean backtickQuoted = false;
        for (int i = 0; i < sql.length(); i++) {
            char value = sql.charAt(i);
            if (value == '\'' && !doubleQuoted && !backtickQuoted) {
                current.append(value);
                if (singleQuoted && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(sql.charAt(i + 1));
                    i++;
                } else {
                    singleQuoted = !singleQuoted;
                }
                continue;
            }
            if (value == '"' && !singleQuoted && !backtickQuoted) {
                doubleQuoted = !doubleQuoted;
            } else if (value == '`' && !singleQuoted && !doubleQuoted) {
                backtickQuoted = !backtickQuoted;
            }
            if (value == ';' && !singleQuoted && !doubleQuoted && !backtickQuoted) {
                addStatement(statements, current);
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    private static String normalizeIdentifier(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if ((normalized.startsWith("`") && normalized.endsWith("`"))
                || (normalized.startsWith("\"") && normalized.endsWith("\""))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    enum OperationType {
        DROP_TABLE,
        DROP_COLUMN
    }

    record Operation(OperationType type, String table, String column) {

        static Operation dropTable(String table) {
            return new Operation(OperationType.DROP_TABLE, table, null);
        }

        static Operation dropColumn(String table, String column) {
            return new Operation(OperationType.DROP_COLUMN, table, column);
        }
    }
}
