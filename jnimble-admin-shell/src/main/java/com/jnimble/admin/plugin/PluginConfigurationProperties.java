package com.jnimble.admin.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuration properties for plugin configuration encryption.
 *
 * <p>Specifies the file path for the AES encryption key used to protect
 * sensitive plugin configuration values. Controlled via the
 * {@code jnimble.plugins.configuration.key-file} property.</p>
 *
 * <p>插件配置加密的配置属性。指定用于保护敏感插件配置值的 AES 加密密钥文件路径，
 * 通过 {@code jnimble.plugins.configuration.key-file} 属性控制。</p>
 */
@ConfigurationProperties(prefix = "jnimble.plugins.configuration")
public class PluginConfigurationProperties {

    private Path keyFile = Path.of("./data/plugin-config.key");

    public Path getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(Path keyFile) {
        this.keyFile = keyFile;
    }
}
