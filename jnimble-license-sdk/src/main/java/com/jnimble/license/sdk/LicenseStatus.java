package com.jnimble.license.sdk;

/** Commercial license status kept separately from plugin runtime status. */
public enum LicenseStatus {
    NOT_REQUIRED,
    MISSING,
    UNVERIFIED,
    VALID,
    EXPIRING,
    EXPIRED,
    INVALID,
    MACHINE_CODE_MISMATCH,
    PLUGIN_MISMATCH,
    UNKNOWN_KEY,
    LOCAL_TIME_BEFORE_LICENSE_ISSUED,
    TIME_ROLLBACK,
    TIME_SNAPSHOT_MISSING,
    TIME_SNAPSHOT_TAMPERED
}
