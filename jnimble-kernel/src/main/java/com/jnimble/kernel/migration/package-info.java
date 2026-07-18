/**
 * Plugin database migration support.
 *
 * <p>Provides migration execution based on Flyway, cleanup plan parsing from SQL
 * scripts, and configuration models. Each plugin can declare its own migration
 * scripts and history table.</p>
 *
 * JNimble 插件数据库迁移支持。提供基于 Flyway 的迁移执行、
 * 从 SQL 脚本解析清理计划以及配置模型。每个插件可以声明自己的迁移脚本和历史表。
 */
package com.jnimble.kernel.migration;