package com.jnimble.platform.audit;

/**
 * Constants for audit action types.
 *
 * <p>Used to identify the type of operation being audited.</p>
 */
public final class AuditActions {

    // Plugin actions
    /** Plugin installed via descriptor. */
    public static final String PLUGIN_DESCRIPTOR_INSTALL = "plugin.install.descriptor";

    /** Plugin installed via JAR upload. */
    public static final String PLUGIN_JAR_INSTALL = "plugin.install.jar";

    /** Plugin package replaced. */
    public static final String PLUGIN_JAR_REPLACE = "plugin.replace.jar";

    /** Plugin enabled. */
    public static final String PLUGIN_ENABLE = "plugin.enable";

    /** Plugin disabled. */
    public static final String PLUGIN_DISABLE = "plugin.disable";

    /** Plugin uninstalled. */
    public static final String PLUGIN_UNINSTALL = "plugin.uninstall";

    /** Plugin reloaded. */
    public static final String PLUGIN_RELOAD = "plugin.reload";

    /** Plugin declarative configuration updated. */
    public static final String PLUGIN_CONFIGURATION_UPDATE = "plugin.configuration.update";

    // User actions
    /** User account created. */
    public static final String USER_CREATE = "user.create";

    /** User profile updated. */
    public static final String USER_PROFILE_UPDATE = "user.profile.update";

    /** User account enabled. */
    public static final String USER_ENABLE = "user.enable";

    /** User account disabled. */
    public static final String USER_DISABLE = "user.disable";

    /** User password reset. */
    public static final String USER_PASSWORD_RESET = "user.password.reset";

    /** User role assignments updated. */
    public static final String USER_ROLES_UPDATE = "user.roles.update";

    /** Signed-in user updated their own profile. */
    public static final String USER_SELF_PROFILE_UPDATE = "user.self-profile.update";

    /** Signed-in user changed their password after current-password verification. */
    public static final String USER_PASSWORD_CHANGE = "user.password.change";

    // Role actions
    /** Role created. */
    public static final String ROLE_CREATE = "role.create";

    /** Role permissions updated. */
    public static final String ROLE_PERMISSIONS_UPDATE = "role.permissions.update";

    // Language actions
    public static final String LANGUAGE_CREATE = "language.create";
    public static final String LANGUAGE_UPDATE = "language.update";
    public static final String LANGUAGE_ENABLE = "language.enable";
    public static final String LANGUAGE_DISABLE = "language.disable";
    public static final String LANGUAGE_DEFAULT = "language.default";

    // System setting actions
    /** System settings (site name, logo, etc.) updated. */
    public static final String SYSTEM_SETTING_UPDATE = "system-setting.update";

    private AuditActions() {
    }
}
