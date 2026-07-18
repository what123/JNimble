package com.jnimble.platform.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * Initializes the default admin user on application startup.
 *
 * <p>Creates the default admin user if it does not already exist.
 * Configuration is read from {@link AuthProperties}.</p>
 */
@Order(0)
public class DefaultUserInitializer implements ApplicationRunner {

    private final AuthProperties authProperties;
    private final UserAccountService userAccountService;

    /**
     * Creates a new default user initializer.
     *
     * @param authProperties     the authentication properties
     * @param userAccountService the user account service
     */
    public DefaultUserInitializer(AuthProperties authProperties, UserAccountService userAccountService) {
        this.authProperties = authProperties;
        this.userAccountService = userAccountService;
    }

    /**
     * Creates the default admin user if it doesn't exist.
     *
     * @param args the application arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        AuthProperties.DefaultUser defaultUser = authProperties.getDefaultUser();
        userAccountService.findByUsername(defaultUser.getUsername())
                .orElseGet(() -> userAccountService.createUser(
                        defaultUser.getUsername(),
                        defaultUser.getPassword(),
                        defaultUser.getDisplayName()));
    }
}
