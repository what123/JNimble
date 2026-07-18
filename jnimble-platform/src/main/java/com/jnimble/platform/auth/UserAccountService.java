package com.jnimble.platform.auth;

import java.util.Collection;
import java.util.Optional;

/**
 * Service interface for managing user accounts.
 *
 * <p>Provides CRUD operations for user accounts including creation, lookup,
 * profile updates, and account status management. Implementations may store
 * data in memory or persist to a database via JDBC.</p>
 */
public interface UserAccountService {

    /**
     * Creates a new user account with the given credentials.
     *
     * @param username    the unique username, must not be blank
     * @param rawPassword the plain-text password (will be encoded before storage)
     * @param displayName the display name for the user
     * @return the created user record
     * @throws IllegalArgumentException if username is blank or already exists
     */
    UserRecord createUser(String username, String rawPassword, String displayName);

    /**
     * Finds a user account by username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user record if found, empty otherwise
     */
    Optional<UserRecord> findByUsername(String username);

    /**
     * Lists all user accounts in the system.
     *
     * @return a collection of all user records
     */
    Collection<UserRecord> listUsers();

    /**
     * Updates the display name for a user account.
     *
     * @param username    the username of the account to update
     * @param displayName the new display name
     * @return the updated user record
     * @throws IllegalArgumentException if user not found
     */
    UserRecord updateDisplayName(String username, String displayName);

    /**
     * Enables a user account, allowing login.
     *
     * @param username the username to enable
     * @throws IllegalArgumentException if user not found
     */
    void enableUser(String username);

    /**
     * Disables a user account, preventing login.
     *
     * @param username the username to disable
     * @throws IllegalArgumentException if user not found
     */
    void disableUser(String username);

    /**
     * Resets the password for a user account.
     *
     * @param username    the username of the account
     * @param rawPassword the new plain-text password
     * @throws IllegalArgumentException if user not found
     */
    void resetPassword(String username, String rawPassword);

    /**
     * Changes the password for a user account.
     *
     * <p>Default implementation delegates to {@link #resetPassword(String, String)}.</p>
     *
     * @param username    the username of the account
     * @param rawPassword the new plain-text password
     */
    default void changePassword(String username, String rawPassword) {
        resetPassword(username, rawPassword);
    }

    /**
     * Changes the username for a user account.
     *
     * @param oldUsername the current username
     * @param newUsername the new username
     * @return the updated user record
     * @throws IllegalArgumentException if user not found or new username already exists
     */
    UserRecord changeUsername(String oldUsername, String newUsername);
}
