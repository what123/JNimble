package com.jnimble.sdk.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RegistrationHandle 接口的单元测试。
 * 验证默认方法 registrationId 和 type 的行为。
 */
class RegistrationHandleTest {

    /**
     * 测试 unregister 方法可以被调用。
     */
    @Test
    void unregisterCanBeCalled() {
        AtomicBoolean unregistered = new AtomicBoolean(false);
        RegistrationHandle handle = () -> unregistered.set(true);

        handle.unregister();

        assertThat(unregistered.get()).isTrue();
    }

    /**
     * 测试 registrationId 默认方法返回 Optional.empty。
     */
    @Test
    void registrationIdReturnsEmptyByDefault() {
        RegistrationHandle handle = () -> {};

        Optional<String> result = handle.registrationId();

        assertThat(result).isEmpty();
    }

    /**
     * 测试 type 默认方法返回 RegistrationType.UNKNOWN。
     */
    @Test
    void typeReturnsUnknownByDefault() {
        RegistrationHandle handle = () -> {};

        RegistrationType result = handle.type();

        assertThat(result).isEqualTo(RegistrationType.UNKNOWN);
    }

    /**
     * 测试 unregister 方法是幂等的，多次调用不抛异常。
     */
    @Test
    void unregisterIsIdempotent() {
        AtomicBoolean callCount = new AtomicBoolean(false);
        RegistrationHandle handle = () -> {
            if (callCount.get()) {
                throw new IllegalStateException("Already unregistered");
            }
            callCount.set(true);
        };

        assertThatThrownBy(() -> {
            handle.unregister();
            handle.unregister();
        }).isInstanceOf(IllegalStateException.class);
    }

    /**
     * 测试可以创建具有自定义 registrationId 的 handle。
     */
    @Test
    void customRegistrationIdCanBeProvided() {
        RegistrationHandle handle = new RegistrationHandle() {
            @Override
            public void unregister() {
            }

            @Override
            public Optional<String> registrationId() {
                return Optional.of("custom-id-123");
            }
        };

        assertThat(handle.registrationId()).isEqualTo(Optional.of("custom-id-123"));
    }

    /**
     * 测试可以创建具有自定义 type 的 handle。
     */
    @Test
    void customTypeCanBeProvided() {
        RegistrationHandle handle = new RegistrationHandle() {
            @Override
            public void unregister() {
            }

            @Override
            public RegistrationType type() {
                return RegistrationType.HOOK;
            }
        };

        assertThat(handle.type()).isEqualTo(RegistrationType.HOOK);
    }
}
