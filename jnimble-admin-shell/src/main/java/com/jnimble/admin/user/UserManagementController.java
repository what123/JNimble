package com.jnimble.admin.user;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.permission.RoleRecord;
import com.jnimble.platform.permission.RoleService;
import com.jnimble.platform.permission.SubjectRoleGrant;
import com.jnimble.platform.permission.SystemPermissions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for user management operations.
 *
 * <p>Provides endpoints for listing, creating, editing, and managing user accounts.
 * All operations require appropriate permissions.</p>
 */
@Controller
public class UserManagementController {

    private final ObjectProvider<UserAccountService> userAccountServiceProvider;
    private final RoleService roleService;
    private final AdminAuditRecorder auditRecorder;
    private final ControllerAuthorization authorization;

    /**
     * Creates a new user management controller.
     *
     * @param userAccountServiceProvider the user account service provider
     * @param roleService                the role service
     * @param auditRecorder              the audit recorder
     * @param authorization              the authorization service
     */
    public UserManagementController(
            ObjectProvider<UserAccountService> userAccountServiceProvider,
            RoleService roleService,
            AdminAuditRecorder auditRecorder,
            ControllerAuthorization authorization
    ) {
        this.userAccountServiceProvider = userAccountServiceProvider;
        this.roleService = roleService;
        this.auditRecorder = auditRecorder;
        this.authorization = authorization;
    }

    /**
     * Lists all users.
     *
     * @param model the view model
     * @return the user list template name
     */
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        authorization.requirePermission(SystemPermissions.USER_VIEW);
        UserAccountService userAccountService = userAccountServiceProvider.getIfAvailable();
        List<UserRecord> users = userAccountService == null
                ? List.of()
                : userAccountService.listUsers().stream()
                .sorted(Comparator.comparing(UserRecord::username))
                .toList();
        model.addAttribute("users", users);
        model.addAttribute("roles", sortedRoles());
        model.addAttribute("userRoleIds", userRoleIds());
        model.addAttribute("userStoreAvailable", userAccountService != null);
        model.addAttribute("activeNav", "users");
        return "page/user/list";
    }

    /**
     * Creates a new user.
     *
     * @param username          the username
     * @param password          the password
     * @param displayName       the display name (optional)
     * @param roleIds           selected role IDs
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to user list
     */
    @PostMapping("/admin/users")
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String displayName,
            @RequestParam(name = "roleIds", required = false) List<String> roleIds,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.USER_MANAGE);
        return execute(redirectAttributes, AuditActions.USER_CREATE, username, "用户已创建。", () -> {
            userAccountService().createUser(username, password, displayName);
            synchronizeUserRoles(username, roleIds);
        }, "redirect:/admin/users");
    }

    /**
     * Updates a user's username, display name, password (if provided), and role assignments.
     */
    @PostMapping("/admin/users/{username}/profile")
    public String updateProfile(
            @PathVariable String username,
            @RequestParam String newUsername,
            @RequestParam String displayName,
            @RequestParam(required = false) String password,
            @RequestParam(name = "roleIds", required = false) List<String> roleIds,
            RedirectAttributes redirectAttributes
    ) {
        authorization.requirePermission(SystemPermissions.USER_MANAGE);
        return execute(redirectAttributes, AuditActions.USER_PROFILE_UPDATE, username, "用户资料已保存。", () -> {
            String effectiveUsername = userAccountService().changeUsername(username, newUsername).username();
            userAccountService().updateDisplayName(effectiveUsername, displayName);
            if (password != null && !password.isBlank()) {
                userAccountService().resetPassword(effectiveUsername, password);
            }
            synchronizeUserRoles(effectiveUsername, roleIds);
        }, "redirect:/admin/users");
    }

    private void synchronizeUserRoles(String username, List<String> roleIds) {
        Set<String> submittedRoleIds = roleIds == null ? Set.of() : new HashSet<>(roleIds);
        Set<String> activeRoleIds = sortedRoles().stream()
                .filter(RoleRecord::active)
                .map(RoleRecord::id)
                .collect(Collectors.toSet());
        Set<String> currentRoleIds = roleService.listSubjectRoles(username).stream()
                .map(SubjectRoleGrant::roleId)
                .collect(Collectors.toSet());

        submittedRoleIds.stream()
                .filter(activeRoleIds::contains)
                .filter(roleId -> !currentRoleIds.contains(roleId))
                .forEach(roleId -> roleService.grantRoleToSubject(username, roleId));
        currentRoleIds.stream()
                .filter(roleId -> !submittedRoleIds.contains(roleId))
                .forEach(roleId -> roleService.revokeRoleFromSubject(username, roleId));
    }

    private List<RoleRecord> sortedRoles() {
        return roleService.listRoles().stream()
                .sorted(Comparator.comparing(RoleRecord::code))
                .toList();
    }

    private Map<String, String> userRoleIds() {
        return roleService.listSubjectRoles().stream()
                .sorted(Comparator.comparing(SubjectRoleGrant::roleId))
                .collect(Collectors.groupingBy(
                        SubjectRoleGrant::subjectId,
                        Collectors.mapping(SubjectRoleGrant::roleId, Collectors.joining(","))));
    }

    private UserAccountService userAccountService() {
        UserAccountService userAccountService = userAccountServiceProvider.getIfAvailable();
        if (userAccountService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "User account store is not enabled.");
        }
        return userAccountService;
    }

    private String execute(
            RedirectAttributes redirectAttributes,
            String action,
            String username,
            String successMessage,
            UserOperation operation,
            String redirectPath
    ) {
        try {
            operation.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
            auditRecorder.success(action, "user", username, successMessage);
        } catch (RuntimeException ex) {
            String errorMessage = failureMessage(ex);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            auditRecorder.failure(action, "user", username, errorMessage);
        }
        return redirectPath;
    }

    private String failureMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    private interface UserOperation {
        void run();
    }
}
