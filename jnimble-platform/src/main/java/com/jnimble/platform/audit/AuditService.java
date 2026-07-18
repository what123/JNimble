package com.jnimble.platform.audit;

import java.util.Collection;

/**
 * Service interface for recording and querying audit logs.
 *
 * <p>Audit logs track important operations performed in the system, such as
 * plugin installation, user management, and permission changes. Each record
 * captures the actor, action, target, and outcome.</p>
 */
public interface AuditService {

    /**
     * Records an audit event.
     *
     * @param actor      the identifier of the user or system performing the action
     * @param action     the action being performed (e.g., "plugin.install")
     * @param targetType the type of target entity (e.g., "plugin", "user")
     * @param targetId   the identifier of the target entity
     * @param outcome    the outcome of the action ({@link AuditOutcome#SUCCESS} or {@link AuditOutcome#FAILURE})
     * @param message    a human-readable message describing the event
     * @return the created audit record
     */
    AuditRecord record(
            String actor,
            String action,
            String targetType,
            String targetId,
            AuditOutcome outcome,
            String message
    );

    /**
     * Records a successful audit event for the current actor.
     *
     * @param action     the action being performed
     * @param targetType the type of target entity
     * @param targetId   the identifier of the target entity
     * @param message    a human-readable message
     * @return the created audit record
     */
    default AuditRecord recordSuccess(String action, String targetType, String targetId, String message) {
        return record(AuditActors.currentActor(), action, targetType, targetId, AuditOutcome.SUCCESS, message);
    }

    /**
     * Records a failed audit event for the current actor.
     *
     * @param action     the action being performed
     * @param targetType the type of target entity
     * @param targetId   the identifier of the target entity
     * @param message    a human-readable message
     * @return the created audit record
     */
    default AuditRecord recordFailure(String action, String targetType, String targetId, String message) {
        return record(AuditActors.currentActor(), action, targetType, targetId, AuditOutcome.FAILURE, message);
    }

    /**
     * Lists the most recent audit records.
     *
     * @param limit the maximum number of records to return
     * @return a collection of recent audit records, ordered by timestamp descending
     */
    Collection<AuditRecord> listRecent(int limit);
}
