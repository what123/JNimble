package com.jnimble.license.core;

import java.util.List;
import java.util.Optional;

interface PluginLicenseStore {

    Optional<PluginLicenseRecord> find(String pluginId);

    List<PluginLicenseRecord> list();

    void save(PluginLicenseRecord record);

    void delete(String pluginId);

    void event(String pluginId, String action, String status, String detail);
}
