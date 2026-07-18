package com.jnimble.admin.plugin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.Set;

/**
 * Provides and manages the AES encryption key for plugin configuration secrets.
 *
 * <p>Loads the encryption key from a file on disk, or generates a new 256-bit key
 * if the file does not exist. The key file is restricted to owner-only read/write
 * permissions on POSIX systems.</p>
 *
 * <p>插件配置加密密钥提供者。从磁盘文件加载 AES 加密密钥，如果文件不存在则生成
 * 新的 256 位密钥。在 POSIX 系统上密钥文件将被限制为仅所有者可读写。</p>
 */
@Component
@EnableConfigurationProperties(PluginConfigurationProperties.class)
class PluginConfigurationKeyProvider {

    private final PluginConfigurationProperties properties;
    private volatile SecretKey cached;

    PluginConfigurationKeyProvider(PluginConfigurationProperties properties) {
        this.properties = properties;
    }

    synchronized SecretKey key() {
        if (cached != null) {
            return cached;
        }
        Path path = properties.getKeyFile().toAbsolutePath().normalize();
        try {
            if (Files.exists(path)) {
                byte[] encoded = Files.readAllBytes(path);
                if (encoded.length != 32) {
                    throw new IllegalStateException("Plugin configuration key must contain 32 bytes");
                }
                cached = new SecretKeySpec(encoded, "AES");
                return cached;
            }
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey generated = generator.generateKey();
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(temporary, generated.getEncoded());
            ownerOnly(temporary);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            ownerOnly(path);
            cached = generated;
            return cached;
        } catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to load plugin configuration encryption key", ex);
        }
    }

    private void ownerOnly(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX systems rely on their configured OS key/file protection.
        }
    }
}
