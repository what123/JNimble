package com.jnimble.platform.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.PluginStateEntity;
import com.jnimble.platform.persistence.mapper.PluginStateMapper;
import com.jnimble.sdk.plugin.PluginSource;
import com.jnimble.sdk.plugin.PluginStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Optional;

/**
 * MyBatis-backed implementation of {@link PluginStateStore}.
 *
 * <p>Persists plugin runtime state to the database via MyBatis-Plus mappers.
 * Handles plugin descriptor JSON serialization and SHA-256 hash computation
 * for change detection.</p>
 *
 * <p>基于 MyBatis 的 PluginStateStore 实现。通过 MyBatis-Plus 映射器将插件运行时
 * 状态持久化到数据库。处理插件描述符 JSON 序列化和 SHA-256 哈希计算以检测变更。</p>
 */
public class MybatisPluginStateStore implements PluginStateStore {

    private final PluginStateMapper pluginStateMapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new MyBatis-backed plugin state store.
     *
     * @param pluginStateMapper the MyBatis mapper for plugin state table
     * @param objectMapper      the Jackson ObjectMapper for descriptor serialization
     */
    public MybatisPluginStateStore(PluginStateMapper pluginStateMapper, ObjectMapper objectMapper) {
        this.pluginStateMapper = pluginStateMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(PluginRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        Instant now = Instant.now();
        String descriptorJson = descriptorJson(snapshot);
        PluginStateEntity entity = toEntity(snapshot, descriptorJson, now);
        if (find(snapshot.pluginId()).isEmpty()) {
            entity.setCreatedAt(now);
            MapperUtils.insert(pluginStateMapper, entity);
            return;
        }
        MapperUtils.updateOne(pluginStateMapper, entity, PluginStateEntity.class,
                wrapper -> wrapper.eq("plugin_id", snapshot.pluginId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PluginStateRecord> find(String pluginId) {
        return Optional.ofNullable(MapperUtils.selectOne(pluginStateMapper, PluginStateEntity.class,
                        wrapper -> wrapper.eq("plugin_id", pluginId)))
                .map(this::toRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PluginStateRecord> list() {
        return MapperUtils.selectList(pluginStateMapper, PluginStateEntity.class,
                        wrapper -> wrapper.orderByAsc("plugin_id"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private PluginStateEntity toEntity(PluginRuntimeSnapshot snapshot, String descriptorJson, Instant now) {
        PluginStateEntity entity = new PluginStateEntity();
        entity.setPluginId(snapshot.pluginId());
        entity.setName(snapshot.descriptor().name());
        entity.setVersion(snapshot.descriptor().version());
        entity.setSource(snapshot.source().name());
        entity.setArtifactPath(snapshot.artifactPath() == null ? null : snapshot.artifactPath().toString());
        entity.setEnabled(snapshot.status() == PluginStatus.ENABLED);
        entity.setStatus(snapshot.status().name());
        entity.setInstalledAt(snapshot.installedAt());
        entity.setLastStartedAt(snapshot.lastStartedAt());
        entity.setLastStoppedAt(snapshot.lastStoppedAt());
        entity.setLastError(snapshot.lastError());
        entity.setDescriptorJson(descriptorJson);
        entity.setDescriptorHash(sha256(descriptorJson));
        entity.setUpdatedAt(now);
        return entity;
    }

    private PluginStateRecord toRecord(PluginStateEntity entity) {
        return new PluginStateRecord(
                entity.getPluginId(),
                entity.getName(),
                entity.getVersion(),
                PluginSource.valueOf(entity.getSource()),
                entity.getArtifactPath(),
                Boolean.TRUE.equals(entity.getEnabled()),
                PluginStatus.valueOf(entity.getStatus()),
                entity.getDescriptorJson(),
                entity.getDescriptorHash(),
                entity.getLastError(),
                entity.getInstalledAt(),
                entity.getLastStartedAt(),
                entity.getLastStoppedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String descriptorJson(PluginRuntimeSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot.descriptor());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize plugin descriptor " + snapshot.pluginId(), ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }
}
