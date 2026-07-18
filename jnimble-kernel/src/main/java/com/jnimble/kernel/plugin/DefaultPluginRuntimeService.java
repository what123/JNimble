package com.jnimble.kernel.plugin;

import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginDependency;
import com.jnimble.sdk.plugin.PluginLifecycleEvent;
import com.jnimble.sdk.plugin.PluginLifecyclePhase;
import com.jnimble.sdk.plugin.PluginStatus;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteRegistry;
import com.jnimble.kernel.migration.PluginMigrationException;
import com.jnimble.kernel.migration.PluginMigrationExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link PluginRuntimeService} that manages the full lifecycle
 * of plugins including installation, enabling, disabling, and uninstallation.
 * Handles plugin boot loading, hook/route/asset registration, and migration execution.
 */
public class DefaultPluginRuntimeService implements PluginRuntimeService {

    private final Map<String, PluginRuntimeState> plugins = new ConcurrentHashMap<>();
    private final PluginBootLoader bootLoader;
    private final HookRegistry hookRegistry;
    private final RouteRegistry routeRegistry;
    private final AssetRegistry assetRegistry;
    private final PluginBeanResolver beanResolver;
    private final PluginContributionRegistries contributionRegistries;
    private final PluginMigrationExecutor migrationExecutor;
    private final PluginI18nRegistry i18nRegistry;
    private final PluginActivationGuard activationGuard;
    private final PluginBeanContainerFactory beanContainerFactory;
    private final PluginClassLoaderRegistry classLoaderRegistry;
    private final Clock clock;

    /**
     * Creates a default runtime service with all no-op registries and a {@link ReflectivePluginBootLoader}.
     */
    public DefaultPluginRuntimeService() {
        this(new ReflectivePluginBootLoader(),
                NoopPluginRegistries.hooks(),
                NoopPluginRegistries.routes(),
                NoopPluginRegistries.assets(),
                PluginBeanResolver.empty(),
                null,
                PluginMigrationExecutor.noop(),
                PluginI18nRegistry.noop(),
                Clock.systemUTC());
    }

