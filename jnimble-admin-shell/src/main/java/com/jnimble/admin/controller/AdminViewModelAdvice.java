package com.jnimble.admin.controller;

import com.jnimble.admin.i18n.LanguageRecord;
import com.jnimble.admin.i18n.LanguageService;
import com.jnimble.admin.setting.SiteBranding;
import com.jnimble.admin.setting.SystemSettingService;
import com.jnimble.platform.auth.UserAccountService;
import com.jnimble.platform.auth.UserRecord;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Shared view data used by the admin shell, login page and standalone plugin workbenches. */
@ControllerAdvice
public class AdminViewModelAdvice {

    private final LanguageService languageService;
    private final SystemSettingService systemSettingService;
    private final ObjectProvider<UserAccountService> userAccountServiceProvider;

    public AdminViewModelAdvice(
            LanguageService languageService,
            SystemSettingService systemSettingService,
            ObjectProvider<UserAccountService> userAccountServiceProvider
    ) {
        this.languageService = languageService;
        this.systemSettingService = systemSettingService;
        this.userAccountServiceProvider = userAccountServiceProvider;
    }

    /**
     * Provides the list of enabled languages to all admin views.
     *
     * @return a list of enabled language records
     */
    @ModelAttribute("availableLanguages")
    public List<LanguageRecord> availableLanguages() {
        return languageService.listEnabled();
    }

    /**
     * Provides the current authenticated user to all admin views.
     *
     * @param principal the currently authenticated user principal
     * @return the current user record, or null if not authenticated
     */
    @ModelAttribute("currentUser")
    public UserRecord currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        UserAccountService userAccountService = userAccountServiceProvider.getIfAvailable();
        return userAccountService == null
                ? null
                : userAccountService.findByUsername(principal.getName()).orElse(null);
    }

    /**
     * Provides the site branding information (name, subtitle, logo) to all admin views.
     *
     * @return the site branding configuration
     */
    @ModelAttribute("site")
    public SiteBranding siteBranding() {
        return systemSettingService.siteBranding();
    }
}
