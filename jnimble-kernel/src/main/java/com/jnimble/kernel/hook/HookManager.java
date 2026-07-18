package com.jnimble.kernel.hook;

import com.jnimble.kernel.hook.InMemoryHookRegistry.HookRegistration;
import com.jnimble.kernel.hook.InMemoryHookRegistry.HookResolution;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manager for querying and resolving hook contributions.
 *
 * <p>Provides a facade over the hook registry for listing and resolving hooks.</p>
 */
@Component
public class HookManager {

    private final InMemoryHookRegistry registry;

    /**
     * Creates a new hook manager.
     *
     * @param registry the hook registry
     */
    public HookManager(InMemoryHookRegistry registry) {
        this.registry = registry;
    }

    /**
     * Lists all hook registrations.
     *
     * @return list of all registrations
     */
    public List<HookRegistration> list() {
        return registry.list();
    }

    /**
     * Lists all registrations for a hook name.
     *
     * @param hookName the hook name
     * @return list of registrations
     */
    public List<HookRegistration> list(String hookName) {
        return registry.list(hookName);
    }

    /**
     * Resolves contributions for a hook name.
     *
     * @param hookName the hook name
     * @return the resolution result
     */
    public HookResolution resolve(String hookName) {
        return registry.resolve(hookName);
    }
}
