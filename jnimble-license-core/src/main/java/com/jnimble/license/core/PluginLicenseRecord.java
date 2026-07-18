package com.jnimble.license.core;

import com.jnimble.license.sdk.LicenseStatus;

import java.time.Instant;

record PluginLicenseRecord(
        String pluginId,
        String licenseId,
        String token,
        String tokenHash,
        String issuer,
        String keyId,
        String productCode,
        String machineCode,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt,
        String timeSnapshot,
        long snapshotSequence,
        LicenseStatus status,
        String failureCode,
        Instant createdAt,
        Instant updatedAt
) {
}
