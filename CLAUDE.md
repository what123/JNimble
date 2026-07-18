# CLAUDE.md

> This file is for AI coding assistants (Claude / Codex / Cursor, etc.) working in the JNimble project — for **plugin development** and **project maintenance**.
> If you are an AI assistant receiving this file, treat it as the highest-priority project guideline.

---

## 1. Project Overview

**JNimble** is a plugin-driven Java admin framework (Spring Boot 3 + Java 21 + MyBatis-Plus + Thymeleaf).

**Core principle: Business capabilities are always delivered as plugins. The framework source code does not carry business logic.**
When a user asks to "add a feature", **the default deliverable is a new plugin JAR**, not a change to `jnimble-kernel` / `jnimble-platform` / `jnimble-admin-shell`.

Module map:

| Module | Role | AI may modify? |
|--------|------|----------------|
| `jnimble-bom` | Dependency version BOM | ⚠️ Only on dependency upgrades |
| `jnimble-plugin-sdk` | Plugin contracts (interfaces, DTOs, descriptor) | ⚠️ Only when new SDK capability is genuinely needed |
| `jnimble-kernel` | Plugin runtime, hooks, routes, assets, migration infra | ⚠️ Sparingly — affects all plugins |
| `jnimble-platform` | Users, roles, permissions, audit, i18n, plugin state, system settings | ⚠️ Sparingly |
| `jnimble-admin-shell` | Admin layout, pages, templates | ⚠️ Sparingly |
| `jnimble-starter` | Runnable Spring Boot app, system migrations | ✅ Adding migration scripts is OK |
| `jnimble-demo-plugin` | Reference plugin | ✅ Can be copied as a template |
| Business plugins (new) | Real business features | ✅ **Primary AI workspace** |

---

## 2. Local Run & Build

### Environment
- JDK 21+ (compile target `release=21`, runtime requires 21+)
- Maven 3.9+
- MySQL 8+ (or any Spring Boot DataSource)

### Required env vars (set before startup)
```bash
export JNIMBLE_DB_URL='jdbc:mysql://localhost:3306/jnimble?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&createDatabaseIfNotExist=true'
export JNIMBLE_DB_USERNAME=jnimble
export JNIMBLE_DB_PASSWORD='<mysql password>'
export JNIMBLE_DEFAULT_ADMIN_PASSWORD='<admin password>'
```
Full variable list: `jnimble-starter/src/main/resources/application.yml`.

### Build & Run
```bash
# Full build (skip tests)
mvn clean install -DskipTests -Dcheckstyle.skip=true -Dspotbugs.skip=true

# Run the app
mvn -pl jnimble-starter spring-boot:run

# Build a single plugin JAR
mvn -pl <module> package
```

### After startup
- Admin URL: `http://localhost:8080/admin`
- Default account: `admin` / the `JNIMBLE_DEFAULT_ADMIN_PASSWORD` you set
- Plugin directory (actually watched at runtime): `jnimble-starter/data/plugins/` (note: `spring-boot:run` runs with working dir `jnimble-starter/`, not the repo root)
- Logs: console or `/tmp/jnimble.log`

### Tests
- Framework/platform tests use an in-memory H2 DB (`jnimble-starter` test scope).
- Run tests: `mvn test`
- Surefire is configured with `-javaagent:${mockito.agent}` — do not remove it.

---

## 3. Plugin Development Guide

### 3.1 When to build a plugin vs. modify the framework

| Need | Approach |
|------|----------|
| New business feature (orders, members, reports, exports…) | **New plugin** |
| New admin menu / page | **New plugin, register hook + route** |
| New permission code | **Declare in the plugin descriptor `permissions`** |
| New business table | **Add `V*.sql` in the plugin migration dir** |
| Framework-level capability gap (e.g. a new hook point) | Modify `jnimble-admin-shell` template + `jnimble-kernel` |
| Platform capability gap (e.g. new user field) | Add a system migration script — **do not hand-edit tables** |

### 3.2 Creating a new plugin

1. **Copy `jnimble-demo-plugin` as a template**, change `artifactId` and `id`.
2. Add `<module>your-plugin</module>` to the root `pom.xml` `<modules>`.
3. Directory layout (**path conventions are non-negotiable**):
   ```
   your-plugin/
   ├── pom.xml                                 # depends on jnimble-plugin-sdk (scope=provided)
   └── src/main/
       ├── java/.../YourPluginBoot.java        # implements PluginBoot
       └── resources/
           ├── META-INF/jnimble-plugin.json    # descriptor (required)
           ├── templates/plugin/{pluginId}/    # template paths MUST start with plugin/{pluginId}/
           │   ├── fragment/                   # hook fragments
           │   └── page/                       # page templates
           ├── i18n/                            # i18n bundles (basename declared in descriptor)
           └── db/migration/plugin/{pluginId}/  # plugin-scoped migrations
               └── V1__init.sql
   ```

