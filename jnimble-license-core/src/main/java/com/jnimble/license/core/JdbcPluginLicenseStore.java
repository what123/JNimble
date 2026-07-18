package com.jnimble.license.core;

import com.jnimble.license.sdk.LicenseStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
class JdbcPluginLicenseStore implements PluginLicenseStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcPluginLicenseStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PluginLicenseRecord> find(String pluginId) {
        return jdbcTemplate.query("""
                        select plugin_id, license_id, token, token_hash, issuer, key_id, product_code,
                               machine_code, issued_at, not_before, expires_at, time_snapshot,
                               snapshot_sequence, status, failure_code, created_at, updated_at
                          from jnimble_plugin_license
                         where plugin_id = ?
                        """, JdbcPluginLicenseStore::mapRow, pluginId).stream().findFirst();
    }

    @Override
    public List<PluginLicenseRecord> list() {
        return jdbcTemplate.query("""
                select plugin_id, license_id, token, token_hash, issuer, key_id, product_code,
                       machine_code, issued_at, not_before, expires_at, time_snapshot,
                       snapshot_sequence, status, failure_code, created_at, updated_at
                  from jnimble_plugin_license
                 order by plugin_id
                """, JdbcPluginLicenseStore::mapRow);
    }

    @Override
    public void save(PluginLicenseRecord record) {
        if (find(record.pluginId()).isEmpty()) {
            jdbcTemplate.update("""
                            insert into jnimble_plugin_license (
                                plugin_id, license_id, token, token_hash, issuer, key_id, product_code,
                                machine_code, issued_at, not_before, expires_at, time_snapshot,
                                snapshot_sequence, status, failure_code, created_at, updated_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, values(record));
            return;
        }
        jdbcTemplate.update("""
                        update jnimble_plugin_license
                           set license_id = ?, token = ?, token_hash = ?, issuer = ?, key_id = ?,
                               product_code = ?, machine_code = ?, issued_at = ?, not_before = ?,
                               expires_at = ?, time_snapshot = ?, snapshot_sequence = ?, status = ?,
                               failure_code = ?, updated_at = ?
                         where plugin_id = ?
                        """,
                record.licenseId(), record.token(), record.tokenHash(), record.issuer(), record.keyId(),
                record.productCode(), record.machineCode(), timestamp(record.issuedAt()),
                timestamp(record.notBefore()), timestamp(record.expiresAt()), record.timeSnapshot(),
                record.snapshotSequence(), record.status().name(), record.failureCode(),
                timestamp(record.updatedAt()), record.pluginId());
    }

    @Override
    public void delete(String pluginId) {
        jdbcTemplate.update("delete from jnimble_plugin_license where plugin_id = ?", pluginId);
    }

    @Override
    public void event(String pluginId, String action, String status, String detail) {
        jdbcTemplate.update("""
                        insert into jnimble_plugin_license_event (
                            plugin_id, action, status, detail, occurred_at
                        ) values (?, ?, ?, ?, ?)
                        """, pluginId, action, status, detail, timestamp(Instant.now()));
    }

    private Object[] values(PluginLicenseRecord record) {
        return new Object[]{
                record.pluginId(), record.licenseId(), record.token(), record.tokenHash(), record.issuer(),
                record.keyId(), record.productCode(), record.machineCode(), timestamp(record.issuedAt()),
                timestamp(record.notBefore()), timestamp(record.expiresAt()), record.timeSnapshot(),
                record.snapshotSequence(), record.status().name(), record.failureCode(),
                timestamp(record.createdAt()), timestamp(record.updatedAt())
        };
    }

    private static PluginLicenseRecord mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PluginLicenseRecord(
                rs.getString("plugin_id"), rs.getString("license_id"), rs.getString("token"),
                rs.getString("token_hash"), rs.getString("issuer"), rs.getString("key_id"),
                rs.getString("product_code"), rs.getString("machine_code"),
                instant(rs.getTimestamp("issued_at")), instant(rs.getTimestamp("not_before")),
                instant(rs.getTimestamp("expires_at")), rs.getString("time_snapshot"),
                rs.getLong("snapshot_sequence"), LicenseStatus.valueOf(rs.getString("status")),
                rs.getString("failure_code"), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at"))
        );
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
