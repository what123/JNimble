package com.jnimble.platform.permission;

import com.jnimble.kernel.migration.PluginMigrationExecutor;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.plugin.PluginStateStore;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * A decorator around {@link PluginRuntimeService} that synchronizes plugin permissions
 * during lifecycle operations. Automatically registers and manages permissions when
 * plugins are installed, enabled, disabled, or uninstalled.
 */
@Service
@Primary
public class PermissionAwarePluginRuntimeService implements PluginRuntimeService {

    private final PluginRuntimeService delegate;
    private final PermissionService permissionService;
    private final PluginStateStore pluginStateStore;
    private final SuperAdminPermissionService superAdminPermissionService;

    public PermissionAwarePluginRuntimeService(
            @Qualifier("pluginRuntimeService") PluginRuntimeService delegate,
            PermissionService permissionService,
            PluginStateStore pluginStateStore,
            SuperAdminPermissionService superAdminPermissionService
    ) {
        this.delegate = delegate;
        this.permissionService = permissionService;
        this.pluginStateStore = pluginStateStore;
        this.superAdminPermissionService = superAdminPermissionService;
    }

    @Override
    public void install(PluginDescriptor descriptor) {
        requirePluginNotEnabled(descriptor.id());
        synchronizePermissions(descriptor, false);
        delegate.install(descriptor);
        saveSnapshot(descriptor.id());
    }

    @Override
    public void install(PluginDescriptor descriptor, Path artifactPath) {
        requirePluginNotEnabled(descriptor.id());
        synchronizePermissions(descriptor, false);
        delegate.install(descriptor, artifactPath);
        saveSnapshot(descriptor.id());
    }

    @Override
    public void replace(PluginDescriptor descriptor, Path artifactPath) {
        requirePluginNotEnabled(descriptor.id());
        synchronizePermissions(descriptor, false);
        delegate.replace(descriptor, artifactPath);
        saveSnapshot(descriptor.id());
    }

    @Override
    public void migrate(PluginDescriptor descriptor, PluginMigrationExecutor migrationExecutor) {
        delegate.migrate(descriptor, migrationExecutor);
    }

    @Override
    public void enable(PluginDescriptor descriptor, PluginMigrationExecutor migrationExecutor) {
        try {
            delegate.enable(descriptor, migrationExecutor);
            synchronizeEnabledPermissions(descriptor);
        } finally {
            saveSnapshot(descriptor.id());
        }
    }

    @Override
    public void enable(String pluginId) {
        try {
            delegate.enable(pluginId);
            delegate.find(pluginId)
                    .map(PluginRuntimeSnapshot::descriptor)
                    .ifPresent(this::synchronizeEnabledPermissions);
        } finally {
            saveSnapshot(pluginId);
        }
    }

    @Override
    public void disable(String pluginId) {
        try {
            delegate.disable(pluginId);
        } finally {
            permissionService.markPluginPermissionsUnavailable(pluginId);
            saveSnapshot(pluginId);
        }
    }

    @Override
    public void uninstall(String pluginId) {
        try {
            delegate.uninstall(pluginId);
        } finally {
            permissionService.markPluginPermissionsUnavailable(pluginId);
            saveSnapshot(pluginId);
        }
    }

    @Override
    public void uninstall(String pluginId, boolean cleanData) {
        try {
            delegate.uninstall(pluginId, cleanData);
        } finally {
            permissionService.markPluginPermissionsUnavailable(pluginId);
            saveSnapshot(pluginId);
        }
    }

    @Override
    public void reload(String pluginId) {
        try {
            delegate.reload(pluginId);
            synchronizeSnapshotPermissions(pluginId, true);
        } catch (RuntimeException ex) {
            permissionService.markPluginPermissionsUnavailable(pluginId);
            throw ex;
        } finally {
            saveSnapshot(pluginId);
        }
    }

    @Override
    public void recordRuntimeError(String pluginId, String message) {
        try {
            delegate.recordRuntimeError(pluginId, message);
        } finally {
            saveSnapshot(pluginId);
        }
    }

    @Override
    public Optional<PluginRuntimeSnapshot> find(String pluginId) {
        return delegate.find(pluginId);
    }

    @Override
    public List<PluginRuntimeSnapshot> list() {
        return delegate.list();
    }

    private void synchronizeSnapshotPermissions(String pluginId, boolean available) {
        delegate.find(pluginId)
                .map(PluginRuntimeSnapshot::descriptor)
                .ifPresent(descriptor -> synchronizePermissions(descriptor, available));
    }

    private void synchronizeEnabledPermissions(PluginDescriptor descriptor) {
        try {
            synchronizePermissions(descriptor, true);
        } catch (RuntimeException ex) {
            try {
                delegate.disable(descriptor.id());
            } catch (RuntimeException disableFailure) {
                ex.addSuppressed(disableFailure);
            } finally {
                permissionService.markPluginPermissionsUnavailable(descriptor.id());
            }
            throw ex;
        }
    }

    private void synchronizePermissions(PluginDescriptor descriptor, boolean available) {
        permissionService.registerPluginPermissions(descriptor.id(), permissionDefinitions(descriptor));
        if (!available) {
            permissionService.markPluginPermissionsUnavailable(descriptor.id());
            return;
        }
        superAdminPermissionService.grantAllAvailablePermissions();
    }

    private Collection<PermissionDefinition> permissionDefinitions(PluginDescriptor descriptor) {
        if (descriptor.permissions() == null) {
            return List.of();
        }
        return descriptor.permissions().stream()
                .map(PermissionDefinition::from)
                .toList();
    }

    private void saveSnapshot(String pluginId) {
        delegate.find(pluginId).ifPresent(pluginStateStore::save);
    }

    private void requirePluginNotEnabled(String pluginId) {
        delegate.find(pluginId)
                .filter(snapshot -> snapshot.status() == PluginStatus.ENABLED)
                .ifPresent(snapshot -> {
                    throw new IllegalStateException("Cannot install enabled plugin " + pluginId);
                });
    }
}
