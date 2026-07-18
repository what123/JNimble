# JNimble

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/maven-3.9+-blue.svg)](https://maven.apache.org/)

[English](README.md) | 中文

> **授权提示**：JNimble 采用 **双重授权模式**。
> - **开源版（Apache 2.0）**：允许商用、二开、分发，但**必须保留管理后台和登录页底部的版权信息**（`© 2026 JNimble. All rights reserved.`）。
> - **商业授权**：如需去除版权信息或将 JNimble 用于不显示版权的场景，请联系 `178277164@qq.com` 购买商业授权。
>
> 完整授权条款见 [NOTICE](NOTICE) 与 [LICENSE](LICENSE)。

JNimble 是一个**插件化**的 Java 后台管理框架，基于 Spring Boot 3、Java 21、MyBatis-Plus 和 Thymeleaf 构建。所有业务能力都以插件形式存在 —— 安装即用、卸载即走，让你通过**搭积木式**地组合插件来构建垂直后台产品，而无需重新造平台轮子。

## 为什么选 JNimble

JNimble 的核心是**插件机制**，它带来三种工作方式：

- **搭积木式二开** —— 不要 fork 主项目改代码。把你要的功能写成一个插件 JAR，丢进插件目录就被自动识别、注册菜单、注册路由、注册权限，框架主代码零修改。
- **跨项目复用** —— 一次开发，多项目使用。订单管理、会员体系、报表导出、消息推送……每个能力都是一个独立插件，在新项目里装上对应 JAR 即可。
- **按需付费购买** —— 插件是独立单元，客户需要什么功能就购买什么插件。（许可证校验委托给独立的 SDK，由业务插件按需依赖，框架本身不内置许可证逻辑。）

框架本身只提供"地基"：用户、角色、权限、审计、i18n、插件运行时、管理后台 UI 外壳。**所有业务功能都通过插件交付**。

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8+（或任何 Spring Boot 可配置的 DataSource）

### 构建

```bash
mvn clean install
```

### 配置

JNimble 从环境变量读取所有敏感配置。首次启动前至少设置：

```bash
export JNIMBLE_DB_URL='jdbc:mysql://localhost:3306/jnimble?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true'
export JNIMBLE_DB_USERNAME=jnimble
export JNIMBLE_DB_PASSWORD='你的MySQL密码'
export JNIMBLE_DEFAULT_ADMIN_PASSWORD='你的管理员密码'
```

完整环境变量列表见 `jnimble-starter/src/main/resources/application.yml`。

### 运行

```bash
mvn -pl jnimble-starter spring-boot:run
```

打开 `http://localhost:8080/admin`，使用 `admin` / 你设置的密码登录。

### 数据库迁移（Flyway 自动建表/升级）

JNimble 使用 [Flyway](https://flywaydb.org/) 管理数据库版本，**应用启动时会自动执行迁移脚本**，无需手动建表。

- **框架迁移**：位于 `jnimble-starter/src/main/resources/db/migration/system/`，文件名约定 `V{版本号}__{描述}.sql`，所有 `jnimble_*` 系统表（用户、角色、权限、审计、插件状态、系统设置等）都由这些脚本创建和维护。
- **插件迁移**：每个插件拥有独立的迁移目录和历史表（详见 [插件机制 → 数据库迁移](#数据库迁移)），互不影响。
- **历史表**：框架使用 `flyway_schema_history` 记录已执行迁移；插件使用 `flyway_schema_history_{pluginId}`。
- **升级机制**：增加新表或字段时，**只需新增一个版本号更高的 `V*.sql` 脚本**，下次启动 Flyway 会自动识别并执行。**严禁**直接修改已发布的迁移脚本或手动改库结构 —— 已应用的脚本一旦变更，Flyway 校验会失败导致启动报错。
- **基线策略**：`application.yml` 中 `spring.flyway.baseline-on-migrate=true`，对已有库的首次迁移会自动建立基线，不会清空数据。

如需查看迁移状态：

```bash
mysql -u<user> -p<pwd> jnimble -e "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
```

如启动时 Flyway 校验失败（例如历史中存在已删除的迁移），可对历史表执行 `flyway repair` 或清理对应记录后重启。

## 插件机制

### 一个插件能做什么

每个插件拥有独立类加载器、独立数据库迁移历史表、独立 i18n 资源包，通过 `PluginContext` 拿到以下能力：

| 能力 | 入口 | 用途 |
|------|------|------|
| 钩子（Hooks） | `context.hooks()` | 向后台布局的预定义挂载点贡献 UI 片段（侧边栏菜单、顶栏按钮等） |
| 路由（Routes） | `context.routes()` | 注册 `/admin/plugins/{pluginId}/**` 命名空间下的页面路由 |
| 静态资源（Assets） | `context.assets()` | 注册 CSS / JS / 图片等静态资源 |
| 平台 Bean | `context.bean(Class)` | 查询框架容器中的服务（如 `UserAccountService`） |
| 描述符 | `context.descriptor()` | 读取 `jnimble-plugin.json` 解析结果 |

此外，插件描述符里还可以声明以下能力（运行时自动加载，无需在 `boot()` 中处理）：

| 能力 | 描述符字段 | 用途 |
|------|-----------|------|
| 权限 | `permissions` | 声明权限码，自动同步到角色管理界面 |
| 数据库迁移 | `migration` | 启用插件专属 Flyway 迁移目录与历史表 |
| 国际化 | `i18n` | 注册插件专属消息包 |
| 后台入口 | `admin` | 声明插件在后台菜单中的入口路由和标签 |
| 配置表单 | `configuration` | 声明插件的可配置项，后台自动渲染表单 |
| Spring 子上下文 | `spring` | 加载插件专属 `@Configuration` 类 |
| 插件依赖 | `dependencies` | 声明依赖的其他插件 |

### 插件目录结构

一个标准的 JNimble 插件 JAR 包内结构如下（参考 `jnimble-demo-plugin`）：

```
my-plugin/
├── META-INF/
│   └── jnimble-plugin.json          # 插件描述符（必需）
├── com/
│   └── example/
│       └── MyPluginBoot.java        # 实现 PluginBoot 的入口类（必需）
├── templates/
│   └── plugin/
│       └── my-plugin/               # 模板路径必须以 plugin/{pluginId}/ 开头
│           ├── fragment/
│           │   └── sidebar.html     # 钩子片段
│           └── page/
│               └── index.html       # 页面模板
├── i18n/
│   ├── demo.properties              # 默认语言
│   └── demo_zh_CN.properties        # 中文
└── db/
    └── migration/
        └── plugin/
            └── my-plugin/           # 插件专属迁移目录
                └── V1__init.sql
```

> **模板路径约定**：所有插件模板路径必须以 `plugin/{pluginId}/` 开头（例如 `plugin/my-plugin/page/index`），由 `PluginTemplateResolver` 从插件类加载器加载。实际资源位于 `templates/plugin/{pluginId}/...`。

### 插件描述符

`META-INF/jnimble-plugin.json` 的最小结构：

```json
{
  "schemaVersion": "1.0",
  "id": "my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "platformVersion": "0.1.0",
  "bootClass": "com.example.MyPluginBoot",
  "admin": {
    "entry": "/index",
    "labelKey": "my-plugin.name",
    "permission": "my-plugin.view"
  },
  "permissions": [
    {
      "code": "my-plugin.view",
      "name": "查看 My Plugin",
      "nameKey": "my-plugin.permission.view"
    }
  ]
}
```

完整字段见 `jnimble-plugin-sdk` 的 `PluginDescriptor`。常用可选块：`i18n`、`migration`、`spring`、`dependencies`、`configuration`。

### 编写入口类

```java
package com.example;

import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;

public class MyPluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        // 1. 注册侧边栏菜单项（注意模板路径前缀 plugin/{pluginId}/）
        context.hooks().register(
                "admin.layout.sidebar",
                new HookViewContribution(
                        "plugin/my-plugin/fragment/sidebar",  // 模板视图路径
                        null,                                  // 模型变量
                        100,                                   // 渲染顺序（升序）
                        "my-plugin.view",                      // 所需权限
                        null                                   // 激活条件表达式
                )
        );

        // 2. 注册页面路由
        context.routes().register(new RouteDefinition(
                "/index",                              // 相对插件命名空间的路径
                RouteMethod.GET,
                "plugin/my-plugin/page/index",         // 视图模板路径
                "my-plugin.view"                       // 所需权限
        ));
    }

    @Override
    public void stop(PluginContext context) {
        // 释放插件自有的资源；标准注册项由平台自动注销，无需手动处理
    }
}
```

### 侧边栏片段示例

`templates/plugin/my-plugin/fragment/sidebar.html`：

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <a class="admin-shell__nav-item"
     th:classappend="${activeNav == 'plugin:my-plugin:index'} ? ' admin-shell__nav-item--active' : ''"
     th:href="@{/admin/plugins/my-plugin/index}">
    <span class="admin-shell__nav-icon" aria-hidden="true"></span>
    <span>My Plugin</span>
  </a>
</body>
</html>
```

> `activeNav` 的格式为 `plugin:{pluginId}:{routeKey}`，其中 `routeKey` 是路由路径去掉首尾 `/` 后把 `/` 换成 `.`（如 `/index` → `index`，`/orders/list` → `orders.list`）。

### 内置钩子点

框架在 `templates/layout/admin.html` 中预留了以下钩子点，插件可向任意一个贡献片段：

| 钩子点 | 位置 |
|--------|------|
| `admin.layout.sidebar.start` | 侧边栏顶部 |
| `admin.layout.sidebar` | 侧边栏通用插槽（demo 插件使用） |
| `admin.layout.sidebar.sys.start` | "系统管理"区段开始 |
| `admin.layout.sidebar.plugins.before` / `after` | "插件管理"菜单项前后 |
| `admin.layout.sidebar.roles.before` / `after` | "角色管理"菜单项前后 |
| `admin.layout.sidebar.users.before` / `after` | "用户管理"菜单项前后 |
| `admin.layout.sidebar.sys.end` | "系统管理"区段结束 |
| `admin.layout.sidebar.end` | 侧边栏底部 |
| `admin.layout.topbar` | 顶栏 |

### 权限声明

在 `jnimble-plugin.json` 的 `permissions` 数组中声明，**权限码必须以 `{pluginId}.` 开头**：

```json
{
  "permissions": [
    {
      "code": "my-plugin.view",
      "name": "查看 My Plugin",
      "nameKey": "my-plugin.permission.view",
      "description": "允许查看 My Plugin 页面",
      "descriptionKey": "my-plugin.permission.view.desc"
    },
    {
      "code": "my-plugin.manage",
      "name": "管理 My Plugin"
    }
  ]
}
```

插件启用时，声明的权限会自动同步到角色管理界面，管理员可将其分配给不同角色。

### 数据库迁移

在描述符中启用迁移：

```json
{
  "migration": {
    "enabled": true,
    "location": "classpath:db/migration/plugin/my-plugin",
    "table": "flyway_schema_history_my_plugin",
    "baselineOnMigrate": true,
    "failOnError": true
  }
}
```

约定（不指定时使用默认值）：

- 默认迁移目录：`classpath:db/migration/plugin/{pluginId}`
- 默认历史表：`flyway_schema_history_{pluginId 去掉连字符换成下划线}`（如 `demo-plugin` → `flyway_schema_history_demo_plugin`）
- `enabled` 默认 `false`，必须显式开启

每个插件有独立的迁移历史表，互不影响，卸载插件不会删除业务表。

### 国际化

在描述符中声明 basename：

```json
{
  "i18n": {
    "basename": "i18n.my-plugin"
  }
}
```

对应的资源文件（basename 的 `.` 转成 `/` 作为资源路径）：

- `i18n/my-plugin.properties` —— 默认语言
- `i18n/my-plugin_zh_CN.properties` —— 简体中文
- `i18n/my-plugin_en_US.properties` —— 英文（美国）

> 不声明 `i18n` 时，插件资源包不会被注册。

### 静态资源

```java
context.assets().register(new AssetDefinition(
        "css/my-plugin.css",                       // 请求路径（相对插件资源命名空间）
        "classpath:assets/my-plugin/css/app.css",  // 资源位置
        true                                       // 是否缓存
));
```

### Maven 依赖

插件 JAR 的 `pom.xml` 只需依赖 SDK，scope 设为 `provided`（运行时由框架提供）：

```xml
<project>
    <parent>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>my-plugin</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.jnimble</groupId>
            <artifactId>jnimble-plugin-sdk</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### 构建与安装

```bash
mvn -pl my-plugin package
```

三种安装方式：

1. **后台上传** —— 进入「后台 → 插件管理 → 上传」，选择生成的 JAR 文件。
2. **热部署目录** —— 把 JAR 放入插件目录（默认 `./data/plugins`，可在「系统管理 → 存储配置」修改）。`PluginDirectoryWatcher` 会自动安装或热替换。
3. **Classpath 发现** —— 开发期可直接放在 classpath 下，框架启动时自动发现。

安装后插件进入 `INSTALLED` 状态，需在插件管理页点击「启用」才会调用 `PluginBoot.boot()`。启用失败会自动回滚所有注册项。

### 参考实现

`jnimble-demo-plugin` 模块是一个完整可运行的最小示例，演示了：

- 注册侧边栏菜单（`admin.layout.sidebar` 钩子）
- 注册页面路由（`/index`）
- 声明权限（`demo-plugin.view`）
- 声明后台入口（`admin.entry`）
- 模板路径约定（`plugin/demo-plugin/...`）

直接 `mvn -pl jnimble-demo-plugin package` 构建后丢进插件目录即可在后台看到「Demo 插件」菜单。

## 模块说明

| 模块 | 说明 |
|------|------|
| `jnimble-bom` | 依赖版本 BOM。 |
| `jnimble-plugin-sdk` | 插件启动、钩子、路由、资源、迁移、i18n 等契约。 |
| `jnimble-kernel` | 插件运行时、钩子、路由、资源、迁移基础设施。 |
| `jnimble-platform` | 用户、角色、权限、审计、i18n、插件状态、系统设置。 |
| `jnimble-admin-shell` | 管理后台布局、页面、模板和 UI 外壳。 |
| `jnimble-starter` | 可运行的 Spring Boot 应用。 |
| `jnimble-demo-plugin` | 极简示例插件，演示菜单注册和页面路由。 |

## 功能特性

- **插件生命周期**：安装、启用、禁用、卸载、重载 —— 支持 JAR 上传、classpath、目录热部署三种发现方式。
- **钩子系统**：插件通过 Thymeleaf `jn:hook` 方言贡献侧边栏条目、顶栏操作和模板片段。
- **权限系统**：插件声明权限会同步到角色管理；后台页面强制服务器端权限校验。
- **数据库迁移**：框架 Flyway 迁移在 `db/migration/system`；每个插件拥有独立迁移目录和历史表。
- **国际化**：框架与插件各自维护消息包；后台 UI 默认提供中英文。
- **审计日志**：插件、用户、角色权限操作均被记录。
- **品牌定制**：站点名称、Logo 和存储目录可通过后台 UI 运行时配置。
- **配置表单**：插件可声明 `configuration` 字段，后台自动渲染配置表单并持久化。

## 数据库

JNimble 需要 `DataSource`。框架迁移会创建 `jnimble_user`、`jnimble_role`、`jnimble_permission`、`jnimble_role_permission`、`jnimble_subject_role`、`jnimble_audit_log`、`jnimble_plugin_state`、`jnimble_plugin_configuration`、`jnimble_language` 和 `jnimble_system_setting` 表。

平台单表数据访问使用 `MapperUtils`。服务代码不应直接调用 `BaseMapper` CRUD 方法。

## 贡献

欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解指南。

## 许可证

本项目采用 Apache License 2.0 许可证 — 详情请查看 [LICENSE](LICENSE) 文件。
