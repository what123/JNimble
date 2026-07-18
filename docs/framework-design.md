# JNimble 基础框架设计

## 1. 定位与边界

JNimble 是一个 Java 后台基础框架，提供登录、权限、后台页面壳、插件加载、Hook 扩展、数据库迁移、国际化等通用能力。主框架不内置具体业务，业务能力通过插件扩展。

第一版边界：

- 主框架负责平台能力，不承载具体业务。
- 后台界面以 Admin Shell 为页面壳，页面扩展统一走 Hook。
- 菜单不是独立平台模型，由 `admin.layout.sidebar` Hook 输出。
- 插件既可以复用系统 layout，也可以使用自己的 layout。
- 数据库迁移统一使用 Flyway，分为系统级迁移和插件级迁移。
- 插件采用框架亲和型模型，可以直接使用主框架公开的模型、Service、DTO、工具类和 Spring Bean。
- 插件通过 `platformVersion` 声明适配的主框架版本；主框架不承诺对旧插件做长期兼容。
- 第一版支持插件热插拔，插件可以在系统运行时安装、启用、禁用、卸载和重载。
- 插件权限由系统统一入库，展示到角色权限配置中，由系统统一授权和校验。

第一版不做：

- 插件隔离沙箱
- 微前端
- 插件市场
- 插件依赖冲突强隔离
- 外部 Jar 自动 Spring Bean 扫描

## 2. 技术选型

第一版采用：

- `Java 21`
- `Spring Boot 3.5.x`
- `Spring MVC`
- `Thymeleaf`
- `thymeleaf-layout-dialect`
- `Spring Security`
- `Spring Modulith`
- `MyBatis-Plus`
- `Flyway`

用途边界：

- `Spring Boot` 作为应用底座。
- `Spring MVC + Thymeleaf` 支撑后台服务端渲染和 HTML Hook。
- `thymeleaf-layout-dialect` 支撑系统 layout 和插件 layout 复用。
- `Spring Security` 负责登录、登出、后台路径保护和权限校验。
- `Spring Modulith` 约束主系统内部模块边界，不承担插件运行时。
- `MyBatis-Plus` 支撑平台表和业务表访问。
- `MapperUtils` 统一平台单表新增、删除、修改、查询、分页、过滤和排序。
- `Flyway` 统一管理系统和插件数据库迁移。

## 3. 架构与模块

第一版采用 Maven 多模块结构。

### 3.1 模块职责

`jnimble-kernel`：

- 插件发现与加载
- 插件生命周期
- Hook 管理
- 路由与资源挂载
- 插件迁移调度
- 插件国际化资源装配
- 插件热插拔运行时注册与回收

`jnimble-platform`：

- 用户
- 角色
- 权限
- 登录
- 审计
- 系统配置
- 插件状态管理
- 插件权限目录与角色授权
- 平台 Entity、Mapper 和 `MapperUtils` 数据访问规范

`jnimble-admin-shell`：

- 后台整体布局
- 页面模板
- 通用样式与资源
- HTML Hook 位点
- 页面渲染
- 插件管理页面

`jnimble-plugin-sdk`：

- 插件启动协议
- 插件停止协议
- Hook 基础协议
- 路由与资源注册协议
- 插件描述基础类型

`jnimble-starter`：

- 应用启动入口
- 模块装配
- 默认配置

业务插件：

- 业务页面
- 控制器与服务
- Hook 贡献
- 静态资源
- 国际化资源
- 数据库迁移脚本

### 3.2 依赖关系

```text
jnimble-bom
  └── dependencyManagement only

jnimble-plugin-sdk
  └── minimal plugin protocol

jnimble-kernel
  └── depends on jnimble-plugin-sdk

jnimble-platform
  └── depends on jnimble-kernel、jnimble-plugin-sdk

jnimble-admin-shell
  └── depends on jnimble-kernel、jnimble-platform、jnimble-plugin-sdk

jnimble-starter
  └── depends on jnimble-admin-shell、jnimble-platform、jnimble-kernel

business plugins
  └── may depend on jnimble-plugin-sdk、jnimble-kernel、jnimble-platform、jnimble-admin-shell
```

规则：

