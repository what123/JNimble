/**
 * Provides the route registry and definition model for plugins.
 *
 * <p>Plugins use the {@link com.jnimble.sdk.route.RouteRegistry} to expose
 * HTTP endpoints under their own namespace. Routes are defined by
 * {@link com.jnimble.sdk.route.RouteDefinition}, which specifies the
 * path, HTTP method, view target, and optional permission expression.</p>
 *
 * <p>插件路由注册与定义包。插件通过 {@link com.jnimble.sdk.route.RouteRegistry}
 * 在其命名空间下暴露 HTTP 端点。路由由 {@link com.jnimble.sdk.route.RouteDefinition}
 * 定义，包含路径、HTTP 方法、视图目标和可选的权限表达式。</p>
 */
package com.jnimble.sdk.route;