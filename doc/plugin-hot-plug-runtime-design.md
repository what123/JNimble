# 插件完整热插拔运行时设计

## 1. 目标

采用完整热插拔方案，并保证开发模式与生产模式走同一套插件生命周期：

- 开发环境从 Classpath 中发现 `plugins/` 源码模块，支持 IDE 断点调试。
- 生产环境的纯净 starter 不打包任何业务插件，只从 `./data/plugins` 加载 Jar。
- 后台上传或目录新增 Jar 后，无需重启即可安装和启用。
- 停用、替换、卸载插件时，无需重启即可注销 HTTP、Hook、Route、Asset、i18n、任务、Spring Bean 和 ClassLoader。
- 插件依赖显式声明并按拓扑顺序启用；依赖插件仍被使用时禁止停用或替换。
- 授权、数据库迁移、权限同步和现有插件状态语义保持不变。

## 2. 非目标

V1 不支持以下行为：

- 强制替换仍有已启用依赖方的插件；管理员需要先停用依赖方。
- 在一次请求执行到一半时强制终止线程。
- 不经插件 SDK 直接共享任意内部实现类。
- 从 Maven 仓库在线解析插件依赖；生产部署必须提供依赖插件 Jar。

## 3. 工程结构

基础框架最终只保留框架模块：

```text
jnimble/
  jnimble-bom
  jnimble-plugin-sdk
  jnimble-kernel
  jnimble-platform
  jnimble-admin-shell
  jnimble-starter
```

点餐插件源码由独立聚合 POM 管理：

```text
plugins/
  pom.xml
  jnimble-plugin-menu-manager
  jnimble-plugin-order-core
  jnimble-plugin-order-table
  jnimble-plugin-payment
  jnimble-plugin-printer-core
  jnimble-plugin-printer-feie
```

`jnimble-starter` 默认将业务插件依赖限制在测试范围，生产可执行 Jar 不包含插件。
开发时启用 Maven `dev-plugins` Profile，插件依赖切换为 runtime Classpath，IDE 和
`spring-boot:run` 均可直接断点调试插件源码。Classpath 和外置 Jar 使用相同的
`PluginRuntimeService`、Bean 容器、Controller 注册和启停流程。

## 4. 插件清单扩展

插件描述符增加 Spring 配置和显式依赖：

```json
{
  "id": "order-table",
  "bootClass": "com.jnimble.plugin.order.table.OrderTablePluginBoot",
  "spring": {
    "configurationClass": "com.jnimble.plugin.order.table.OrderTablePluginConfiguration"
  },
  "dependencies": [
    {"pluginId": "order-core", "version": "1.x", "required": true},
    {"pluginId": "menu-manager", "version": "1.x", "required": true}
  ]
}
```

未声明 `spring` 的轻量插件仍可只使用 `PluginBoot`。未声明依赖时按空依赖处理，兼容旧插件描述符。

## 5. ClassLoader

每个 Jar 插件拥有独立、可关闭的 `PluginClassLoader`：

```text
插件自己的类和资源
  -> 显式依赖插件 ClassLoader
  -> 框架 ClassLoader
```

Java、Jakarta、Spring、JNimble SDK/Kernel/Platform 等共享 API 始终父加载优先，防止同一接口被不同 ClassLoader 重复加载。插件业务包优先从自身 Jar 加载，再查询声明的依赖插件。

Classpath 开发插件使用应用 ClassLoader，但仍创建相同的插件 Bean 容器并执行相同的注册和注销流程。

## 6. 插件 Spring 子容器

每个启用插件创建一个子 `ApplicationContext`：

- Parent 为主框架 `ApplicationContext`，因此插件可以注入 DataSource、事务、权限和平台服务。
- 加载描述符声明的 `configurationClass`。
- 插件配置类负责 `@ComponentScan` 和 `@MapperScan`，扫描范围必须限定在自身插件。
- 依赖插件导出的 Bean 作为只读外部单例桥接到当前插件子容器。
- `PluginContext.bean()` 优先查询插件子容器，再回退到平台容器。
- 子容器关闭时只销毁自身创建的 Bean，不销毁依赖插件导出的 Bean。

## 7. HTTP 热注册

子容器刷新后扫描其本地 `@Controller` 和 `@RestController` Bean，通过主框架 `RequestMappingHandlerMapping` 动态注册映射。

启用顺序：

```text
校验授权和依赖
  -> 创建 ClassLoader
  -> 执行 migration
  -> 创建插件子容器
  -> 执行 PluginBoot.boot
  -> 注册 HTTP 映射
  -> 标记 ENABLED
```

停用顺序：

