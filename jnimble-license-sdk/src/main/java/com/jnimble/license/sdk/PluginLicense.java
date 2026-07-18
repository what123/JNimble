package com.jnimble.license.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PluginLicense {

    private static final Duration CACHE_DURATION = Duration.ofHours(6);
    private static final Duration EXPIRING_WINDOW = Duration.ofDays(30);
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private static volatile PluginLicenseBackend backend;

    private PluginLicense() {
    }

    public static void installBackend(PluginLicenseBackend value) {
        backend = Objects.requireNonNull(value, "value");
        CACHE.clear();
    }

    public static PluginLicenseResult verify(
            String pluginId,
            String expectedIssuer,
            String productCode,
            String pluginVersion,
            PublicKey publicKey
    ) {
        return verify(pluginId, expectedIssuer, productCode, pluginVersion, publicKey, false);
    }

    public static PluginLicenseResult verify(
            String pluginId,
            String expectedIssuer,
            String productCode,
            String pluginVersion,
            PublicKey publicKey,
            boolean force
    ) {
        requireArgument(pluginId, "pluginId");
        requireArgument(expectedIssuer, "expectedIssuer");
        requireArgument(productCode, "productCode");
        requireArgument(pluginVersion, "pluginVersion");
        Objects.requireNonNull(publicKey, "publicKey");
        PluginLicenseBackend currentBackend = requireBackend();
        String cacheKey = cacheKey(pluginId, expectedIssuer, productCode, pluginVersion, publicKey);
        Instant now = Instant.now();
        CacheEntry cached = CACHE.get(cacheKey);
        if (!force && cached != null && now.isBefore(cached.validUntil())) {
            return cached.result();
        }
        String token = currentBackend.loadToken(pluginId)
                .orElseThrow(() -> failure(pluginId, LicenseStatus.MISSING, null, null, "MISSING"));
        try {
            Ed25519LicenseTokenVerifier verifier = new Ed25519LicenseTokenVerifier(
                    OBJECT_MAPPER,
                    publicKey);
            VerifiedLicenseToken verified = verifier.verify(token);
            LicenseClaims claims = verified.claims();
            String machineCode = currentBackend.machineCode(pluginId, productCode);
            validate(verified, claims, expectedIssuer, productCode, pluginVersion, machineCode, now);
            Instant expiresAt = Instant.ofEpochSecond(claims.expiresAt());
            LicenseStatus status = now.plus(EXPIRING_WINDOW).isAfter(expiresAt)
                    ? LicenseStatus.EXPIRING
                    : LicenseStatus.VALID;
            PluginLicenseResult result = new PluginLicenseResult(status, machineCode, expiresAt, null);
            Instant validUntil = now.plus(CACHE_DURATION).isBefore(expiresAt)
                    ? now.plus(CACHE_DURATION)
                    : expiresAt;
            CACHE.put(cacheKey, new CacheEntry(result, validUntil));
            currentBackend.report(pluginId, result);
            return result;
        } catch (LicenseVerificationException ex) {
            CACHE.remove(cacheKey);
            LicenseStatus status = failureStatus(ex.code());
            PluginLicenseResult result = new PluginLicenseResult(
                    status,
                    currentBackend.machineCode(pluginId, productCode),
                    null,
                    ex.code());
            currentBackend.report(pluginId, result);
            throw new PluginLicenseException(status, ex.getMessage());
        }
    }

    public static void requireValid(
            String pluginId,
            String expectedIssuer,
            String productCode,
            String pluginVersion,
            PublicKey publicKey
    ) {
        PluginLicenseResult result = verify(pluginId, expectedIssuer, productCode, pluginVersion, publicKey);
        if (!result.usable()) {
            throw new PluginLicenseException(result.status(), "Plugin license is not valid: " + result.status());
        }
    }

    public static void invalidate(String pluginId) {
        CACHE.keySet().removeIf(key -> key.startsWith(pluginId + ":"));
    }

    private static void requireArgument(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static PluginLicenseBackend requireBackend() {
        PluginLicenseBackend current = backend;
        if (current == null) {
            throw new PluginLicenseException(LicenseStatus.MISSING, "Plugin license backend is not available");
        }
        return current;
    }

    private static void validate(
            VerifiedLicenseToken verified,
            LicenseClaims claims,
            String expectedIssuer,
            String productCode,
            String pluginVersion,
            String machineCode,
            Instant now
    ) {
        if (claims.version() != 1 || claims.machineCodeVersion() != 1) {
            throw new LicenseVerificationException("INVALID", "License version is not supported");
        }
        if (!expectedIssuer.equals(verified.issuer()) || !expectedIssuer.equals(claims.issuer())) {
            throw new LicenseVerificationException("INVALID", "License issuer does not match");
        }
        if (!productCode.equals(claims.pluginCode())) {
            throw new LicenseVerificationException("PLUGIN_MISMATCH", "License plugin code does not match");
        }
        if (!MessageDigest.isEqual(
                machineCode.getBytes(StandardCharsets.UTF_8),
                claims.machineCode().getBytes(StandardCharsets.UTF_8))) {
            throw new LicenseVerificationException("MACHINE_CODE_MISMATCH", "License machine code does not match");
        }
        if (!("*".equals(claims.pluginVersionRange()) || pluginVersion.equals(claims.pluginVersionRange()))) {
            throw new LicenseVerificationException("PLUGIN_MISMATCH", "License plugin version does not match");
        }
        Instant issuedAt = Instant.ofEpochSecond(claims.issuedAt());
        Instant notBefore = Instant.ofEpochSecond(claims.notBefore());
        Instant expiresAt = Instant.ofEpochSecond(claims.expiresAt());
        if (now.plus(CLOCK_SKEW).isBefore(issuedAt)) {
            throw new LicenseVerificationException(
                    "LOCAL_TIME_BEFORE_LICENSE_ISSUED",
                    "Machine time is earlier than license creation time");
        }
        if (now.plus(CLOCK_SKEW).isBefore(notBefore)) {
            throw new LicenseVerificationException("INVALID", "License is not active yet");
        }
        if (!now.isBefore(expiresAt)) {
            throw new LicenseVerificationException("EXPIRED", "License has expired");
        }
    }

    private static PluginLicenseException failure(
            String pluginId,
            LicenseStatus status,
            String machineCode,
            Instant expiresAt,
            String failureCode
    ) {
        PluginLicenseBackend current = requireBackend();
        current.report(pluginId, new PluginLicenseResult(status, machineCode, expiresAt, failureCode));
        return new PluginLicenseException(status, "Plugin license is not valid: " + status);
    }

    private static LicenseStatus failureStatus(String code) {
        try {
            return LicenseStatus.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return LicenseStatus.INVALID;
        }
    }

    private static String cacheKey(
            String pluginId,
            String issuer,
            String productCode,
            String pluginVersion,
            PublicKey publicKey
    ) {
        return pluginId + ":" + sha256(issuer + "\0" + productCode + "\0" + pluginVersion + "\0"
                + HexFormat.of().formatHex(publicKey.getEncoded()));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record CacheEntry(PluginLicenseResult result, Instant validUntil) {
    }
}
