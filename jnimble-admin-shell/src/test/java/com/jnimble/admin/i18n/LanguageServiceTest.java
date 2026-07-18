package com.jnimble.admin.i18n;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jnimble.platform.persistence.entity.LanguageEntity;
import com.jnimble.platform.persistence.mapper.LanguageMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @Mock
    private LanguageMapper mapper;

    private LanguageService languageService;

    private LanguageEntity zhEntity;
    private LanguageEntity enEntity;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        zhEntity = new LanguageEntity();
        zhEntity.setLanguageCode("zh_CN");
        zhEntity.setLocaleTag("zh-CN");
        zhEntity.setName("Chinese");
        zhEntity.setNativeName("简体中文");
        zhEntity.setEnabled(true);
        zhEntity.setDefaultLanguage(true);
        zhEntity.setSortOrder(10);
        zhEntity.setCreatedAt(now);
        zhEntity.setUpdatedAt(now);

        enEntity = new LanguageEntity();
        enEntity.setLanguageCode("en_US");
        enEntity.setLocaleTag("en-US");
        enEntity.setName("English");
        enEntity.setNativeName("English");
        enEntity.setEnabled(true);
        enEntity.setDefaultLanguage(false);
        enEntity.setSortOrder(20);
        enEntity.setCreatedAt(now);
        enEntity.setUpdatedAt(now);

        languageService = new LanguageService(mapper);
    }

    @Test
    void listsEnabledLanguagesInConfiguredOrder() {
        when(mapper.selectList(any())).thenReturn(List.of(zhEntity, enEntity));

        var enabled = languageService.listEnabled();

        assertEquals(2, enabled.size());
        assertEquals("zh_CN", enabled.get(0).code());
        assertEquals("en_US", enabled.get(1).code());
    }

    @Test
    void findEnabledMatchesByCodeAndLocaleTag() {
        when(mapper.selectList(any())).thenReturn(List.of(zhEntity, enEntity));

        assertTrue(languageService.findEnabled("en_US").isPresent());
        assertTrue(languageService.findEnabled("en-US").isPresent());
        assertTrue(languageService.findEnabled("en").isPresent());
    }

    @Test
    void defaultLanguageCannotBeDisabled() {
        when(mapper.selectOne(any())).thenReturn(zhEntity);

        assertThrows(IllegalArgumentException.class,
                () -> languageService.setEnabled("zh_CN", false));
    }

    @Test
    void changingDefaultAllowsFormerDefaultToBeDisabled() {
        // setDefault("en_US"): require returns en, clearDefaultFlag, updateOne
        when(mapper.selectOne(any())).thenReturn(enEntity);
        when(mapper.update(any(), any())).thenReturn(1);
        when(mapper.selectCount(any())).thenReturn(2L);

        languageService.setDefault("en_US");

        // Now zh is no longer default; update en to be default
        enEntity.setDefaultLanguage(true);
        zhEntity.setDefaultLanguage(false);

        // For setEnabled("zh_CN", false)
        when(mapper.selectOne(any())).thenReturn(zhEntity);
        when(mapper.update(any(), any())).thenReturn(1);

        languageService.setEnabled("zh_CN", false);

        verify(mapper, atLeast(2)).update(any(), any());
        verify(mapper, atLeast(1)).update(any(), any());
    }

    @Test
    void createsAdditionalLanguageForDynamicSelector() {
        when(mapper.exists(any())).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(null);

        // After insert, require returns the new language
        LanguageEntity frEntity = new LanguageEntity();
        frEntity.setLanguageCode("fr_FR");
        frEntity.setLocaleTag("fr-FR");
        frEntity.setName("French");
        frEntity.setNativeName("Français");
        frEntity.setEnabled(true);
        frEntity.setDefaultLanguage(false);
        frEntity.setSortOrder(30);
        when(mapper.selectOne(any())).thenReturn(frEntity);

        languageService.create("fr_FR", "fr-FR", "French", "Français", 30, true, false);

        verify(mapper).insert(any(LanguageEntity.class));
    }
}
