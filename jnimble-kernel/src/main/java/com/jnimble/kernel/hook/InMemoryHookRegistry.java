package com.jnimble.kernel.hook;

import com.jnimble.kernel.plugin.PluginIds;
import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.hook.RegistrationType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of the hook registry.
 *
 * <p>Stores hook contributions in a concurrent map. Contributions are ordered
 * by priority, plugin ID, and registration sequence.</p>
 */
@Component
public class InMemoryHookRegistry implements HookRegistry {

    private static final String UNKNOWN_PLUGIN_ID = "__unknown__";

    private static final Comparator<HookRegistration> REGISTRATION_ORDER =
            Comparator.comparingInt((HookRegistration registration) -> registration.contribution().order())
                    .thenComparing(HookRegistration::pluginId)
                    .thenComparingLong(HookRegistration::sequence);

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentMap<String, HookRegistration> registrations = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public RegistrationHandle register(String hookName, HookViewContribution contribution) {
        return register(hookName, UNKNOWN_PLUGIN_ID, HookMode.APPEND, contribution);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegistrationHandle register(String hookName, HookMode mode, HookViewContribution contribution) {
        return register(hookName, UNKNOWN_PLUGIN_ID, mode, contribution);
    }

    /**
     * Registers a hook contribution with plugin context.
     *
     * @param hookName     the hook name
     * @param pluginId     the plugin identifier
     * @param mode         the registration mode
     * @param contribution the contribution
     * @return a registration handle
     */
    public RegistrationHandle register(
            String hookName,
            String pluginId,
            HookMode mode,
            HookViewContribution contribution
    ) {
        HookRegistration registration = new HookRegistration(
                UUID.randomUUID().toString(),
                requireText(hookName, "hookName"),
                normalizePluginId(pluginId),
                Objects.requireNonNullElse(mode, HookMode.APPEND),
                Objects.requireNonNull(contribution, "contribution"),
                sequence.incrementAndGet()
        );
        registrations.put(registration.registrationId(), registration);
        return new HookRegistrationHandle(registration.registrationId());
    }

    /**
     * Returns all contributions ordered by priority.
     *
     * @return ordered list of contributions
     */
    public List<HookViewContribution> contributions() {
        return list().stream()
                .map(HookRegistration::contribution)
                .toList();
    }

    /**
     * Returns all registrations.
     *
     * @return list of all registrations
     */
    public List<HookRegistration> list() {
        return registrations.values().stream()
                .sorted(REGISTRATION_ORDER)
                .toList();
    }

    /**
     * Returns all registrations for a hook name.
     *
     * @param hookName the hook name
     * @return list of registrations
     */
    public List<HookRegistration> list(String hookName) {
        String normalizedHookName = requireText(hookName, "hookName");
        return registrations.values().stream()
                .filter(registration -> registration.hookName().equals(normalizedHookName))
                .sorted(REGISTRATION_ORDER)
                .toList();
    }

    /**
     * Resolves contributions for a hook name.
     *
     * @param hookName the hook name
     * @return the resolution result
     */
    public HookResolution resolve(String hookName) {
        List<HookRegistration> sorted = list(hookName);
        Optional<HookRegistration> override = sorted.stream()
                .filter(registration -> registration.mode() == HookMode.REPLACE
                        || registration.mode() == HookMode.REMOVE)
                .findFirst();
        Optional<HookRegistration> replacement = override.filter(registration -> registration.mode() == HookMode.REPLACE);
        Optional<HookRegistration> removal = override.filter(registration -> registration.mode() == HookMode.REMOVE);

        List<HookRegistration> prepends = sorted.stream()
                .filter(registration -> registration.mode() == HookMode.PREPEND)
                .toList();
        List<HookRegistration> appends = sorted.stream()
                .filter(registration -> registration.mode() == HookMode.APPEND)
                .toList();
        List<HookRegistration> suppressed = sorted.stream()
                .filter(registration -> (registration.mode() == HookMode.REPLACE
                        || registration.mode() == HookMode.REMOVE)
                        && override.map(selected -> !selected.registrationId().equals(registration.registrationId()))
                                .orElse(false))
                .toList();

        return new HookResolution(hookName, prepends, appends, replacement, removal, suppressed);
    }

    private boolean unregister(String registrationId) {
        return registrations.remove(registrationId) != null;
    }

    private static String normalizePluginId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return UNKNOWN_PLUGIN_ID;
        }
        return PluginIds.requireValid(pluginId);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * Represents a hook registration.
     */
    public record HookRegistration(
            String registrationId,
            String hookName,
            String pluginId,
            HookMode mode,
            HookViewContribution contribution,
            long sequence
    ) {
    }

    /**
     * Result of resolving a hook.
     */
    public record HookResolution(
            String hookName,
            List<HookRegistration> prepends,
            List<HookRegistration> appends,
            Optional<HookRegistration> replacement,
            Optional<HookRegistration> removal,
            List<HookRegistration> suppressedOverrides
    ) {

        public HookResolution {
            prepends = List.copyOf(prepends);
            appends = List.copyOf(appends);
            replacement = replacement == null ? Optional.empty() : replacement;
            removal = removal == null ? Optional.empty() : removal;
            suppressedOverrides = List.copyOf(suppressedOverrides);
        }

        public Optional<HookRegistration> override() {
            return replacement.isPresent() ? replacement : removal;
        }
    }

    /**
     * Handle for removing a hook registration.
     */
    private final class HookRegistrationHandle implements RegistrationHandle {

        private final String registrationId;

        private HookRegistrationHandle(String registrationId) {
            this.registrationId = registrationId;
        }

        @Override
        public void unregister() {
            InMemoryHookRegistry.this.unregister(registrationId);
        }

        @Override
        public Optional<String> registrationId() {
            return Optional.of(registrationId);
        }

        @Override
        public RegistrationType type() {
            return RegistrationType.HOOK;
        }
    }
}
