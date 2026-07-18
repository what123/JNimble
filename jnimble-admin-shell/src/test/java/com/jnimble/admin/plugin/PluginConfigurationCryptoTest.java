package com.jnimble.admin.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginConfigurationCryptoTest {

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndAuthenticatesSecretConfiguration() {
        PluginConfigurationProperties properties = new PluginConfigurationProperties();
        properties.setKeyFile(tempDir.resolve("plugin-config.key"));
        PluginConfigurationCrypto crypto = new PluginConfigurationCrypto(
                new PluginConfigurationKeyProvider(properties));

        String encrypted = crypto.encrypt("payment", "apiKey", "secret-value");

        assertThat(encrypted).startsWith("v1.").doesNotContain("secret-value");
        assertThat(crypto.decrypt("payment", "apiKey", encrypted)).isEqualTo("secret-value");
        assertThatThrownBy(() -> crypto.decrypt("payment", "otherKey", encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authentication failed");
    }
}
