package com.jnimble.admin.setting;

/** Branding information injected into every admin and login view. */
public record SiteBranding(
        String name,
        String subtitle,
        String logoUrl
) {

    /** Returns true when a custom logo URL has been configured. */
    public boolean hasLogo() {
        return logoUrl != null && !logoUrl.isBlank();
    }

    /** Fallback name used before the database is available. */
    public String nameOrFallback() {
        return name == null || name.isBlank() ? "JNimble" : name;
    }

    /** Fallback subtitle used before the database is available. */
    public String subtitleOrFallback() {
        return subtitle == null || subtitle.isBlank() ? "Operations Console" : subtitle;
    }
}
