package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginDescriptor;

/**
 * Optional guard invoked immediately before a plugin is loaded and booted.
 *
 * <p>Implementations can throw a runtime exception to prevent plugin activation.</p>
 *
 * 可选守卫，在插件加载和启动之前调用。实现可以抛出运行时异常来阻止插件激活。
 */
@FunctionalInterface
public interface PluginActivationGuard {

    /**
     * Checks whether the plugin can be activated.
     *
     * @param descriptor the plugin descriptor to check
     * @throws RuntimeException if the plugin cannot be activated
     */
    void requireCanActivate(PluginDescriptor descriptor);

    /**
     * Returns a guard that allows all plugins to activate.
     *
     * @return a no-op guard
     */
    static PluginActivationGuard allowAll() {
        return descriptor -> {
        };
    }
}
