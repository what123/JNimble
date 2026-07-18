package com.jnimble.platform.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * MyBatis-Plus entity for the {@code jnimble_language} table.
 *
 * <p>Stores managed language records including locale tag, display names,
 * enabled status, and default language flag.</p>
 *
 * <p>{@code jnimble_language} 表的 MyBatis-Plus 实体。
 * 存储受管理的语言记录，包括 Locale 标签、显示名称、启用状态和默认语言标志。</p>
 */
@TableName("jnimble_language")
public class LanguageEntity {

    @TableId(type = IdType.INPUT)
    private String languageCode;
    private String localeTag;
    private String name;
    private String nativeName;
    private Boolean enabled;
    private Boolean defaultLanguage;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Gets the language code (primary key).
     *
     * @return the language code
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Sets the language code (primary key).
     *
     * @param languageCode the language code to set
     */
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * Gets the BCP 47 locale tag.
     *
     * @return the locale tag
     */
    public String getLocaleTag() {
        return localeTag;
    }

    /**
     * Sets the BCP 47 locale tag.
     *
     * @param localeTag the locale tag to set
     */
    public void setLocaleTag(String localeTag) {
        this.localeTag = localeTag;
    }

    /**
     * Gets the language display name.
     *
     * @return the language name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the language display name.
     *
     * @param name the language name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the native language name.
     *
     * @return the native name
     */
    public String getNativeName() {
        return nativeName;
    }

    /**
     * Sets the native language name.
     *
     * @param nativeName the native name to set
     */
    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    /**
     * Returns whether this language is enabled.
     *
     * @return {@code true} if enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether this language is enabled.
     *
     * @param enabled the enabled flag to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether this is the default language.
     *
     * @return {@code true} if this is the default language
     */
    public Boolean getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Sets whether this is the default language.
     *
     * @param defaultLanguage the default language flag to set
     */
    public void setDefaultLanguage(Boolean defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * Gets the display sort order.
     *
     * @return the sort order
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the display sort order.
     *
     * @param sortOrder the sort order to set
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the record creation timestamp.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the record creation timestamp.
     *
     * @param createdAt the creation time to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the update time
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt the update time to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