- 插件可以依赖主框架模块，可以使用主框架公开模型、Service、DTO、工具类和 Spring Bean。
- `jnimble-plugin-sdk` 不封装所有系统能力，只保留插件启动、Hook、路由、资源等最小协议。
- 插件操作系统数据时优先使用主框架已有 Service。
- 插件可以使用系统 Entity 和 Mapper，但必须承担主框架升级后的适配成本。
- 插件不得覆盖系统核心 Bean，除非该 Bean 明确是扩展点。
- 插件不得绕过系统权限、审计、事务和数据一致性规则。
- `jnimble-starter` 只做装配入口，不承载核心业务逻辑。

## 4. 页面与路由

### 4.1 模板与 Layout

模板引擎使用 `Thymeleaf`。

页面支持两种 layout 模式：

- 默认复用系统 layout：插件页面使用系统顶栏、侧栏、面包屑、通用样式、权限与国际化上下文。
- 插件自定义 layout：适合独立工作台或特殊业务页面，仍运行在主系统内。

页面约束：

- 权限判断由系统统一处理。
- 静态资源路径按插件隔离。
- 国际化通过系统统一消息解析。
- 页面扩展优先使用 Hook，不直接修改系统模板。

### 4.2 路由与资源命名空间

第一版固定命名空间：

- 系统后台页面：`/admin/**`
- 插件后台页面：`/admin/plugins/{pluginId}/**`
- 系统静态资源：`/assets/system/**`
- 插件静态资源：`/assets/plugins/{pluginId}/**`
- 插件模板路径：`templates/plugin/{pluginId}/**`
- 插件资源路径：`static/plugin/{pluginId}/**`

规则：

- `pluginId` 必须和插件描述文件中的 `id` 一致。
- 插件不得挂载 `/admin/login`、`/admin/logout`、`/admin/error` 等系统保留路径。
- 插件不得挂载根路径、系统认证路径、系统资源路径。
- URL 生成优先使用系统 URL 构建 API。
- 发现路径冲突时，冲突插件不得启用，并记录失败原因。

## 5. Hook 机制

Hook 是插件接入后台页面和平台流程的统一扩展机制。

### 5.1 Hook 类型

`Contribution Hook`：向页面贡献内容。

第一版保留 Hook 名称：

- `admin.layout.sidebar`
- `admin.layout.sidebar.start`
- `admin.layout.sidebar.end`
- `admin.layout.sidebar.general.before`
- `admin.layout.sidebar.general.start`
- `admin.layout.sidebar.general.end`
- `admin.layout.sidebar.home.before`
- `admin.layout.sidebar.home.after`
- `admin.layout.sidebar.plugins.before`
- `admin.layout.sidebar.plugins.after`
- `admin.layout.sidebar.access.before`
- `admin.layout.sidebar.access.start`
- `admin.layout.sidebar.access.end`
- `admin.layout.sidebar.roles.before`
- `admin.layout.sidebar.roles.after`
- `admin.layout.sidebar.users.before`
- `admin.layout.sidebar.users.after`
- `admin.layout.sidebar.trace.before`
- `admin.layout.sidebar.trace.start`
- `admin.layout.sidebar.trace.end`
- `admin.layout.sidebar.audit.before`
- `admin.layout.sidebar.audit.after`
- `admin.layout.topbar`
- `admin.dashboard.widgets`
- `admin.page.actions`
- `admin.list.toolbar`
- `admin.detail.tabs`
- `admin.form.sections`

`Action Hook`：介入系统流程。

第一版保留 Hook 名称：

- `plugin.loaded`
- `plugin.enabled`
- `plugin.disabled`
- `auth.login.success`
- `auth.logout.success`
- `entity.save.before`
- `entity.save.after`
- `file.upload.after`

### 5.2 HTML Hook 写法

标签式 Hook：

```html
<jn:hook name="admin.layout.sidebar" />
<jn:hook name="admin.order.detail.actions" />
```

属性式 Hook：

```html
<div jn:hook="admin.order.detail.base" jn:mode="append">
  <div>系统默认详情内容</div>
</div>
```

行级 Hook：

```html
<tr th:each="row : ${rows}">
  <td th:text="${row.orderNo}"></td>
  <td>
    <jn:hook name="admin.order.list.row.actions" jn:row="${row}" />
  </td>
</tr>
```

### 5.3 Hook 模式

页面 Hook 支持：

- `append`：在默认内容后插入插件内容。
- `prepend`：在默认内容前插入插件内容。
- `replace`：用插件内容替换默认内容。
- `remove`：移除默认内容。

