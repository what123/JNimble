package com.jnimble.sdk.route;

/**
 * Route exposed by a plugin.
 *
 * @param path path relative to the plugin route namespace
 * @param method HTTP method matched by the route
 * @param view template view or handler target understood by the runtime
 * @param permission optional permission expression required to enter the route
 */
public record RouteDefinition(
        String path,
        RouteMethod method,
        String view,
        String permission
) {

    /**
     * Creates a GET route with no permission restriction.
     *
     * @param path path relative to the plugin route namespace
     * @param view template view or handler target understood by the runtime
     */
    public RouteDefinition(String path, String view) {
        this(path, RouteMethod.GET, view, null);
    }
}
