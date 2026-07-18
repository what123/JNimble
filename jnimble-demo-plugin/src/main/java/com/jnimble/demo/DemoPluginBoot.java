package com.jnimble.demo;

import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;

/**
 * 极简 Demo 插件入口。
 *
 * <p>演示两个核心能力：</p>
 * <ol>
 *   <li>通过 Hook 注册后台侧边栏菜单项</li>
 *   <li>通过 RouteRegistry 注册插件页面路由</li>
 * </ol>
 */
public class DemoPluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        // 1. 注册侧边栏菜单项到 admin.layout.sidebar 钩子
        context.hooks().register(
                "admin.layout.sidebar",
                new HookViewContribution(
                        "plugin/demo-plugin/fragment/sidebar",
                        null,
                        100,
                        "demo-plugin.view",
                        null
                )
        );

        // 2. 注册插件页面路由
        context.routes().register(new RouteDefinition(
                "/index",
                RouteMethod.GET,
                "plugin/demo-plugin/page/index",
                "demo-plugin.view"
        ));
    }
}