核心布局 Hook 默认只开放 `append` 和 `prepend`。允许 `replace` 或 `remove` 的 Hook 点必须显式声明。

### 5.4 Hook 上下文

页面 Hook 默认继承当前模板上下文中的全部变量和对象。

规则：

- 页面级 Hook 可访问当前页面 Model 中的全部变量。
- 行级 Hook 默认可访问当前 `row`。
- 详情页或表单页 Hook 默认可访问当前主对象。
- `jn:with` 用于补充、重命名或覆盖变量，不做默认收缩。

系统页面需要保持高频变量名稳定：

- `row`
- `entity`
- `form`
- `page`

### 5.5 Hook 输出

插件不直接返回原始 HTML 字符串，返回可渲染的模板片段定义。

```java
public record HookViewContribution(
    String view,
    Map<String, Object> model,
    int order,
    String permission,
    String activeWhen
) {}
```

字段含义：

- `view`：插件模板片段路径。
- `model`：传入模板的数据。
- `order`：渲染顺序。
- `permission`：权限表达式。
- `activeWhen`：激活规则。

### 5.6 排序、冲突与异常

排序规则：

1. 按 `order` 升序。
2. `order` 相同时按 `pluginId` 字典序。
3. 仍相同时按注册顺序。

冲突规则：

- `append`、`prepend` 按排序结果渲染。
- 同一 Hook 点只允许一个最终生效的 `replace` 或 `remove`。
- 多个插件同时声明 `replace` 或 `remove` 时，排序第一的贡献生效，其余贡献不渲染。
- Hook 冲突需要记录告警，包含 Hook 名称、冲突插件、最终生效插件。

异常策略：

```yaml
jnimble:
  hooks:
    fail-fast: false
```

- `fail-fast=true`：Hook 失败直接抛出异常，适合开发和测试环境。
- `fail-fast=false`：跳过失败贡献，记录错误日志，并在插件状态中记录最近一次 Hook 错误。
- 权限不通过或 `activeWhen` 不满足时不渲染，不视为错误。
- 后台页面 Hook 由系统服务统一渲染，插件模板输出视为受信 HTML，不再由页面模板直接 `th:replace` 插件片段。

### 5.7 Action Hook 规则

- Action Hook 默认同步执行。
- `before` 类型 Action Hook 可以抛出业务异常阻止流程继续。
- `after` 类型 Action Hook 不应修改已完成的核心结果。
- 多个 Action Hook 使用和页面 Hook 相同的排序规则。
- Action Hook 默认在当前事务内执行，除非 Hook 点明确声明为事务外事件。
- 审计、通知等非关键能力优先使用事务提交后的 Hook 点。

## 6. 插件机制

### 6.1 交付、安装与发现

生产环境：

- 插件以 Jar 形式放入 `./plugins`。
- 插件目录可通过 `jnimble.plugins.dir` 配置。
- 只扫描 `*.jar`。
- Jar 内必须包含 `META-INF/jnimble-plugin.json`。
- 支持运行时上传或复制 Jar 后安装。
- 启动时可通过 `jnimble.plugins.directory-scan-enabled=true` 扫描插件目录中的 Jar。
- 管理端上传 Jar 时先保存插件包，再解析描述文件并写入插件状态。
- 已安装插件不能通过普通上传覆盖，需通过显式替换或重载流程处理。
- 同一 `pluginId` 和 `version` 的 Jar 已存在时，系统使用唯一文件名保存新包，避免直接覆盖旧包。

开发环境：

- 插件以 Maven 模块加入 classpath。
- 通过 `jnimble.plugins.dev-classpath-enabled=true` 启用 classpath 插件发现。
- 启动时扫描 `classpath*:META-INF/jnimble-plugin.json`，发现后自动安装插件。
- 启动主应用时无需手工打包插件 Jar。

冲突规则：

- 同一个 `pluginId` 只能出现一次。
- 生产 Jar 和开发 classpath 同时发现同一 `pluginId` 时视为冲突。
- 冲突插件全部跳过并记录错误。

运行时动作：

- `install`：解析 Jar、校验描述文件、写入插件状态，不启用业务能力。
- `replace`：替换已安装但未启用的插件包；插件 ID 必须一致；替换成功后删除旧包。
- `enable`：执行迁移，注册 Hook、路由、资源、国际化和权限。
- `disable`：取消 Hook、路由、资源、国际化运行时注册，保留插件状态和权限授权关系。
- `uninstall`：禁用插件并移除插件状态；默认不删除插件业务表和权限授权历史。
- `reload`：等价于先 `disable` 再 `enable`，只重启当前已安装包，不替换 Jar 文件。

