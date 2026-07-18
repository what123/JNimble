# JNimble

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-blue.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/maven-3.9+-blue.svg)](https://maven.apache.org/)

English | [中文](README_CN.md)

> **Licensing**: JNimble is distributed under a **dual-license model**.
> - **Open Source (Apache 2.0)**: You may use, modify, and distribute JNimble commercially, **provided that the copyright footer in the admin UI and login page is retained** (`© 2026 JNimble. All rights reserved.`).
> - **Commercial License**: To remove or alter the copyright footer, contact `178277164@qq.com` to purchase a commercial license.
>
> See [NOTICE](NOTICE) and [LICENSE](LICENSE) for full terms.

JNimble is a **plugin-driven** Java admin framework built on Spring Boot 3, Java 21, MyBatis-Plus, and Thymeleaf. Every business capability ships as a plugin — install to use, uninstall to remove — so you build vertical admin products by **composing plugins like building blocks** instead of reinventing the platform.

## Why JNimble

The core of JNimble is its **plugin mechanism**, which enables three workflows:

- **Building-block development** — Don't fork the platform to customize it. Package your feature as a plugin JAR, drop it into the plugin directory, and it's auto-discovered: menus, routes, and permissions register themselves with zero changes to the framework source.
- **Cross-project reuse** — Build once, use everywhere. Order management, membership systems, report exports, message push... each capability is an independent plugin. Install the right JARs in a new project and you're done.
- **Pay-per-feature commercialization** — Plugins are independent units; customers buy only the plugins they need. (License verification is delegated to a standalone SDK that business plugins can depend on — the framework itself stays license-free.)

The framework itself provides only the foundation: users, roles, permissions, audit, i18n, plugin runtime, and the admin UI shell. **All business features are delivered through plugins.**

## Quick Start

### Prerequisites

- JDK 21+
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

### Database migrations (Flyway auto-schema)

JNimble manages schema versions with [Flyway](https://flywaydb.org/). **Migrations run automatically on application startup** — no manual table creation is required.

- **Framework migrations**: Located in `jnimble-starter/src/main/resources/db/migration/system/`. Filenames follow `V{version}__{description}.sql`. All `jnimble_*` system tables (users, roles, permissions, audit log, plugin state, system settings, etc.) are created and maintained by these scripts.
- **Plugin migrations**: Each plugin owns its own migration directory and history table (see [Plugin Architecture → Database migrations](#database-migrations)). They do not interfere with each other.
- **History table**: The framework uses `flyway_schema_history`; plugins use `flyway_schema_history_{pluginId}`.
- **Upgrade mechanism**: To add a new table or column, **add a new `V*.sql` script with a higher version number** — Flyway picks it up on next startup. **Never** edit an already-applied migration script or modify the database manually; doing so fails Flyway's checksum validation and breaks startup.
- **Baseline strategy**: `spring.flyway.baseline-on-migrate=true` in `application.yml` — Flyway baselines an existing database on first run without wiping data.

To inspect migration status:

```bash
mysql -u<user> -p<pwd> jnimble -e "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
```

If Flyway validation fails at startup (e.g. history contains a removed migration), run `flyway repair` or clean the corresponding rows in `flyway_schema_history` before restarting.

## Plugin Architecture

### What a plugin can do

Every plugin gets its own classloader, its own Flyway migration history table, and its own i18n message bundle. Through `PluginContext`, a plugin has access to:

| Capability | Entry point | Purpose |
|------------|-------------|---------|
| Hooks | `context.hooks()` | Contribute UI fragments to predefined mount points in the admin layout (sidebar items, topbar buttons, etc.) |
| Routes | `context.routes()` | Register page routes under the `/admin/plugins/{pluginId}/**` namespace |
| Assets | `context.assets()` | Register CSS / JS / image static assets |
| Platform beans | `context.bean(Class)` | Look up framework services (e.g. `UserAccountService`) |
| Descriptor | `context.descriptor()` | Read the parsed `jnimble-plugin.json` |

Additionally, the plugin descriptor can declare these capabilities (auto-loaded by the runtime, no `boot()` code needed):

| Capability | Descriptor field | Purpose |
|------------|------------------|---------|
| Permissions | `permissions` | Declare permission codes, auto-synced to role management |
| Database migrations | `migration` | Enable plugin-scoped Flyway migration directory and history table |
| Internationalization | `i18n` | Register plugin-scoped message bundles |
| Admin entry | `admin` | Declare the plugin's admin menu entry route and label |
| Configuration form | `configuration` | Declare configurable fields; the admin UI auto-renders the form |
| Spring child context | `spring` | Load a plugin-scoped `@Configuration` class |
| Plugin dependencies | `dependencies` | Declare required plugins |

### Plugin directory structure

A standard JNimble plugin JAR contains (see `jnimble-demo-plugin` for a working example):

```
my-plugin/
├── META-INF/
│   └── jnimble-plugin.json          # Plugin descriptor (required)
├── com/
│   └── example/
│       └── MyPluginBoot.java        # PluginBoot implementation (required)
├── templates/
│   └── plugin/
│       └── my-plugin/               # Template paths MUST start with plugin/{pluginId}/
│           ├── fragment/
│           │   └── sidebar.html     # Hook fragments
│           └── page/
│               └── index.html       # Page templates
├── i18n/
│   ├── demo.properties              # Default locale
│   └── demo_zh_CN.properties        # Chinese
└── db/
    └── migration/
        └── plugin/
            └── my-plugin/           # Plugin-scoped migration directory
                └── V1__init.sql
```

> **Template path convention**: All plugin template paths MUST start with `plugin/{pluginId}/` (e.g. `plugin/my-plugin/page/index`). The `PluginTemplateResolver` loads them from the plugin's classloader. The actual resource lives at `templates/plugin/{pluginId}/...`.

### Plugin descriptor

Minimal `META-INF/jnimble-plugin.json`:

```json
{
  "schemaVersion": "1.0",
  "id": "my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "platformVersion": "0.1.0",
  "bootClass": "com.example.MyPluginBoot",
  "admin": {
    "entry": "/index",
    "labelKey": "my-plugin.name",
    "permission": "my-plugin.view"
  },
  "permissions": [
    {
      "code": "my-plugin.view",
      "name": "View My Plugin",
      "nameKey": "my-plugin.permission.view"
    }
  ]
}
```

See `PluginDescriptor` in `jnimble-plugin-sdk` for the full schema. Common optional blocks: `i18n`, `migration`, `spring`, `dependencies`, `configuration`.

### Write the boot class

```java
package com.example;

import com.jnimble.sdk.hook.HookViewContribution;
import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginContext;
import com.jnimble.sdk.route.RouteDefinition;
import com.jnimble.sdk.route.RouteMethod;

public class MyPluginBoot implements PluginBoot {

    @Override
    public void boot(PluginContext context) {
        // 1. Register a sidebar menu item (note the plugin/{pluginId}/ prefix)
        context.hooks().register(
                "admin.layout.sidebar",
                new HookViewContribution(
                        "plugin/my-plugin/fragment/sidebar",  // template view path
                        null,                                  // model variables
                        100,                                   // render order (ascending)
                        "my-plugin.view",                      // required permission
                        null                                   // activation expression
                )
        );

        // 2. Register a page route
        context.routes().register(new RouteDefinition(
                "/index",                              // path relative to plugin route namespace
                RouteMethod.GET,
                "plugin/my-plugin/page/index",         // view template path
                "my-plugin.view"                       // required permission
        ));
    }

    @Override
    public void stop(PluginContext context) {
        // Release plugin-owned resources only; standard registrations are
        // rolled back automatically by the platform.
    }
}
```

### Sidebar fragment example

`templates/plugin/my-plugin/fragment/sidebar.html`:

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <a class="admin-shell__nav-item"
     th:classappend="${activeNav == 'plugin:my-plugin:index'} ? ' admin-shell__nav-item--active' : ''"
     th:href="@{/admin/plugins/my-plugin/index}">
    <span class="admin-shell__nav-icon" aria-hidden="true"></span>
    <span>My Plugin</span>
  </a>
</body>
</html>
```

> The `activeNav` value follows the format `plugin:{pluginId}:{routeKey}`, where `routeKey` is the route path with leading/trailing `/` stripped and remaining `/` replaced by `.` (e.g. `/index` → `index`, `/orders/list` → `orders.list`).

### Built-in hook points

The framework exposes the following hook points in `templates/layout/admin.html`. A plugin can contribute fragments to any of them:

| Hook point | Location |
|------------|----------|
| `admin.layout.sidebar.start` | Top of the sidebar |
| `admin.layout.sidebar` | Generic sidebar slot (used by the demo plugin) |
| `admin.layout.sidebar.sys.start` | Start of the "System Management" section |
| `admin.layout.sidebar.plugins.before` / `after` | Around the "Plugins" nav item |
| `admin.layout.sidebar.roles.before` / `after` | Around the "Roles" nav item |
| `admin.layout.sidebar.users.before` / `after` | Around the "Users" nav item |
| `admin.layout.sidebar.sys.end` | End of the "System Management" section |
| `admin.layout.sidebar.end` | Bottom of the sidebar |
| `admin.layout.topbar` | Top bar |

### Permission declaration

Declare permissions in the `permissions` array of `jnimble-plugin.json`. **Codes must start with `{pluginId}.`**:

```json
{
  "permissions": [
    {
      "code": "my-plugin.view",
      "name": "View My Plugin",
      "nameKey": "my-plugin.permission.view",
      "description": "Allows viewing My Plugin pages",
      "descriptionKey": "my-plugin.permission.view.desc"
    },
    {
      "code": "my-plugin.manage",
      "name": "Manage My Plugin"
    }
  ]
}
```

When the plugin is enabled, declared permissions are synced to the role management UI automatically, so administrators can assign them to roles.

### Database migrations

Enable migrations in the descriptor:

```json
{
  "migration": {
    "enabled": true,
    "location": "classpath:db/migration/plugin/my-plugin",
    "table": "flyway_schema_history_my_plugin",
    "baselineOnMigrate": true,
    "failOnError": true
  }
}
```

Defaults (used when fields are omitted):

- Default location: `classpath:db/migration/plugin/{pluginId}`
- Default history table: `flyway_schema_history_{pluginId with dashes replaced by underscores}` (e.g. `demo-plugin` → `flyway_schema_history_demo_plugin`)
- `enabled` defaults to `false` — must be explicitly turned on

Each plugin has its own migration history table and cannot interfere with others. Uninstalling a plugin does not drop its business tables.

### Internationalization

Declare a basename in the descriptor:

```json
{
  "i18n": {
    "basename": "i18n.my-plugin"
  }
}
```

The corresponding resource files (dots in basename become slashes for the resource path):

- `i18n/my-plugin.properties` — default locale
- `i18n/my-plugin_zh_CN.properties` — Simplified Chinese
- `i18n/my-plugin_en_US.properties` — English (US)

> If `i18n` is not declared, the plugin's message bundle is not registered.

### Static assets

```java
context.assets().register(new AssetDefinition(
        "css/my-plugin.css",                       // request path (relative to plugin asset namespace)
        "classpath:assets/my-plugin/css/app.css",  // resource location
        true                                       // cacheable
));
```

### Maven dependencies

A plugin JAR's `pom.xml` only needs to depend on the SDK with `provided` scope (supplied by the framework at runtime):

```xml
<project>
    <parent>
        <groupId>com.jnimble</groupId>
        <artifactId>jnimble-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>my-plugin</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.jnimble</groupId>
            <artifactId>jnimble-plugin-sdk</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### Build and install

```bash
mvn -pl my-plugin package
```

Three installation methods:

1. **Admin upload** — Go to **Admin → Plugins → Upload** and select the generated JAR.
2. **Hot-deploy directory** — Drop the JAR into the plugin directory (default `./data/plugins`, configurable under **System Management → Storage Config**). `PluginDirectoryWatcher` auto-installs or hot-replaces it.
3. **Classpath discovery** — During development, place it on the classpath; the framework discovers it on startup.

After installation, the plugin enters the `INSTALLED` state. Click **Enable** in the plugin management page to invoke `PluginBoot.boot()`. A failed enable automatically rolls back all registrations.

### Reference implementation

The `jnimble-demo-plugin` module is a complete, runnable minimal example that demonstrates:

- Registering a sidebar menu item (via the `admin.layout.sidebar` hook)
- Registering a page route (`/index`)
- Declaring a permission (`demo-plugin.view`)
- Declaring an admin entry (`admin.entry`)
- The template path convention (`plugin/demo-plugin/...`)

Run `mvn -pl jnimble-demo-plugin package`, drop the JAR into the plugin directory, and a "Demo Plugin" menu item appears in the admin sidebar.

## Modules

| Module | Description |
|--------|-------------|
| `jnimble-bom` | Dependency version BOM. |
| `jnimble-plugin-sdk` | Plugin boot, hook, route, asset, migration, and i18n contracts. |
| `jnimble-kernel` | Plugin runtime, hook, route, asset, and migration infrastructure. |
| `jnimble-platform` | Users, roles, permissions, audit, i18n, plugin state, system settings. |
| `jnimble-admin-shell` | Admin layout, pages, templates, and UI shell. |
| `jnimble-starter` | Runnable Spring Boot application. |
| `jnimble-demo-plugin` | Minimal demo plugin showing sidebar menu and route registration. |

## Features

- **Plugin lifecycle**: install, enable, disable, uninstall, reload — via JAR upload, classpath, or hot-deploy directory.
- **Hook system**: plugins contribute sidebar entries, top-bar actions, and template fragments through the Thymeleaf `jn:hook` dialect.
- **Permission system**: plugins declare permissions that are synchronized into role management; admin pages enforce server-side permissions.
- **Database migrations**: framework Flyway migrations under `db/migration/system`; each plugin owns its own migration directory and history table.
- **Internationalization**: framework and plugins ship their own message bundles; admin UI ships with English and Chinese.
- **Audit logging**: plugin, user, and role-permission operations are recorded.
- **Branding**: site name, logo, and storage directories are configurable at runtime through the admin UI.
- **Configuration forms**: plugins can declare `configuration` fields; the admin UI auto-renders the form and persists values.

## Database

JNimble requires a `DataSource`. Framework migrations create `jnimble_user`, `jnimble_role`, `jnimble_permission`, `jnimble_role_permission`, `jnimble_subject_role`, `jnimble_audit_log`, `jnimble_plugin_state`, `jnimble_plugin_configuration`, `jnimble_language`, and `jnimble_system_setting`.

Platform single-table data access uses `MapperUtils`. Service code should not call `BaseMapper` CRUD methods directly.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
