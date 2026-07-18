package com.jnimble.license.sdk;

/** Raised when a signed license token cannot be trusted or parsed. */
public class LicenseVerificationException extends RuntimeException {

    private final String code;

    public LicenseVerificationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public LicenseVerificationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
