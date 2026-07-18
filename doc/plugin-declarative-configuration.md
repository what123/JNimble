# 插件声明式参数配置

## 1. 入口边界

插件列表明确区分两类入口：

- **参数配置**：由框架读取插件描述文件中的 `configuration.fields` 自动生成，适合商户号、接口密钥、网关地址、开关等简单参数。
- **管理入口**：由插件描述文件中的 `admin` 声明，进入插件自己的订单、客户、打印机等业务管理页面。

插件未声明 `configuration` 时，列表显示“无可配置参数”，不会把 `admin` 页面错误地当成参数配置页。

## 2. 描述文件格式

在插件的 `META-INF/jnimble-plugin.json` 中增加：

```json
{
  "configuration": {
    "title": "Payment Settings",
    "titleKey": "payment.configuration.title",
    "description": "Basic payment provider settings.",
    "descriptionKey": "payment.configuration.description",
    "fields": [
      {
        "key": "merchantId",
        "label": "Merchant ID",
        "labelKey": "payment.configuration.merchantId",
        "type": "TEXT",
        "required": true
      },
      {
        "key": "apiKey",
        "label": "API Secret",
        "labelKey": "payment.configuration.apiKey",
        "type": "SECRET",
        "required": true
      },
      {
        "key": "sandbox",
        "label": "Sandbox mode",
        "labelKey": "payment.configuration.sandbox",
        "type": "BOOLEAN",
        "required": false,
        "defaultValue": "false"
      }
    ]
  }
}
```

支持的字段类型：

- `TEXT`：单行文本。
- `SECRET`：密码输入；加密保存，页面不回显，留空表示保留原值。
- `NUMBER`：数字。
- `BOOLEAN`：开关。
- `SELECT`：下拉选择，需要声明 `options`。
- `TEXTAREA`：多行文本。

字段、选项和表单标题均支持 `labelKey`、`titleKey` 等国际化键，同时保留英文回退文案。

## 3. 插件读取配置

插件可通过 Spring 注入，或从 `PluginContext` 获取 SDK 服务：

```java
PluginConfiguration configuration = context.bean(PluginConfiguration.class);
String merchantId = configuration.getOrDefault("payment", "merchantId", "");
String apiKey = configuration.getOrDefault("payment", "apiKey", "");
boolean sandbox = configuration.getBoolean("payment", "sandbox", false);
```

`PluginConfiguration` 是只读 SDK；配置写入统一经过框架页面、权限校验和审计，插件不能绕过框架修改其他插件的配置。

## 4. 存储和安全

- 配置保存到系统表 `jnimble_plugin_configuration`，主键为插件编码和配置键。
- `SECRET` 使用 AES-256-GCM 加密，附加认证数据绑定插件编码和配置键。
- 加密主密钥默认保存为 `./data/plugin-config.key`，可通过 `JNIMBLE_PLUGIN_CONFIG_KEY_FILE` 指定。
- 密钥文件在 POSIX 系统上限制为当前用户读写；生产环境需要备份此文件，丢失后已保存密钥无法解密。
- 更新配置需要 `system.plugins.manage`，查看需要 `system.plugins.view`，每次保存写入审计日志。

## 5. 校验规则

- 配置键必须唯一，并符合 `[a-z][A-Za-z0-9._-]{0,127}`。
- `SECRET` 禁止在插件描述文件中声明默认值。
- `NUMBER` 默认值必须为有效数字。
- `BOOLEAN` 默认值只能为 `true` 或 `false`。
- `SELECT` 必须声明非空、值唯一的选项，默认值必须属于选项集合。
- 每个插件最多声明 100 个参数，单个保存值最多 10000 个字符。