    /**
     * Creates a runtime service with custom registries, boot loader, and clock.
     *
     * @param bootLoader      the boot loader for plugin classes
     * @param hookRegistry    the hook/extension point registry
     * @param routeRegistry   the HTTP route registry
     * @param assetRegistry   the static asset registry
     * @param beanResolver    the resolver for platform beans
     * @param clock           the clock for timestamps
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            HookRegistry hookRegistry,
            RouteRegistry routeRegistry,
            AssetRegistry assetRegistry,
            PluginBeanResolver beanResolver,
            Clock clock
    ) {
        this(bootLoader, hookRegistry, routeRegistry, assetRegistry, beanResolver, null,
                PluginMigrationExecutor.noop(), PluginI18nRegistry.noop(), clock);
    }

    /**
     * Creates a runtime service with scoped contribution registries.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param contributionRegistries   the scoped contribution registries
     * @param beanResolver             the resolver for platform beans
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            PluginContributionRegistries contributionRegistries,
            PluginBeanResolver beanResolver
    ) {
        this(bootLoader, contributionRegistries, beanResolver, PluginMigrationExecutor.noop());
    }

    /**
     * Creates a runtime service with scoped contribution registries and a migration executor.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param contributionRegistries   the scoped contribution registries
     * @param beanResolver             the resolver for platform beans
     * @param migrationExecutor        the migration executor for plugin database migrations
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            PluginContributionRegistries contributionRegistries,
            PluginBeanResolver beanResolver,
            PluginMigrationExecutor migrationExecutor
    ) {
        this(bootLoader, contributionRegistries, beanResolver, migrationExecutor, PluginI18nRegistry.noop());
    }

    /**
     * Creates a runtime service with scoped contribution registries, migration, and i18n support.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param contributionRegistries   the scoped contribution registries
     * @param beanResolver             the resolver for platform beans
     * @param migrationExecutor        the migration executor for plugin database migrations
     * @param i18nRegistry             the i18n message registry for plugins
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            PluginContributionRegistries contributionRegistries,
            PluginBeanResolver beanResolver,
            PluginMigrationExecutor migrationExecutor,
            PluginI18nRegistry i18nRegistry
    ) {
        this(bootLoader,
                NoopPluginRegistries.hooks(),
                NoopPluginRegistries.routes(),
                NoopPluginRegistries.assets(),
                beanResolver,
                contributionRegistries,
                migrationExecutor,
                i18nRegistry,
                Clock.systemUTC());
    }

    /**
     * Creates a runtime service with separate registries and contribution registries.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param hookRegistry             the hook/extension point registry
     * @param routeRegistry            the HTTP route registry
     * @param assetRegistry            the static asset registry
     * @param beanResolver             the resolver for platform beans
     * @param contributionRegistries   the scoped contribution registries (may be null)
     * @param migrationExecutor        the migration executor for plugin database migrations
     * @param i18nRegistry             the i18n message registry for plugins
     * @param clock                    the clock for timestamps
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            HookRegistry hookRegistry,
            RouteRegistry routeRegistry,
            AssetRegistry assetRegistry,
            PluginBeanResolver beanResolver,
            PluginContributionRegistries contributionRegistries,
            PluginMigrationExecutor migrationExecutor,
            PluginI18nRegistry i18nRegistry,
            Clock clock
    ) {
        this(
                bootLoader,
                hookRegistry,
                routeRegistry,
                assetRegistry,
                beanResolver,
                contributionRegistries,
                migrationExecutor,
                i18nRegistry,
                PluginActivationGuard.allowAll(),
                clock
        );
    }

    /**
     * Creates a runtime service with an activation guard.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param hookRegistry             the hook/extension point registry
     * @param routeRegistry            the HTTP route registry
     * @param assetRegistry            the static asset registry
     * @param beanResolver             the resolver for platform beans
     * @param contributionRegistries   the scoped contribution registries (may be null)
     * @param migrationExecutor        the migration executor for plugin database migrations
     * @param i18nRegistry             the i18n message registry for plugins
     * @param activationGuard          the guard for plugin activation decisions
     * @param clock                    the clock for timestamps
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            HookRegistry hookRegistry,
            RouteRegistry routeRegistry,
            AssetRegistry assetRegistry,
            PluginBeanResolver beanResolver,
            PluginContributionRegistries contributionRegistries,
            PluginMigrationExecutor migrationExecutor,
            PluginI18nRegistry i18nRegistry,
            PluginActivationGuard activationGuard,
            Clock clock
    ) {
        this(
                bootLoader,
                hookRegistry,
                routeRegistry,
                assetRegistry,
                beanResolver,
                contributionRegistries,
                migrationExecutor,
                i18nRegistry,
                activationGuard,
                PluginBeanContainerFactory.resolverBacked(),
                PluginClassLoaderRegistry.inMemory(),
                clock
        );
    }

    /**
     * Creates a fully configured runtime service with all optional components.
     *
     * @param bootLoader               the boot loader for plugin classes
     * @param hookRegistry             the hook/extension point registry
     * @param routeRegistry            the HTTP route registry
     * @param assetRegistry            the static asset registry
     * @param beanResolver             the resolver for platform beans
     * @param contributionRegistries   the scoped contribution registries (may be null)
     * @param migrationExecutor        the migration executor for plugin database migrations
     * @param i18nRegistry             the i18n message registry for plugins
     * @param activationGuard          the guard for plugin activation decisions
     * @param beanContainerFactory     the factory for plugin bean containers
     * @param classLoaderRegistry      the registry for plugin class loaders
     * @param clock                    the clock for timestamps
     */
    public DefaultPluginRuntimeService(
            PluginBootLoader bootLoader,
            HookRegistry hookRegistry,
            RouteRegistry routeRegistry,
            AssetRegistry assetRegistry,
            PluginBeanResolver beanResolver,
            PluginContributionRegistries contributionRegistries,
            PluginMigrationExecutor migrationExecutor,
            PluginI18nRegistry i18nRegistry,
            PluginActivationGuard activationGuard,
            PluginBeanContainerFactory beanContainerFactory,
            PluginClassLoaderRegistry classLoaderRegistry,
            Clock clock
    ) {
        this.bootLoader = Objects.requireNonNull(bootLoader, "bootLoader");
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry");
        this.routeRegistry = Objects.requireNonNull(routeRegistry, "routeRegistry");
        this.assetRegistry = Objects.requireNonNull(assetRegistry, "assetRegistry");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver");
        this.contributionRegistries = contributionRegistries;
        this.migrationExecutor = Objects.requireNonNull(migrationExecutor, "migrationExecutor");
        this.i18nRegistry = Objects.requireNonNull(i18nRegistry, "i18nRegistry");
        this.activationGuard = Objects.requireNonNull(activationGuard, "activationGuard");
        this.beanContainerFactory = Objects.requireNonNull(beanContainerFactory, "beanContainerFactory");
        this.classLoaderRegistry = Objects.requireNonNull(classLoaderRegistry, "classLoaderRegistry");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void install(PluginDescriptor descriptor) {
        install(descriptor, null);
    }

    @Override
    public void install(PluginDescriptor descriptor, Path artifactPath) {
        Objects.requireNonNull(descriptor, "descriptor");
        String pluginId = requirePluginId(descriptor);
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState existing = plugins.get(pluginId);
            if (existing != null && existing.status() == PluginStatus.ENABLED) {
                throw new PluginRuntimeException("Cannot install enabled plugin " + pluginId);
            }
            if (existing != null) {
                requireNoEnabledDependents(pluginId, "install");
            }
            installState(pluginId, existing, descriptor, artifactPath, "install");
        }
    }

