package com.jnimble.platform.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for determining the current audit actor.
 *
 * <p>Extracts the current user from the Spring Security context.</p>
 */
final class AuditActors {

    private AuditActors() {
    }

    /**
     * Returns the current actor identifier.
     *
     * @return the username of the authenticated user, or "system" if not authenticated
     */
    static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }
}
