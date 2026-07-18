package com.jnimble.starter.plugin.runtime;

import com.jnimble.kernel.plugin.PluginRuntimeException;
import java.time.Duration;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks in-flight requests for dynamically registered plugin controllers.
 */
@Component
public class PluginRequestDrainRegistry {

    private final Map<Object, String> handlerOwners = java.util.Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<String, DrainState> states = new ConcurrentHashMap<>();

    void registerHandlers(String pluginId, Collection<?> handlers) {
        DrainState state = states.computeIfAbsent(pluginId, ignored -> new DrainState());
        synchronized (state) {
            state.accepting = true;
        }
        synchronized (handlerOwners) {
            handlers.forEach(handler -> handlerOwners.put(handler, pluginId));
        }
    }

    void unregisterHandlers(String pluginId, Collection<?> handlers) {
        synchronized (handlerOwners) {
            handlers.forEach(handler -> handlerOwners.remove(handler));
        }
        states.remove(pluginId);
    }

    Optional<RequestLease> acquire(Object handler) {
        String pluginId;
        synchronized (handlerOwners) {
            pluginId = handlerOwners.get(handler);
        }
        if (pluginId == null) {
            return Optional.empty();
        }
        DrainState state = states.get(pluginId);
        if (state == null) {
            return Optional.empty();
        }
        synchronized (state) {
            if (!state.accepting) {
                throw new PluginRequestDrainingException(pluginId);
            }
            state.activeRequests++;
        }
        return Optional.of(new RequestLease(state));
    }

    void beginDrain(String pluginId) {
        DrainState state = states.computeIfAbsent(pluginId, ignored -> new DrainState());
        synchronized (state) {
            state.accepting = false;
        }
    }

    void resume(String pluginId) {
        DrainState state = states.computeIfAbsent(pluginId, ignored -> new DrainState());
        synchronized (state) {
            state.accepting = true;
            state.notifyAll();
        }
    }

    void awaitDrained(String pluginId, Duration timeout) {
        DrainState state = states.get(pluginId);
        if (state == null) {
            return;
        }
        long remainingNanos = timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        synchronized (state) {
            while (state.activeRequests > 0 && remainingNanos > 0) {
                try {
                    long millis = Math.max(1L, remainingNanos / 1_000_000L);
                    state.wait(millis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new PluginRuntimeException("Interrupted while draining plugin " + pluginId, ex);
                }
                remainingNanos = deadline - System.nanoTime();
            }
            if (state.activeRequests > 0) {
                throw new PluginRuntimeException(
                        "Timed out draining plugin " + pluginId
                                + "; active requests: " + state.activeRequests);
            }
        }
    }

    static final class RequestLease implements AutoCloseable {

        private final DrainState state;
        private boolean closed;

        private RequestLease(DrainState state) {
            this.state = state;
        }

        @Override
        public void close() {
            synchronized (state) {
                if (closed) {
                    return;
                }
                closed = true;
                state.activeRequests--;
                state.notifyAll();
            }
        }
    }

    static final class PluginRequestDrainingException extends PluginRuntimeException {

        private PluginRequestDrainingException(String pluginId) {
            super("Plugin is draining: " + pluginId);
        }
    }

    private static final class DrainState {
        private boolean accepting = true;
        private int activeRequests;
    }
}
