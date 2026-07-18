package com.jnimble.platform.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record representing an audit log entry.
 *
 * @param id         the unique record identifier (auto-generated if null)
 * @param actor      the identifier of the user or system performing the action
 * @param action     the action being performed
 * @param targetType the type of target entity
 * @param targetId   the identifier of the target entity
 * @param outcome    the outcome of the action
 * @param message    a human-readable message
 * @param occurredAt the timestamp when the event occurred
 */
public record AuditRecord(
        String id,
        String actor,
        String action,
        String targetType,
        String targetId,
        AuditOutcome outcome,
        String message,
        Instant occurredAt
) {

    /**
     * Compact constructor that applies defaults for null fields.
     */
    public AuditRecord {
        id = blankToDefault(id, UUID.randomUUID().toString());
        actor = blankToDefault(actor, "system");
        action = requireNonBlank(action, "action");
        targetType = blankToDefault(targetType, "unknown");
        targetId = blankToNull(targetId);
        outcome = outcome == null ? AuditOutcome.SUCCESS : outcome;
        message = blankToNull(message);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    /**
     * Creates a new audit record with auto-generated id and timestamp.
     *
     * @param actor      the actor identifier
     * @param action     the action
     * @param targetType the target type
     * @param targetId   the target id
     * @param outcome    the outcome
     * @param message    the message
     * @return a new audit record
     */
    public static AuditRecord create(
            String actor,
            String action,
            String targetType,
            String targetId,
            AuditOutcome outcome,
            String message
    ) {
        return new AuditRecord(null, actor, action, targetType, targetId, outcome, message, null);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
