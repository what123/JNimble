package com.jnimble.platform.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the audit logging system.
 * Supports both in-memory and JDBC-based audit storage.
 */
@ConfigurationProperties(prefix = "jnimble.audit")
public class AuditProperties {

    /**
     * Maximum number of audit entries to keep in memory.
     */
    private int maxEntries = 1000;

    /**
     * Gets the maximum number of audit entries to keep in memory.
     *
     * @return the maximum number of entries
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Sets the maximum number of audit entries to keep in memory.
     *
     * @param maxEntries the maximum number of entries
     */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
