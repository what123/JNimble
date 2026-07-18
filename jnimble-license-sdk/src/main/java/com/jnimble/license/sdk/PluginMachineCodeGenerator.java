package com.jnimble.license.sdk;

import com.google.common.net.InternetDomainName;

import java.net.IDN;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.CRC32;

/** Generates stable machine codes from a canonical domain and plugin product code. */
public final class PluginMachineCodeGenerator {

    private static final String PREFIX = "JNM1";
    private static final String CONTEXT = "JNIMBLE-MACHINE-CODE\0v1\0";

    public MachineCode generate(String canonicalDomain, String productCode) {
        String domain = normalizeDomain(canonicalDomain);
        String product = normalizeProductCode(productCode);
        String encodedProduct = Base32Codec.encode(product.getBytes(StandardCharsets.UTF_8));
        String fingerprint = Base32Codec.encode(sha256((CONTEXT + domain + "\0" + product)
                .getBytes(StandardCharsets.UTF_8)));
        String body = PREFIX + "." + encodedProduct + "." + fingerprint;
        return new MachineCode(1, product, body + "." + checksum(body));
    }

    public MachineCode parse(String machineCode) {
        String value = Objects.requireNonNull(machineCode, "machineCode").trim().toUpperCase(Locale.ROOT);
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Invalid machine code format");
        }
        String body = String.join(".", parts[0], parts[1], parts[2]);
        if (!checksum(body).equals(parts[3])) {
            throw new IllegalArgumentException("Invalid machine code checksum");
        }
        byte[] productBytes = Base32Codec.decode(parts[1]);
        String productCode = normalizeProductCode(new String(productBytes, StandardCharsets.UTF_8));
        if (parts[2].length() != 52) {
            throw new IllegalArgumentException("Invalid machine code fingerprint");
        }
        Base32Codec.decode(parts[2]);
        return new MachineCode(1, productCode, value);
    }

    private String normalizeDomain(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Canonical domain is required");
        }
        String value = input.trim();
        if (value.contains("://")) {
            URI uri = URI.create(value);
            value = uri.getHost();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Canonical domain is invalid");
        }
        value = IDN.toASCII(value.replaceFirst("\\.$", "").toLowerCase(Locale.ROOT));
        try {
            InternetDomainName domain = InternetDomainName.from(value);
            if (domain.hasPublicSuffix() && !domain.isPublicSuffix()) {
                return domain.topPrivateDomain().toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Local development hosts and IP literals remain unchanged.
        }
        return value;
    }

    private String normalizeProductCode(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,127}")) {
            throw new IllegalArgumentException("Product code is invalid");
        }
        return value;
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String checksum(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes(StandardCharsets.US_ASCII));
        byte[] bytes = new byte[]{
                (byte) (crc32.getValue() >>> 24),
                (byte) (crc32.getValue() >>> 16),
                (byte) (crc32.getValue() >>> 8),
                (byte) crc32.getValue()
        };
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