热插拔边界：

- 启用和禁用插件不需要重启主应用。
- 插件 Jar 文件替换必须通过 `replace`，启用中的插件不能替换。
- 替换后的 Jar 需要重新 `enable` 或手动 `reload` 当前包。
- 正在处理中的插件请求允许完成；禁用动作阻止新请求进入插件路由。
- 热插拔失败时，插件状态写入失败原因，已注册的运行时贡献必须回滚。

### 6.2 插件描述文件

每个插件至少包含：

```text
META-INF/jnimble-plugin.json
```

示例：

```json
{
  "schemaVersion": "1.0",
  "id": "crm",
  "name": "CRM Plugin",
  "nameKey": "plugin.crm.name",
  "description": "Customer management plugin",
  "descriptionKey": "plugin.crm.description",
  "version": "0.1.0",
  "platformVersion": "0.1.x",
  "author": "JNimble Team",
  "website": "https://example.com",
  "bootClass": "com.jnimble.plugin.crm.CrmPluginBoot",
  "i18n": {
    "basename": "i18n/messages"
  },
  "permissions": [
    {
      "code": "crm.customer.view",
      "name": "View customers",
      "nameKey": "permission.crm.customer.view",
      "description": "Allows reading customer records",
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

字段规则：

- `schemaVersion`：必填，第一版固定为 `1.0`。
- `id`：必填，全局唯一，只允许小写字母、数字和中划线，建议正则为 `[a-z][a-z0-9-]*`。
- `name`：必填，作为国际化缺失时的回退展示名。
- `nameKey`：选填，展示时优先使用国际化 key。
- `description`：选填，作为国际化缺失时的回退描述。
- `descriptionKey`：选填，展示时优先使用国际化 key。
- `version`：必填，使用语义化版本。
- `platformVersion`：必填，声明插件适配的主框架版本，第一版支持精确版本、`0.1.x`、`>= 0.1.0` 和 `*`。
- `bootClass`：必填，必须实现 `PluginBoot`。
- `i18n.basename`：选填，声明插件国际化资源 basename。
- `permissions`：选填，声明插件权限点。
- `migration`：选填，声明插件数据库迁移配置。

校验规则：

- 描述文件缺失或格式错误时，插件不得启用。
- `id` 重复时，冲突插件均不得启用。
- `platformVersion` 不匹配当前主框架版本时，插件不得启用。
- `bootClass` 不存在或未实现 `PluginBoot` 时，插件不得启用。
- 第一版提供 `jnimble-plugin.schema.json`。

### 6.3 插件目录内容

插件建议结构：

```text
META-INF/jnimble-plugin.json
templates/
static/
db/migration/
i18n/
Java classes
application-plugin.yml
```

### 6.4 插件启动

插件启动类：

```java
public class CrmPluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        context.hooks().register("admin.layout.sidebar", new CrmSidebarHook());
        context.hooks().register("admin.detail.tabs", new CrmCustomerTabHook());
    }
}
```

启动规则：

- `bootClass` 第一版通过反射创建实例。
- `bootClass` 必须有无参构造方法。
- 插件初始化入口统一为 `PluginBoot.boot(PluginContext context)`。
- 插件停止入口统一为 `PluginBoot.stop(PluginContext context)`，默认空实现。
- 插件可以通过 `PluginContext` 获取 Spring `ApplicationContext` 或 BeanProvider。
- 插件可以注入或获取主框架提供的用户、权限、审计、配置等 Service。

控制器接入：

- 开发模式下，插件模块可以参与 Spring 扫描并注册 `@Controller`、`@Service`、`@Component`。
- 生产 Jar 模式第一版优先通过 `PluginContext.routes()` 显式注册控制器或处理器。
- 后续版本再扩展 Jar 插件 Spring Bean 注册能力。

### 6.5 生命周期

安装顺序：

1. discover
2. parse descriptor
3. validate descriptor
4. create classloader
5. write plugin state
6. mark installed

启用顺序：

1. validate installed plugin
2. run plugin migrations
3. instantiate boot class
4. register permissions
5. register i18n resources
6. register hooks
7. mount routes
8. mount assets
9. call boot
10. mark enabled

禁用顺序：

1. stop accepting new plugin requests
2. call stop
3. unmount routes
4. unmount assets
5. unregister hooks
6. unregister i18n resources
7. keep permissions and role grants
8. close classloader when possible
9. mark disabled

### 6.6 ClassLoader

生产 Jar 插件使用独立 `URLClassLoader`。

第一版规则：

- ClassLoader 使用 parent-first。
- `org.springframework.*`、`jakarta.*`、`com.jnimble.*` 优先从主应用加载。
- 插件不得覆盖主应用中的框架类。
- 插件第三方依赖如果和主应用冲突，由插件 shading 或由平台统一提供。

### 6.7 插件状态

Platform 持久化插件状态。

核心字段：

```text
plugin_id
name
version
source
artifact_path
enabled
status
installed_at
last_started_at
last_stopped_at
last_error
descriptor_json
descriptor_hash
created_at
updated_at
```

使用 MyBatis-Plus 将状态持久化到 `jnimble_plugin_state` 表。

状态：

- `DISCOVERED`：已发现但尚未启用。
- `INSTALLED`：已安装但未启用。
- `ENABLED`：已启用并完成加载。
- `DISABLED`：已禁用。
- `FAILED`：启动或注册失败。
- `MIGRATION_FAILED`：迁移失败。
- `INCOMPATIBLE`：插件声明的主框架版本不匹配。
- `UNINSTALLED`：已卸载。

启用规则：

- 已存在数据库状态时，以数据库中的 `enabled` 为准。
- 启动恢复默认开启：`jnimble.plugins.restore-enabled=true`。
- 启动恢复先读取持久化状态，再扫描插件目录，最后执行 classpath 开发插件发现。
- 已恢复或已安装的插件，目录扫描和 classpath 发现阶段会跳过，避免重复安装。
- 开发模式下，新发现插件默认启用。
- 生产模式下，新安装插件默认不启用，除非配置 `jnimble.plugins.auto-enable=true` 或显式列入 `jnimble.plugins.enabled`。
- `jnimble.plugins.disabled` 中列出的插件始终禁用，优先级最高。

### 6.8 失败策略

```yaml
jnimble:
  plugins:
    fail-fast: false
