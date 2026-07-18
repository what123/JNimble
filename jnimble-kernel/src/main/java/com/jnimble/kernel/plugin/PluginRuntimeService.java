package com.jnimble.kernel.plugin;

import com.jnimble.kernel.migration.PluginMigrationExecutor;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service interface for managing plugin lifecycle operations.
 *
 * <p>Provides methods for installing, enabling, disabling, uninstalling, and
 * reloading plugins. Manages the plugin state machine and coordinates with
 * other registries for hooks, routes, and assets.</p>
 */
public interface PluginRuntimeService {

    /**
     * Register a plugin descriptor and optional package artifact without starting it.
     *
     * <p>The plugin enters the {@link PluginStatus#INSTALLED} state. Use
     * {@link #enable(String)} to start the plugin.</p>
     *
     * @param descriptor the plugin descriptor to install
     * @throws PluginRuntimeException if the plugin is already installed
     */
    void install(PluginDescriptor descriptor);

    /**
     * Register a plugin descriptor with an optional package artifact path.
     *
     * @param descriptor   the plugin descriptor to install
     * @param artifactPath the path to the plugin package (may be null)
     */
    default void install(PluginDescriptor descriptor, Path artifactPath) {
        install(descriptor);
    }

    /**
     * Replace the descriptor and optional package artifact for an installed, non-enabled plugin.
     *
     * <p>Implementations must not replace an enabled plugin. Reload only restarts the current package.</p>
     *
     * @param descriptor   the new plugin descriptor
     * @param artifactPath the path to the new plugin package (may be null)
     * @throws PluginRuntimeException if the plugin is not installed or is enabled
     */
    default void replace(PluginDescriptor descriptor, Path artifactPath) {
        Objects.requireNonNull(descriptor, "descriptor is required");
        find(descriptor.id())
                .filter(snapshot -> snapshot.status() != PluginStatus.UNINSTALLED)
                .orElseThrow(() -> new PluginRuntimeException("Plugin is not installed: " + descriptor.id()));
        install(descriptor, artifactPath);
    }

    /**
     * Runs database migrations for a plugin.
     *
     * @param descriptor        the plugin descriptor
     * @param migrationExecutor the migration executor to use
     * @throws NullPointerException if migrationExecutor is null
     */
    default void migrate(PluginDescriptor descriptor, PluginMigrationExecutor migrationExecutor) {
        Objects.requireNonNull(migrationExecutor, "migrationExecutor is required").migrate(descriptor);
    }

    /**
     * Enables a plugin, running migrations first.
     *
     * @param descriptor        the plugin descriptor
     * @param migrationExecutor the migration executor to use
     * @throws PluginRuntimeException if the plugin is not installed
     */
    default void enable(PluginDescriptor descriptor, PluginMigrationExecutor migrationExecutor) {
        Objects.requireNonNull(descriptor, "descriptor is required");
        migrate(descriptor, migrationExecutor);
        enable(descriptor.id());
    }

    /**
     * Enables a plugin by ID.
     *
     * <p>The plugin transitions from {@link PluginStatus#INSTALLED} to {@link PluginStatus#ENABLED}.
     * The plugin's boot class is loaded and started.</p>
     *
     * @param pluginId the plugin identifier
     * @throws PluginRuntimeException if the plugin is not installed or cannot be started
     */
    void enable(String pluginId);

    /**
     * Disables a plugin by ID.
     *
     * <p>The plugin transitions from {@link PluginStatus#ENABLED} to {@link PluginStatus#DISABLED}.
     * The plugin's resources are released but the installation is preserved.</p>
     *
     * @param pluginId the plugin identifier
     * @throws PluginRuntimeException if the plugin is not enabled
     */
    void disable(String pluginId);

    /**
     * Uninstalls a plugin by ID.
     *
     * <p>The plugin transitions to {@link PluginStatus#UNINSTALLED} and all
     * associated resources are cleaned up.</p>
     *
     * @param pluginId the plugin identifier
     * @throws PluginRuntimeException if the plugin is not installed
     */
    void uninstall(String pluginId);

    /**
     * Uninstalls a plugin and optionally removes plugin database objects.
     *
     * @param pluginId  the plugin identifier
     * @param cleanData whether to clean plugin data tables and columns
     * @throws PluginRuntimeException if cleanup is requested but unsupported
     */
    default void uninstall(String pluginId, boolean cleanData) {
        if (cleanData) {
            throw new PluginRuntimeException("Plugin data cleanup is not supported by this runtime service.");
        }
        uninstall(pluginId);
    }

    /**
     * Reloads a plugin by disabling and re-enabling it.
     *
     * <p>This is useful for picking up configuration changes or refreshing
     * plugin state without a full uninstall/install cycle.</p>
     *
     * @param pluginId the plugin identifier
     * @throws PluginRuntimeException if the plugin cannot be reloaded
     */
    void reload(String pluginId);

    /**
     * Records a runtime error for a plugin.
     *
     * <p>Used to track plugin errors that don't prevent the plugin from
     * remaining enabled.</p>
     *
     * @param pluginId the plugin identifier
     * @param message  the error message
     */
    default void recordRuntimeError(String pluginId, String message) {
    }

    /**
     * Finds the runtime state of a plugin.
     *
     * @param pluginId the plugin identifier
     * @return an {@link Optional} containing the runtime snapshot if found, empty otherwise
     */
    default Optional<PluginRuntimeSnapshot> find(String pluginId) {
        return Optional.empty();
    }

    /**
     * Lists the runtime state of all installed plugins.
     *
     * @return a list of plugin runtime snapshots
     */
    default List<PluginRuntimeSnapshot> list() {
        return List.of();
    }
}
