/**
 * Provides the core plugin descriptor model, lifecycle abstractions, and
 * runtime entry point for the JNimble plugin system.
 *
 * <p>This package defines the fundamental building blocks of the plugin
 * system: metadata descriptors (parsed from {@code META-INF/jnimble-plugin.json}),
 * lifecycle phases and events, the {@link com.jnimble.sdk.plugin.PluginBoot}
 * entry point, and the {@link com.jnimble.sdk.plugin.PluginContext} accessor
 * supplied to plugins at runtime.</p>
 *
 * <p>JNimble 插件系统的核心包，提供插件描述符模型、生命周期抽象、引导接口和运行时上下文。
 * 包含从 {@code META-INF/jnimble-plugin.json} 解析出的所有元数据结构，
 * 以及插件生命周期状态、事件、异常等基础类型。</p>
 */
package com.jnimble.sdk.plugin;