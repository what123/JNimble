# JNimble

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/maven-3.9+-blue.svg)](https://maven.apache.org/)

English | [中文](README_CN.md)

JNimble is a plugin-friendly Java admin framework built on Spring Boot 3, Java 21, MyBatis-Plus, and Thymeleaf. It provides a plugin runtime, hook system, role-based access control, audit logging, internationalization, and an admin UI shell out of the box so you can build vertical admin products by writing plugins rather than reinventing the platform.

## Quick Start

### Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8+ (or any DataSource Spring Boot can configure)

### Build

```bash
mvn clean install
```

### Configure

JNimble reads all sensitive values from environment variables. Before the first run, set at least:

```bash
export JNIMBLE_DB_URL='jdbc:mysql://localhost:3306/jnimble?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true'
export JNIMBLE_DB_USERNAME=jnimble
export JNIMBLE_DB_PASSWORD='your-mysql-password'
export JNIMBLE_DEFAULT_ADMIN_PASSWORD='your-admin-password'
```

See `jnimble-starter/src/main/resources/application.yml` for the full list of environment variables.

### Run

```bash
mvn -pl jnimble-starter spring-boot:run
```

Open `http://localhost:8080/admin` and sign in with `admin` / the password you set above.

## Modules

- `jnimble-bom`: dependency version BOM.
- `jnimble-plugin-sdk`: plugin boot, hook, route, and asset contracts.
- `jnimble-kernel`: plugin runtime, hook, route, asset, and migration infrastructure.
- `jnimble-platform`: users, roles, permissions, audit, i18n, plugin state, system settings.
- `jnimble-admin-shell`: admin layout, pages, templates, and UI shell.
- `jnimble-starter`: runnable Spring Boot application.

## Features

- Plugin lifecycle: install, enable, disable, uninstall, reload — via JAR upload, classpath, or `./data/plugins` directory.
- Hook system: plugins contribute sidebar entries, top-bar actions, and template fragments through Thymeleaf `jn:hook` dialect.
- Permission system: plugins declare permissions that are synchronized into role management; admin pages enforce server-side permissions.
- Database migrations: framework Flyway migrations under `db/migration/system`; each plugin owns its own migration directory and history table.
- Internationalization: framework and plugins ship their own message bundles; admin UI ships with English and Chinese.
- Audit logging: plugin, user, and role-permission operations are recorded.
- Branding: site name and logo are configurable at runtime through the admin UI.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        JNimble Architecture                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    jnimble-starter                      │   │
│  │              (Spring Boot Application)                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-admin-shell                   │   │
│  │              (Admin UI, Controllers)                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-platform                      │   │
│  │     (Users, Roles, Permissions, Audit, i18n)            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    jnimble-kernel                       │   │
│  │      (Plugin Runtime, Hooks, Routes, Migration)         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   jnimble-plugin-sdk                    │   │
│  │            (Plugin Contracts, SPI)                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Your Plugins                         │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │   │
│  │  │ pluginA │ │ pluginB │ │ pluginC │ │ pluginD │ ...  │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Documentation

- [Framework Design](docs/framework-design.md)
- [Plugin Development Guide](docs/plugin-development-guide.md)
- [Mapper Utils Guide](docs/mapper-utils-guide.md)
- [Framework / Plugin Separation](doc/framework-plugin-separation-design.md)
- [Plugin Hot-Plug Runtime](doc/plugin-hot-plug-runtime-design.md)

## Database

JNimble requires a `DataSource`. Framework migrations create `jnimble_user`, `jnimble_role`, `jnimble_permission`, `jnimble_role_permission`, `jnimble_subject_role`, `jnimble_audit_log`, `jnimble_plugin_state`, `jnimble_plugin_configuration`, `jnimble_language`, and `jnimble_system_setting`.

Platform single-table data access uses `MapperUtils`. Service code should not call `BaseMapper` CRUD methods directly.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
