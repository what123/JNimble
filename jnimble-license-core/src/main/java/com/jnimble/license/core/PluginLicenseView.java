package com.jnimble.license.core;

import com.jnimble.license.sdk.LicenseStatus;

import java.time.Instant;

public record PluginLicenseView(
        boolean required,
        LicenseStatus status,
        String machineCode,
        Instant expiresAt,
        String failureCode,
        String issuerUrl
) {

    public boolean usable() {
        return status == LicenseStatus.NOT_REQUIRED
                || status == LicenseStatus.VALID
                || status == LicenseStatus.EXPIRING;
    }
}
