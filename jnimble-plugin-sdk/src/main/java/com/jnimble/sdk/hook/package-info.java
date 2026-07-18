/**
 * Provides the hook registry and contribution model for plugins.
 *
 * <p>Plugins use the {@link com.jnimble.sdk.hook.HookRegistry} to contribute
 * renderable template fragments to named hook points exposed by the platform.
 * Contributions are described by {@link com.jnimble.sdk.hook.HookViewContribution}
 * and can be merged using various {@link com.jnimble.sdk.hook.HookMode} strategies.
 * The {@link com.jnimble.sdk.hook.RegistrationHandle} interface provides a
 * uniform way for the runtime to track and roll back registrations.</p>
 *
 * <p>插件 Hook 注册与贡献包。插件通过 {@link com.jnimble.sdk.hook.HookRegistry}
 * 向平台暴露的命名 Hook 点贡献可渲染的模板片段。贡献由
 * {@link com.jnimble.sdk.hook.HookViewContribution} 描述，支持多种
 * {@link com.jnimble.sdk.hook.HookMode} 合并策略。
 * {@link com.jnimble.sdk.hook.RegistrationHandle} 为运行时追踪和回滚注册提供统一方式。</p>
 */
package com.jnimble.sdk.hook;