    @Override
    public void replace(PluginDescriptor descriptor, Path artifactPath) {
        Objects.requireNonNull(descriptor, "descriptor");
        String pluginId = requirePluginId(descriptor);
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState existing = requireInstalled(pluginId);
            if (existing.status() == PluginStatus.ENABLED) {
                throw new PluginRuntimeException("Cannot replace enabled plugin " + pluginId);
            }
            if (existing.status() == PluginStatus.UNINSTALLED) {
                throw new PluginRuntimeException("Cannot replace uninstalled plugin " + pluginId);
            }
            requireNoEnabledDependents(pluginId, "replace");
            installState(pluginId, existing, descriptor, artifactPath, "replace");
        }
    }

    @Override
    public void enable(String pluginId) {
        enable(pluginId, migrationExecutor);
    }

    @Override
    public void enable(PluginDescriptor descriptor, PluginMigrationExecutor migrationExecutor) {
        Objects.requireNonNull(descriptor, "descriptor is required");
        enable(descriptor.id(), Objects.requireNonNull(migrationExecutor, "migrationExecutor is required"));
    }

    private void enable(String pluginId, PluginMigrationExecutor migrationExecutor) {
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState state = requireInstalled(pluginId);
            if (state.status() == PluginStatus.ENABLED) {
                return;
            }
            if (state.status() == PluginStatus.UNINSTALLED) {
                throw new PluginRuntimeException("Cannot enable uninstalled plugin " + pluginId);
            }

            state.clearRuntime();
            state.setLastError(null);
            state.addEvent(event(pluginId, PluginLifecyclePhase.ENABLING, "enable"));
            LoadedPluginBoot loaded = null;
            PluginBeanContainer beanContainer = null;
            TrackingPluginContext context = null;
            try {
                activationGuard.requireCanActivate(state.descriptor());
                Map<String, PluginRuntimeState> dependencies = resolveDependencies(state);
                if (contributionRegistries != null) {
                    contributionRegistries.enable(pluginId);
                }
                List<ClassLoader> dependencyClassLoaders = dependencies.values().stream()
                        .map(PluginRuntimeState::loadedBoot)
                        .filter(Objects::nonNull)
                        .map(LoadedPluginBoot::classLoader)
                        .toList();
                loaded = dependencyClassLoaders.isEmpty()
                        ? bootLoader.load(state.descriptor(), state.artifactPath())
                        : bootLoader.load(state.descriptor(), state.artifactPath(), dependencyClassLoaders);
                classLoaderRegistry.register(pluginId, loaded.classLoader());
                migratePlugin(state, loaded.classLoader(), migrationExecutor);
                beanContainer = beanContainerFactory.create(
                        state.descriptor(),
                        loaded.classLoader(),
                        dependencyContainers(dependencies),
                        beanResolver);
                if (beanContainer == null) {
                    throw new PluginRuntimeException("Plugin bean container factory returned null for " + pluginId);
                }
                state.setLoadedBoot(loaded);
                state.setBeanContainer(beanContainer);
                context = new TrackingPluginContext(
                        state.descriptor(),
                        hooksFor(pluginId),
                        routesFor(pluginId),
                        assetsFor(pluginId),
                        beanContainer::resolve,
                        state::addHandle);
                state.setContext(context);
                loaded.boot().boot(context);
                beanContainer.activate();
                state.setStatus(PluginStatus.ENABLED);
                state.setLastStartedAt(now());
                state.addEvent(event(pluginId, PluginLifecyclePhase.ENABLED, "enable"));
            } catch (PluginMigrationException ex) {
                rollbackEnableFailure(state, loaded, beanContainer, ex, PluginStatus.MIGRATION_FAILED);
                throw ex;
            } catch (RuntimeException ex) {
                rollbackEnableFailure(state, loaded, beanContainer, ex);
                throw ex;
            }
        }
    }

    @Override
    public void disable(String pluginId) {
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState state = requireInstalled(pluginId);
            if (state.status() != PluginStatus.ENABLED) {
                return;
            }
            requireNoEnabledDependents(pluginId, "disable");

            state.addEvent(event(pluginId, PluginLifecyclePhase.DISABLING, "disable"));
            try {
                if (state.beanContainer() != null) {
                    state.beanContainer().deactivate();
                }
            } catch (RuntimeException ex) {
                state.setLastError(errorMessage(ex));
                state.addEvent(event(pluginId, PluginLifecyclePhase.FAILED, errorMessage(ex)));
                throw new PluginRuntimeException(
                        "Failed to drain plugin " + pluginId + "; plugin remains enabled",
                        ex);
            }
            if (contributionRegistries != null) {
                contributionRegistries.disable(pluginId);
            }
            RuntimeException failure = null;
            try {
                if (state.loadedBoot() != null && state.context() != null) {
                    state.loadedBoot().boot().stop(state.context());
                }
            } catch (RuntimeException ex) {
                failure = combine(failure, ex);
            }

            RuntimeException unregisterFailure = unregisterHandles(state.handles());
            if (failure == null) {
                failure = unregisterFailure;
            } else if (unregisterFailure != null) {
                failure.addSuppressed(unregisterFailure);
            }

            RuntimeException containerCloseFailure = closeBeanContainer(state.beanContainer());
            if (failure == null) {
                failure = containerCloseFailure;
            } else if (containerCloseFailure != null) {
                failure.addSuppressed(containerCloseFailure);
            }

            if (state.loadedBoot() != null) {
                classLoaderRegistry.unregister(pluginId, state.loadedBoot().classLoader());
            }
            RuntimeException closeFailure = closeLoadedBoot(state.loadedBoot());
            failure = combine(failure, closeFailure);

            state.clearRuntime();
            state.setLastStoppedAt(now());
            if (failure != null) {
                markFailed(state, failure);
                throw new PluginRuntimeException("Failed to disable plugin " + pluginId, failure);
            }

            state.setStatus(PluginStatus.DISABLED);
            state.setLastError(null);
            state.addEvent(event(pluginId, PluginLifecyclePhase.DISABLED, "disable"));
        }
    }

    @Override
    public void uninstall(String pluginId) {
        uninstall(pluginId, false);
    }

    @Override
    public void uninstall(String pluginId, boolean cleanData) {
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState state = requireInstalled(pluginId);
            requireNoEnabledDependents(pluginId, "uninstall");
            if (state.status() == PluginStatus.ENABLED) {
                disable(pluginId);
                state = requireInstalled(pluginId);
            }
            state.addEvent(event(pluginId, PluginLifecyclePhase.UNINSTALLING, "uninstall"));
            try {
                if (cleanData) {
                    migrationExecutor.clean(state.descriptor(), state.artifactPath());
                }
            } catch (RuntimeException ex) {
                markFailed(state, ex);
                throw ex;
            }
            RuntimeException i18nFailure = unregisterInstalledI18n(state);
            if (i18nFailure != null) {
                markFailed(state, i18nFailure);
                throw new PluginRuntimeException("Failed to unregister plugin messages for " + pluginId,
                        i18nFailure);
            }
            state.clearRuntime();
            state.setStatus(PluginStatus.UNINSTALLED);
            state.setLastError(null);
            state.addEvent(event(pluginId, PluginLifecyclePhase.UNINSTALLED, "uninstall"));
        }
    }

    @Override
    public void reload(String pluginId) {
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState state = requireInstalled(pluginId);
            if (state.status() != PluginStatus.ENABLED) {
                throw new PluginRuntimeException("Cannot reload plugin unless it is enabled: " + pluginId);
            }
            state.addEvent(event(pluginId, PluginLifecyclePhase.RELOADING, "reload"));
            disable(pluginId);
            enable(pluginId);
        }
    }

    @Override
    public void recordRuntimeError(String pluginId, String message) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        synchronized (lockFor(pluginId)) {
            PluginRuntimeState state = plugins.get(pluginId);
            if (state != null) {
                state.setLastError(message);
            }
        }
    }

    @Override
    public Optional<PluginRuntimeSnapshot> find(String pluginId) {
        PluginRuntimeState state = plugins.get(pluginId);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    @Override
    public List<PluginRuntimeSnapshot> list() {
        return plugins.values().stream()
                .map(PluginRuntimeState::snapshot)
                .sorted(Comparator.comparing(PluginRuntimeSnapshot::pluginId))
                .toList();
    }

    private void rollbackEnableFailure(
            PluginRuntimeState state,
            LoadedPluginBoot loaded,
            PluginBeanContainer beanContainer,
            Exception failure
    ) {
        rollbackEnableFailure(state, loaded, beanContainer, failure, PluginStatus.FAILED);
    }

    private void rollbackEnableFailure(
            PluginRuntimeState state,
            LoadedPluginBoot loaded,
            PluginBeanContainer beanContainer,
            Exception failure,
            PluginStatus failureStatus
    ) {
        try {
            if (beanContainer != null) {
                beanContainer.deactivate();
            }
        } catch (RuntimeException deactivateFailure) {
            failure.addSuppressed(deactivateFailure);
        }
        RuntimeException unregisterFailure = unregisterHandles(state.handles());
        RuntimeException containerCloseFailure = closeBeanContainer(beanContainer);
        if (loaded != null) {
            classLoaderRegistry.unregister(state.pluginId(), loaded.classLoader());
        }
        RuntimeException closeFailure = closeLoadedBoot(loaded);
        if (unregisterFailure != null) {
            failure.addSuppressed(unregisterFailure);
        }
        if (closeFailure != null) {
            failure.addSuppressed(closeFailure);
        }
        if (containerCloseFailure != null) {
            failure.addSuppressed(containerCloseFailure);
        }
        if (contributionRegistries != null) {
            try {
                contributionRegistries.disable(state.pluginId());
            } catch (RuntimeException contributionFailure) {
                failure.addSuppressed(contributionFailure);
            }
        }
        state.clearRuntime();
        markFailed(state, failure, failureStatus);
    }

    private Map<String, PluginRuntimeState> resolveDependencies(PluginRuntimeState state) {
        List<PluginDependency> declarations = state.descriptor().dependencies();
        if (declarations == null || declarations.isEmpty()) {
            return Map.of();
        }
        Map<String, PluginRuntimeState> dependencies = new LinkedHashMap<>();
        for (PluginDependency declaration : declarations) {
            if (declaration == null) {
                continue;
            }
            PluginRuntimeState dependency = plugins.get(declaration.pluginId());
            if (dependency == null || dependency.status() == PluginStatus.UNINSTALLED) {
                requireDependency(state, declaration, "is not installed");
                continue;
            }
            if (!PluginVersionMatcher.matches(dependency.descriptor().version(), declaration.version())) {
                requireDependency(
                        state,
                        declaration,
                        "has incompatible version " + dependency.descriptor().version());
                continue;
            }
            if (dependency.status() != PluginStatus.ENABLED
                    || dependency.loadedBoot() == null
                    || dependency.beanContainer() == null) {
                requireDependency(state, declaration, "is not enabled");
                continue;
            }
            dependencies.put(declaration.pluginId(), dependency);
        }
        return Map.copyOf(dependencies);
    }

    private void requireDependency(
            PluginRuntimeState owner,
            PluginDependency declaration,
            String problem
    ) {
        if (declaration.required()) {
            throw new PluginRuntimeException(
                    "Cannot enable plugin " + owner.pluginId()
                            + ": required dependency " + declaration.pluginId() + " " + problem);
        }
    }

    private Map<String, PluginBeanContainer> dependencyContainers(
            Map<String, PluginRuntimeState> dependencies
    ) {
        Map<String, PluginBeanContainer> containers = new LinkedHashMap<>();
        dependencies.forEach((pluginId, state) -> containers.put(pluginId, state.beanContainer()));
        return Map.copyOf(containers);
    }

    private void requireNoEnabledDependents(String pluginId, String operation) {
        List<String> dependents = plugins.values().stream()
                .filter(state -> state.status() == PluginStatus.ENABLED)
                .filter(state -> state.descriptor().dependencies() != null)
                .filter(state -> state.descriptor().dependencies().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(dependency -> pluginId.equals(dependency.pluginId())))
                .map(PluginRuntimeState::pluginId)
                .sorted()
                .toList();
        if (!dependents.isEmpty()) {
            throw new PluginRuntimeException(
                    "Cannot " + operation + " plugin " + pluginId
                            + "; enabled dependents: " + String.join(", ", dependents));
        }
    }

    private void migratePlugin(
            PluginRuntimeState state,
            ClassLoader classLoader,
            PluginMigrationExecutor migrationExecutor
    ) {
        try {
            migrationExecutor.migrate(state.descriptor(), classLoader);
        } catch (PluginMigrationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PluginMigrationException("Plugin migration failed for plugin " + state.pluginId(), ex);
        }
    }

    private void installState(
            String pluginId,
            PluginRuntimeState existing,
            PluginDescriptor descriptor,
            Path artifactPath,
            String reason
    ) {
        if (existing != null) {
            RuntimeException unregisterFailure = unregisterInstalledI18n(existing);
            if (unregisterFailure != null) {
                throw new PluginRuntimeException("Failed to replace plugin messages for " + pluginId,
                        unregisterFailure);
            }
        }
        PluginRuntimeState state = existing == null
                ? new PluginRuntimeState(descriptor).replaceDescriptor(descriptor, artifactPath)
                : existing.replaceDescriptor(descriptor, artifactPath);
        state.setLastError(null);
        state.addEvent(event(pluginId, PluginLifecyclePhase.INSTALLING, reason));
        try {
            registerInstalledI18n(state);
        } catch (RuntimeException ex) {
            markFailed(state, ex);
            plugins.put(pluginId, state);
            throw ex;
        }
        state.setStatus(PluginStatus.INSTALLED);
        state.setInstalledAt(now());
        state.addEvent(event(pluginId, PluginLifecyclePhase.INSTALLED, reason));
        plugins.put(pluginId, state);
    }

    private void registerInstalledI18n(PluginRuntimeState state) {
        if (state.descriptor().i18n() == null
                || state.descriptor().i18n().basename() == null
                || state.descriptor().i18n().basename().isBlank()) {
            return;
        }
        AutoCloseable resource = null;
        ClassLoader classLoader = getClass().getClassLoader();
        if (state.artifactPath() != null) {
            try {
                URL artifactUrl = state.artifactPath().toUri().toURL();
                URLClassLoader messageClassLoader = new URLClassLoader(
                        new URL[]{artifactUrl}, getClass().getClassLoader());
                classLoader = messageClassLoader;
                resource = messageClassLoader;
            } catch (MalformedURLException ex) {
                throw new PluginRuntimeException(
                        "Invalid plugin artifact path for i18n: " + state.artifactPath(), ex);
            }
        }

        try {
            RegistrationHandle handle = i18nRegistry.register(
                    state.pluginId(),
                    state.descriptor().i18n().basename().trim(),
                    classLoader);
            state.setI18nRegistration(handle, resource);
        } catch (RuntimeException ex) {
            RuntimeException closeFailure = closeResource(resource);
            if (closeFailure != null) {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }
    }

    private RuntimeException unregisterInstalledI18n(PluginRuntimeState state) {
        RuntimeException failure = null;
        try {
            if (state.i18nHandle() != null) {
                state.i18nHandle().unregister();
            }
        } catch (RuntimeException ex) {
            failure = ex;
        }
        failure = combine(failure, closeResource(state.i18nResource()));
        state.clearI18nRegistration();
        return failure;
    }

    private RuntimeException closeResource(AutoCloseable resource) {
        if (resource == null) {
            return null;
        }
        try {
            resource.close();
            return null;
        } catch (RuntimeException ex) {
            return ex;
        } catch (Exception ex) {
            return new PluginRuntimeException("Failed to close plugin i18n resources", ex);
        }
    }

    private void markFailed(PluginRuntimeState state, Exception failure) {
        markFailed(state, failure, PluginStatus.FAILED);
    }

    private void markFailed(PluginRuntimeState state, Exception failure, PluginStatus failureStatus) {
        state.setStatus(failureStatus);
        state.setLastError(errorMessage(failure));
        state.addEvent(event(state.pluginId(), PluginLifecyclePhase.FAILED, errorMessage(failure)));
    }

    private RuntimeException unregisterHandles(List<RegistrationHandle> handles) {
        List<RegistrationHandle> snapshot = new ArrayList<>(handles);
        RuntimeException failure = null;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            try {
                snapshot.get(i).unregister();
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new PluginRuntimeException("Failed to unregister plugin contribution", ex);
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        handles.clear();
        return failure;
    }

    private RuntimeException closeLoadedBoot(LoadedPluginBoot loaded) {
        if (loaded == null) {
            return null;
        }
        try {
            loaded.close();
            return null;
        } catch (RuntimeException ex) {
            return ex;
        } catch (Exception ex) {
            return new PluginRuntimeException("Failed to close plugin boot resources", ex);
        }
    }

    private RuntimeException closeBeanContainer(PluginBeanContainer beanContainer) {
        if (beanContainer == null) {
            return null;
        }
        try {
            beanContainer.close();
            return null;
        } catch (RuntimeException ex) {
            return ex;
        } catch (Exception ex) {
            return new PluginRuntimeException("Failed to close plugin bean container", ex);
        }
    }

    private RuntimeException combine(RuntimeException primary, RuntimeException additional) {
        if (primary == null) {
            return additional;
        }
        if (additional != null) {
            primary.addSuppressed(additional);
        }
        return primary;
    }

    private PluginRuntimeState requireInstalled(String pluginId) {
        PluginRuntimeState state = plugins.get(pluginId);
        if (state == null) {
            throw new PluginRuntimeException("Plugin is not installed: " + pluginId);
        }
        return state;
    }

    private Object lockFor(String pluginId) {
        return PluginOperationLocks.lockFor(requirePluginId(pluginId));
    }

    private String requirePluginId(PluginDescriptor descriptor) {
        return requirePluginId(descriptor.id());
    }

    private String requirePluginId(String pluginId) {
        try {
            return PluginIds.requireValid(pluginId, "Plugin id");
        } catch (IllegalArgumentException ex) {
            throw new PluginRuntimeException(ex.getMessage(), ex);
        }
    }

    private PluginLifecycleEvent event(String pluginId, PluginLifecyclePhase phase, String reason) {
        return new PluginLifecycleEvent(pluginId, phase, now(), reason);
    }

    private Instant now() {
        return clock.instant();
    }

    private String errorMessage(Exception failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getName() : message;
    }

    private HookRegistry hooksFor(String pluginId) {
        return contributionRegistries == null ? hookRegistry : contributionRegistries.hooks(pluginId);
    }

    private RouteRegistry routesFor(String pluginId) {
        return contributionRegistries == null ? routeRegistry : contributionRegistries.routes(pluginId);
    }

    private AssetRegistry assetsFor(String pluginId) {
        return contributionRegistries == null ? assetRegistry : contributionRegistries.assets(pluginId);
    }
}
