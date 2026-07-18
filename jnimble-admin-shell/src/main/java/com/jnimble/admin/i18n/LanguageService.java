package com.jnimble.admin.i18n;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.LanguageEntity;
import com.jnimble.platform.persistence.mapper.LanguageMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores the languages available to the framework and plugin views.
 *
 * <p>语言管理服务，负责存储框架和插件视图可用的语言列表。
 * 使用 MyBatis-Plus 和 MapperUtils 进行数据库操作。</p>
 */
@Service
public class LanguageService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{1,31}");

    private final LanguageMapper mapper;
    private volatile List<LanguageRecord> enabledCache;

    /**
     * Creates a new language service.
     *
     * @param mapper the MyBatis mapper for languages
     */
    public LanguageService(LanguageMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Lists all language records ordered by sort order and language code.
     *
     * @return a list of all language records
     */
    public List<LanguageRecord> listAll() {
        return MapperUtils.selectList(mapper, LanguageEntity.class, w -> {})
                .stream()
                .sorted(LanguageService::bySortThenCode)
                .map(LanguageService::toRecord)
                .toList();
    }

    /**
     * Lists only enabled language records, using a cached result.
     *
     * @return a list of enabled language records
     */
    public List<LanguageRecord> listEnabled() {
        List<LanguageRecord> cached = enabledCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (enabledCache == null) {
                enabledCache = List.copyOf(
                        MapperUtils.selectList(mapper, LanguageEntity.class,
                                        w -> w.eq("enabled", true))
                                .stream()
                                .sorted(LanguageService::bySortThenCode)
                                .map(LanguageService::toRecord)
                                .toList());
            }
            return enabledCache;
        }
    }

    /**
     * Finds an enabled language by its code or locale tag.
     *
     * @param codeOrLocaleTag the language code or BCP 47 locale tag
     * @return an optional containing the matching language record
     */
    public Optional<LanguageRecord> findEnabled(String codeOrLocaleTag) {
        if (codeOrLocaleTag == null || codeOrLocaleTag.isBlank()) {
            return Optional.empty();
        }
        String value = codeOrLocaleTag.trim();
        String localeTag = canonicalLocaleTag(value);
        Locale requestedLocale = Locale.forLanguageTag(localeTag);
        return listEnabled().stream()
                .filter(language -> language.code().equalsIgnoreCase(value)
                        || language.localeTag().equalsIgnoreCase(localeTag)
                        || (requestedLocale.getCountry().isBlank()
                        && language.locale().getLanguage().equalsIgnoreCase(requestedLocale.getLanguage())))
                .findFirst();
    }

    /**
     * Finds an enabled language by its Java locale.
     *
     * @param locale the Java locale to look up
     * @return an optional containing the matching language record
     */
    public Optional<LanguageRecord> findEnabled(Locale locale) {
        return locale == null ? Optional.empty() : findEnabled(locale.toLanguageTag());
    }

    /**
     * Returns the default language from the enabled languages.
     *
     * @return the default language record
     * @throws IllegalStateException if no enabled languages exist
     */
    public LanguageRecord defaultLanguage() {
        List<LanguageRecord> enabled = listEnabled();
        if (enabled.isEmpty()) {
            throw new IllegalStateException("系统至少需要启用一种语言");
        }
        return enabled.stream().filter(LanguageRecord::defaultLanguage).findFirst().orElse(enabled.getFirst());
    }

    /**
     * Creates a new language record in the database.
     *
     * @param code            the language code
     * @param localeTag       the BCP 47 locale tag
     * @param name            the language name
     * @param nativeName      the native language name
     * @param sortOrder       the display sort order
     * @param enabled         whether the language is enabled
     * @param defaultLanguage whether this is the default language
     * @return the created language record
     * @throws IllegalArgumentException if validation fails or a duplicate exists
     */
    @Transactional
    public LanguageRecord create(
            String code,
            String localeTag,
            String name,
            String nativeName,
            int sortOrder,
            boolean enabled,
            boolean defaultLanguage
    ) {
        String normalizedCode = normalizeCode(code);
        String normalizedLocaleTag = canonicalLocaleTag(localeTag);
        String normalizedName = requireNonBlank(name, "name");
        String normalizedNativeName = requireNonBlank(nativeName, "nativeName");
        if (defaultLanguage && !enabled) {
            throw new IllegalArgumentException("默认语言必须启用");
        }
        boolean duplicateByCode = MapperUtils.existsByCondition(mapper, LanguageEntity.class,
                w -> w.apply("lower(language_code) = {0}", normalizedCode.toLowerCase()));
        boolean duplicateByLocale = MapperUtils.existsByCondition(mapper, LanguageEntity.class,
                w -> w.apply("lower(locale_tag) = {0}", normalizedLocaleTag.toLowerCase()));
        if (duplicateByCode || duplicateByLocale) {
            throw new IllegalArgumentException("语言编码或 Locale 已存在");
        }
        Instant now = Instant.now();
        if (defaultLanguage) {
            clearDefaultFlag(now);
        }
        LanguageEntity entity = new LanguageEntity();
        entity.setLanguageCode(normalizedCode);
        entity.setLocaleTag(normalizedLocaleTag);
        entity.setName(normalizedName);
        entity.setNativeName(normalizedNativeName);
        entity.setEnabled(enabled);
        entity.setDefaultLanguage(defaultLanguage);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        MapperUtils.insert(mapper, entity);
        invalidateCache();
        return require(normalizedCode);
    }

    /**
     * Updates an existing language record.
     *
     * @param code       the language code to update
     * @param localeTag  the BCP 47 locale tag
     * @param name       the language name
     * @param nativeName the native language name
     * @param sortOrder  the display sort order
     * @return the updated language record
     * @throws IllegalArgumentException if the language is not found or validation fails
     */
    @Transactional
    public LanguageRecord update(
            String code,
            String localeTag,
            String name,
            String nativeName,
            int sortOrder
    ) {
        String normalizedCode = normalizeCode(code);
        LanguageEntity entity = MapperUtils.selectOne(mapper, LanguageEntity.class,
                w -> w.eq("language_code", normalizedCode));
        if (entity == null) {
            throw new IllegalArgumentException("语言不存在：" + normalizedCode);
        }
        entity.setLocaleTag(canonicalLocaleTag(localeTag));
        entity.setName(requireNonBlank(name, "name"));
        entity.setNativeName(requireNonBlank(nativeName, "nativeName"));
        entity.setSortOrder(sortOrder);
        entity.setUpdatedAt(Instant.now());
        MapperUtils.updateById(mapper, entity);
        invalidateCache();
        return require(normalizedCode);
    }

    /**
     * Enables or disables a language.
     *
     * @param code    the language code
     * @param enabled true to enable, false to disable
     * @return the updated language record
     * @throws IllegalArgumentException if the language is the default and cannot be disabled
     */
    @Transactional
    public LanguageRecord setEnabled(String code, boolean enabled) {
        LanguageRecord language = require(code);
        if (!enabled) {
            if (language.defaultLanguage()) {
                throw new IllegalArgumentException("默认语言不能停用，请先设置其他默认语言");
            }
            long enabledCount = mapper.selectCount(
                    MapperUtils.buildWrapper(LanguageEntity.class, w -> w.eq("enabled", true)));
            if (enabledCount <= 1) {
                throw new IllegalArgumentException("系统至少需要启用一种语言");
            }
        }
        LanguageEntity updateEntity = new LanguageEntity();
        updateEntity.setEnabled(enabled);
        updateEntity.setUpdatedAt(Instant.now());
        MapperUtils.updateOne(mapper, updateEntity, LanguageEntity.class,
                w -> w.eq("language_code", language.code()));
        invalidateCache();
        return require(language.code());
    }

    /**
     * Sets a language as the system default.
     *
     * @param code the language code to set as default
     * @return the updated language record
     * @throws IllegalArgumentException if the language is not enabled
     */
    @Transactional
    public LanguageRecord setDefault(String code) {
        LanguageRecord language = require(code);
        if (!language.enabled()) {
            throw new IllegalArgumentException("请先启用该语言再设为默认语言");
        }
        Instant now = Instant.now();
        clearDefaultFlag(now);
        LanguageEntity updateEntity = new LanguageEntity();
        updateEntity.setDefaultLanguage(true);
        updateEntity.setUpdatedAt(now);
        MapperUtils.updateOne(mapper, updateEntity, LanguageEntity.class,
                w -> w.eq("language_code", language.code()));
        invalidateCache();
        return require(language.code());
    }

    /**
     * Finds a language by code, throwing an exception if not found.
     *
     * @param code the language code
     * @return the language record
     * @throws IllegalArgumentException if the language does not exist
     */
    public LanguageRecord require(String code) {
        String normalizedCode = normalizeCode(code);
        LanguageEntity entity = MapperUtils.selectOne(mapper, LanguageEntity.class,
                w -> w.eq("language_code", normalizedCode));
        if (entity == null) {
            throw new IllegalArgumentException("语言不存在：" + normalizedCode);
        }
        return toRecord(entity);
    }

    private void clearDefaultFlag(Instant now) {
        LanguageEntity clearEntity = new LanguageEntity();
        clearEntity.setDefaultLanguage(false);
        clearEntity.setUpdatedAt(now);
        MapperUtils.updateByCondition(mapper, clearEntity, LanguageEntity.class,
                w -> w.eq("default_language", true));
    }

    private static int bySortThenCode(LanguageEntity a, LanguageEntity b) {
        int bySort = Integer.compare(
                a.getSortOrder() == null ? 0 : a.getSortOrder(),
                b.getSortOrder() == null ? 0 : b.getSortOrder());
        if (bySort != 0) {
            return bySort;
        }
        return a.getLanguageCode().compareToIgnoreCase(b.getLanguageCode());
    }

    private static LanguageRecord toRecord(LanguageEntity entity) {
        return new LanguageRecord(
                entity.getLanguageCode(),
                entity.getLocaleTag(),
                entity.getName(),
                entity.getNativeName(),
                Boolean.TRUE.equals(entity.getEnabled()),
                Boolean.TRUE.equals(entity.getDefaultLanguage()),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String normalizeCode(String code) {
        String normalized = requireNonBlank(code, "code").replace('-', '_');
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("语言编码格式不正确");
        }
        return normalized;
    }

    private static String canonicalLocaleTag(String localeTag) {
        String normalized = requireNonBlank(localeTag, "localeTag").replace('_', '-');
        Locale locale = Locale.forLanguageTag(normalized);
        if (locale.getLanguage().isBlank() || "und".equals(locale.toLanguageTag())) {
            throw new IllegalArgumentException("Locale 格式不正确");
        }
        return locale.toLanguageTag();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private void invalidateCache() {
        enabledCache = null;
    }
}
