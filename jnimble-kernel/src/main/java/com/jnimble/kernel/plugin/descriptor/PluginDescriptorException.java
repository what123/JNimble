package com.jnimble.kernel.plugin.descriptor;

/**
 * Base exception for plugin descriptor-related errors.
 *
 * <p>Thrown when descriptor loading, validation, or processing fails.</p>
 *
 * 插件描述符相关异常的基础类。当描述符加载、校验或处理失败时抛出。
 */
public class PluginDescriptorException extends RuntimeException {

    /**
     * Creates a new {@code PluginDescriptorException} with the specified message.
     *
     * @param message the detail message describing the error
     */
    public PluginDescriptorException(String message) {
        super(message);
    }

    /**
     * Creates a new {@code PluginDescriptorException} with the specified message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the underlying cause of the error
     */
    public PluginDescriptorException(String message, Throwable cause) {
        super(message, cause);
    }
}
