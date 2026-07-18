package com.jnimble.platform.permission;

import java.util.List;

/**
 * Constants for system-level permission codes.
 *
 * <p>Defines the built-in permissions for admin operations.</p>
 */
public final class SystemPermissions {

    /** The system owner identifier. */
    public static final String OWNER = "system";

    /** Permission to view plugins. */
    public static final String PLUGIN_VIEW = "system.plugins.view";

    /** Permission to manage plugins (install, enable, disable, uninstall). */
    public static final String PLUGIN_MANAGE = "system.plugins.manage";

    /** Permission to view roles and permissions. */
    public static final String ROLE_VIEW = "system.roles.view";

    /** Permission to manage roles and permissions. */
    public static final String ROLE_MANAGE = "system.roles.manage";

    /** Permission to view users. */
    public static final String USER_VIEW = "system.users.view";

    /** Permission to manage users (create, edit, enable/disable). */
    public static final String USER_MANAGE = "system.users.manage";

    /** Permission to view audit logs. */
    public static final String AUDIT_VIEW = "system.audit.view";

    /** Permission to view configured framework languages. */
    public static final String LANGUAGE_VIEW = "system.languages.view";

    /** Permission to add, edit, enable and select the default language. */
    public static final String LANGUAGE_MANAGE = "system.languages.manage";

    /** Permission to view system settings (site name, logo, etc.). */
    public static final String SETTING_VIEW = "system.settings.view";

    /** Permission to manage system settings. */
    public static final String SETTING_MANAGE = "system.settings.manage";

    private SystemPermissions() {
    }

    /**
     * Returns the definitions of all system permissions.
     *
     * @return a list of permission definitions
     */
    public static List<PermissionDefinition> definitions() {
        return List.of(
                new PermissionDefinition(PLUGIN_VIEW, "查看插件", null, "查看插件列表和插件详情", null),
                new PermissionDefinition(PLUGIN_MANAGE, "管理插件", null, "安装、上传、替换、启用、禁用、卸载和重载插件", null),
                new PermissionDefinition(ROLE_VIEW, "查看角色权限", null, "查看角色列表和角色权限配置", null),
                new PermissionDefinition(ROLE_MANAGE, "管理角色权限", null, "保存角色权限配置", null),
                new PermissionDefinition(USER_VIEW, "查看用户", null, "查看用户列表和用户详情", null),
                new PermissionDefinition(USER_MANAGE, "管理用户", null, "创建用户、维护资料、启用禁用、重置密码和分配角色", null),
                new PermissionDefinition(AUDIT_VIEW, "查看审计日志", null, "查看系统后台审计日志", null),
                new PermissionDefinition(LANGUAGE_VIEW, "查看语言", null, "查看框架语言列表", null),
                new PermissionDefinition(LANGUAGE_MANAGE, "管理语言", null, "新增、编辑、启停语言并设置默认语言", null),
                new PermissionDefinition(SETTING_VIEW, "查看系统设置", null, "查看网站名称、Logo 等系统设置", null),
                new PermissionDefinition(SETTING_MANAGE, "管理系统设置", null, "修改网站名称、Logo 等系统设置", null)
        );
    }
}
