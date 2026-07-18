package com.jnimble.platform.permission;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.PermissionEntity;
import com.jnimble.platform.persistence.entity.RoleEntity;
import com.jnimble.platform.persistence.entity.RolePermissionEntity;
import com.jnimble.platform.persistence.entity.SubjectRoleEntity;
import com.jnimble.platform.persistence.mapper.PermissionMapper;
import com.jnimble.platform.persistence.mapper.RoleMapper;
import com.jnimble.platform.persistence.mapper.RolePermissionMapper;
import com.jnimble.platform.persistence.mapper.SubjectRoleMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis-backed implementation combining {@link RoleService}, {@link PermissionService},
 * and {@link AuthorizationService}.
 *
 * <p>Provides complete RBAC functionality including role CRUD, permission registration
 * and management, subject-role assignments, and authorization checks. All data is
 * persisted via MyBatis-Plus mappers with transactional support where needed.</p>
 *
 * <p>基于 MyBatis 的组合实现，整合了 RoleService、PermissionService 和 AuthorizationService。
 * 提供完整的 RBAC 功能，包括角色 CRUD、权限注册与管理、主体-角色分配和授权检查。
 * 所有数据通过 MyBatis-Plus 映射器持久化，必要时支持事务。</p>
 */
public class MybatisAccessControlService implements RoleService, PermissionService, AuthorizationService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final SubjectRoleMapper subjectRoleMapper;

    /**
     * Creates a new MyBatis-backed access control service.
     *
     * @param roleMapper           the MyBatis mapper for role table
     * @param permissionMapper     the MyBatis mapper for permission table
     * @param rolePermissionMapper the MyBatis mapper for role-permission association table
     * @param subjectRoleMapper    the MyBatis mapper for subject-role association table
     */
    public MybatisAccessControlService(
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            SubjectRoleMapper subjectRoleMapper
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.subjectRoleMapper = subjectRoleMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleRecord createRole(String code, String name) {
        Instant now = Instant.now();
        String normalizedCode = normalizeRoleCode(code);
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCode(normalizedCode);
        entity.setName(blankToDefault(name, normalizedCode));
        entity.setStatus(RoleStatus.ACTIVE.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toRecord(MapperUtils.insert(roleMapper, entity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RoleRecord> findRole(String roleId) {
        return Optional.ofNullable(MapperUtils.getById(roleMapper, requireNonBlank(roleId, "roleId"), null))
                .map(this::toRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RoleRecord> findRoleByCode(String code) {
        String normalizedCode = normalizeRoleCode(code);
        return Optional.ofNullable(MapperUtils.selectOne(roleMapper, RoleEntity.class,
                        wrapper -> wrapper.eq("code", normalizedCode)))
                .map(this::toRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RoleRecord> listRoles() {
        return MapperUtils.selectList(roleMapper, RoleEntity.class, wrapper -> wrapper.orderByAsc("code"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRoleName(String roleId, String name) {
        RoleEntity entity = new RoleEntity();
        entity.setId(requireNonBlank(roleId, "roleId"));
        entity.setName(requireNonBlank(name, "name"));
        entity.setUpdatedAt(Instant.now());
        MapperUtils.updateById(roleMapper, entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableRole(String roleId) {
        RoleEntity entity = new RoleEntity();
        entity.setId(requireNonBlank(roleId, "roleId"));
        entity.setStatus(RoleStatus.DISABLED.name());
        entity.setUpdatedAt(Instant.now());
        MapperUtils.updateById(roleMapper, entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void grantPermission(String roleId, String permissionCode) {
        String normalizedRoleId = requireNonBlank(roleId, "roleId");
        String normalizedPermissionCode = requireNonBlank(permissionCode, "permissionCode");
        requireActiveRole(normalizedRoleId);
        PermissionStatus status = isPermissionAvailable(normalizedPermissionCode)
                ? PermissionStatus.AVAILABLE
                : PermissionStatus.UNAVAILABLE;
        Instant now = Instant.now();
        if (rolePermissionExists(normalizedRoleId, normalizedPermissionCode)) {
            RolePermissionEntity update = new RolePermissionEntity();
            update.setStatus(status.name());
            update.setUpdatedAt(now);
            MapperUtils.updateOne(rolePermissionMapper, update, RolePermissionEntity.class,
                    wrapper -> wrapper.eq("role_id", normalizedRoleId)
                            .eq("permission_code", normalizedPermissionCode));
            return;
        }

        RolePermissionEntity entity = new RolePermissionEntity();
        entity.setRoleId(normalizedRoleId);
        entity.setPermissionCode(normalizedPermissionCode);
        entity.setStatus(status.name());
        entity.setGrantedAt(now);
        entity.setUpdatedAt(now);
        MapperUtils.insert(rolePermissionMapper, entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokePermission(String roleId, String permissionCode) {
        MapperUtils.deleteByCondition(rolePermissionMapper, RolePermissionEntity.class,
                wrapper -> wrapper.eq("role_id", requireNonBlank(roleId, "roleId"))
                        .eq("permission_code", requireNonBlank(permissionCode, "permissionCode")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RolePermissionGrant> listRolePermissions(String roleId) {
        return MapperUtils.selectList(rolePermissionMapper, RolePermissionEntity.class,
                        wrapper -> wrapper.eq("role_id", requireNonBlank(roleId, "roleId"))
                                .orderByAsc("permission_code"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markPermissionGrantsAvailable(String permissionCode) {
        markPermissionGrantsStatus(permissionCode, PermissionStatus.AVAILABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markPermissionGrantsUnavailable(String permissionCode) {
        markPermissionGrantsStatus(permissionCode, PermissionStatus.UNAVAILABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void grantRoleToSubject(String subjectId, String roleId) {
        String normalizedSubjectId = requireNonBlank(subjectId, "subjectId");
        String normalizedRoleId = requireNonBlank(roleId, "roleId");
        requireActiveRole(normalizedRoleId);
        if (subjectRoleExists(normalizedSubjectId, normalizedRoleId)) {
            return;
        }
        SubjectRoleEntity entity = new SubjectRoleEntity();
        entity.setSubjectId(normalizedSubjectId);
        entity.setRoleId(normalizedRoleId);
        entity.setGrantedAt(Instant.now());
        MapperUtils.insert(subjectRoleMapper, entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeRoleFromSubject(String subjectId, String roleId) {
        MapperUtils.deleteByCondition(subjectRoleMapper, SubjectRoleEntity.class,
                wrapper -> wrapper.eq("subject_id", requireNonBlank(subjectId, "subjectId"))
                        .eq("role_id", requireNonBlank(roleId, "roleId")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SubjectRoleGrant> listSubjectRoles(String subjectId) {
        return MapperUtils.selectList(subjectRoleMapper, SubjectRoleEntity.class,
                        wrapper -> wrapper.eq("subject_id", requireNonBlank(subjectId, "subjectId"))
                                .orderByAsc("role_id"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SubjectRoleGrant> listSubjectRoles() {
        return MapperUtils.selectList(subjectRoleMapper, SubjectRoleEntity.class,
                        wrapper -> wrapper.orderByAsc("subject_id").orderByAsc("role_id"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SubjectRoleGrant> listRoleSubjects(String roleId) {
        return MapperUtils.selectList(subjectRoleMapper, SubjectRoleEntity.class,
                        wrapper -> wrapper.eq("role_id", requireNonBlank(roleId, "roleId"))
                                .orderByAsc("subject_id"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean subjectHasPermission(String subjectId, String permissionCode) {
        return hasPermission(subjectId, permissionCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void registerPluginPermissions(String pluginId, Collection<PermissionDefinition> permissions) {
        String ownerPluginId = requireNonBlank(pluginId, "pluginId");
        List<PermissionDefinition> definitions = normalizedDefinitions(permissions);
        requireUniquePermissionCodes(definitions);
        Set<String> incomingCodes = definitions.stream()
                .map(PermissionDefinition::code)
                .collect(Collectors.toSet());
        requirePluginPermissionPrefix(ownerPluginId, incomingCodes);
        requirePermissionOwnership(ownerPluginId, incomingCodes);

        Set<String> existingCodes = permissions(ownerPluginId).stream()
                .map(PermissionRecord::code)
                .collect(Collectors.toSet());
        existingCodes.stream()
                .filter(code -> !incomingCodes.contains(code))
                .forEach(code -> {
                    updatePermissionStatus(code, PermissionStatus.UNAVAILABLE);
                    markPermissionGrantsUnavailable(code);
                });

        definitions.forEach(permission -> upsertPermission(ownerPluginId, permission));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void markPluginPermissionsUnavailable(String pluginId) {
        permissions(requireNonBlank(pluginId, "pluginId")).forEach(permission -> {
            updatePermissionStatus(permission.code(), PermissionStatus.UNAVAILABLE);
            markPermissionGrantsUnavailable(permission.code());
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PluginPermissionGroup> listPermissionsByPlugin() {
        return MapperUtils.selectList(permissionMapper, PermissionEntity.class,
                        wrapper -> wrapper.orderByAsc("plugin_id").orderByAsc("code"))
                .stream()
                .map(this::toRecord)
                .collect(Collectors.groupingBy(
                        PermissionRecord::pluginId,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new PluginPermissionGroup(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PermissionRecord> findPermission(String permissionCode) {
        return Optional.ofNullable(MapperUtils.getById(permissionMapper,
                        requireNonBlank(permissionCode, "permissionCode"), null))
                .map(this::toRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPermissionAvailable(String permissionCode) {
        return findPermission(permissionCode)
                .map(PermissionRecord::available)
                .orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPermission(String permissionCode) {
        return isPermissionAvailable(permissionCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPermission(String subjectId, String permissionCode) {
        return subjectRoleMapper.countSubjectPermission(
                requireNonBlank(subjectId, "subjectId"),
                requireNonBlank(permissionCode, "permissionCode"),
                RoleStatus.ACTIVE.name(),
                PermissionStatus.AVAILABLE.name()) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean roleHasPermission(String roleId, String permissionCode) {
        return rolePermissionMapper.countActiveRolePermission(
                requireNonBlank(roleId, "roleId"),
                requireNonBlank(permissionCode, "permissionCode"),
                RoleStatus.ACTIVE.name(),
                PermissionStatus.AVAILABLE.name()) > 0;
    }

    private List<PermissionRecord> permissions(String pluginId) {
        return MapperUtils.selectList(permissionMapper, PermissionEntity.class,
                        wrapper -> wrapper.eq("plugin_id", pluginId).orderByAsc("code"))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private void upsertPermission(String pluginId, PermissionDefinition permission) {
        Instant now = Instant.now();
        PermissionEntity entity = new PermissionEntity();
        entity.setCode(permission.code());
        entity.setPluginId(pluginId);
        entity.setName(permission.name());
        entity.setNameKey(permission.nameKey());
        entity.setDescription(permission.description());
        entity.setDescriptionKey(permission.descriptionKey());
        entity.setStatus(PermissionStatus.AVAILABLE.name());
        entity.setUpdatedAt(now);
        if (findPermission(permission.code()).isPresent()) {
            MapperUtils.updateById(permissionMapper, entity);
        } else {
            MapperUtils.insert(permissionMapper, entity);
        }
        markPermissionGrantsAvailable(permission.code());
    }

    private void updatePermissionStatus(String permissionCode, PermissionStatus status) {
        PermissionEntity update = new PermissionEntity();
        update.setCode(permissionCode);
        update.setStatus(status.name());
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateById(permissionMapper, update);
    }

    private void markPermissionGrantsStatus(String permissionCode, PermissionStatus status) {
        RolePermissionEntity update = new RolePermissionEntity();
        update.setStatus(status.name());
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateByCondition(rolePermissionMapper, update, RolePermissionEntity.class,
                wrapper -> wrapper.eq("permission_code", requireNonBlank(permissionCode, "permissionCode")));
    }

    private boolean rolePermissionExists(String roleId, String permissionCode) {
        return MapperUtils.existsByCondition(rolePermissionMapper, RolePermissionEntity.class,
                wrapper -> wrapper.eq("role_id", requireNonBlank(roleId, "roleId"))
                        .eq("permission_code", requireNonBlank(permissionCode, "permissionCode")));
    }

    private boolean subjectRoleExists(String subjectId, String roleId) {
        return MapperUtils.existsByCondition(subjectRoleMapper, SubjectRoleEntity.class,
                wrapper -> wrapper.eq("subject_id", requireNonBlank(subjectId, "subjectId"))
                        .eq("role_id", requireNonBlank(roleId, "roleId")));
    }

    private void requireActiveRole(String roleId) {
        if (findRole(roleId).filter(RoleRecord::active).isEmpty()) {
            throw new IllegalArgumentException("Role is not active: " + roleId);
        }
    }

    private List<PermissionDefinition> normalizedDefinitions(Collection<PermissionDefinition> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream()
                .filter(permission -> permission != null)
                .map(permission -> new PermissionDefinition(
                        requireNonBlank(permission.code(), "permission code"),
                        trimToNull(permission.name()),
                        trimToNull(permission.nameKey()),
                        trimToNull(permission.description()),
                        trimToNull(permission.descriptionKey())))
                .sorted(Comparator.comparing(PermissionDefinition::code))
                .toList();
    }

    private void requireUniquePermissionCodes(Collection<PermissionDefinition> definitions) {
        Set<String> seen = new HashSet<>();
        for (PermissionDefinition definition : definitions) {
            if (!seen.add(definition.code())) {
                throw new IllegalArgumentException("Duplicate permission code " + definition.code());
            }
        }
    }

    private void requirePermissionOwnership(String pluginId, Set<String> permissionCodes) {
        for (String permissionCode : permissionCodes) {
            PermissionRecord existing = findPermission(permissionCode).orElse(null);
            if (existing != null && !existing.pluginId().equals(pluginId)) {
                throw new IllegalArgumentException("Permission code " + permissionCode
                        + " already belongs to plugin " + existing.pluginId());
            }
        }
    }

    private void requirePluginPermissionPrefix(String pluginId, Set<String> permissionCodes) {
        String requiredPrefix = pluginId + ".";
        for (String permissionCode : permissionCodes) {
            if (!permissionCode.startsWith(requiredPrefix)) {
                throw new IllegalArgumentException("Permission code " + permissionCode
                        + " must start with " + requiredPrefix);
            }
        }
    }

    private RoleRecord toRecord(RoleEntity entity) {
        return new RoleRecord(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                RoleStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PermissionRecord toRecord(PermissionEntity entity) {
        return new PermissionRecord(
                entity.getPluginId(),
                entity.getCode(),
                entity.getName(),
                entity.getNameKey(),
                entity.getDescription(),
                entity.getDescriptionKey(),
                PermissionStatus.valueOf(entity.getStatus()),
                entity.getUpdatedAt()
        );
    }

    private RolePermissionGrant toRecord(RolePermissionEntity entity) {
        return new RolePermissionGrant(
                entity.getRoleId(),
                entity.getPermissionCode(),
                PermissionStatus.valueOf(entity.getStatus()),
                entity.getGrantedAt(),
                entity.getUpdatedAt()
        );
    }

    private SubjectRoleGrant toRecord(SubjectRoleEntity entity) {
        return new SubjectRoleGrant(entity.getSubjectId(), entity.getRoleId(), entity.getGrantedAt());
    }

    private static String normalizeRoleCode(String roleCode) {
        return requireNonBlank(roleCode, "role code").toUpperCase();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
