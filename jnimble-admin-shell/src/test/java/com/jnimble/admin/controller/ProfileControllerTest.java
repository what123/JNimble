package com.jnimble.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.admin.user.ProfileController;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import com.jnimble.platform.auth.UserStatus;
import java.security.Principal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ProfileControllerTest {

    private UserAccountService userAccountService;
    private PasswordEncoder passwordEncoder;
    private ProfileController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        userAccountService = mock(UserAccountService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        AdminAuditRecorder auditRecorder = mock(AdminAuditRecorder.class);
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("admin.profile.message.updated", Locale.getDefault(), "updated");
        messages.addMessage("admin.profile.message.passwordChanged", Locale.getDefault(), "password changed");
        messages.addMessage("admin.profile.message.failed", Locale.getDefault(), "failed: {0}");
        messages.addMessage("admin.profile.password.currentInvalid", Locale.getDefault(), "current invalid");
        messages.addMessage("admin.profile.password.tooShort", Locale.getDefault(), "too short");
        messages.addMessage("admin.profile.password.mismatch", Locale.getDefault(), "mismatch");
        controller = new ProfileController(userAccountService, passwordEncoder, auditRecorder, messages);
        principal = () -> "admin";
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.of(user()));
    }

    @Test
    void signedInUserCanUpdateOwnDisplayName() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.updateProfile(principal, "店长", redirect);

        verify(userAccountService).updateDisplayName("admin", "店长");
        assertThat(view).isEqualTo("redirect:/admin/profile#profile");
    }

    @Test
    void currentPasswordIsRequiredBeforePasswordChange() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(passwordEncoder.matches("wrong", "{noop}888888")).thenReturn(false);

        controller.changePassword(principal, "wrong", "new888", "new888", redirect);

        verify(userAccountService, never()).changePassword("admin", "new888");
        assertThat(redirect.getFlashAttributes()).containsKey("errorMessage");
    }

    @Test
    void verifiedPasswordCanBeChanged() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(passwordEncoder.matches("888888", "{noop}888888")).thenReturn(true);

        controller.changePassword(principal, "888888", "new888", "new888", redirect);

        verify(userAccountService).changePassword("admin", "new888");
        assertThat(redirect.getFlashAttributes()).containsKey("successMessage");
    }

    private UserRecord user() {
        return new UserRecord("1", "admin", "{noop}888888", "Administrator",
                UserStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH);
    }
}
