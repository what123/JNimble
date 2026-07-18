# JNimble 插件开发说明书

## 1. 适用范围

本文档面向 JNimble 插件开发者，说明插件如何接入主系统、注册菜单与页面、声明权限、加载资源、执行迁移、打包安装以及适配主框架升级。

当前插件模型是框架亲和型模型：

- 插件可以直接依赖主框架模块。
- 插件可以通过 `PluginContext` 使用主系统 Spring Bean、模型、Service、DTO 和工具类。
- 插件权限由系统统一入库，统一展示到角色权限配置中，并由系统统一授权与校验。
- 插件需要通过 `platformVersion` 声明兼容的主框架版本。
- 插件负责适配主框架升级，主系统不为旧插件长期保留兼容层。

当前版本不提供插件沙箱、微前端、插件市场、依赖冲突强隔离和外部 Jar 自动 Spring Bean 扫描。

## 2. 核心模块

插件开发主要涉及以下模块：

| 模块 | 用途 |
| --- | --- |
| `jnimble-plugin-sdk` | 插件启动协议、Hook、路由、资源、描述符基础类型 |
| `jnimble-kernel` | 插件运行时、生命周期、Hook、路由、资源、迁移、国际化 |
| `jnimble-platform` | 用户、角色、权限、审计、插件状态等平台能力 |
| `jnimble-admin-shell` | 后台布局、页面模板、Hook 位点、插件管理页面 |
| `jnimble-starter` | 应用启动、插件发现、插件目录扫描、启动恢复 |

插件至少依赖 `jnimble-plugin-sdk`。如果插件需要直接调用系统服务、模型或后台模板能力，可以按需依赖 `jnimble-kernel`、`jnimble-platform`、`jnimble-admin-shell`。

## 3. 插件目录结构

推荐结构：

```text
my-plugin/
  pom.xml
  src/main/java/
    com/example/plugin/my/MyPluginBoot.java
  src/main/resources/
    META-INF/jnimble-plugin.json
    templates/plugin/my/
      fragment/sidebar.html
      page/index.html
    static/plugin/my/
      my.css
    i18n/
      messages.properties
      messages_zh_CN.properties
      messages_en_US.properties
    db/migration/plugin/my/
      V1__init_my_plugin.sql
```

约定：

- 插件 ID 使用小写字母、数字和短横线，必须以小写字母开头，例如 `crm`、`order-center`。
- 模板放在 `templates/plugin/{pluginId}/` 下。
- 静态资源放在 `static/plugin/{pluginId}/` 下。
- 数据库迁移脚本放在插件自己的目录下，避免和系统迁移混用。
- 权限编码必须以 `{pluginId}.` 开头，例如 `crm.customer.view`。

## 4. Maven 依赖

开发环境使用 JDK 21。示例插件依赖如下：

```xml
<dependencies>
    <dependency>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-plugin-sdk</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-kernel</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-platform</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-admin-shell</artifactId>
    </dependency>
</dependencies>
```

规则：

- 只做基础 Hook、路由、资源注册时，依赖 `jnimble-plugin-sdk` 即可。
- 需要调用主系统权限、用户、审计、插件状态等能力时，按需依赖主框架模块。
- 插件启动类不是 Spring Bean，不能依赖自动注入；需要系统能力时，通过 `PluginContext.bean(...)` 获取。
- Jar 安装模式只会把插件 Jar 加入插件类加载器。插件使用的三方库如果主系统没有提供，需要打进插件包，例如使用 shade 方式合并为单 Jar。

## 5. 插件描述符

每个插件必须提供：

```text
src/main/resources/META-INF/jnimble-plugin.json
```

完整示例：

