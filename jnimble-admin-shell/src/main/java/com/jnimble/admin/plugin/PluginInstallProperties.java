package com.jnimble.admin.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for plugin installation settings.
 */
@ConfigurationProperties(prefix = "jnimble.plugins")
public class PluginInstallProperties {

    private String dir = "./plugins";
    private String platformVersion = "0.1.0";

    /**
     * Returns the directory where plugin JARs are stored.
     *
     * @return the plugin directory path
     */
    public String getDir() {
        return dir;
    }

    /**
     * Sets the directory where plugin JARs are stored.
     *
     * @param dir the plugin directory path
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * Returns the target platform version for plugin compatibility checks.
     *
     * @return the platform version string
     */
    public String getPlatformVersion() {
        return platformVersion;
    }

    /**
     * Sets the target platform version for plugin compatibility checks.
     *
     * @param platformVersion the platform version string
     */
    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }
}
