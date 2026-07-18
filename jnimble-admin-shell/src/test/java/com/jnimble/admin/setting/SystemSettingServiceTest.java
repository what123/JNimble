package com.jnimble.admin.setting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jnimble.platform.persistence.entity.SystemSettingEntity;
import com.jnimble.platform.persistence.mapper.SystemSettingMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SystemSettingService}. */
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingMapper mapper;

    @InjectMocks
    private SystemSettingService service;

    private SystemSettingEntity nameEntity;
    private SystemSettingEntity subtitleEntity;
    private SystemSettingEntity logoEntity;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        nameEntity = new SystemSettingEntity();
        nameEntity.setSettingKey("site.name");
        nameEntity.setSettingValue("MyShop");
        nameEntity.setUpdatedBy("admin");
        nameEntity.setUpdatedAt(now);

        subtitleEntity = new SystemSettingEntity();
        subtitleEntity.setSettingKey("site.subtitle");
        subtitleEntity.setSettingValue("POS");
        subtitleEntity.setUpdatedBy("admin");
        subtitleEntity.setUpdatedAt(now);

        logoEntity = new SystemSettingEntity();
        logoEntity.setSettingKey("site.logoUrl");
        logoEntity.setSettingValue("/admin/system-settings/logo/abc.png");
        logoEntity.setUpdatedBy("admin");
        logoEntity.setUpdatedAt(now);
    }

    @Test
    void listAllReturnsOrderedRecords() {
        when(mapper.selectList(any())).thenReturn(List.of(nameEntity, subtitleEntity, logoEntity));

        List<SystemSettingRecord> records = service.listAll();

        assertEquals(3, records.size());
        assertEquals("site.name", records.get(0).settingKey());
    }

    @Test
    void findAllCachesAndReturnsMap() {
        when(mapper.selectList(any())).thenReturn(List.of(nameEntity, subtitleEntity, logoEntity));

        Map<String, String> first = service.findAll();
        Map<String, String> second = service.findAll();

        assertEquals("MyShop", first.get("site.name"));
        assertSame(first, second, "Cache should return the same map instance");
        verify(mapper, times(1)).selectList(any());
    }

    @Test
    void siteBrandingComposesFromCache() {
        when(mapper.selectList(any())).thenReturn(List.of(nameEntity, subtitleEntity, logoEntity));

        SiteBranding branding = service.siteBranding();

        assertEquals("MyShop", branding.name());
        assertEquals("POS", branding.subtitle());
        assertEquals("/admin/system-settings/logo/abc.png", branding.logoUrl());
        assertTrue(branding.hasLogo());
    }

    @Test
    void siteBrandingFallbackWhenValuesAreBlank() {
        SystemSettingEntity blankName = new SystemSettingEntity();
        blankName.setSettingKey("site.name");
        blankName.setSettingValue("");
        blankName.setUpdatedBy("system");
        blankName.setUpdatedAt(Instant.now());

        SystemSettingEntity blankSubtitle = new SystemSettingEntity();
        blankSubtitle.setSettingKey("site.subtitle");
        blankSubtitle.setSettingValue("");
        blankSubtitle.setUpdatedBy("system");
        blankSubtitle.setUpdatedAt(Instant.now());

        SystemSettingEntity blankLogo = new SystemSettingEntity();
        blankLogo.setSettingKey("site.logoUrl");
        blankLogo.setSettingValue("");
        blankLogo.setUpdatedBy("system");
        blankLogo.setUpdatedAt(Instant.now());

        when(mapper.selectList(any())).thenReturn(List.of(blankName, blankSubtitle, blankLogo));

        SiteBranding branding = service.siteBranding();

        assertEquals("JNimble", branding.nameOrFallback());
        assertEquals("Operations Console", branding.subtitleOrFallback());
        assertFalse(branding.hasLogo());
    }

    @Test
    void saveUpdatesExistingAndInvalidatesCache() {
        when(mapper.selectOne(any())).thenReturn(nameEntity);
        when(mapper.updateById(any(SystemSettingEntity.class))).thenReturn(1);

        service.save("site.name", "NewName", "admin");

        verify(mapper).updateById(any(SystemSettingEntity.class));
    }

    @Test
    void saveAllUpsertsMultipleSettings() {
        when(mapper.selectOne(any())).thenReturn(null);

        service.saveAll(Map.of("site.name", "Shop", "site.subtitle", "Console"), "admin");

        verify(mapper, times(2)).insert(any(SystemSettingEntity.class));
    }

    @Test
    void saveRejectsBlankKey() {
        assertThrows(IllegalArgumentException.class, () -> service.save("  ", "value", "admin"));
    }
}
