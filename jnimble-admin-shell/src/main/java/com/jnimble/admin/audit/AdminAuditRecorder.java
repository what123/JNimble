package com.jnimble.admin.audit;

import com.jnimble.platform.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component for recording audit events in the admin interface.
 * Provides convenience methods for success and failure audit recording
 * with built-in error handling.
 */
@Component
public class AdminAuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditRecorder.class);

    private final AuditService auditService;

    public AdminAuditRecorder(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Records a successful audit event.
     *
     * @param action     the action that was performed
     * @param targetType the type of the target entity
     * @param targetId   the ID of the target entity
     * @param message    optional message describing the event
     */
    public void success(String action, String targetType, String targetId, String message) {
        try {
            auditService.recordSuccess(action, targetType, targetId, message);
        } catch (RuntimeException ex) {
            log.warn("Failed to record success audit: action={}, targetType={}, targetId={}",
                    action, targetType, targetId, ex);
        }
    }

    /**
     * Records a failed audit event.
     *
     * @param action     the action that was attempted
     * @param targetType the type of the target entity
     * @param targetId   the ID of the target entity
     * @param message    optional message describing the failure
     */
    public void failure(String action, String targetType, String targetId, String message) {
        try {
            auditService.recordFailure(action, targetType, targetId, message);
        } catch (RuntimeException ex) {
            log.warn("Failed to record failure audit: action={}, targetType={}, targetId={}",
                    action, targetType, targetId, ex);
        }
    }
}
