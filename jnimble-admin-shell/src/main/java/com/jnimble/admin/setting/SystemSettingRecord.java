package com.jnimble.admin.setting;

import java.time.Instant;

/** A key-value system setting record. */
public record SystemSettingRecord(
        String settingKey,
        String settingValue,
        String updatedBy,
        Instant updatedAt
) {
}
