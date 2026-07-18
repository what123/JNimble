# 框架与点餐插件分离设计

## 目标

将 JNimble 拆分为两个独立开源项目：

| 项目 | 说明 | 仓库 |
|------|------|------|
| **JNimble** | 插件化后台管理框架 | 当前仓库 |
| **JNimbleOrder** | 点餐业务插件集 | 新建独立仓库 |

## 框架保留模块

```
JNimble/
├── jnimble-bom              (BOM)
├── jnimble-plugin-sdk       (插件 SDK)
├── jnimble-kernel           (运行时内核)
├── jnimble-platform         (平台层)
├── jnimble-admin-shell      (管理后台壳)
├── jnimble-starter          (启动器)
└── plugins/
    └── jnimble-plugin-demo-crm          (示例 CRM 插件)
```

## 点餐插件项目结构

```
JNimbleOrder/
├── pom.xml                  (独立根 pom, parent 指向 jnimble-parent)
├── jnimble-plugin-menu-manager/
├── jnimble-plugin-order-core/
├── jnimble-plugin-order-table/
├── jnimble-plugin-payment/
├── jnimble-plugin-printer-core/
└── jnimble-plugin-printer-feie/
```

## 构建方式

### 框架独立构建

```bash
cd JNimble
mvn install -DskipTests
```

框架不引用任何点餐插件，可独立编译和运行。

### 点餐插件独立构建

```bash
# 先安装框架到本地 Maven 仓库
cd JNimble && mvn install -DskipTests

# 构建点餐插件
cd JNimbleOrder && mvn install -DskipTests
```

点餐插件通过 Maven 依引用框架的 SDK 和平台模块。

### 运行时集成

在 `jnimble-starter` 的 `dev-plugins` profile 中，将点餐插件以 `runtime` scope 引入即可在开发环境运行。生产环境通过插件目录热部署加载。
