package com.jnimble.sdk.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PluginContext 接口的单元测试。
 * 验证默认方法 findBean 和 registerHandle 的行为。
 */
class PluginContextTest {

    private TestPluginContext context;

    @BeforeEach
    void setUp() {
        context = new TestPluginContext();
    }

    /**
     * 测试 findBean 方法在 bean 存在时返回 Optional.of。
     */
    @Test
    void findBeanWhenBeanExists() {
        context.setBean(String.class, "testBean");

        Optional<String> result = context.findBean(String.class);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("testBean");
    }

    /**
     * 测试 findBean 方法在 bean 不存在时返回 Optional.empty。
     */
    @Test
    void findBeanWhenBeanDoesNotExist() {
        Optional<String> result = context.findBean(String.class);

        assertThat(result).isEmpty();
    }

    /**
     * 测试 bean 方法在 bean 不存在时抛出 RuntimeException。
     */
    @Test
    void beanWhenBeanDoesNotExistThrowsException() {
        assertThatThrownBy(() -> context.bean(String.class))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试 bean 方法在 bean 存在时返回正确的值。
     */
    @Test
    void beanWhenBeanExistsReturnsValue() {
        context.setBean(Integer.class, 42);

        assertThat(context.bean(Integer.class)).isEqualTo(42);
    }

    /**
     * 测试 registerHandle 方法返回传入的 handle。
     */
    @Test
    void registerHandleReturnsInputHandle() {
        RegistrationHandle handle = () -> {};

        RegistrationHandle result = context.registerHandle(handle);

        assertThat(result).isSameAs(handle);
    }

    /**
     * 测试 descriptor 方法返回正确的值。
     */
    @Test
    void descriptorReturnsCorrectValue() {
        PluginDescriptor expected = createTestDescriptor();
        context.setDescriptor(expected);

        assertThat(context.descriptor()).isEqualTo(expected);
    }

    /**
     * 测试 hooks 方法返回正确的实例。
     */
    @Test
    void hooksReturnsCorrectInstance() {
        HookRegistry expected = createTestHookRegistry();
        context.setHooks(expected);

        assertThat(context.hooks()).isSameAs(expected);
    }

    /**
     * 测试 routes 方法返回正确的实例。
     */
    @Test
    void routesReturnsCorrectInstance() {
        RouteRegistry expected = createTestRouteRegistry();
        context.setRoutes(expected);

        assertThat(context.routes()).isSameAs(expected);
    }

    /**
     * 测试 assets 方法返回正确的实例。
     */
    @Test
    void assetsReturnsCorrectInstance() {
        AssetRegistry expected = createTestAssetRegistry();
        context.setAssets(expected);

        assertThat(context.assets()).isSameAs(expected);
    }

    private PluginDescriptor createTestDescriptor() {
        return new PluginDescriptor(
                "1.0", "test-plugin", "Test Plugin", null, "A test plugin", null,
                "1.0.0", ">=1.0", null, null, "com.example.TestBoot", null, null, null
        );
    }

    private HookRegistry createTestHookRegistry() {
        return new HookRegistry() {
            @Override
            public RegistrationHandle register(String hookName, com.jnimble.sdk.hook.HookViewContribution contribution) {
                return () -> {};
            }
        };
    }

    private RouteRegistry createTestRouteRegistry() {
        return new RouteRegistry() {
            @Override
            public RegistrationHandle register(com.jnimble.sdk.route.RouteDefinition route) {
                return () -> {};
            }
        };
    }

    private AssetRegistry createTestAssetRegistry() {
        return new AssetRegistry() {
            @Override
            public RegistrationHandle register(com.jnimble.sdk.resource.AssetDefinition asset) {
                return () -> {};
            }
        };
    }

    /**
     * 用于测试的 PluginContext 实现。
     */
    static class TestPluginContext implements PluginContext {
        private PluginDescriptor descriptor;
        private HookRegistry hooks;
        private RouteRegistry routes;
        private AssetRegistry assets;
        private final Map<Class<?>, Object> beans = new HashMap<>();

        void setDescriptor(PluginDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        void setHooks(HookRegistry hooks) {
            this.hooks = hooks;
        }

        void setRoutes(RouteRegistry routes) {
            this.routes = routes;
        }

        void setAssets(AssetRegistry assets) {
            this.assets = assets;
        }

        <T> void setBean(Class<T> type, T value) {
            beans.put(type, value);
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public HookRegistry hooks() {
            return hooks;
        }

        @Override
        public RouteRegistry routes() {
            return routes;
        }

        @Override
        public AssetRegistry assets() {
            return assets;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T bean(Class<T> type) {
            T bean = (T) beans.get(type);
            if (bean == null) {
                throw new RuntimeException("No bean of type " + type.getName());
            }
            return bean;
        }
    }
}