```json
{
  "schemaVersion": "1.0",
  "id": "crm",
  "name": "CRM Plugin",
  "nameKey": "plugin.crm.name",
  "description": "Customer management plugin",
  "descriptionKey": "plugin.crm.description",
  "version": "1.0.0",
  "platformVersion": "0.1.x",
  "author": "JNimble Team",
  "website": "https://example.com",
  "bootClass": "com.example.plugin.crm.CrmPluginBoot",
  "i18n": {
    "basename": "i18n/messages"
  },
  "permissions": [
    {
      "code": "crm.customer.view",
      "name": "View customers",
      "nameKey": "permission.crm.customer.view",
      "description": "允许查看 CRM 客户",
      "descriptionKey": "permission.crm.customer.view.description"
    }
  ],
  "migration": {
    "enabled": true,
    "location": "classpath:db/migration/plugin/crm",
    "table": "flyway_schema_history_crm",
    "baselineOnMigrate": true,
    "failOnError": true
  }
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 是 | 当前固定为 `1.0` |
| `id` | 是 | 全局唯一插件 ID，格式为 `^[a-z][a-z0-9-]*$` |
| `name` | 是 | 默认显示名称 |
| `nameKey` | 否 | 名称国际化 key |
| `description` | 否 | 默认描述 |
| `descriptionKey` | 否 | 描述国际化 key |
| `version` | 是 | 插件版本 |
| `platformVersion` | 是 | 兼容的主框架版本表达式 |
| `author` | 否 | 作者 |
| `website` | 否 | 插件网址 |
| `bootClass` | 是 | 实现 `PluginBoot` 的启动类全限定名 |
| `i18n.basename` | 否 | 插件国际化资源 basename |
| `permissions` | 否 | 插件权限声明 |
| `migration` | 否 | 插件数据库迁移配置 |

`platformVersion` 支持：

| 写法 | 含义 |
| --- | --- |
| `*` | 允许任意平台版本 |
| `0.1.0` | 精确匹配 `0.1.0` |
| `0.1.x` | 匹配 `0.1.*` |
| `>= 0.1.0` | 当前平台版本大于等于 `0.1.0` |

## 6. 启动类与生命周期

插件启动类必须实现 `PluginBoot`：

```java
package com.example.plugin.crm;

import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.resource.AssetDefinition;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;
import java.util.Map;

public class CrmPluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        context.hooks().register(
                "admin.layout.sidebar.plugins.after",
                new HookViewContribution(
                        "plugin/crm/fragment/sidebar",
                        Map.of("pluginId", context.descriptor().id()),
                        100,
                        "crm.customer.view",
                        null
                )
        );

        context.routes().register(new RouteDefinition(
                "/customers",
                RouteMethod.GET,
                "plugin/crm/page/customers",
                "crm.customer.view"
        ));

        context.assets().register(new AssetDefinition(
                "/",
                "classpath:static/plugin/crm/",
                true
        ));
    }

    @Override
    public void stop(PluginContext context) {
        // 只释放插件自己持有的线程、连接、缓存等资源。
        // Hook、路由、静态资源注册由运行时自动回收。
    }
}
```

生命周期：

| 阶段 | 行为 |
| --- | --- |
| 安装 | 解析描述符，写入插件状态，注册权限目录但标记为不可用 |
| 启用 | 加载启动类，执行迁移，注册 i18n，执行 `boot`，注册 Hook、路由、资源 |
| 禁用 | 调用 `stop`，回收 Hook、路由、资源和自定义句柄，权限标记为不可用 |
| 重载 | 仅允许已启用插件执行，流程为禁用后重新启用 |
| 卸载 | 若插件已启用会先禁用，再标记为卸载 |

`PluginContext` 可用能力：

| 方法 | 说明 |
| --- | --- |
| `descriptor()` | 获取插件描述符 |
| `hooks()` | 注册页面 Hook |
| `routes()` | 注册插件页面路由 |
| `assets()` | 注册插件静态资源 |
| `bean(Class<T>)` | 获取主系统 Spring Bean，找不到时抛出异常 |
| `findBean(Class<T>)` | 尝试获取主系统 Spring Bean，返回 `Optional` |
| `registerHandle(...)` | 注册插件自定义句柄，热拔出时由运行时回收 |

## 7. 调用主系统能力

插件可以直接使用主系统方法、模型和 Spring Bean。推荐方式是在 `boot` 中通过 `PluginContext` 获取：

```java
import com.jnimble.platform.auth.UserAccountService;

