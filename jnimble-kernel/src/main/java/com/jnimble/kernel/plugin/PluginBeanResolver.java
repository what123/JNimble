package com.jnimble.kernel.plugin;

/**
 * Functional interface for resolving Spring beans for plugins.
 *
 * <p>Allows plugins to access platform beans without direct Spring dependency.</p>
 */
@FunctionalInterface
public interface PluginBeanResolver {

    /**
     * Resolves a bean by type.
     *
     * @param <T>    the bean type
     * @param type   the bean class
     * @return the bean instance
     * @throws PluginRuntimeException if the bean cannot be resolved
     */
    <T> T resolve(Class<T> type);

    /**
     * Returns an empty resolver that throws on any resolution attempt.
     *
     * @return an empty resolver
     */
    static PluginBeanResolver empty() {
        return new PluginBeanResolver() {
            @Override
            public <T> T resolve(Class<T> type) {
                throw new PluginRuntimeException("No plugin bean resolver configured for " + type.getName());
            }
        };
    }
}