```

规则：

- `fail-fast=true`：任何插件加载失败都阻止应用启动，适合开发和 CI。
- `fail-fast=false`：失败插件跳过加载，平台继续启动。
- 描述文件错误、主框架版本不匹配、迁移失败、Boot 执行失败都记录 `last_error`。
- 插件失败后，不注册它的 Hook、路由、静态资源、国际化和权限。
- 插件部分注册成功后如果后续失败，系统回滚该插件已注册的运行时贡献。
- 热插拔动作失败时，不影响其他已启用插件。

### 6.9 插件配置

插件可以提供默认配置：

```text
application-plugin.yml
```

规则：

- 插件默认配置只作为默认值，不覆盖主应用配置。
- 主应用配置优先级高于插件默认配置。
- 插件配置 key 必须以插件 id 为前缀，例如 `crm.customer.default-page-size`。
- 插件不得通过配置修改系统认证、数据源、Flyway 主配置等全局关键配置。

## 7. 数据库迁移

数据库迁移统一使用 Flyway。

### 7.1 系统级迁移

系统迁移由 Spring Boot 自动装配的 Flyway 执行。

目录：

```text
jnimble-starter/src/main/resources/db/migration/system
```

配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration/system
    table: flyway_schema_history
```

### 7.2 插件级迁移

插件迁移由系统在插件加载流程中执行。每个插件使用独立 Flyway history table。

示例：

- 系统：`flyway_schema_history`
- 插件 `crm`：`flyway_schema_history_crm`
- 插件 `oa`：`flyway_schema_history_oa`

插件迁移目录：

```text
src/main/resources/db/migration/plugin/<pluginId>/
```

执行顺序：

1. 系统迁移先执行。
2. 插件启用时执行对应插件迁移。
3. 启动时对已启用插件做迁移兜底。
4. 插件迁移成功后再注册页面、路由、Hook、资源、权限。

执行边界：

- 应用提供 `DataSource` 时，插件启用流程使用 Flyway 自动执行插件迁移。
- Jar 插件迁移使用插件 Jar 的 ClassLoader，确保能读取插件包内的迁移脚本。

默认值：

