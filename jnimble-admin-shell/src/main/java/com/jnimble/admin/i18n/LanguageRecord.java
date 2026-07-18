package com.jnimble.admin.i18n;

import java.time.Instant;
import java.util.Locale;

/** A language exposed by the framework language selector. */
public record LanguageRecord(
        String code,
        String localeTag,
        String name,
        String nativeName,
        boolean enabled,
        boolean defaultLanguage,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Converts the stored locale tag to a Java {@link Locale} object.
     *
     * @return the Java locale representation
     */
    public Locale locale() {
        return Locale.forLanguageTag(localeTag);
    }
}