4. **Minimal descriptor** `META-INF/jnimble-plugin.json`:
   ```json
   {
     "schemaVersion": "1.0",
     "id": "your-plugin",
     "name": "Your Plugin",
     "version": "1.0.0",
     "platformVersion": "0.1.0",
     "bootClass": "com.example.YourPluginBoot",
     "admin": { "entry": "/index", "labelKey": "your-plugin.name", "permission": "your-plugin.view" },
     "permissions": [
       { "code": "your-plugin.view", "name": "View Your Plugin", "nameKey": "your-plugin.permission.view" }
     ]
   }
   ```
   **Permission codes MUST start with `{pluginId}.`** or they won't sync to role management.

5. **Boot class**:
   ```java
   public class YourPluginBoot implements PluginBoot {
       @Override
       public void boot(PluginContext context) {
           context.hooks().register("admin.layout.sidebar",
               new HookViewContribution("plugin/your-plugin/fragment/sidebar", null, 100, "your-plugin.view", null));
           context.routes().register(new RouteDefinition("/index", RouteMethod.GET, "plugin/your-plugin/page/index", "your-plugin.view"));
       }
       @Override public void stop(PluginContext context) { /* release own resources */ }
   }
   ```

6. **Install**: drop the built JAR into `jnimble-starter/data/plugins/` — `PluginDirectoryWatcher` auto-deploys; or upload via admin "Plugin Management → Upload".
   On first install it enters `INSTALLED`; click "Enable" to call `boot()` (set `jnimble.plugins.auto-enable=true` to auto-enable).

### 3.3 Available hook points

Template paths must start with `plugin/{pluginId}/`.

| Hook point | Location |
|------------|----------|
| `admin.layout.sidebar.start` | Top of sidebar |
| `admin.layout.sidebar` | Generic sidebar slot (used by the demo plugin) |
| `admin.layout.sidebar.sys.start/end` | Start/end of "System" section |
| `admin.layout.sidebar.{plugins,roles,users}.{before/after}` | Before/after each menu item |
| `admin.layout.sidebar.end` | Bottom of sidebar |
| `admin.layout.topbar` | Top bar |

### 3.4 Database migrations (**critical**)

- **Framework migrations**: `jnimble-starter/src/main/resources/db/migration/system/`, filename `V{n}__{desc}.sql`, history table `flyway_schema_history`.
- **Plugin migrations**: declared in the descriptor:
  ```json
  "migration": {
    "enabled": true,
    "location": "classpath:db/migration/plugin/your-plugin",
    "table": "flyway_schema_history_your_plugin",
    "baselineOnMigrate": true,
    "failOnError": true
  }
  ```
  Default history table: `flyway_schema_history_{pluginId with dashes -> underscores}`.

**Migration rules the AI MUST follow**:

1. **Only add new scripts, never edit existing ones.** For column/table changes, write a new `V{next}__{desc}.sql` where next = current max version + 1.
2. **Never** modify any published `V*.sql` — Flyway checksum changes will break startup.
3. **Never** run DDL at runtime (`CREATE TABLE` / `ALTER TABLE`) to bypass Flyway.
4. **Never** hand-write SQL to alter table structure directly in the database. All schema changes go through migration scripts.
5. Uninstalling a plugin **does not** drop business tables (by design) — do not write `drop table` in migrations.
6. Use `MapperUtils` for platform single-table data access — do not call `BaseMapper` CRUD directly.
7. Migration SQL should be idempotent/replayable (use `IF NOT EXISTS`, etc.) so behavior is consistent on fresh vs existing DBs.

### 3.5 Internationalization

- Framework: `jnimble-admin-shell/src/main/resources/i18n/messages*.properties` (default / `_zh_CN` / `_en_US`).
- Plugins: declare `"i18n": { "basename": "i18n.your-plugin" }` in the descriptor, files at `i18n/your-plugin*.properties`.
- When adding a message entry, **sync all three files** (default / zh_CN / en_US) or some locales will render `??key??`.
- Match existing file style for Chinese: this project uses raw UTF-8 Chinese (not `\uXXXX` escapes).

### 3.6 Static assets

```java
context.assets().register(new AssetDefinition(
    "css/your-plugin.css",                        // request path
    "classpath:assets/your-plugin/css/app.css",    // resource location
    true                                          // cacheable
));
```