- `enabled`：默认 `false`
- `location`：默认 `classpath:db/migration/plugin/<pluginId>`
- `table`：默认 `flyway_schema_history_<pluginIdNormalized>`
- `baselineOnMigrate`：默认 `true`
- `failOnError`：默认 `true`

`pluginIdNormalized` 将中划线转换为下划线。

失败规则：

- 插件迁移失败时，该插件不得继续注册 Hook、路由、资源和权限。
- 插件状态标记为 `MIGRATION_FAILED`。
- `failOnError=true` 时，是否阻止应用启动由 `jnimble.plugins.fail-fast` 决定。
- 禁用插件不执行迁移。
- 卸载插件默认不回滚数据库迁移，不删除插件业务表。

## 8. 国际化

国际化资源按模块归属：

- 系统文案归系统模块维护。
- 插件文案归插件模块维护。

系统资源目录：

```text
jnimble-admin-shell/src/main/resources/i18n/
```

插件资源目录：

```text
src/main/resources/i18n/
```

插件描述文件声明：

```json
"i18n": {
  "basename": "i18n/messages"
}
```

规则：

- 系统加载插件时，将插件 `basename` 装配进统一消息解析体系。
- 插件启用时注册国际化资源，禁用或卸载时注销，避免停用插件继续污染消息空间。
- 插件元数据展示时优先使用 `nameKey`、`descriptionKey`，解析失败再回退 `name`、`description`。
- 插件权限展示时优先使用 `nameKey`，解析失败再回退权限名称或权限编码。
- 插件 Thymeleaf 模板可以直接使用 `#{message.key}` 解析自身资源。
- Hook 贡献中的标题、标签等文案支持国际化 key，由系统在渲染阶段解析。
- 系统 key 使用 `system.`、`admin.`、`auth.` 等前缀。
- 插件 key 使用 `plugin.<pluginId>.`、`permission.<pluginId>.` 等前缀。
- 插件不得复用系统 key 覆盖系统文案。
- 多个插件出现相同 key 时，系统记录告警，并按插件加载顺序使用第一个。

## 9. 权限与系统能力

### 9.1 权限规则

插件权限使用统一表达式。

示例：

- `crm.customer.view`
- `crm.customer.edit`
- `crm.customer.delete`

规则：

- 插件权限必须以 `pluginId` 作为前缀。
- 推荐格式：`<pluginId>.<resource>.<action>`。
- 系统权限保留 `system.*`、`admin.*`、`auth.*` 前缀。
- 系统后台自身权限使用 `system` 作为权限分组，并与插件权限一起显示在角色权限配置界面。
- 默认管理员角色启动时自动获得全部系统后台权限，避免首次登录后无法进入后台管理功能。
- 插件启用时注册或更新权限点到系统权限目录。
- 插件权限必须显示在角色权限配置界面中，由系统统一授权。
- 插件权限按插件分组展示，权限名称优先使用国际化 key。
- 插件禁用时不删除权限记录和角色授权关系，只标记为不可用或在授权界面置灰。
- 插件重新启用时复用原权限记录和已有授权关系。
- Hook、菜单、Controller 使用同一套权限表达式。
- Controller 层必须做服务端权限校验，不能只依赖页面隐藏。
- 系统后台 Controller 按查看和管理拆分权限；写操作必须使用管理权限，审计日志只提供查看权限。
- 插件卸载默认不删除权限记录，避免历史授权和审计记录丢失。

状态存储：

- 角色、插件权限、角色权限授权和用户角色授权写入数据库。
- 第一版访问控制表为 `jnimble_role`、`jnimble_permission`、`jnimble_role_permission`、`jnimble_subject_role`。
- `jnimble_subject_role.subject_id` 第一版使用登录用户名；接入用户表后改为用户主键或稳定账号 ID。
- 禁用插件时，插件权限和已有关联授权标记为不可用，不删除授权记录。
- 重新启用插件时，权限恢复为可用，原角色授权继续生效。

菜单规则：

- 菜单通过侧边栏 Hook 贡献；插件可按目标位置选择 `admin.layout.sidebar.*` Hook。
- `admin.layout.sidebar` 保留为兼容入口，默认位于系统菜单末尾。
- 插件菜单允许混入系统菜单；是否分组由插件选择的 Hook 和输出片段决定。
- 菜单项必须声明权限。
- 权限不通过时不渲染菜单项。
- 系统后台侧边栏也按 `system.*.view` 权限渲染。

