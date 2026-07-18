package com.jnimble.license.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

public final class Ed25519LicenseTokenVerifier {

    private static final int MAX_TOKEN_LENGTH = 16 * 1024;

    private final ObjectMapper objectMapper;
    private final PublicKey publicKey;

    public Ed25519LicenseTokenVerifier(ObjectMapper objectMapper, PublicKey publicKey) {
        this.objectMapper = objectMapper;
        this.publicKey = publicKey;
    }

    public VerifiedLicenseToken verify(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new LicenseVerificationException("INVALID_TOKEN", "License token is missing or too large");
        }
        try {
            String[] parts = token.trim().split("\\.", -1);
            if (parts.length != 3) {
                throw new LicenseVerificationException("INVALID_TOKEN", "License token format is invalid");
            }
            JsonNode header = objectMapper.readTree(decode(parts[0]));
            if (!"JNIMBLE-LICENSE".equals(header.path("typ").asText())
                    || !"EdDSA".equals(header.path("alg").asText())) {
                throw new LicenseVerificationException("UNSUPPORTED_ALGORITHM", "License algorithm is not allowed");
            }
            String issuer = required(header, "iss");
            String keyId = required(header, "kid");
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            boolean signatureValid;
            try {
                signatureValid = signature.verify(Base64.getUrlDecoder().decode(parts[2]));
            } catch (SignatureException ex) {
                throw new LicenseVerificationException(
                        "INVALID_SIGNATURE",
                        "License signature is invalid",
                        ex);
            }
            if (!signatureValid) {
                throw new LicenseVerificationException("INVALID_SIGNATURE", "License signature is invalid");
            }
            LicenseClaims claims = objectMapper.readValue(decode(parts[1]), LicenseClaims.class);
            return new VerifiedLicenseToken(issuer, keyId, claims);
        } catch (LicenseVerificationException ex) {
            throw ex;
        } catch (GeneralSecurityException | RuntimeException | java.io.IOException ex) {
            throw new LicenseVerificationException("INVALID_TOKEN", "License token cannot be parsed", ex);
        }
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) {
            throw new LicenseVerificationException("INVALID_TOKEN", "License header " + field + " is required");
        }
        return value;
    }
}
