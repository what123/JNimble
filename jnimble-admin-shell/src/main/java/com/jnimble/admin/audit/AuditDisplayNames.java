package com.jnimble.admin.audit;

import com.jnimble.platform.audit.AuditActions;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides Chinese display name mappings for audit actions, target types, and outcomes.
 *
 * <p>Maps internal audit action codes, target type identifiers, and outcome statuses
 * to human-readable Chinese labels for admin UI rendering.</p>
 *
 * <p>提供审计操作、目标类型和结果的简体中文显示名称映射。
 * 将内部审计操作码、目标类型标识符和结果状态映射为管理界面可读的中文标签。</p>
 */
public final class AuditDisplayNames {

    private static final Map<String, String> ACTION_DISPLAY = new LinkedHashMap<>();
    private static final Map<String, String> TARGET_TYPE_DISPLAY = new LinkedHashMap<>();
    private static final Map<String, String> OUTCOME_DISPLAY = new LinkedHashMap<>();

    static {
        ACTION_DISPLAY.put(AuditActions.PLUGIN_DESCRIPTOR_INSTALL, "安装插件");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_JAR_INSTALL, "安装插件");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_JAR_REPLACE, "替换插件包");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_ENABLE, "启用插件");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_DISABLE, "禁用插件");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_UNINSTALL, "卸载插件");
        ACTION_DISPLAY.put(AuditActions.PLUGIN_RELOAD, "重载插件");
        ACTION_DISPLAY.put(AuditActions.USER_CREATE, "创建用户");
        ACTION_DISPLAY.put(AuditActions.USER_PROFILE_UPDATE, "更新用户资料");
        ACTION_DISPLAY.put(AuditActions.USER_ENABLE, "启用用户");
        ACTION_DISPLAY.put(AuditActions.USER_DISABLE, "禁用用户");
        ACTION_DISPLAY.put(AuditActions.USER_PASSWORD_RESET, "重置密码");
        ACTION_DISPLAY.put(AuditActions.USER_ROLES_UPDATE, "更新用户角色");
        ACTION_DISPLAY.put(AuditActions.USER_SELF_PROFILE_UPDATE, "修改个人资料");
        ACTION_DISPLAY.put(AuditActions.USER_PASSWORD_CHANGE, "修改登录密码");
        ACTION_DISPLAY.put(AuditActions.ROLE_CREATE, "创建角色");
        ACTION_DISPLAY.put(AuditActions.ROLE_PERMISSIONS_UPDATE, "更新角色权限");
        ACTION_DISPLAY.put(AuditActions.LANGUAGE_CREATE, "创建语言");
        ACTION_DISPLAY.put(AuditActions.LANGUAGE_UPDATE, "更新语言");
        ACTION_DISPLAY.put(AuditActions.LANGUAGE_ENABLE, "启用语言");
        ACTION_DISPLAY.put(AuditActions.LANGUAGE_DISABLE, "停用语言");
        ACTION_DISPLAY.put(AuditActions.LANGUAGE_DEFAULT, "设置默认语言");
        ACTION_DISPLAY.put(AuditActions.SYSTEM_SETTING_UPDATE, "更新系统设置");

        TARGET_TYPE_DISPLAY.put("plugin", "插件");
        TARGET_TYPE_DISPLAY.put("user", "用户");
        TARGET_TYPE_DISPLAY.put("role", "角色");
        TARGET_TYPE_DISPLAY.put("language", "语言");
        TARGET_TYPE_DISPLAY.put("system-setting", "系统设置");

        OUTCOME_DISPLAY.put("SUCCESS", "成功");
        OUTCOME_DISPLAY.put("FAILURE", "失败");
    }

    /**
     * Returns the full mapping of action codes to Chinese display names.
     *
     * @return an unmodifiable map of action codes to display names
     */
    public static Map<String, String> actionDisplay() {
        return ACTION_DISPLAY;
    }

    /**
     * Returns the full mapping of target type identifiers to Chinese display names.
     *
     * @return an unmodifiable map of target type identifiers to display names
     */
    public static Map<String, String> targetTypeDisplay() {
        return TARGET_TYPE_DISPLAY;
    }

    /**
     * Returns the full mapping of outcome statuses to Chinese display names.
     *
     * @return an unmodifiable map of outcome statuses to display names
     */
    public static Map<String, String> outcomeDisplay() {
        return OUTCOME_DISPLAY;
    }

    /**
     * Returns the Chinese display name for the given action code, falling back to the code itself.
     *
     * @param action the action code to look up
     * @return the display name, or the action code if no mapping exists
     */
    public static String actionDisplay(String action) {
        return ACTION_DISPLAY.getOrDefault(action, action);
    }

    /**
     * Returns the Chinese display name for the given target type, falling back to the type itself.
     *
     * @param targetType the target type identifier to look up
     * @return the display name, or the target type if no mapping exists
     */
    public static String targetTypeDisplay(String targetType) {
        return TARGET_TYPE_DISPLAY.getOrDefault(targetType, targetType);
    }

    /**
     * Returns the Chinese display name for the given outcome, falling back to the outcome itself.
     *
     * @param outcome the outcome status to look up
     * @return the display name, or the outcome if no mapping exists
     */
    public static String outcomeDisplay(String outcome) {
        return OUTCOME_DISPLAY.getOrDefault(outcome, outcome);
    }

    private AuditDisplayNames() {
    }
}
