package com.jnimble.admin.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

class ManagedLocaleInterceptorTest {

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void disabledCookieLocaleFallsBackToManagedDefault() {
        LanguageService languageService = mock(LanguageService.class);
        LocaleResolver localeResolver = mock(LocaleResolver.class);
        LanguageRecord chinese = language("zh_CN", "zh-CN", true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(localeResolver.resolveLocale(request)).thenReturn(Locale.US);
        when(languageService.findEnabled(Locale.US)).thenReturn(Optional.empty());
        when(languageService.defaultLanguage()).thenReturn(chinese);

        new ManagedLocaleInterceptor(languageService, localeResolver)
                .preHandle(request, response, new Object());

        verify(localeResolver).setLocale(request, response, Locale.SIMPLIFIED_CHINESE);
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    private LanguageRecord language(String code, String localeTag, boolean defaultLanguage) {
        return new LanguageRecord(code, localeTag, code, code, true, defaultLanguage,
                10, Instant.EPOCH, Instant.EPOCH);
    }
}
