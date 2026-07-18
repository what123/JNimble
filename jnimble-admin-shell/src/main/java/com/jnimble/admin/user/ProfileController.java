package com.jnimble.admin.user;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.platform.audit.AuditActions;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import java.security.Principal;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Self-service profile and password management for the signed-in user. */
@Controller
public class ProfileController {

    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditRecorder auditRecorder;
    private final MessageSource messageSource;

    /**
     * Constructs a new profile controller.
     *
     * @param userAccountService the user account service
     * @param passwordEncoder    the password encoder
     * @param auditRecorder      the audit recorder
     * @param messageSource      the message source for i18n
     */
    public ProfileController(
            UserAccountService userAccountService,
            PasswordEncoder passwordEncoder,
            AdminAuditRecorder auditRecorder,
            MessageSource messageSource
    ) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.messageSource = messageSource;
    }

    /**
     * Displays the profile page for the current user.
     *
     * @param principal the authenticated user principal
     * @param model     the view model
     * @return the profile template name
     */
    @GetMapping("/admin/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("profileUser", requireCurrentUser(principal));
        model.addAttribute("activeNav", "profile");
        return "page/user/profile";
    }

    /**
     * Updates the display name for the current user.
     *
     * @param principal          the authenticated user principal
     * @param displayName        the new display name
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the profile page
     */
    @PostMapping("/admin/profile")
    public String updateProfile(
            Principal principal,
            @RequestParam String displayName,
            RedirectAttributes redirectAttributes
    ) {
        UserRecord user = requireCurrentUser(principal);
        try {
            userAccountService.updateDisplayName(user.username(), displayName);
            auditRecorder.success(AuditActions.USER_SELF_PROFILE_UPDATE,
                    "user", user.username(), "self profile updated");
            redirectAttributes.addFlashAttribute("successMessage",
                    message("admin.profile.message.updated"));
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.USER_SELF_PROFILE_UPDATE,
                    "user", user.username(), ex.getMessage());
            addFailure(redirectAttributes, ex);
        }
        return "redirect:/admin/profile#profile";
    }

    /**
     * Changes the password for the current user.
     *
     * @param principal          the authenticated user principal
     * @param currentPassword    the current password for verification
     * @param newPassword        the new password
     * @param confirmPassword    the new password confirmation
     * @param redirectAttributes redirect attributes for flash messages
     * @return redirect to the profile page
     */
    @PostMapping("/admin/profile/password")
    public String changePassword(
            Principal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        UserRecord user = requireCurrentUser(principal);
        try {
            if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
                throw new IllegalArgumentException(message("admin.profile.password.currentInvalid"));
            }
            if (newPassword == null || newPassword.length() < 6) {
                throw new IllegalArgumentException(message("admin.profile.password.tooShort"));
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException(message("admin.profile.password.mismatch"));
            }
            userAccountService.changePassword(user.username(), newPassword);
            auditRecorder.success(AuditActions.USER_PASSWORD_CHANGE,
                    "user", user.username(), "password changed");
            redirectAttributes.addFlashAttribute("successMessage",
                    message("admin.profile.message.passwordChanged"));
        } catch (RuntimeException ex) {
            auditRecorder.failure(AuditActions.USER_PASSWORD_CHANGE,
                    "user", user.username(), ex.getMessage());
            addFailure(redirectAttributes, ex);
        }
        return "redirect:/admin/profile#password";
    }

    private UserRecord requireCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userAccountService.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void addFailure(RedirectAttributes redirectAttributes, RuntimeException exception) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("admin.profile.message.failed", new Object[]{detail},
                        LocaleContextHolder.getLocale()));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
