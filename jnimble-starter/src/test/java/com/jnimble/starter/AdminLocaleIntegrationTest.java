package com.jnimble.starter;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jnimble.admin.i18n.AdminLocaleConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
                "jnimble.plugins.auto-enable=false",
                "jnimble.plugins.restore-enabled=false",
                "jnimble.plugins.directory-scan-enabled=false"
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminLocaleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void languageSelectionPersistsAcrossLoginAndAdminPages() throws Exception {
        MvcResult englishResult = mockMvc.perform(get("/login").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AdminLocaleConfiguration.LOCALE_COOKIE_NAME))
                .andExpect(content().string(containsString("<html lang=\"en-US\"")))
                .andExpect(content().string(containsString("Sign in to JNimble Admin")))
                .andExpect(content().string(containsString("value=\"zh_CN\"")))
                .andExpect(content().string(containsString("value=\"en_US\"")))
                .andReturn();

        Cookie languageCookie = englishResult.getResponse()
                .getCookie(AdminLocaleConfiguration.LOCALE_COOKIE_NAME);

        mockMvc.perform(get("/login").cookie(languageCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in to JNimble Admin")));

        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user("admin")
                        .password("test-admin-password"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/admin").session(session).cookie(languageCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administration")))
                .andExpect(content().string(containsString("Sign out")))
                .andExpect(content().string(containsString("value=\"en_US\"")));

        mockMvc.perform(get("/admin/languages").session(session).cookie(languageCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Framework Languages")))
                .andExpect(content().string(containsString("en_US")));

        mockMvc.perform(get("/admin/profile").session(session).cookie(languageCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Profile and Security")))
                .andExpect(content().string(containsString("admin")));

        mockMvc.perform(get("/login").param("lang", "zh_CN").cookie(languageCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AdminLocaleConfiguration.LOCALE_COOKIE_NAME))
                .andExpect(content().string(containsString("<html lang=\"zh-CN\"")))
                .andExpect(content().string(containsString("登录 JNimble 后台")));
    }
}
