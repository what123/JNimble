# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Plugin-friendly Java admin framework with Maven multi-module setup
- Plugin SDK with boot, hook, route, and asset contracts
- Plugin runtime with lifecycle management (install, enable, disable, uninstall, reload)
- Plugin discovery via classpath, JAR upload, and `./data/plugins` directory scan/watch
- Admin shell with layout, pages, and templates
- User, role, and permission management with server-side enforcement
- Audit logging system
- Plugin state management
- Database migration support for system and plugins (Flyway)
- Internationalization (i18n) support for framework and plugins
- Hook system with Thymeleaf `jn:hook` dialect
- Plugin configuration with encrypted secret fields
- System settings (site name and logo) with runtime admin UI
- Framework design documentation
- Plugin development guide
- Mapper utils guide
