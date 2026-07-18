package com.jnimble.admin.plugin;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM based encryption and decryption utility for plugin configuration secrets.
 *
 * <p>Uses AES-256 in GCM mode with additional authenticated data (AAD) derived
 * from the plugin ID and configuration key to bind encrypted values to their context.</p>
 *
 * <p>插件配置加密工具。基于 AES-GCM 模式对插件配置中的机密字段进行加密和解密，
 * 使用插件 ID 和配置键推导附加认证数据（AAD），将加密值与上下文绑定。</p>
 */
@Component
class PluginConfigurationCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PluginConfigurationKeyProvider keyProvider;

    PluginConfigurationCrypto(PluginConfigurationKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    String encrypt(String pluginId, String key, String value) {
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.key(), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(pluginId, key));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "v1." + encoder.encodeToString(nonce) + "." + encoder.encodeToString(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt plugin configuration", ex);
        }
    }

    String decrypt(String pluginId, String key, String envelope) {
        try {
            String[] parts = envelope == null ? new String[0] : envelope.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalStateException("Plugin configuration encryption envelope is invalid");
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] nonce = decoder.decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyProvider.key(), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(pluginId, key));
            return new String(cipher.doFinal(decoder.decode(parts[2])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Plugin configuration authentication failed", ex);
        }
    }

    private byte[] aad(String pluginId, String key) {
        return ("1|" + pluginId + "|" + key).getBytes(StandardCharsets.UTF_8);
    }
}
