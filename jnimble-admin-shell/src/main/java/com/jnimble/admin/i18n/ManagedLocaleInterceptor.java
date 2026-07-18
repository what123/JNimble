package com.jnimble.admin.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

/** Restricts locale changes to languages currently enabled in language management. */
final class ManagedLocaleInterceptor implements HandlerInterceptor {

    private final LanguageService languageService;
    private final LocaleResolver localeResolver;

    /**
     * Creates a new managed locale interceptor.
     *
     * @param languageService the language service for resolving enabled languages
     * @param localeResolver  the locale resolver for setting the managed locale
     */
    ManagedLocaleInterceptor(LanguageService languageService, LocaleResolver localeResolver) {
        this.languageService = languageService;
        this.localeResolver = localeResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Locale resolvedLocale = localeResolver.resolveLocale(request);
        Optional<LanguageRecord> selected = Optional.empty();
        String requestedLanguage = request.getParameter(AdminLocaleConfiguration.LOCALE_PARAMETER_NAME);
        if ("GET".equalsIgnoreCase(request.getMethod())
                && requestedLanguage != null
                && !requestedLanguage.isBlank()) {
            selected = languageService.findEnabled(requestedLanguage);
        }
        if (selected.isEmpty()) {
            selected = languageService.findEnabled(resolvedLocale);
        }
        LanguageRecord language = selected.orElseGet(languageService::defaultLanguage);
        Locale managedLocale = language.locale();
        if (!managedLocale.equals(resolvedLocale)
                || (requestedLanguage != null && !requestedLanguage.isBlank())) {
            localeResolver.setLocale(request, response, managedLocale);
        }
        LocaleContextHolder.setLocale(managedLocale);
        return true;
    }
}
