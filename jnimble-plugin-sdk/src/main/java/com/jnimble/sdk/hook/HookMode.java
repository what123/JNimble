package com.jnimble.sdk.hook;

/**
 * Rendering mode requested by a plugin contribution for a hook point.
 */
public enum HookMode {
    /** Append contribution after existing ones. */
    APPEND,
    /** Prepend contribution before existing ones. */
    PREPEND,
    /** Replace all existing contributions at the hook point. */
    REPLACE,
    /** Remove the matching contribution from the hook point. */
    REMOVE
}
