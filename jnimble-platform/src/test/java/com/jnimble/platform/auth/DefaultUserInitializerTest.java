package com.jnimble.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

/**
 * DefaultUserInitializer 单元测试。
 *
 * <p>验证默认用户初始化逻辑，包括用户存在和不存在两种情况。</p>
 */
class DefaultUserInitializerTest {

    private AuthProperties authProperties;
    private UserAccountService userAccountService;
    private DefaultUserInitializer initializer;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        userAccountService = mock(UserAccountService.class);
        initializer = new DefaultUserInitializer(authProperties, userAccountService);
    }

    /**
     * 测试当用户不存在时创建默认用户。
     */
    @Test
    void runShouldCreateUserWhenUserDoesNotExist() {
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.empty());
        when(userAccountService.createUser("admin", "admin", "Administrator"))
                .thenReturn(new UserRecord("1", "admin", "admin", "Administrator", 
                        UserStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(userAccountService).findByUsername("admin");
        verify(userAccountService).createUser("admin", "admin", "Administrator");
    }

    /**
     * 测试当用户已存在时不创建用户。
     */
    @Test
    void runShouldNotCreateUserWhenUserAlreadyExists() {
        UserRecord existingUser = new UserRecord("1", "admin", "admin", "Administrator", 
                UserStatus.ACTIVE, Instant.now(), Instant.now());
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(userAccountService).findByUsername("admin");
        verify(userAccountService, never()).createUser(anyString(), anyString(), anyString());
    }

    /**
     * 测试使用自定义配置创建用户。
     */
    @Test
    void runShouldUseCustomConfiguration() {
        authProperties.getDefaultUser().setUsername("customadmin");
        authProperties.getDefaultUser().setPassword("custompass");
        authProperties.getDefaultUser().setDisplayName("Custom Admin");

        when(userAccountService.findByUsername("customadmin")).thenReturn(Optional.empty());
        when(userAccountService.createUser("customadmin", "custompass", "Custom Admin"))
                .thenReturn(new UserRecord("1", "customadmin", "custompass", "Custom Admin", 
                        UserStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(userAccountService).findByUsername("customadmin");
        verify(userAccountService).createUser("customadmin", "custompass", "Custom Admin");
    }

    /**
     * 测试创建用户时传递正确的参数。
     */
    @Test
    void runShouldPassCorrectParametersToCreateUser() {
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.empty());
        when(userAccountService.createUser(anyString(), anyString(), anyString()))
                .thenReturn(new UserRecord("1", "admin", "admin", "Administrator", 
                        UserStatus.ACTIVE, Instant.now(), Instant.now()));

        ApplicationArguments args = mock(ApplicationArguments.class);
        initializer.run(args);

        verify(userAccountService).createUser("admin", "admin", "Administrator");
    }

    /**
     * 测试初始化器实现 ApplicationRunner 接口。
     */
    @Test
    void shouldImplementApplicationRunner() {
        assertThat(initializer).isInstanceOf(org.springframework.boot.ApplicationRunner.class);
    }
}