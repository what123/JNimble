package com.jnimble.platform.auth;

import com.jnimble.platform.persistence.crud.MapperUtils;
import com.jnimble.platform.persistence.entity.UserEntity;
import com.jnimble.platform.persistence.mapper.UserMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * MyBatis-backed implementation of {@link UserAccountService}.
 *
 * <p>Persists user account data to the database via MyBatis-Plus mappers.
 * Handles password encoding, username normalization, and account status
 * management. Usernames are treated as unique identifiers.</p>
 *
 * <p>基于 MyBatis 的 UserAccountService 实现。通过 MyBatis-Plus 映射器将用户账户
 * 数据持久化到数据库。处理密码编码、用户名规范化和账户状态管理。用户名为唯一标识。</p>
 */
public class MybatisUserAccountService implements UserAccountService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new MyBatis-backed user account service.
     *
     * @param userMapper      the MyBatis mapper for user table
     * @param passwordEncoder the password encoder for credential hashing
     */
    public MybatisUserAccountService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRecord createUser(String username, String rawPassword, String displayName) {
        Instant now = Instant.now();
        String normalizedUsername = requireNonBlank(username, "username");
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsername(normalizedUsername);
        entity.setPasswordHash(passwordEncoder.encode(requireNonBlank(rawPassword, "password")));
        entity.setDisplayName(blankToDefault(displayName, normalizedUsername));
        entity.setStatus(UserStatus.ACTIVE.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toRecord(MapperUtils.insert(userMapper, entity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserRecord> findByUsername(String username) {
        String normalizedUsername = requireNonBlank(username, "username");
        return Optional.ofNullable(MapperUtils.selectOne(userMapper, UserEntity.class,
                        wrapper -> wrapper.eq("username", normalizedUsername)))
                .map(this::toRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<UserRecord> listUsers() {
        return MapperUtils.selectList(userMapper, UserEntity.class, wrapper -> wrapper.orderByAsc("username")).stream()
                .map(this::toRecord)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRecord updateDisplayName(String username, String displayName) {
        String normalizedUsername = requireNonBlank(username, "username");
        String normalizedDisplayName = blankToDefault(displayName, normalizedUsername);
        UserEntity update = new UserEntity();
        update.setDisplayName(normalizedDisplayName);
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateOne(userMapper, update, UserEntity.class, wrapper -> wrapper.eq("username", normalizedUsername));
        return findByUsername(normalizedUsername).orElseThrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableUser(String username) {
        updateStatus(username, UserStatus.ACTIVE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableUser(String username) {
        updateStatus(username, UserStatus.DISABLED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetPassword(String username, String rawPassword) {
        String normalizedUsername = requireNonBlank(username, "username");
        UserEntity update = new UserEntity();
        update.setPasswordHash(passwordEncoder.encode(requireNonBlank(rawPassword, "password")));
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateOne(userMapper, update, UserEntity.class, wrapper -> wrapper.eq("username", normalizedUsername));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRecord changeUsername(String oldUsername, String newUsername) {
        String normalizedOld = requireNonBlank(oldUsername, "oldUsername");
        String normalizedNew = requireNonBlank(newUsername, "newUsername");
        if (normalizedOld.equals(normalizedNew)) {
            return findByUsername(normalizedOld).orElseThrow(() ->
                    new IllegalArgumentException("User not found: " + normalizedOld));
        }
        UserEntity update = new UserEntity();
        update.setUsername(normalizedNew);
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateOne(userMapper, update, UserEntity.class, wrapper -> wrapper.eq("username", normalizedOld));
        return findByUsername(normalizedNew).orElseThrow();
    }

    private void updateStatus(String username, UserStatus status) {
        String normalizedUsername = requireNonBlank(username, "username");
        UserEntity update = new UserEntity();
        update.setStatus(status.name());
        update.setUpdatedAt(Instant.now());
        MapperUtils.updateOne(userMapper, update, UserEntity.class, wrapper -> wrapper.eq("username", normalizedUsername));
    }

    private UserRecord toRecord(UserEntity entity) {
        return new UserRecord(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                UserStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
}
