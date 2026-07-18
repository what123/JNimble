package com.jnimble.kernel.plugin.descriptor;

import java.util.List;

/**
 * Exception thrown when a plugin descriptor fails validation.
 *
 * <p>Contains a list of violation messages describing what was invalid
 * in the descriptor.</p>
 *
 * 当插件描述符校验失败时抛出的异常。包含描述描述符中无效内容的违规消息列表。
 */
public class PluginDescriptorValidationException extends PluginDescriptorException {

    private final List<String> violations;

    /**
     * Creates a new {@code PluginDescriptorValidationException} with the given violations.
     *
     * @param violations the list of validation violation messages
     */
    public PluginDescriptorValidationException(List<String> violations) {
        super("Invalid plugin descriptor: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the list of validation violations.
     *
     * @return an immutable list of violation messages
     */
    public List<String> violations() {
        return violations;
    }
}
