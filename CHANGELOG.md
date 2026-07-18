# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Plugin-driven Java admin framework with Maven multi-module setup
- Plugin SDK with boot, hook, route, asset, migration, and i18n contracts
- Plugin runtime with lifecycle management (install, enable, disable, uninstall, reload)
- Plugin discovery via classpath, JAR upload, and hot-deploy directory scan/watch
- Admin shell with layout, pages, and templates
- User, role, and permission management with server-side enforcement
- Audit logging system
- Plugin state management
- Database migration support for system and plugins (Flyway)
- Internationalization (i18n) support for framework and plugins
- Hook system with Thymeleaf `jn:hook` dialect
- Plugin configuration with encrypted secret fields and declarative configuration forms
- System settings (site name, logo, and storage directories) with runtime admin UI
- Minimal demo plugin showing sidebar menu registration, page route, and permission declaration
- Rewritten root READMEs highlighting the plugin mechanism (building-block development, cross-project reuse, pay-per-feature commercialization)

### Removed

- License verification modules (`jnimble-license-sdk` and `jnimble-license-core`) — license verification is now delegated to a standalone SDK that business plugins can depend on, keeping the framework license-free
- Redundant `doc/` and `docs/` directories (design notes and guides) — the root READMEs are now the single source of truth for plugin development
