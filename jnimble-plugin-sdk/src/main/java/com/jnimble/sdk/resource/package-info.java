/**
 * Provides the static asset registry and definition model for plugins.
 *
 * <p>Plugins use the {@link com.jnimble.sdk.resource.AssetRegistry} to
 * register static resources (such as JavaScript, CSS, and images) that
 * should be served while the plugin is enabled. Each asset is described
 * by an {@link com.jnimble.sdk.resource.AssetDefinition}.</p>
 *
 * <p>插件静态资源注册与定义包。插件通过 {@link com.jnimble.sdk.resource.AssetRegistry}
 * 注册静态资源（如 JS、CSS、图片），每个资源由 {@link com.jnimble.sdk.resource.AssetDefinition}
 * 描述其请求路径、资源位置和缓存策略。</p>
 */
package com.jnimble.sdk.resource;