public class ExamplePluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        UserAccountService userAccountService = context.bean(UserAccountService.class);
        userAccountService.findByUsername("admin");
    }
}
```

兼容规则：

- 插件可以依赖主框架公开类型，但需要跟随主框架升级进行适配。
- 插件调用主系统写操作时，必须遵守系统权限、审计、事务和数据一致性规则。
- 插件如直接使用 `jnimble-platform` 的 Entity、Mapper 或 `MapperUtils`，需要跟随主框架升级适配。
- 插件单表增删改查应使用 `MapperUtils`，不要在业务代码里直接调用 `BaseMapper` CRUD 方法。
- 插件不得覆盖系统核心 Bean，除非该 Bean 明确是扩展点。
- 对可能不存在的能力，优先使用 `findBean(...)` 做兼容处理。
- 插件需要跨版本兼容时，应在 `platformVersion` 中收窄范围，并在启动时校验必要 Bean 和方法。

## 8. 菜单与 Hook

后台菜单通过 Hook 输出，不需要独立菜单表。插件可自由选择菜单位置，也可以混入系统菜单组。

菜单片段示例：

```html
<a class="admin-shell__nav-item"
   th:classappend="${activeNav == 'plugin:' + pluginId + ':customers'} ? ' admin-shell__nav-item--active' : ''"
   th:href="@{/admin/plugins/{pluginId}/customers(pluginId=${pluginId})}">
  <span class="admin-shell__nav-icon" aria-hidden="true"></span>
  <span th:text="#{plugin.crm.name}">CRM</span>
