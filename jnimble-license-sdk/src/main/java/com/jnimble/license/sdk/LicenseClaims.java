package com.jnimble.license.sdk;

/** Signed license payload. Times are UTC epoch seconds. */
public record LicenseClaims(
        int version,
        String licenseId,
        String issuer,
        String pluginCode,
        String pluginVersionRange,
        int machineCodeVersion,
        String machineCode,
        long issuedAt,
        long notBefore,
        long expiresAt
) {
}