```text
标记 DRAINING，拒绝新请求
  -> 注销 HTTP 映射
  -> 等待在途请求结束
  -> PluginBoot.stop
  -> 注销 Hook/Route/Asset/i18n
  -> 关闭插件子容器
  -> 关闭 ClassLoader
  -> 标记 DISABLED
```

插件管理、授权接口不属于插件业务请求，不参与插件请求排空。

## 8. 模板和静态资源

主框架维护启用插件的 ClassLoader 注册表：

- `PluginTemplateResolver` 根据 `plugin/{pluginId}/...` 从对应插件 ClassLoader 加载 `templates/`。
- `PluginAssetController` 使用对应插件 ClassLoader 加载 `static/`，不再使用默认应用 ClassLoader。
- 停用插件时先注销 ClassLoader 注册，随后模板和静态资源立即不可访问。

## 9. 依赖和替换规则

当前依赖关系：

```text
order-table -> order-core, menu-manager
payment -> order-core
printer-core -> order-core
printer-feie -> printer-core
```

- 依赖缺失、未启用或版本不兼容时拒绝启用。
- 有已启用依赖方时拒绝停用、卸载和替换提供方，并列出依赖方。
- 替换已启用插件时先排空并停用自身，加载新 Jar；任一步失败时恢复旧描述符、旧 Jar 和原启用状态。
- 旧 Jar 仅在新插件成功启用后删除。

## 10. 目录监听

生产运行时使用 `WatchService` 监听插件目录：

- 新增 Jar：等待文件稳定后解析、安装，并按配置自动启用。
- 修改/替换 Jar：执行带回滚的热替换。
- 删除 Jar：默认只记录告警，不自动卸载，避免复制工具的临时删除造成误卸载。
- 后台上传与目录监听共用安装协调锁，避免重复安装。

## 11. 测试门槛

必须同时通过：

1. Classpath 源码模式完整业务测试。
2. 纯净 starter 不含任何插件业务类。
3. 将真实插件 Jar 复制到临时 `plugins/` 后，无需重启即可访问页面和 API。
4. 停用后 Controller、模板、静态资源和 Hook 立即不可访问。
5. 重新启用后功能恢复且无重复映射。
6. 替换 Jar 后旧 ClassLoader 可被回收，失败时旧版本恢复。
7. 依赖缺失和依赖方仍启用时给出明确错误。
8. 连续启停测试检测线程、ClassLoader、JDBC Mapper 和 MVC 映射泄漏。

## 12. 实施顺序

1. 扩展描述符和依赖校验。
2. 增加插件 ClassLoader 注册表与依赖 ClassLoader。
3. 增加插件 Bean 容器 SPI 和 Spring 子容器实现。
4. 动态注册/注销 MVC Controller。
5. 增加插件模板解析和静态资源 ClassLoader 支持。
6. 改造运行时启停、失败回滚和依赖保护。
7. 适配现有插件配置类和描述符。
8. 拆分 starter 生产依赖与 `dev-plugins` 开发 Profile。
9. 增加目录监听和真实 Jar 端到端测试。

## 13. 构建与运行

开发模式（插件源码参与 Classpath）：

```bash
mvn -Pdev-plugins -pl jnimble-starter -am spring-boot:run
```

服务端口固定为 `8080`。开发模式仍会从 `plugins/` 下的 Maven 模块编译插件，
但运行时外置 Jar 目录默认为 `./data/plugins`，两者不会互相覆盖。

构建不携带业务插件的正式可执行 Jar：

```bash
mvn -pl jnimble-starter -am -DskipTests package
java -jar jnimble-starter/target/jnimble-starter-0.1.0-SNAPSHOT.jar
```

单独构建全部业务插件：

```bash
mvn -f plugins/pom.xml -DskipTests package
```

每个插件模块都会生成自己的 Jar。部署时将所需插件 Jar 放入
`${JNIMBLE_PLUGIN_DIR}`（默认 `./data/plugins`），或在后台插件页面上传。
插件管理页对拥有 `system.plugins.manage` 权限的用户显示“上传插件 Jar”入口，
支持点击选择或拖拽单个 `.jar` 文件，浏览器先执行扩展名和 5 MB 大小校验；
服务端仍会重新校验文件类型、插件描述符、平台版本和重复安装状态，失败原因返回插件管理页展示。
目录监听默认开启，新 Jar 会自动安装并按配置启用；替换失败时使用
`.runtime-cache` 中的旧 Jar 回滚。

## 14. 当前实现边界

- 插件替换时如果仍有已启用依赖方，会明确拒绝；需要先停用依赖方。
- 外置 Jar 修改应更新插件版本或清单；目录监听按文件 SHA-256 判断实际变化。
- 删除磁盘 Jar 只产生告警，卸载必须通过插件管理生命周期执行。