### 9.2 用户账号

第一版账号规则：

- 登录账号读取 `jnimble_user` 表。
- 数据库账号模式启动时会自动创建默认管理员账号；已存在同名账号时不覆盖密码。
- 登录主体第一版使用 `username`，并作为 `jnimble_subject_role.subject_id`。
- 用户状态为 `DISABLED` 时禁止登录。
- 密码使用 Spring Security `PasswordEncoder` 编码后存储到 `password_hash`。
- 第一版提供账号存储、登录读取、用户列表、创建用户、启用禁用、重置密码和角色分配。

### 9.3 审计日志

审计日志由系统统一记录和展示。

第一版记录范围：

- 插件描述符安装
- 插件 Jar 上传安装
- 插件 Jar 替换
- 插件启用、停用、卸载、重载
- 用户创建、资料修改、启用、禁用、重置密码
- 用户角色分配
- 角色权限分配

核心字段：

```text
id
actor
action
target_type
target_id
outcome
message
occurred_at
```

存储规则：

- 审计日志写入 `jnimble_audit_log`。
- 后台通过 `/admin/audit` 查看最近审计日志。
- 审计写入失败不得阻断原业务操作；系统应记录告警并继续返回业务结果。

### 9.4 插件调用主框架能力

插件可以直接调用主框架能力：

- 当前用户
- 权限判断
- 国际化取值
- 系统配置读取
- 文件上传
- 审计日志
- URL 构建
- Hook 注册
- 用户、角色、权限等平台 Service
- 平台 Entity、DTO、枚举和工具类

典型用法：

```java
public class CrmCustomerService {

    private final UserService userService;
    private final AuditService auditService;

    public CrmCustomerService(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    public List<UserEntity> findAssignableUsers() {
        return userService.findEnabledUsers();
    }
}
```

兼容规则：

- 插件必须声明 `platformVersion`。
- 主框架只根据 `platformVersion` 判断插件是否允许加载。
- 插件使用主框架模型或方法后，需要跟随主框架版本变化适配。
- 主框架可以在版本升级中调整公开模型和方法，不承诺旧插件自动兼容。
- 核心基础设施类和 Bean 应在文档中标记为不建议插件直接调用或覆盖。

## 10. 项目结构

```text
JNimble/
├── pom.xml
├── README.md
├── docs/
│   └── framework-design.md
├── jnimble-bom/
├── jnimble-plugin-sdk/
├── jnimble-kernel/
│   └── src/main/java/com/jnimble/kernel/
│       ├── plugin/
│       ├── hook/
│       ├── route/
│       ├── resource/
│       └── thymeleaf/
├── jnimble-platform/
│   └── src/main/java/com/jnimble/platform/
│       ├── auth/
│       ├── permission/
│       ├── audit/
│       ├── plugin/
│       └── persistence/
│           ├── crud/
│           ├── entity/
│           └── mapper/
├── jnimble-admin-shell/
│   └── src/main/
│       ├── java/com/jnimble/admin/
│       └── resources/
│           ├── i18n/
│           ├── templates/
│           └── static/
├── jnimble-starter/
│   └── src/main/
│       ├── java/com/jnimble/starter/JNimbleApplication.java
│       └── resources/
│           ├── application.yml
│           ├── jnimble-plugin.schema.json
│           └── db/migration/system/
├── plugins/
│   ├── pom.xml
│   └── jnimble-plugin-demo-crm/
│       └── src/main/
│           ├── java/com/jnimble/plugin/crm/
│           └── resources/
│               ├── META-INF/jnimble-plugin.json
│               ├── i18n/
│               ├── templates/plugin/crm/
│               ├── static/plugin/crm/
│               └── db/migration/plugin/crm/
└── data/plugins/                       ← 生产外置插件 Jar 目录
```

## 11. 第一版 MVP

第一版交付范围：

1. Maven 多模块骨架
2. 可启动的 `jnimble-starter`
3. 登录、登出、后台路径保护
4. 后台基础 layout
5. 插件描述文件解析与校验
6. classpath 开发模式插件发现
7. 生产 Jar 插件安装、启用、禁用、卸载、重载
8. 插件状态持久化和失败记录
9. `PluginBoot` 启停调用和 `PluginContext`
10. `HookManager` 与 Thymeleaf `HookDialect`
11. `admin.layout.sidebar` 闭环
12. 插件路由、模板、静态资源访问
13. 系统级和插件级 Flyway 迁移
14. 插件权限声明、注册和角色授权界面展示
15. 审计日志记录和后台查询页面
16. 系统与插件国际化资源装配
17. 插件管理页面
18. demo CRM 插件

