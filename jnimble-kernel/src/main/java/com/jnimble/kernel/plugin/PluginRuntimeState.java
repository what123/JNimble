package com.jnimble.kernel.plugin;

import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginLifecycleEvent;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state holder for a plugin's runtime information.
 *
 * <p>Tracks the plugin's descriptor, status, registered handles, loaded boot class,
 * and lifecycle events. Used internally by {@link DefaultPluginRuntimeService}.</p>
 */
final class PluginRuntimeState {

    private PluginDescriptor descriptor;
    private Path artifactPath;
    private PluginSource source = PluginSource.CLASSPATH;
    private PluginStatus status = PluginStatus.DISCOVERED;
    private final List<RegistrationHandle> handles = new ArrayList<>();
    private final List<PluginLifecycleEvent> events = new ArrayList<>();
    private RegistrationHandle i18nHandle;
    private AutoCloseable i18nResource;
    private LoadedPluginBoot loadedBoot;
    private PluginBeanContainer beanContainer;
    private PluginContext context;
    private String lastError;
    private Instant installedAt;
    private Instant lastStartedAt;
    private Instant lastStoppedAt;

    /**
     * Creates a new plugin runtime state.
     *
     * @param descriptor the plugin descriptor
     */
    PluginRuntimeState(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Replaces the descriptor and artifact path.
     *
     * @param descriptor   the new descriptor
     * @param artifactPath the new artifact path
     * @return this state instance
     */
    PluginRuntimeState replaceDescriptor(PluginDescriptor descriptor, Path artifactPath) {
        this.descriptor = descriptor;
        this.artifactPath = artifactPath;
        this.source = artifactPath == null ? PluginSource.CLASSPATH : PluginSource.JAR;
        return this;
    }

    /**
     * Returns the plugin identifier.
     *
     * @return the plugin ID
     */
    String pluginId() {
        return descriptor.id();
    }

    /**
     * Returns the plugin descriptor.
     *
     * @return the descriptor
     */
    PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * Returns the artifact path.
     *
     * @return the path, or null for classpath plugins
     */
    Path artifactPath() {
        return artifactPath;
    }

    /**
     * Returns the current plugin status.
     *
     * @return the status
     */
    PluginStatus status() {
        return status;
    }

    /**
     * Sets the plugin status.
     *
     * @param status the new status
     */
    void setStatus(PluginStatus status) {
        this.status = status;
    }

    /**
     * Returns the list of registration handles.
     *
     * @return the handles
     */
    List<RegistrationHandle> handles() {
        return handles;
    }

    /**
     * Adds a registration handle.
     *
     * @param handle the handle to add
     */
    void addHandle(RegistrationHandle handle) {
        if (handle != null && !handles.contains(handle)) {
            handles.add(handle);
        }
    }

    /**
     * Stores the installed plugin message registration. Unlike runtime handles, this
     * registration remains active while the plugin is disabled so its metadata can
     * still be localized in plugin management.
     */
    void setI18nRegistration(RegistrationHandle handle, AutoCloseable resource) {
        this.i18nHandle = handle;
        this.i18nResource = resource;
    }

    RegistrationHandle i18nHandle() {
        return i18nHandle;
    }

    AutoCloseable i18nResource() {
        return i18nResource;
    }

    void clearI18nRegistration() {
        i18nHandle = null;
        i18nResource = null;
    }

    /**
     * Returns the loaded plugin boot instance.
     *
     * @return the loaded boot, or null if not loaded
     */
    LoadedPluginBoot loadedBoot() {
        return loadedBoot;
    }

    /**
     * Sets the loaded plugin boot instance.
     *
     * @param loadedBoot the loaded boot
     */
    void setLoadedBoot(LoadedPluginBoot loadedBoot) {
        this.loadedBoot = loadedBoot;
    }

    /**
     * Returns the plugin bean container.
     *
     * @return the bean container, or null if not initialized
     */
    PluginBeanContainer beanContainer() {
        return beanContainer;
    }

    /**
     * Sets the plugin bean container.
     *
     * @param beanContainer the bean container
     */
    void setBeanContainer(PluginBeanContainer beanContainer) {
        this.beanContainer = beanContainer;
    }

    /**
     * Returns the plugin context.
     *
     * @return the context, or null if not initialized
     */
    PluginContext context() {
        return context;
    }

    /**
     * Sets the plugin context.
     *
     * @param context the context
     */
    void setContext(PluginContext context) {
        this.context = context;
    }

    /**
     * Sets the last error message.
     *
     * @param lastError the error message
     */
    void setLastError(String lastError) {
        this.lastError = lastError;
    }

    /**
     * Sets the installation timestamp.
     *
     * @param installedAt the timestamp
     */
    void setInstalledAt(Instant installedAt) {
        this.installedAt = installedAt;
    }

    /**
     * Sets the last start timestamp.
     *
     * @param lastStartedAt the timestamp
     */
    void setLastStartedAt(Instant lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    /**
     * Sets the last stop timestamp.
     *
     * @param lastStoppedAt the timestamp
     */
    void setLastStoppedAt(Instant lastStoppedAt) {
        this.lastStoppedAt = lastStoppedAt;
    }

    /**
     * Adds a lifecycle event.
     *
     * @param event the event to add
     */
    void addEvent(PluginLifecycleEvent event) {
        events.add(event);
    }

    /**
     * Clears all runtime state (handles, boot, context).
     */
    void clearRuntime() {
        handles.clear();
        loadedBoot = null;
        beanContainer = null;
        context = null;
    }

    /**
     * Creates an immutable snapshot of the current state.
     *
     * @return the snapshot
     */
    PluginRuntimeSnapshot snapshot() {
        return new PluginRuntimeSnapshot(
                pluginId(),
                descriptor,
                source,
                artifactPath,
                status,
                handles.size(),
                lastError,
                installedAt,
                lastStartedAt,
                lastStoppedAt,
                List.copyOf(events));
    }
}
