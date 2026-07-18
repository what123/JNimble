package com.jnimble.platform.auth;

import com.jnimble.platform.permission.AuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Component for checking authorization in controllers.
 *
 * <p>Provides convenience methods for checking if the current user has
 * a specific permission. Throws {@link AccessDeniedException} if
 * permission is missing.</p>
 */
@Component
public class ControllerAuthorization {

    private final AuthorizationService authorizationService;

    /**
     * Creates a new controller authorization component.
     *
     * @param authorizationService the authorization service
     */
    public ControllerAuthorization(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Checks if the current user has the specified permission.
     *
     * @param permissionCode the permission code to check
     * @return {@code true} if the user has the permission
     */
    public boolean hasPermission(String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authorizationService.hasPermission(authentication.getName(), permissionCode);
    }

    /**
     * Requires the current user to have the specified permission.
     *
     * @param permissionCode the permission code required
     * @throws AccessDeniedException if the user does not have the permission
     */
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new AccessDeniedException("Missing permission: " + permissionCode);
        }
    }
}
