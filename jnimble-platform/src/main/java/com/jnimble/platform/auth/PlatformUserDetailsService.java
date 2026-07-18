package com.jnimble.platform.auth;

import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Spring Security {@link UserDetailsService} implementation backed by the platform's
 * {@link UserAccountService}.
 *
 * <p>Loads user details from the database for authentication. Only active users
 * are allowed to authenticate. Authorities are delegated to the permission system
 * at the authorization layer.</p>
 *
 * <p>基于平台 UserAccountService 的 Spring Security UserDetailsService 实现。
 * 从数据库加载用户详情进行认证。仅允许活跃用户认证。权限授权委托给权限系统在授权层处理。</p>
 */
public class PlatformUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    /**
     * Creates a new platform user details service.
     *
     * @param userAccountService the user account service to load users from
     */
    public PlatformUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * Loads a user by username for authentication.
     *
     * @param username the username identifying the user
     * @return the UserDetails for the active user
     * @throws UsernameNotFoundException if the user is not found or is not active
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRecord user = userAccountService.findByUsername(username)
                .filter(UserRecord::active)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new User(user.username(), user.passwordHash(), List.of());
    }
}
