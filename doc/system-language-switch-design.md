# 系统语言切换设计

## 目标

登录页和登录后的后台公共顶栏提供统一的中文、English 切换入口。语言选择对当前请求立即生效，并在浏览器后续访问中保持。

## 实现约定

- 请求参数：`lang`，当前界面提供 `zh_CN` 和 `en` 两个值。
- 持久化 Cookie：`jnimble.lang`，路径为 `/`，有效期 365 天，SameSite 为 `Lax`。
- 默认语言：简体中文。
- 切换请求：当前页面使用 GET 查询参数切换，不需要登录状态或 CSRF Token。
- 插件视图：插件继续使用平台的动态 `MessageSource` 和自身 `i18n.basename` 语言包；请求 Locale 会自动传递给插件消息解析。

## 界面入口

- 登录页：页面右上角。
- 后台：公共顶栏右侧、退出登录按钮之前。

共享片段位于 `jnimble-admin-shell/src/main/resources/templates/fragment/language-switcher.html`，两个入口必须复用该片段，避免行为和样式分叉。

## 后续扩展

新增语言时，需要补充语言包、扩展切换入口，并在语言管理配置启用后才对用户显示。业务插件不得自行保存独立语言状态，应始终使用系统请求 Locale。

## 插件开发约定

- 每个带界面的插件必须在 `META-INF/jnimble-plugin.json` 声明 `i18n.basename`。
- `i18n/messages.properties` 是英文默认包，`i18n/messages_zh_CN.properties` 是简体中文包，两者 key 必须完全一致。
- Thymeleaf 静态文案使用 `#{message.key}`；属性文案使用 `th:attr`、`th:placeholder` 或对应的 Thymeleaf 属性处理器。
- Vue/JavaScript 运行期文案通过 `th:inline="javascript"` 注入当前请求的翻译字典，不得在浏览器中再保存独立语言状态。
- 状态码、Hook 名称、插件 ID、JSON 字段和接口枚举保持稳定，只翻译展示标签。
- 独立工作台页面同样读取 `jnimble.lang` Cookie；桌台 POS 在自身顶栏提供语言切换，后台布局页面复用系统公共切换器。
- 使用 Element UI 的插件页面必须引用 `fragment/element-locale :: scripts`，英文请求下同步切换组件库内置文案。

当前已完成多语言改造的界面插件包括 CRM、菜品管理、订单核心、桌台点餐、支付记录和打印核心。飞鹅打印驱动没有独立页面，但提供插件名称和描述语言包，用于插件管理列表；插件 SDK 不是可安装业务插件，不创建空语言包。
