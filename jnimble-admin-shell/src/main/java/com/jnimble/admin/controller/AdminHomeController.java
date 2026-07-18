package com.jnimble.admin.controller;

import com.jnimble.admin.audit.AuditDisplayNames;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.platform.audit.AuditRecord;
import com.jnimble.platform.audit.AuditService;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.sdk.plugin.PluginStatus;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the admin home page and login.
 *
 * <p>Provides the main dashboard view with system statistics and the login page.</p>
 */
@Controller
public class AdminHomeController {

    private final PluginRuntimeService pluginRuntimeService;
    private final RoleService roleService;
    private final UserAccountService userAccountService;
    private final AuditService auditService;

    /**
     * Creates a new admin home controller.
     *
     * @param pluginRuntimeService the plugin runtime service
     * @param roleService          the role service
     * @param userAccountService   the user account service
     * @param auditService         the audit service
     */
    public AdminHomeController(
            PluginRuntimeService pluginRuntimeService,
            RoleService roleService,
            UserAccountService userAccountService,
            AuditService auditService
    ) {
        this.pluginRuntimeService = pluginRuntimeService;
        this.roleService = roleService;
        this.userAccountService = userAccountService;
        this.auditService = auditService;
    }

    /**
     * Displays the admin dashboard home page.
     *
     * @param model the view model
     * @return the home page template name
     */
    @GetMapping({"/", "/admin"})
    public String home(Model model) {
        List<AuditRecord> recentAudit = auditService.listRecent(5).stream().toList();
        long pluginTotal = pluginRuntimeService.list().size();
        long pluginEnabled = pluginRuntimeService.list().stream()
                .filter(plugin -> plugin.status() == PluginStatus.ENABLED)
                .count();

        model.addAttribute("activeNav", "home");
        model.addAttribute("pluginTotal", pluginTotal);
        model.addAttribute("pluginEnabled", pluginEnabled);
        model.addAttribute("roleTotal", roleService.listRoles().size());
        model.addAttribute("userTotal", userAccountService.listUsers().size());
        model.addAttribute("recentAudit", recentAudit);
        model.addAttribute("actionDisplay", AuditDisplayNames.actionDisplay());
        model.addAttribute("targetTypeDisplay", AuditDisplayNames.targetTypeDisplay());
        model.addAttribute("outcomeDisplay", AuditDisplayNames.outcomeDisplay());
        return "page/home";
    }

    /**
     * Displays the login page.
     *
     * @param error present when authentication fails; drives the error banner
     * @param model the view model
     * @return the login template name
     */
    @GetMapping("/login")
    public String login(
            @org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("loginError", Boolean.TRUE);
        }
        return "auth/login";
    }
}
