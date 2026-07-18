package com.jnimble.sdk.hook;

/**
 * Registry used by plugins to contribute renderable fragments to named hooks.
 */
public interface HookRegistry {

    /**
     * Registers a view contribution with the default append mode.
     *
     * @param hookName hook point name exposed by the platform
     * @param contribution renderable plugin fragment
     * @return handle used by the runtime to remove the contribution
     */
    RegistrationHandle register(String hookName, HookViewContribution contribution);

    /**
     * Registers a view contribution with an explicit merge mode.
     *
     * @param hookName hook point name exposed by the platform
     * @param mode contribution merge mode
     * @param contribution renderable plugin fragment
     * @return handle used by the runtime to remove the contribution
     */
    default RegistrationHandle register(String hookName, HookMode mode, HookViewContribution contribution) {
        return register(hookName, contribution);
    }
}
