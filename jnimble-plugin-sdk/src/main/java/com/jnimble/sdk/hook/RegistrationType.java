package com.jnimble.sdk.hook;

/**
 * Runtime contribution categories that can be removed through a
 * {@link RegistrationHandle}.
 */
public enum RegistrationType {
    /** Hook view contribution. */
    HOOK,
    /** Route registration. */
    ROUTE,
    /** Static asset registration. */
    ASSET,
    /** Internationalization resource registration. */
    I18N,
    /** Permission registration. */
    PERMISSION,
    /** Unrecognised or unspecified contribution kind. */
    UNKNOWN
}
