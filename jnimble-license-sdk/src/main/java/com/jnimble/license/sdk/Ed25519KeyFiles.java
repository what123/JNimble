package com.jnimble.license.sdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Utility for loading and generating Ed25519 key pairs from/to files.
 * <p>
 * File layout:
 * <ul>
 *   <li>{@code <base>.pk8} — PKCS#8 encoded private key</li>
 *   <li>{@code <base>.pub} — X.509 encoded public key</li>
 * </ul>
 */
public final class Ed25519KeyFiles {

    private Ed25519KeyFiles() {
    }

    /**
     * Loads an Ed25519 key pair from files, or generates and persists a new pair if files don't exist.
     *
     * @param base the base path; {@code .pk8} and {@code .pub} suffixes are appended
     * @return the loaded or generated key pair
     */
    public static KeyPair loadOrCreate(Path base) {
        Path privatePath = Path.of(base + ".pk8");
        Path publicPath = Path.of(base + ".pub");
        try {
            if (Files.exists(privatePath) && Files.exists(publicPath)) {
                KeyFactory factory = KeyFactory.getInstance("Ed25519");
                PrivateKey privateKey = factory.generatePrivate(
                        new PKCS8EncodedKeySpec(Files.readAllBytes(privatePath)));
                PublicKey publicKey = factory.generatePublic(
                        new X509EncodedKeySpec(Files.readAllBytes(publicPath)));
                return new KeyPair(publicKey, privateKey);
            }
            Path parent = privatePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            KeyPair generated = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            writeAtomically(privatePath, generated.getPrivate().getEncoded(), true);
            writeAtomically(publicPath, generated.getPublic().getEncoded(), false);
            return generated;
        } catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to load Ed25519 key pair from " + base, ex);
        }
    }

    /**
     * Loads only the public key from the {@code .pub} file.
     *
     * @param base the base path; {@code .pub} suffix is appended
     * @return the loaded public key, or {@code null} if the file doesn't exist
     */
    public static PublicKey loadPublicKey(Path base) {
        Path publicPath = Path.of(base + ".pub");
        if (!Files.exists(publicPath)) {
            return null;
        }
        try {
            byte[] keyBytes = Files.readAllBytes(publicPath);
            return KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to load Ed25519 public key from " + publicPath, ex);
        }
    }

    private static void writeAtomically(Path path, byte[] bytes, boolean privateFile) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        if (privateFile) {
            ownerOnly(temporary);
        }
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        if (privateFile) {
            ownerOnly(path);
        }
    }

    private static void ownerOnly(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX systems rely on their configured file protection.
        }
    }
}
