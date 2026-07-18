package com.jnimble.kernel.plugin.descriptor;

/**
 * Exception thrown when a plugin descriptor cannot be loaded.
 *
 * <p>Indicates that the descriptor file could not be read, parsed, or
 * retrieved from the specified location.</p>
 *
 * 当插件描述符无法加载时抛出的异常。
 * 指示描述符文件无法从指定位置读取、解析或获取。
 */
public class PluginDescriptorLoadException extends PluginDescriptorException {

    /**
     * Creates a new {@code PluginDescriptorLoadException} with the specified message and cause.
     *
     * @param message the detail message describing the loading failure
     * @param cause   the underlying cause of the loading failure
     */
    public PluginDescriptorLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
