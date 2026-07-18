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
├── jnimble-license-sdk      (授权 SDK — 给插件开发者用)
├── jnimble-kernel           (运行时内核)
├── jnimble-license-core     (授权存储 + 管理界面后端)
├── jnimble-platform         (平台层)
├── jnimble-admin-shell      (管理后台壳)
├── jnimble-starter          (启动器)
└── plugins/
    ├── jnimble-plugin-demo-crm          (示例 CRM 插件，演示授权自检)
    └── jnimble-plugin-license-issuer    (授权码生成插件)
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

## 授权机制设计（核心）

### 职责边界

> **授权码由框架保存；授权验证由插件通过 SDK 完成；框架管理界面通过调用插件注册的 Hook 获取验证结果。**

| 角色 | 职责 | 不做的事 |
|------|------|----------|
| 框架 | 提供授权码填写界面、按 pluginId 存储授权码、展示插件上报的授权状态、维护 Hook 注册表 | 不持有插件公钥、不执行验签、不在 enable() 时统一拦截 |
| license-sdk | 提供静态校验方法、Ed25519 验签、6 小时缓存、机器码/产品码/有效期校验 | 不存储授权码、不依赖框架 |
| 插件 | 持有自己的公钥、注册验证 Hook、决定在哪些功能调用 `PluginLicense.requireValid()` | 不依赖框架的授权实现 |

### 调用链

**界面链（用户填写授权码）**：
```
用户填授权码 → PluginLicenseController.activate()
  → backend.saveToken(pluginId, token)       // 框架只存储原始授权码
  → hookRegistry.verify(pluginId)            // 调插件注册的 Hook
  → 插件 Hook 内部调 PluginLicense.requireValid()
  → SDK 用插件公钥验签 + 6h 缓存
  → 返回 PluginLicenseView 给界面显示状态
```

**插件运行链（插件业务方法自检）**：
```
插件业务方法 → PluginLicense.requireValid(pluginId, publicKey)
  → SDK 检查缓存（6h 内有效直接返回）
  → 缓存过期 → backend.loadToken(pluginId) → 验签 → 更新缓存
  → 无效抛 LicenseException，拦截业务执行
```

### 防破解设计

框架不再统一拦截授权，`PluginLicenseInterceptor` 移除。即使破解框架的插件启用逻辑，也无法跳过插件业务方法里的 `PluginLicense.requireValid(...)`。每个插件用自己的公钥，破解一个插件不影响其他插件。

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

## 插件开发者使用授权 SDK

### Maven 依赖

```xml
<dependency>
    <groupId>com.jnimble</groupId>
    <artifactId>jnimble-license-sdk</artifactId>
    <scope>provided</scope>
</dependency>
```

### 插件 boot() 注册 Hook + 自检

```java
@Override
public void boot(PluginContext context) {
    String pluginId = context.descriptor().id();
    PluginLicenseDescriptor policy = context.descriptor().license();
    PublicKey publicKey = loadPluginPublicKey();
    PluginLicenseVerifierRegistry registry = context.bean(PluginLicenseVerifierRegistry.class);

    context.registerHandle(registry.register(
        pluginId,
        force -> PluginLicense.verify(
            pluginId,
            policy.issuer(),
            policy.productCode(),
            context.descriptor().version(),
            publicKey,
            force)));

    PluginLicense.requireValid(
        pluginId,
        policy.issuer(),
        policy.productCode(),
        context.descriptor().version(),
        publicKey);
}
```

### 业务方法自检

```java
public OrderResult createOrder(CreateOrderCommand command) {
    PluginLicense.requireValid(
        "order-core",
        "order-vendor",
        "order-core",
        pluginVersion,
        orderCorePublicKey);
    return orderService.create(command);
}
```

## 6 小时缓存规则

- 按 `pluginId + issuer + productCode + pluginVersion + 公钥指纹` 组合隔离缓存
- 有效结果缓存最多 6 小时
- 缓存截止时间 = `min(当前时间 + 6h, 授权码过期时间)`
- 授权码变化时立即失效
- 点击"重新检测"时强制跳过缓存
- 无效结果不缓存