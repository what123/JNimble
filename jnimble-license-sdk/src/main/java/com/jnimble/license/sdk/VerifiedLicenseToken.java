package com.jnimble.license.sdk;

/** Successfully signature-verified license token. */
public record VerifiedLicenseToken(
        String issuer,
        String keyId,
        LicenseClaims claims
) {
}
