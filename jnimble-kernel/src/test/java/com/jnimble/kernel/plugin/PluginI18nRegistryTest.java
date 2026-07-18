package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.jnimble.sdk.hook.RegistrationHandle;
import org.junit.jupiter.api.Test;

/**
 * PluginI18nRegistry 单元测试。
 *
 * <p>验证插件国际化资源注册表接口及空操作实现的正确行为。</p>
 */
class PluginI18nRegistryTest {

    /**
     * 测试 noop() 方法返回的注册表不为 null。
     */
    @Test
    void noopReturnsNonNullRegistry() {
        PluginI18nRegistry registry = PluginI18nRegistry.noop();

        assertThat(registry).isNotNull();
    }

    /**
     * 测试空操作注册表的 register 方法返回有效的处理句柄。
     */
    @Test
    void noopRegisterReturnsValidHandle() {
        PluginI18nRegistry registry = PluginI18nRegistry.noop();

        RegistrationHandle handle = registry.register("test-plugin", "messages", getClass().getClassLoader());

        assertThat(handle).isNotNull();
    }

    /**
     * 测试空操作注册表的处理句柄可以安全调用 unregister 方法。
     */
    @Test
    void noopHandleUnregisterDoesNotThrow() {
        PluginI18nRegistry registry = PluginI18nRegistry.noop();

        RegistrationHandle handle = registry.register("test-plugin", "messages", getClass().getClassLoader());

        assertThatCode(handle::unregister).doesNotThrowAnyException();
    }

    /**
     * 测试空操作注册表对不同插件 ID 的注册都返回有效句柄。
     */
    @Test
    void noopRegisterWithDifferentPluginIdsReturnsHandles() {
        PluginI18nRegistry registry = PluginI18nRegistry.noop();

        RegistrationHandle handle1 = registry.register("plugin-a", "bundle-a", getClass().getClassLoader());
        RegistrationHandle handle2 = registry.register("plugin-b", "bundle-b", getClass().getClassLoader());

        assertThat(handle1).isNotNull();
        assertThat(handle2).isNotNull();
    }

    /**
     * 测试空操作注册表对不同 basename 的注册都返回有效句柄。
     */
    @Test
    void noopRegisterWithDifferentBasenamesReturnsHandles() {
        PluginI18nRegistry registry = PluginI18nRegistry.noop();

        RegistrationHandle handle1 = registry.register("test-plugin", "messages", getClass().getClassLoader());
        RegistrationHandle handle2 = registry.register("test-plugin", "errors", getClass().getClassLoader());

        assertThat(handle1).isNotNull();
        assertThat(handle2).isNotNull();
    }
}