---

## 4. Code Conventions

### Java
- Java 21 features allowed (records, switch pattern matching, text blocks).
- Package prefixes: framework `com.jnimble.*`; plugins use custom packages (e.g. `com.example.*`).
- **Do not write comments** unless explicitly asked — code is self-explanatory.
- Service layer must not call `BaseMapper` CRUD directly — use `MapperUtils`.
- Entity classes extend platform base classes (see `jnimble-platform` entity package).

### Templates (Thymeleaf)
- All plugin template paths MUST start with `plugin/{pluginId}/`.
- Admin layout uses `layout:fragment="content"` for content, `layout:fragment="page-scripts"` for scripts.
- Pages must have server-side permission checks (declare `permission` on the route) — **do not** rely only on frontend hiding.

### Commit conventions
- Conventional Commits: `feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `chore:`
- One commit = one concern. Don't mix framework changes with plugin changes.

### Tests
- Platform/framework changes must have tests (H2 in-memory DB + `@SpringBootTest`).
- Plugins must at least have a unit test for `boot()` verifying hook/route registration.
- Run: `mvn test` (surefire is configured with the mockito javaagent).

---

## 5. AI Development Rules (**must follow**)

1. **❌ Do not** modify `jnimble-kernel` / `jnimble-platform` / `jnimble-admin-shell` source to add business features — build a plugin instead.
2. **❌ Do not** modify any existing `V*.sql` migration script (checksum will change).
3. **❌ Do not** hand-write SQL to alter table structure directly — all schema changes go through migration scripts.
4. **❌ Do not** execute DDL at runtime (`CREATE TABLE` / `ALTER TABLE`, etc.).
5. **❌ Do not** bypass Flyway to auto-create tables via `BaseMapper`.
6. **❌ Do not** remove or tamper with the copyright footer in the admin UI or login page — this is a legal requirement of the dual-license model (see `NOTICE`). Use the commercial license for copyright removal.
7. **❌ Do not** call `BaseMapper` CRUD methods directly — use `MapperUtils`.
8. **❌ Do not** write comments unless the user asks for them.
9. **❌ Do not** commit (`git commit`) unless the user explicitly says "commit" or "push".
10. **❌ Do not** refactor working code for "elegance" — refactoring requires prior approval.
11. **❌ Do not** add features the user didn't ask for (avoid over-engineering).
12. **❌ Do not** use emoji (unless the user asks).

---

## 6. Common Commands

```bash
# Full build (skip tests and static checks — for development)
mvn clean install -DskipTests -Dcheckstyle.skip=true -Dspotbugs.skip=true

# Run the app
mvn -pl jnimble-starter spring-boot:run

# Run tests
mvn test

# Build a single plugin JAR
mvn -pl <plugin-module> package

# Check Flyway migration status
mysql -u<user> -p<pwd> jnimble -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# Check plugin state
mysql -u<user> -p<pwd> jnimble -e "SELECT plugin_id, status, enabled, last_error FROM jnimble_plugin_state;"
```

---

## 7. Key Paths

- App entry: `jnimble-starter/src/main/java/com/jnimble/starter/JNimbleApplication.java`
- Main config: `jnimble-starter/src/main/resources/application.yml`
- System migrations: `jnimble-starter/src/main/resources/db/migration/system/`
- Admin layout: `jnimble-admin-shell/src/main/resources/templates/layout/admin.html`
- Login page: `jnimble-admin-shell/src/main/resources/templates/auth/login.html`
- Copyright footer: `jnimble-admin-shell/src/main/resources/templates/fragment/copyright-footer.html`
- Framework i18n: `jnimble-admin-shell/src/main/resources/i18n/messages*.properties`
- Security config: `jnimble-platform/src/main/java/com/jnimble/platform/auth/JNimbleSecurityConfiguration.java`
- Plugin SDK contracts: `jnimble-plugin-sdk/src/main/java/com/jnimble/sdk/`
- Plugin runtime: `jnimble-kernel/src/main/java/com/jnimble/kernel/`
- Reference plugin: `jnimble-demo-plugin/`

---

## 8. Licensing

This project uses a **dual-license model: Apache 2.0 + Commercial**.
- Open-source use MUST retain the copyright footer in the admin UI and login page.
- Copyright removal requires a commercial license from `178277164@qq.com`.
- Code generated by AI assistants is bound by the same terms — **do not** auto-remove copyright info.

See [NOTICE](NOTICE) and [LICENSE](LICENSE) for details.
