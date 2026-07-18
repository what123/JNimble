# JNimble

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/maven-3.9+-blue.svg)](https://maven.apache.org/)

[English](README.md) | 中文

JNimble 是一个插件化 Java 后台管理框架，基于 Spring Boot 3、Java 21、MyBatis-Plus 和 Thymeleaf 构建。开箱提供插件运行时、钩子系统、基于角色的权限控制、审计日志、国际化和管理后台 UI 外壳，让你通过编写插件来构建垂直后台产品，而无需重新造平台轮子。

## 快速开始

### 环境要求

- JDK 21
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

## 模块说明

- `jnimble-bom`：依赖版本 BOM。
- `jnimble-plugin-sdk`：插件启动、钩子、路由和资源契约。
- `jnimble-license-sdk`：许可证校验 SDK（Ed25519 签名）。
- `jnimble-kernel`：插件运行时、钩子、路由、资源和迁移基础设施。
- `jnimble-license-core`：许可证存储与校验核心（JDBC 实现）。
- `jnimble-platform`：用户、角色、权限、审计、国际化、插件状态、系统设置。
- `jnimble-admin-shell`：管理后台布局、页面、模板和 UI 外壳。
- `jnimble-starter`：可运行的 Spring Boot 应用。

## 功能特性

- 插件生命周期：安装、启用、禁用、卸载、重载 —— 支持 JAR 上传、classpath、`./data/plugins` 目录三种发现方式。
- 钩子系统：插件通过 Thymeleaf `jn:hook` 方言贡献侧边栏条目、顶栏操作和模板片段。
- 权限系统：插件声明权限会同步到角色管理；后台页面强制服务器端权限校验。
- 数据库迁移：框架 Flyway 迁移在 `db/migration/system`；每个插件拥有独立迁移目录和历史表。
- 国际化：框架与插件各自维护消息包；后台 UI 默认提供中英文。
- 许可证校验：插件可选启用 Ed25519 签名的许可证 token，运行时校验。
- 审计日志：插件、用户、角色权限操作均被记录。
- 品牌定制：站点名称和 Logo 可通过后台 UI 运行时配置。

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        JNimble 架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    jnimble-starter                      │   │
│  │              (Spring Boot 应用)                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-admin-shell                   │   │
│  │              (管理后台 UI、控制器)                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-platform                      │   │
│  │         (用户、角色、权限、审计、国际化)                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    jnimble-kernel                       │   │
│  │      (插件运行时、钩子、路由、迁移)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-plugin-sdk                    │   │
│  │            (插件契约、SPI)                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    你的插件                              │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │   │
│  │  │ 插件A   │ │ 插件B   │ │ 插件C   │ │ 插件D   │ ...  │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 文档

- [框架设计](docs/framework-design.md)
- [插件开发指南](docs/plugin-development-guide.md)
- [Mapper 工具指南](docs/mapper-utils-guide.md)
- [框架/插件分离设计](doc/framework-plugin-separation-design.md)
- [插件热插拔运行时](doc/plugin-hot-plug-runtime-design.md)
- [插件许可证授权](doc/plugin-license-authorization-design.md)

## 数据库

JNimble 需要 `DataSource`。框架迁移会创建 `jnimble_user`、`jnimble_role`、`jnimble_permission`、`jnimble_role_permission`、`jnimble_subject_role`、`jnimble_audit_log`、`jnimble_plugin_state`、`jnimble_plugin_license`、`jnimble_plugin_configuration`、`jnimble_language` 和 `jnimble_system_setting` 表。

平台单表数据访问使用 `MapperUtils`。服务代码不应直接调用 `BaseMapper` CRUD 方法。

## 贡献

欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解指南。

## 许可证

本项目采用 Apache License 2.0 许可证 — 详情请查看 [LICENSE](LICENSE) 文件。