</a>
```

注册菜单：

```java
context.hooks().register(
        "admin.layout.sidebar.plugins.after",
        new HookViewContribution(
                "plugin/crm/fragment/sidebar",
                Map.of("pluginId", context.descriptor().id()),
                100,
                "crm.customer.view",
                null
        )
);
```

`HookViewContribution` 字段：

| 字段 | 说明 |
| --- | --- |
| `view` | Thymeleaf 模板或片段路径 |
| `model` | 传给模板的变量 |
| `order` | 同一 Hook 下的渲染顺序，数字越小越靠前 |
| `permission` | 渲染该 Hook 需要的权限，为空则不校验 |
| `activeWhen` | 激活表达式；当前支持空、`true`、`false` 的基础判断 |

菜单 Hook 位点：

| Hook | 位置 |
| --- | --- |
| `admin.layout.sidebar.start` | 侧栏导航最前面 |
| `admin.layout.sidebar.end` | 侧栏导航最后面 |
| `admin.layout.sidebar.general.before` | General 分组前 |
| `admin.layout.sidebar.general.start` | General 分组标题后 |
| `admin.layout.sidebar.general.end` | General 分组末尾 |
| `admin.layout.sidebar.home.before` | 工作台菜单前 |
| `admin.layout.sidebar.home.after` | 工作台菜单后 |
| `admin.layout.sidebar.plugins.before` | 插件管理菜单前 |
| `admin.layout.sidebar.plugins.after` | 插件管理菜单后 |
| `admin.layout.sidebar.access.before` | Access 分组前 |
| `admin.layout.sidebar.access.start` | Access 分组标题后 |
| `admin.layout.sidebar.access.end` | Access 分组末尾 |
| `admin.layout.sidebar.roles.before` | 角色权限菜单前 |
| `admin.layout.sidebar.roles.after` | 角色权限菜单后 |
| `admin.layout.sidebar.users.before` | 用户管理菜单前 |
| `admin.layout.sidebar.users.after` | 用户管理菜单后 |
| `admin.layout.sidebar.trace.before` | Trace 分组前 |
| `admin.layout.sidebar.trace.start` | Trace 分组标题后 |
| `admin.layout.sidebar.trace.end` | Trace 分组末尾 |
| `admin.layout.sidebar.audit.before` | 审计日志菜单前 |
| `admin.layout.sidebar.audit.after` | 审计日志菜单后 |
| `admin.layout.sidebar` | 兼容旧 Hook，位于系统菜单尾部 |

其他保留 Hook：

```text
admin.layout.topbar
admin.dashboard.widgets
admin.page.actions
admin.list.toolbar
admin.detail.tabs
admin.form.sections
```

Hook 合并模式：

```java
context.hooks().register("admin.layout.topbar", HookMode.APPEND, contribution);
context.hooks().register("admin.layout.topbar", HookMode.PREPEND, contribution);
context.hooks().register("admin.layout.topbar", HookMode.REPLACE, contribution);
context.hooks().register("admin.layout.topbar", HookMode.REMOVE, contribution);
```

默认使用 `APPEND`。`REPLACE` 和 `REMOVE` 会影响同一 Hook 下其他贡献，多个插件同时覆盖时，运行时会选择排序后的第一个覆盖项，并记录冲突日志。

## 9. 插件页面路由

插件后台页面统一挂载到：

```text
/admin/plugins/{pluginId}/**
```

注册示例：

```java
context.routes().register(new RouteDefinition(
        "/customers",
        RouteMethod.GET,
        "plugin/crm/page/customers",
        "crm.customer.view"
));
```

访问路径：

```text
/admin/plugins/crm/customers
```

路由规则：

- `path` 是插件命名空间下的相对路径。
- `RouteMethod` 支持 `GET`、`POST`、`PUT`、`PATCH`、`DELETE`。
- `view` 是 Thymeleaf 视图名。
- `permission` 不为空时，系统进入页面前会做服务端权限校验。
- 路由启用期间可访问，插件禁用后返回不可用。
- 不允许使用 `..` 形式的父级路径。
- 不要注册登录、登出、系统错误页等系统保留路径。

页面模板示例：

```html
<!doctype html>
<html lang="zh-CN"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/admin}">
<head>
  <title th:text="#{crm.customer.title}">CRM 客户</title>
  <link rel="stylesheet" th:href="@{/assets/plugins/{pluginId}/crm.css(pluginId=${pluginId})}">
</head>
<body>
  <h1 layout:fragment="page-title" th:text="#{crm.customer.title}">CRM 客户</h1>

  <main layout:fragment="content">
    <section class="admin-page-section">
      <div class="admin-page-section__header">
        <div>
          <p class="admin-shell__eyebrow">CRM</p>
          <h2 th:text="#{crm.customer.list}">客户列表</h2>
        </div>
      </div>
    </section>
  </main>
</body>
</html>
```

运行时会向模板加入常用变量：

| 变量 | 说明 |
| --- | --- |
| `pluginId` | 当前插件 ID |
| `pluginRoute` | 当前注册路由快照 |
| `pluginRouteDefinition` | 当前路由定义 |
| `activeNav` | 当前菜单激活标识，格式通常为 `plugin:{pluginId}:{routeKey}` |

## 10. 静态资源

注册示例：

```java
context.assets().register(new AssetDefinition(
        "/",
        "classpath:static/plugin/crm/",
        true
));
```

访问示例：

```html
<link rel="stylesheet" th:href="@{/assets/plugins/{pluginId}/crm.css(pluginId=${pluginId})}">
```

实际 URL：

```text
/assets/plugins/crm/crm.css
```

资源规则：

- 当前资源控制器只支持 `classpath:` 资源位置。
- `requestPath` 是插件资源命名空间下的相对路径。
- 资源完整路径为 `/assets/plugins/{pluginId}/{requestPath}`。
- `cacheable=true` 时响应使用长期缓存。
- `cacheable=false` 时响应使用 `no-store`。
- 不允许使用 `..` 形式的父级路径。

## 11. 国际化

描述符中声明：

```json
{
  "i18n": {
    "basename": "i18n/messages"
  }
}
```

资源文件：

```text
src/main/resources/i18n/messages.properties
src/main/resources/i18n/messages_zh_CN.properties
src/main/resources/i18n/messages_en_US.properties
```

示例：

```properties
plugin.crm.name=CRM
plugin.crm.description=客户管理插件
permission.crm.customer.view=查看 CRM 客户
crm.customer.title=CRM 客户
crm.customer.list=客户列表
```

使用方式：

```html
<span th:text="#{plugin.crm.name}">CRM</span>
```

规则：

- 插件启用时注册 i18n，禁用后移除。
- 描述符里的 `nameKey`、`descriptionKey` 和权限 `nameKey` 会使用系统消息源解析。
- key 必须稳定，不要在小版本中随意改名。

## 12. 权限

插件权限在描述符中声明：

```json
{
  "permissions": [
    {
      "code": "crm.customer.view",
      "name": "View customers",
      "nameKey": "permission.crm.customer.view"
    },
    {
      "code": "crm.customer.manage",
      "name": "Manage customers",
      "nameKey": "permission.crm.customer.manage"
    }
  ]
}
```

权限规则：

- 权限编码必须以 `{pluginId}.` 开头。
- 插件安装后，权限进入系统权限目录。
- 插件启用后，权限标记为可用，并显示在角色权限配置中。
- 插件禁用或卸载后，权限标记为不可用。
- 超级管理员会自动获得所有可用权限。
- 菜单 Hook 和页面路由都应填写权限，避免只隐藏菜单不校验页面。
- 业务写操作仍需要在实际处理请求的系统接口或服务方法中做权限校验，不能只依赖前端按钮隐藏。

## 13. 数据库迁移

插件迁移通过 Flyway 执行。描述符示例：

```json
{
  "migration": {
    "enabled": true,
    "location": "classpath:db/migration/plugin/crm",
    "table": "flyway_schema_history_crm",
    "baselineOnMigrate": true,
    "failOnError": true
  }
}
```

迁移脚本示例：

```text
src/main/resources/db/migration/plugin/crm/V1__init_crm.sql
src/main/resources/db/migration/plugin/crm/V2__add_customer_status.sql
```

规则：

- 插件启用时执行迁移。
- 有 `DataSource` 时使用 Flyway 执行。
- 无数据库的开发模式使用 no-op 迁移执行器。
- 每个插件使用独立的 Flyway history table，避免和系统迁移互相污染。
- `failOnError=true` 时迁移失败会导致插件启用失败，状态为 `MIGRATION_FAILED`。
- 已发布脚本不得修改，只能新增新版本脚本。

## 14. 数据访问

插件可以选择两种数据访问方式：

- 使用主系统已有 Service，例如用户、角色、权限、审计、插件状态等能力。
- 自己定义业务表、Entity、Mapper 和 Service。

插件使用主系统能力时：

```java
import com.jnimble.platform.audit.AuditService;
import com.jnimble.platform.auth.UserAccountService;

public class ExamplePluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        UserAccountService users = context.bean(UserAccountService.class);
        AuditService audit = context.bean(AuditService.class);
        users.findByUsername("admin");
        audit.recordSuccess("plugin.example.query", "user", "admin", "插件查询后台用户");
    }
}
```

插件自建业务表时：

- Entity 使用 MyBatis-Plus 注解。
- Mapper 继承 `BaseMapper<T>`。
- 单表新增、删除、修改、查询使用 `MapperUtils`。
- join、批量同步、跨表校验可以写 Mapper 自定义方法。
- 数据库迁移脚本放在插件自己的 `db/migration/plugin/{pluginId}/` 目录。

示例：

```java
import com.jnimble.platform.persistence.crud.MapperUtils;

CustomerEntity customer = new CustomerEntity();
MapperUtils.insert(customerMapper, customer);

MapperUtils.selectList(customerMapper, CustomerEntity.class,
        wrapper -> wrapper.eq("status", "ACTIVE"));
```

不要在插件业务代码里直接调用：

```java
customerMapper.insert(customer);
customerMapper.selectList(wrapper);
customerMapper.updateById(customer);
```

## 15. 热插拔与状态

插件管理支持：

| 操作 | 说明 |
| --- | --- |
| 安装 | 通过描述符或 Jar 包安装插件 |
| 启用 | 加载插件并注册运行时贡献 |
| 禁用 | 停止插件并回收运行时贡献 |
| 重载 | 已启用插件先禁用再启用 |
| 替换 Jar | 禁用插件后上传同 ID 新 Jar |
| 卸载 | 禁用后标记为卸载 |

插件状态：

| 状态 | 说明 |
| --- | --- |
| `INSTALLED` | 已安装，未启用 |
| `ENABLED` | 已启用 |
| `DISABLED` | 已禁用 |
| `FAILED` | 启用、禁用或运行时失败 |
| `MIGRATION_FAILED` | 迁移失败 |
| `UNINSTALLED` | 已卸载 |

热拔出要求：

- `boot` 中注册的 Hook、路由、资源由运行时自动回收。
- 插件自己创建的线程、连接、订阅、缓存、临时文件必须在 `stop` 中释放。
- 插件自己创建的可回收对象应通过 `context.registerHandle(...)` 交给运行时统一回收。
- `stop` 必须可重复调用，不应因为资源已释放而报错。

## 16. 开发调试

开发模式默认开启 classpath 插件发现：

```yaml
jnimble:
  plugins:
    dev-classpath-enabled: true
    auto-enable: false
```

当插件模块在应用 classpath 中，系统会扫描：

```text
classpath*:META-INF/jnimble-plugin.json
```

开发流程：

```bash
source .tools/env.sh
mvn -s .tools/maven/settings-cn.xml \
  -Dmaven.repo.local=/home/tanxy/workspace/JNimble/.tools/m2 \
  test
```

启动应用：

```bash
source .tools/env.sh
mvn -s .tools/maven/settings-cn.xml \
  -Dmaven.repo.local=/home/tanxy/workspace/JNimble/.tools/m2 \
  -pl jnimble-starter spring-boot:run
```

本地访问：

```text
http://192.168.4.150:8080/admin
```

默认账号：

```text
username: admin
password: admin
```

## 17. 打包与安装

打包：

```bash
source .tools/env.sh
mvn -s .tools/maven/settings-cn.xml \
  -Dmaven.repo.local=/home/tanxy/workspace/JNimble/.tools/m2 \
  -pl my-plugin -am package
```

Jar 包要求：

- 必须包含 `META-INF/jnimble-plugin.json`。
- 必须包含 `bootClass` 对应 class。
- 必须包含插件模板、静态资源、i18n 和迁移脚本。
- 三方依赖如果不由主系统提供，需要合并进插件 Jar。
- 插件包文件名必须以 `.jar` 结尾。

安装方式：

- 后台插件管理页面上传 Jar。
- 将 Jar 放到 `jnimble.plugins.dir` 目录，默认 `./plugins`，启动时自动扫描安装。
- 通过开发 classpath 自动发现。

替换 Jar：

- 插件必须先禁用。
- 新 Jar 的 `id` 必须和目标插件一致。
- 替换后再启用或重载。

## 18. 插件开发检查清单

发布前必须确认：

- `META-INF/jnimble-plugin.json` 字段完整，`id`、`version`、`platformVersion` 正确。
- 权限编码全部以 `{pluginId}.` 开头。
- 菜单 Hook 有权限控制。
- 页面路由有服务端权限控制。
- 写操作在实际处理请求的系统接口或服务方法中有权限校验和必要审计。
- 模板路径位于 `templates/plugin/{pluginId}/`。
- 静态资源路径位于 `static/plugin/{pluginId}/`。
- 资源 URL 使用 `/assets/plugins/{pluginId}/...`。
- i18n key 有默认值和中文资源。
- 数据库迁移表与系统迁移表隔离。
- 单表增删改查遵守 `MapperUtils` 规则。
- `stop` 能释放插件自有资源。
- 插件禁用后菜单、页面、资源不可访问。
- 超级管理员可以看到并访问插件权限对应功能。
- 普通角色只有授权后才能看到菜单并访问页面。
- Jar 包安装、启用、禁用、重载、卸载流程均验证通过。

## 19. 常见问题

### 插件菜单可以放到系统菜单中间吗？

可以。通过具体的侧栏 Hook 位点决定位置，例如放到插件管理后使用 `admin.layout.sidebar.plugins.after`，放到用户管理前使用 `admin.layout.sidebar.users.before`。

### 插件可以调用系统用户、角色、权限服务吗？

可以。插件通过 `context.bean(...)` 获取主系统 Spring Bean，然后调用系统方法。插件需要跟随主框架版本适配这些 API。

### 插件权限在哪里配置？

插件权限由描述符声明，系统安装插件时同步到权限目录。启用后会显示在角色权限配置中，由系统统一授权和校验。

### 插件禁用后菜单为什么不显示？

插件禁用时，运行时会回收该插件注册的 Hook、路由、资源，并将插件权限标记为不可用。

### 上传 Jar 后无法启用怎么办？

优先检查：

- `META-INF/jnimble-plugin.json` 是否存在。
- `bootClass` 是否正确并实现 `PluginBoot`。
- `platformVersion` 是否匹配当前平台版本。
- 权限编码是否以插件 ID 开头。
- 迁移脚本是否执行失败。
- 插件依赖的三方类是否已经打进 Jar 或由主系统提供。

### 插件能不能注册 Spring Controller？

当前外部 Jar 不做自动 Spring Bean 扫描。插件页面应通过 `context.routes().register(...)` 注册到插件路由，由系统统一渲染模板和校验权限。需要系统能力时通过 `PluginContext.bean(...)` 调用已有 Bean。
