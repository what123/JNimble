package com.jnimble.license.sdk;

import java.time.Instant;

public record PluginLicenseResult(
        LicenseStatus status,
        String machineCode,
        Instant expiresAt,
        String failureCode
) {

    public boolean usable() {
        return status == LicenseStatus.VALID || status == LicenseStatus.EXPIRING;
    }
}
