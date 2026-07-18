# 插件独立授权设计

## 1. 目标与边界

授权能力采用插件主动接入模式：

- 框架提供授权码填写界面、授权码存储、机器码和状态展示。
- 插件持有自己的公钥，并决定在 `boot()`、业务方法、定时任务或消息入口中何时校验。
- `jnimble-license-sdk` 提供静态验签方法、声明校验、有效结果缓存和授权异常。
- 框架不持有插件公钥，不统一执行验签，不统一拦截插件启用或 HTTP 请求。
- 未声明 `license.required=true` 的插件不显示授权入口，也不参与授权流程。

核心原则：

> 授权码由框架保存；授权验证由插件通过 SDK 完成；管理界面通过插件注册的验证 Hook 获取结果。

## 2. 密码学方案

授权码使用 Ed25519 数字签名和紧凑 JWS 格式：

```
base64url(header).base64url(payload).base64url(signature)
```

- 授权签发端保存私钥并签名。
- 每个商业插件持有自己的公钥。
- 私钥不得进入插件、框架、数据库或客户配置。
- 一个插件的公钥或校验代码被修改，不会直接影响其他插件。

SDK 校验以下内容：

- JWS 类型和算法；
- Ed25519 签名；
- issuer；
- productCode；
- pluginVersion 或 `*`；
- machineCode；
- issuedAt、notBefore、expiresAt。

## 3. 机器码

框架根据部署域名和插件产品码生成机器码：

```text
canonicalDomain + productCode
```

管理界面展示机器码，用户将其交给授权签发方。SDK 校验授权声明中的机器码必须与框架当前生成值一致。

配置项：

```yaml
jnimble:
  licensing:
    canonical-domain: localhost
    issuer-url: http://localhost:8080/admin/plugins/license-issuer/generate
```

## 4. 授权声明

```java
public record LicenseClaims(
        int version,
        String licenseId,
        String issuer,
        String pluginCode,
        String pluginVersionRange,
        int machineCodeVersion,
        String machineCode,
        long issuedAt,
        long notBefore,
        long expiresAt
) {}
```

## 5. 模块职责

### jnimble-license-sdk

- `PluginLicense`：插件调用的静态门面。
- `PluginLicenseBackend`：框架提供授权码和机器码的接口。
- `PluginLicenseVerifier`：插件注册到管理界面的验证 Hook。
- `PluginLicenseVerifierRegistry`：验证 Hook 注册表合约。
- `Ed25519LicenseTokenVerifier`：直接使用插件传入的公钥验签。
- `PluginLicenseResult`：插件上报的校验结果。
- `PluginLicenseException`：无效授权的运行时异常。

SDK 不依赖 Spring，不访问数据库，不持有共享公钥注册表。

### jnimble-license-core

- 按 pluginId 保存和读取原始授权码。
- 生成并展示机器码。
- 实现 `PluginLicenseBackend`。
- 实现 `PluginLicenseVerifierRegistry`。
- 保存插件上报的状态、到期时间和失败码。

授权核心不执行 Ed25519 验签，不实现 kernel 的 `PluginActivationGuard`。

### jnimble-admin-shell

- 保留授权码填写弹窗。
- 保留激活、重新检测和移除接口。
- 激活时先保存原始授权码；若插件 Hook 已注册，则立即强制复检。
- 插件未启用、Hook 不存在时显示 `UNVERIFIED`，下次启用插件时由插件自检。
- 重新检测只调用插件 Hook，不禁用插件，也不替插件决定业务是否继续。

## 6. 插件接入

插件依赖：

```xml
<dependency>
    <groupId>com.jnimble</groupId>
    <artifactId>jnimble-license-sdk</artifactId>
    <scope>provided</scope>
</dependency>
```

插件在 `boot()` 中加载自己的公钥、注册 Hook 并执行自检：

```java
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
```

插件也可以在收费业务方法中调用同一静态方法。无效时 SDK 抛出 `PluginLicenseException`，由该插件调用点中止执行。

## 7. 管理界面流程

### 填写授权码

```text
用户填写授权码
  → 框架按 pluginId 保存原始授权码
  → 框架清除该插件 SDK 缓存
  → Hook 已注册：调用插件 Hook 强制验证
  → Hook 未注册：状态为 UNVERIFIED
  → 界面展示插件上报结果
```

### 重新检测

```text
用户点击重新检测
  → 框架查找 pluginId 对应 Hook
  → 调用 verifier.verify(true)
  → 插件通过 SDK 验签
  → 框架保存并展示结果
```

### 插件业务调用

```text
插件业务方法
  → PluginLicense.requireValid(...)
  → 有效缓存命中：继续
  → 未命中：从框架读取授权码并验签
  → 有效：缓存并继续
  → 无效：抛异常并中止当前插件业务
```

## 8. 缓存规则

- 只缓存有效结果。
- 默认缓存 6 小时。
- 缓存截止时间为 `min(当前时间 + 6h, expiresAt)`。
- 缓存按 `pluginId + issuer + productCode + pluginVersion + 公钥指纹` 隔离。
- 填写、替换或移除授权码时，框架立即调用 `PluginLicense.invalidate(pluginId)`。
- 点击重新检测时传入 `force=true`，跳过缓存。
- 无效结果不缓存，下一次调用会重新读取并校验。

## 9. 状态

- `NOT_REQUIRED`：插件不需要授权。
- `MISSING`：未填写授权码。
- `UNVERIFIED`：授权码已保存，但插件 Hook 尚未运行。
- `VALID`：有效。
- `EXPIRING`：有效，但将在 30 天内过期。
- `EXPIRED`：已过期。
- `INVALID`：格式、签名或声明无效。
- `MACHINE_CODE_MISMATCH`：机器码不匹配。
- `PLUGIN_MISMATCH`：产品码或版本不匹配。
- `LOCAL_TIME_BEFORE_LICENSE_ISSUED`：本机时间早于签发时间。

## 10. 安全边界

本方案避免将所有商业插件的授权门禁集中在框架中。修改框架的启用流程或 HTTP 拦截器，不能直接绕过插件业务代码中的 SDK 调用。

授权仍然不能绝对阻止客户修改插件自身字节码。需要更高防护时，可增加插件 Jar 签名、完整性校验、代码混淆或在线授权服务，但这些不属于当前离线 SDK 的职责。

## 11. 测试要求

- 有效授权完成 Ed25519 验签并继续执行。
- 有效结果重复调用命中缓存，不重复读取授权码。
- `force=true` 跳过缓存。
- 无效签名抛出 `PluginLicenseException` 且不缓存。
- 机器码、产品码、版本、issuer 和有效期不匹配时拒绝。
- 授权码变更后缓存立即失效。
- 插件 Hook 注册句柄在插件停止或启动失败时正确清理。
- 管理界面在 Hook 未注册时显示 `UNVERIFIED`。
- 框架不含统一授权 HTTP 拦截器或授权 `PluginActivationGuard` 实现。