package com.jnimble.license.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginLicenseTest {

    private static final String PLUGIN_ID = "crm";
    private static final String ISSUER = "jnimble";
    private static final String PRODUCT_CODE = "crm";
    private static final String PLUGIN_VERSION = "1.0.0";
    private static final String MACHINE_CODE = "JN1-CRM-TEST";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private KeyPair keyPair;
    private CountingBackend backend;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        backend = new CountingBackend();
        PluginLicense.installBackend(backend);
        PluginLicense.invalidate(PLUGIN_ID);
    }

    @AfterEach
    void tearDown() {
        PluginLicense.invalidate(PLUGIN_ID);
    }

    @Test
    void validLicenseUsesCacheForRepeatedChecks() throws Exception {
        backend.token = token(keyPair, MACHINE_CODE, Instant.now().plusSeconds(86400));

        PluginLicense.requireValid(PLUGIN_ID, ISSUER, PRODUCT_CODE, PLUGIN_VERSION, keyPair.getPublic());
        PluginLicense.requireValid(PLUGIN_ID, ISSUER, PRODUCT_CODE, PLUGIN_VERSION, keyPair.getPublic());

        assertEquals(1, backend.loadCount.get());
        assertEquals(1, backend.reportCount.get());
    }

    @Test
    void forceVerificationBypassesCache() throws Exception {
        backend.token = token(keyPair, MACHINE_CODE, Instant.now().plusSeconds(86400));

        PluginLicense.verify(PLUGIN_ID, ISSUER, PRODUCT_CODE, PLUGIN_VERSION, keyPair.getPublic());
        PluginLicenseResult result = PluginLicense.verify(
                PLUGIN_ID,
                ISSUER,
                PRODUCT_CODE,
                PLUGIN_VERSION,
                keyPair.getPublic(),
                true);

        assertTrue(result.usable());
        assertEquals(2, backend.loadCount.get());
        assertEquals(2, backend.reportCount.get());
    }

    @Test
    void invalidLicenseThrowsAndIsNotCached() throws Exception {
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        backend.token = token(otherKeyPair, MACHINE_CODE, Instant.now().plusSeconds(86400));

        assertThrows(
                PluginLicenseException.class,
                () -> PluginLicense.requireValid(
                        PLUGIN_ID,
                        ISSUER,
                        PRODUCT_CODE,
                        PLUGIN_VERSION,
                        keyPair.getPublic()));
        assertThrows(
                PluginLicenseException.class,
                () -> PluginLicense.requireValid(
                        PLUGIN_ID,
                        ISSUER,
                        PRODUCT_CODE,
                        PLUGIN_VERSION,
                        keyPair.getPublic()));

        assertEquals(2, backend.loadCount.get());
        assertEquals(2, backend.reportCount.get());
        assertEquals(LicenseStatus.INVALID, backend.lastResult.status());
    }

    private String token(KeyPair signingKeyPair, String machineCode, Instant expiresAt) throws Exception {
        Instant now = Instant.now();
        LicenseClaims claims = new LicenseClaims(
                1,
                "license-id",
                ISSUER,
                PRODUCT_CODE,
                PLUGIN_VERSION,
                1,
                machineCode,
                now.getEpochSecond(),
                now.getEpochSecond(),
                expiresAt.getEpochSecond());
        Map<String, String> header = new LinkedHashMap<>();
        header.put("typ", "JNIMBLE-LICENSE");
        header.put("alg", "EdDSA");
        header.put("iss", ISSUER);
        header.put("kid", "plugin-key");
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String encodedHeader = encoder.encodeToString(objectMapper.writeValueAsBytes(header));
        String encodedPayload = encoder.encodeToString(objectMapper.writeValueAsBytes(claims));
        String signingInput = encodedHeader + "." + encodedPayload;
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(signingKeyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + encoder.encodeToString(signature.sign());
    }

    private static final class CountingBackend implements PluginLicenseBackend {

        private final AtomicInteger loadCount = new AtomicInteger();
        private final AtomicInteger reportCount = new AtomicInteger();
        private String token;
        private PluginLicenseResult lastResult;

        @Override
        public Optional<String> loadToken(String pluginId) {
            loadCount.incrementAndGet();
            return Optional.ofNullable(token);
        }

        @Override
        public String machineCode(String pluginId, String productCode) {
            return MACHINE_CODE;
        }

        @Override
        public void report(String pluginId, PluginLicenseResult result) {
            reportCount.incrementAndGet();
            lastResult = result;
        }
    }
}
