package com.jnimble.sdk.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PluginException 异常类单元测试。
 * 测试消息构造、原因构造以及异常传播特性。
 */
class PluginExceptionTest {

    /**
     * 测试仅消息构造：验证异常消息和类型正确。
     */
    @Test
    void testConstructorWithMessage() {
        PluginException exception = new PluginException("插件启动失败");

        assertEquals("插件启动失败", exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    /**
     * 测试消息和原因构造：验证异常链正确保留。
     */
    @Test
    void testConstructorWithMessageAndCause() {
        RuntimeException rootCause = new RuntimeException("数据库连接超时");
        PluginException exception = new PluginException("插件安装失败", rootCause);

        assertEquals("插件安装失败", exception.getMessage());
        assertSame(rootCause, exception.getCause());
    }

    /**
     * 测试异常可被正确抛出和捕获。
     */
    @Test
    void testExceptionIsThrownAndCaught() {
        assertThrows(PluginException.class, () -> {
            throw new PluginException("强制异常");
        });
    }

    /**
     * 测试异常链传播：捕获外层异常时能追溯到原始原因。
     */
    @Test
    void testExceptionCauseChainPreserved() {
        IllegalArgumentException originalCause = new IllegalArgumentException("无效参数");
        PluginException exception = new PluginException("权限校验失败", originalCause);

        assertNotNull(exception.getCause());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("无效参数", exception.getCause().getMessage());
    }
}
