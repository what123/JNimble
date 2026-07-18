package com.jnimble.admin.i18n;

import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/**
 * Configures request locale selection for the admin UI and plugin web views.
 */
@Configuration
public class AdminLocaleConfiguration implements WebMvcConfigurer {

    public static final String LOCALE_COOKIE_NAME = "jnimble.lang";
    public static final String LOCALE_PARAMETER_NAME = "lang";

    private final LanguageService languageService;

    /**
     * Creates a new locale configuration.
     *
     * @param languageService the language service for managed locale resolution
     */
    public AdminLocaleConfiguration(LanguageService languageService) {
        this.languageService = languageService;
    }

    /**
     * Creates a cookie-based locale resolver for the admin UI.
     *
     * <p>The resolver stores the selected locale in a cookie named {@value #LOCALE_COOKIE_NAME}
     * and defaults to {@link Locale#SIMPLIFIED_CHINESE}.</p>
     *
     * @return the locale resolver bean
     */
    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME);
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setCookiePath("/");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookieSameSite("Lax");
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ManagedLocaleInterceptor(languageService, localeResolver()));
    }
}
