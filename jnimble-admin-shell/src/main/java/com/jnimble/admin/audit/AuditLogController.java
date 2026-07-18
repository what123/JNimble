package com.jnimble.admin.audit;

import com.jnimble.admin.audit.AuditDisplayNames;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.audit.AuditRecord;
import com.jnimble.platform.audit.AuditService;
import com.jnimble.platform.permission.SystemPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

/**
 * Controller for viewing audit logs.
 *
 * <p>Provides endpoints for listing audit log entries with configurable limits.</p>
 */
@Controller
public class AuditLogController {

    private final AuditService auditService;
    private final ControllerAuthorization authorization;

    /**
     * Creates a new audit log controller.
     *
     * @param auditService   the audit service
     * @param authorization  the authorization service
     */
    public AuditLogController(AuditService auditService, ControllerAuthorization authorization) {
        this.auditService = auditService;
        this.authorization = authorization;
    }

    /**
     * Lists recent audit log entries.
     *
     * @param limit the maximum number of entries to return (1-500, default 100)
     * @param model the view model
     * @return the audit log list template name
     */
    @GetMapping("/admin/audit")
    public String listAuditLogs(
            @RequestParam(name = "limit", required = false, defaultValue = "100") int limit,
            Model model
    ) {
        authorization.requirePermission(SystemPermissions.AUDIT_VIEW);
        int normalizedLimit = Math.max(1, Math.min(limit, 500));
        Collection<AuditRecord> records = auditService.listRecent(normalizedLimit);
        model.addAttribute("records", records);
        model.addAttribute("limit", normalizedLimit);
        model.addAttribute("activeNav", "audit");
        model.addAttribute("actionDisplay", AuditDisplayNames.actionDisplay());
        model.addAttribute("targetTypeDisplay", AuditDisplayNames.targetTypeDisplay());
        model.addAttribute("outcomeDisplay", AuditDisplayNames.outcomeDisplay());
        return "page/audit/list";
    }
}
