package com.jnimble.starter.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for plugin discovery and initialization behavior.
 */
@ConfigurationProperties(prefix = "jnimble.plugins")
public class PluginDiscoveryProperties {

    private String platformVersion = "0.1.0";
    private String dir = "./plugins";
    private boolean devClasspathEnabled = true;
    private boolean restoreEnabled = true;
    private boolean directoryScanEnabled = true;
    private boolean directoryWatchEnabled = true;
    private long watchSettleDelayMillis = 1000L;
    private boolean autoEnable;
    private boolean failFast;

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

    /**
     * Returns the directory to scan for plugin JARs.
     *
     * @return the plugin directory path
     */
    public String getDir() {
        return dir;
    }

    /**
     * Sets the directory to scan for plugin JARs.
     *
     * @param dir the plugin directory path
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * Returns whether classpath plugin discovery is enabled.
     *
     * @return true if classpath discovery is enabled
     */
    public boolean isDevClasspathEnabled() {
        return devClasspathEnabled;
    }

    /**
     * Sets whether classpath plugin discovery is enabled.
     *
     * @param devClasspathEnabled true to enable classpath discovery
     */
    public void setDevClasspathEnabled(boolean devClasspathEnabled) {
        this.devClasspathEnabled = devClasspathEnabled;
    }

    /**
     * Returns whether to restore previously persisted plugin states on startup.
     *
     * @return true if state restoration is enabled
     */
    public boolean isRestoreEnabled() {
        return restoreEnabled;
    }

    /**
     * Sets whether to restore previously persisted plugin states on startup.
     *
     * @param restoreEnabled true to enable state restoration
     */
    public void setRestoreEnabled(boolean restoreEnabled) {
        this.restoreEnabled = restoreEnabled;
    }

    /**
     * Returns whether to scan the plugin directory for JAR files.
     *
     * @return true if directory scanning is enabled
     */
    public boolean isDirectoryScanEnabled() {
        return directoryScanEnabled;
    }

    /**
     * Sets whether to scan the plugin directory for JAR files.
     *
     * @param directoryScanEnabled true to enable directory scanning
     */
    public void setDirectoryScanEnabled(boolean directoryScanEnabled) {
        this.directoryScanEnabled = directoryScanEnabled;
    }

    public boolean isDirectoryWatchEnabled() {
        return directoryWatchEnabled;
    }

    public void setDirectoryWatchEnabled(boolean directoryWatchEnabled) {
        this.directoryWatchEnabled = directoryWatchEnabled;
    }

    public long getWatchSettleDelayMillis() {
        return watchSettleDelayMillis;
    }

    public void setWatchSettleDelayMillis(long watchSettleDelayMillis) {
        this.watchSettleDelayMillis = Math.max(250L, watchSettleDelayMillis);
    }

    /**
     * Returns whether to automatically enable discovered plugins.
     *
     * @return true if auto-enable is enabled
     */
    public boolean isAutoEnable() {
        return autoEnable;
    }

    /**
     * Sets whether to automatically enable discovered plugins.
     *
     * @param autoEnable true to enable auto-enable behavior
     */
    public void setAutoEnable(boolean autoEnable) {
        this.autoEnable = autoEnable;
    }

    /**
     * Returns whether to fail fast on plugin initialization errors.
     *
     * @return true if fail-fast is enabled
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether to fail fast on plugin initialization errors.
     *
     * @param failFast true to enable fail-fast behavior
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