验收标准：

- 空系统可以启动并进入登录页。
- 登录后可以进入后台首页。
- demo CRM 插件在开发模式下可以被发现、启用并记录状态。
- demo CRM 插件 Jar 可以在运行时安装、启用、禁用、卸载和重载。
- demo CRM 插件可以贡献 Sidebar 菜单。
- demo CRM 页面可以通过 `/admin/plugins/crm/**` 访问。
- demo CRM 客户页可以通过 `/admin/plugins/crm/customers` 访问。
- demo CRM 静态资源可以通过 `/assets/plugins/crm/**` 访问。
- demo CRM 权限能注册到系统权限表，并显示在角色权限配置中。
- 系统后台权限能以 `system` 分组显示在角色权限配置中。
- 默认管理员角色启动后自动拥有系统后台查看和管理权限。
- 角色授权后，demo CRM 权限能控制菜单、Hook 和页面访问。
- 角色授权后，系统后台的插件、角色、用户和审计页面能按权限控制访问。
- 禁用 demo CRM 插件后，其菜单、路由、资源和 Hook 立即失效，角色授权关系保留。
- 重新启用 demo CRM 插件后，原角色授权关系继续生效。
- 系统迁移和插件迁移分别写入独立 Flyway history table。
- 有 `DataSource` 时，插件启用前会自动执行插件迁移；无数据库开发模式不会阻断插件启用。
- 插件描述文件错误时，系统能记录失败原因，并按配置决定是否继续启动。
- Hook 渲染失败时，系统能按 `jnimble.hooks.fail-fast` 执行对应策略。
- 插件、用户和角色权限的后台写操作会产生审计日志，并可在审计日志页面查看。

## 12. 风险与控制点

| 风险 | 控制方式 |
| --- | --- |
| Hook 默认暴露模板变量，插件依赖页面变量命名 | 固定高频变量名；页面重构时保留兼容上下文 |
| 插件页面风格不一致 | 默认复用系统 layout；统一权限、国际化和资源路径规范 |
| 插件直接使用主框架模型导致适配成本上升 | 强制声明 `platformVersion`；主框架升级说明列出模型和 Service 变化；插件随版本重新测试 |
| 插件加载失败影响启动 | 提供 `jnimble.plugins.fail-fast`；失败插件回滚运行时贡献；记录状态和错误 |
| 热插拔卸载不完整 | 插件所有 Hook、路由、资源、i18n 注册都要有 registration handle；禁用时按 handle 回收 |
| 插件请求和禁用并发 | 禁用时先阻止新请求进入插件路由，允许已进入请求完成或超时 |
| 路由和资源冲突 | 固定命名空间；启动时检测冲突；禁止挂载系统保留路径 |
| 插件依赖冲突 | parent-first 加载；优先使用主框架公共依赖；冲突依赖由插件 shading |
| 权限只做页面隐藏 | Controller 层统一做服务端权限校验 |
| 插件权限和角色授权丢失 | 禁用和卸载默认保留权限记录和授权关系，权限状态标记为不可用 |
| 插件迁移半成功 | 插件迁移成功后才注册 Hook、路由、资源和权限 |

## 13. 下一步

实施顺序：

1. 建立 Maven 多模块骨架。
2. 建立 `jnimble-plugin-sdk` 最小插件协议。
3. 定义 `jnimble-plugin.json` 字段规范和 JSON Schema。
4. 实现插件发现、描述文件校验、状态持久化。
5. 实现插件安装、启用、禁用、卸载、重载的运行时状态机。
6. 实现 `HookManager`、Hook 排序、冲突和失败策略。
7. 实现 Thymeleaf `HookDialect`。
8. 打通 `admin.layout.sidebar`。
9. 实现插件路由和静态资源命名空间。
10. 接入系统级和插件级 Flyway 迁移。
11. 实现插件权限注册、角色权限配置展示和服务端权限校验。
12. 补齐 `PluginContext` 对主框架 Bean 和上下文的访问能力。
13. 实现插件管理页面。
14. 实现 demo CRM 插件。
15. 补充 README、启动说明和插件开发指南。
