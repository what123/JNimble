package com.jnimble.sdk.plugin;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.RegistrationType;
import com.jnimble.sdk.route.RouteMethod;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SDK 模块所有枚举类单元测试。
 * 验证枚举值定义完整性和 valueOf 正确性。
 */
class EnumCoverageTest {

    /**
     * 测试 PluginLifecyclePhase 枚举值数量和 valueOf 解析。
     */
    @Test
    void testPluginLifecyclePhaseEnum() {
        assertEquals(10, PluginLifecyclePhase.values().length);
        assertSame(PluginLifecyclePhase.INSTALLING, PluginLifecyclePhase.valueOf("INSTALLING"));
        assertSame(PluginLifecyclePhase.FAILED, PluginLifecyclePhase.valueOf("FAILED"));
        assertSame(PluginLifecyclePhase.UNINSTALLED, PluginLifecyclePhase.valueOf("UNINSTALLED"));
    }

    /**
     * 测试 PluginSource 枚举值数量和 valueOf 解析。
     */
    @Test
    void testPluginSourceEnum() {
        assertEquals(3, PluginSource.values().length);
        Set<String> names = Arrays.stream(PluginSource.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertTrue(names.contains("CLASSPATH"));
        assertTrue(names.contains("JAR"));
        assertTrue(names.contains("DIRECTORY"));
    }

    /**
     * 测试 PluginStatus 枚举值数量和 valueOf 解析。
     */
    @Test
    void testPluginStatusEnum() {
        assertEquals(8, PluginStatus.values().length);
        assertSame(PluginStatus.DISCOVERED, PluginStatus.valueOf("DISCOVERED"));
        assertSame(PluginStatus.MIGRATION_FAILED, PluginStatus.valueOf("MIGRATION_FAILED"));
        assertSame(PluginStatus.INCOMPATIBLE, PluginStatus.valueOf("INCOMPATIBLE"));
    }

    /**
     * 测试 RouteMethod 枚举值数量和 valueOf 解析。
     */
    @Test
    void testRouteMethodEnum() {
        assertEquals(5, RouteMethod.values().length);
        assertSame(RouteMethod.GET, RouteMethod.valueOf("GET"));
        assertSame(RouteMethod.POST, RouteMethod.valueOf("POST"));
        assertSame(RouteMethod.PUT, RouteMethod.valueOf("PUT"));
        assertSame(RouteMethod.PATCH, RouteMethod.valueOf("PATCH"));
        assertSame(RouteMethod.DELETE, RouteMethod.valueOf("DELETE"));
    }

    /**
     * 测试 HookMode 枚举值数量和 valueOf 解析。
     */
    @Test
    void testHookModeEnum() {
        assertEquals(4, HookMode.values().length);
        Set<String> names = Arrays.stream(HookMode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertTrue(names.contains("APPEND"));
        assertTrue(names.contains("PREPEND"));
        assertTrue(names.contains("REPLACE"));
        assertTrue(names.contains("REMOVE"));
    }

    /**
     * 测试 RegistrationType 枚举值数量和 valueOf 解析。
     */
    @Test
    void testRegistrationTypeEnum() {
        assertEquals(6, RegistrationType.values().length);
        assertSame(RegistrationType.HOOK, RegistrationType.valueOf("HOOK"));
        assertSame(RegistrationType.ROUTE, RegistrationType.valueOf("ROUTE"));
        assertSame(RegistrationType.ASSET, RegistrationType.valueOf("ASSET"));
        assertSame(RegistrationType.UNKNOWN, RegistrationType.valueOf("UNKNOWN"));
    }

    /**
     * 测试所有枚举类的 values() 返回非空数组。
     */
    @Test
    void testAllEnumValuesNonEmpty() {
        assertTrue(PluginLifecyclePhase.values().length > 0);
        assertTrue(PluginSource.values().length > 0);
        assertTrue(PluginStatus.values().length > 0);
        assertTrue(RouteMethod.values().length > 0);
        assertTrue(HookMode.values().length > 0);
        assertTrue(RegistrationType.values().length > 0);
    }
}
