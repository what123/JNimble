package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * PluginBeanResolver 单元测试。
 *
 * <p>验证插件 Bean 解析器接口及空操作实现的正确行为。</p>
 */
class PluginBeanResolverTest {

    /**
     * 测试 empty() 方法返回的解析器不为 null。
     */
    @Test
    void emptyReturnsNonNullResolver() {
        PluginBeanResolver resolver = PluginBeanResolver.empty();

        assertThat(resolver).isNotNull();
    }

    /**
     * 测试 empty() 方法返回的解析器在解析 Bean 时抛出 PluginRuntimeException。
     */
    @Test
    void emptyResolverThrowsExceptionOnResolve() {
        PluginBeanResolver resolver = PluginBeanResolver.empty();

        assertThatThrownBy(() -> resolver.resolve(String.class))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("No plugin bean resolver configured")
                .hasMessageContaining("java.lang.String");
    }

    /**
     * 测试 empty() 方法返回的解析器在解析不同类型时抛出包含正确类名的异常。
     */
    @Test
    void emptyResolverThrowsExceptionWithCorrectClassName() {
        PluginBeanResolver resolver = PluginBeanResolver.empty();

        assertThatThrownBy(() -> resolver.resolve(Integer.class))
                .isInstanceOf(PluginRuntimeException.class)
                .hasMessageContaining("java.lang.Integer");
    }

    /**
     * 测试自定义实现的解析器可以正常解析 Bean。
     */
    @Test
    void customResolverCanResolveBean() {
        String expectedValue = "test-bean";
        PluginBeanResolver resolver = new PluginBeanResolver() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T resolve(Class<T> type) {
                if (type == String.class) {
                    return (T) expectedValue;
                }
                return null;
            }
        };

        String result = resolver.resolve(String.class);

        assertThat(result).isEqualTo(expectedValue);
    }

    /**
     * 测试自定义实现的解析器在无法解析时返回 null。
     */
    @Test
    void customResolverReturnsNullWhenCannotResolve() {
        PluginBeanResolver resolver = new PluginBeanResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                return null;
            }
        };

        Object result = resolver.resolve(String.class);

        assertThat(result).isNull();
    }
}
