package com.jnimble.license.sdk;

/** Raised when licensed plugin functionality is not currently allowed. */
public class PluginLicenseException extends RuntimeException {

    private final LicenseStatus status;

    public PluginLicenseException(LicenseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public LicenseStatus status() {
        return status;
    }
}
