package com.jnimble.admin.hook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the admin hook system.
 */
@ConfigurationProperties(prefix = "jnimble.hooks")
public class HookProperties {

    private boolean failFast;

    /**
     * Returns whether to fail fast on hook rendering errors.
     *
     * @return true if fail-fast is enabled
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether to fail fast on hook rendering errors.
     *
     * @param failFast true to enable fail-fast behavior
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
