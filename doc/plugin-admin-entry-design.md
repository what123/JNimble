# 插件后台配置入口设计

## 目标

插件管理页用于日常管理，不再平铺所有运行时操作。管理员可以从插件列表直接进入插件自己的配置或管理页面；替换插件包、重载、卸载等低频或高风险操作统一放在插件详情页。

## 页面职责

插件列表保留：

- 安装插件
- 直接配置
- 查看详情
- 启用、停用

插件详情保留：

- 插件包、启动类、运行事件等技术信息
- 替换插件包
- 重载
- 卸载及数据清理

“登记描述符”和按插件 ID 替换包不再作为普通页面功能展示，原有后台端点暂时保留用于兼容已有调用。

## 描述符格式

插件可在 `META-INF/jnimble-plugin.json` 中声明可选的 `admin` 对象：

```json
{
  "admin": {
    "entry": "/items",
    "labelKey": "menu.item.manage",
    "permission": "menu-manager.admin.view"
  }
}
```

- `entry`：插件内部的 GET 路由，必须以 `/` 开头。例如 `/items` 最终生成 `/admin/plugins/menu-manager/items`。
- `labelKey`：配置入口文案的多语言 key。无法解析时回退为系统“配置”文案。
- `permission`：显示配置入口所需权限，必须已在当前插件的 `permissions` 中声明。

未声明 `admin` 的插件不会显示配置按钮，适用于仅提供 Hook、SPI 或驱动能力且无需独立配置页的插件。

## 运行时规则

配置入口仅在以下条件全部满足时显示：

1. 插件状态为 `ENABLED`。
2. 当前用户拥有 `admin.permission`。
3. `admin.entry` 对应的 GET 路由已由该插件注册且当前可用。

后台不按插件 ID 硬编码配置 URL，插件可以独立调整自己的入口。描述符加载时会拒绝外部 URL、路径穿越、查询参数以及未声明的入口权限。

## 当前插件入口

| 插件 | 入口 | 权限 |
| --- | --- | --- |
| `crm` | `/customers` | `crm.customer.view` |
| `menu-manager` | `/items` | `menu-manager.admin.view` |
| `order-core` | `/orders` | `order-core.admin.view` |
| `order-table` | `/tables` | `order-table.admin.view` |
| `payment` | `/records` | `payment.view` |
| `printer-core` | `/printers` | `printer-core.config` |

`printer-feie` 只贡献打印驱动，当前不提供独立配置入口；具体打印机参数由 `printer-core` 的打印机管理页面配置。
