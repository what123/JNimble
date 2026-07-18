package com.jnimble.kernel.plugin;

import com.jnimble.sdk.hook.HookMode;
import com.jnimble.sdk.hook.HookRegistry;
import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.resource.AssetRegistry;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteRegistry;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link PluginContext} implementation that tracks all registrations made by a plugin.
 * Wraps registries to capture registration handles for lifecycle management.
 */
public class TrackingPluginContext implements PluginContext {

    private final PluginDescriptor descriptor;
    private final HookRegistry hooks;
    private final RouteRegistry routes;
    private final AssetRegistry assets;
    private final PluginBeanResolver beanResolver;
    private final Consumer<RegistrationHandle> handleSink;

    /**
     * Creates a tracking context that wraps the given registries and captures all registration handles.
     *
     * @param descriptor   the plugin descriptor
     * @param hooks        the hook registry for the plugin
     * @param routes       the route registry for the plugin
     * @param assets       the asset registry for the plugin
     * @param beanResolver the bean resolver for the plugin
     * @param handleSink   the consumer that receives all registration handles for lifecycle tracking
     */
    public TrackingPluginContext(
            PluginDescriptor descriptor,
            HookRegistry hooks,
            RouteRegistry routes,
            AssetRegistry assets,
            PluginBeanResolver beanResolver,
            Consumer<RegistrationHandle> handleSink
    ) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.handleSink = Objects.requireNonNull(handleSink, "handleSink");
        this.hooks = trackingHooks(Objects.requireNonNull(hooks, "hooks"));
        this.routes = trackingRoutes(Objects.requireNonNull(routes, "routes"));
        this.assets = trackingAssets(Objects.requireNonNull(assets, "assets"));
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver");
    }

    @Override
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public HookRegistry hooks() {
        return hooks;
    }

    @Override
    public RouteRegistry routes() {
        return routes;
    }

    @Override
    public AssetRegistry assets() {
        return assets;
    }

    @Override
    public <T> T bean(Class<T> type) {
        return beanResolver.resolve(type);
    }

    @Override
    public RegistrationHandle registerHandle(RegistrationHandle handle) {
        track(handle);
        return handle;
    }

    private HookRegistry trackingHooks(HookRegistry delegate) {
        return new HookRegistry() {
            @Override
            public RegistrationHandle register(String hookName, HookViewContribution contribution) {
                return track(delegate.register(hookName, contribution));
            }

            @Override
            public RegistrationHandle register(String hookName, HookMode mode, HookViewContribution contribution) {
                return track(delegate.register(hookName, mode, contribution));
            }
        };
    }

    private RouteRegistry trackingRoutes(RouteRegistry delegate) {
        return new RouteRegistry() {
            @Override
            public RegistrationHandle register(RouteDefinition route) {
                return track(delegate.register(route));
            }
        };
    }

    private AssetRegistry trackingAssets(AssetRegistry delegate) {
        return new AssetRegistry() {
            @Override
            public RegistrationHandle register(AssetDefinition asset) {
                return track(delegate.register(asset));
            }
        };
    }

    private RegistrationHandle track(RegistrationHandle handle) {
        if (handle == null) {
            throw new PluginRuntimeException("Plugin registry returned a null RegistrationHandle for " + descriptor.id());
        }
        handleSink.accept(handle);
        return handle;
    }
}
