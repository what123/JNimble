package com.jnimble.platform.plugin;

import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;

import java.util.Collection;
import java.util.Optional;

/**
 * Service interface for persisting plugin runtime state.
 *
 * <p>Stores plugin state information including status, installation time,
 * and other metadata. Implementations may store data in memory or
 * persist to a database via JDBC.</p>
 */
public interface PluginStateStore {

    /**
     * Saves the plugin runtime state.
     *
     * <p>If a state record already exists for the plugin, it is updated.
     * Otherwise, a new record is created.</p>
     *
     * @param snapshot the plugin runtime snapshot to save
     */
    void save(PluginRuntimeSnapshot snapshot);

    /**
     * Finds the state record for a plugin.
     *
     * @param pluginId the plugin identifier
     * @return an {@link Optional} containing the state record if found, empty otherwise
     */
    Optional<PluginStateRecord> find(String pluginId);

    /**
     * Lists all plugin state records.
     *
     * @return a collection of all plugin state records
     */
    Collection<PluginStateRecord> list();
}
