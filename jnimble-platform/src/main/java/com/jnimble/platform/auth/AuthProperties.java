package com.jnimble.platform.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for authentication and security settings.
 *
 * <p>Bound to the {@code jnimble.security} prefix. Configures the default admin
 * user credentials and role for initial application startup.</p>
 *
 * <p>认证与安全设置的配置属性。绑定到 {@code jnimble.security} 前缀。
 * 配置默认管理员用户的凭证和角色，用于应用程序初始启动。</p>
 */
@ConfigurationProperties(prefix = "jnimble.security")
public class AuthProperties {

    private final DefaultUser defaultUser = new DefaultUser();
    private final Users users = new Users();

    /**
     * Gets the default user configuration.
     *
     * @return the default user properties
     */
    public DefaultUser getDefaultUser() {
        return defaultUser;
    }

    /**
     * Gets the users configuration (reserved for future use).
     *
     * @return the users properties
     */
    public Users getUsers() {
        return users;
    }

    /**
     * Nested properties for the default administrator user account.
     *
     * <p>默认管理员用户账户的嵌套配置属性。</p>
     */
    public static class DefaultUser {

        private String username = "admin";
        private String password = "admin";
        private String displayName = "Administrator";
        private String role = "ADMIN";

        /**
         * Gets the default admin username.
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the default admin username.
         *
         * @param username the username to set
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Gets the default admin password (plain text, will be encoded).
         *
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the default admin password.
         *
         * @param password the password to set
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets the default admin display name.
         *
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Sets the default admin display name.
         *
         * @param displayName the display name to set
         */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Gets the default admin role code.
         *
         * @return the role code
         */
        public String getRole() {
            return role;
        }

        /**
         * Sets the default admin role code.
         *
         * @param role the role code to set
         */
        public void setRole(String role) {
            this.role = role;
        }
    }

    /**
     * Reserved for additional user-related configuration properties.
     *
     * <p>预留的用户相关配置属性。</p>
     */
    public static class Users {
    }
}
