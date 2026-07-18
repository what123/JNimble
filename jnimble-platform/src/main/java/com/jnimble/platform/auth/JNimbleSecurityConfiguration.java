package com.jnimble.platform.auth;

import com.jnimble.platform.persistence.mapper.UserMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the JNimble platform.
 *
 * <p>Configures the security filter chain for admin console access, form-based
 * login/logout, and creates essential beans for user account management,
 * password encoding, and user details loading.</p>
 *
 * <p>JNimble 平台的 Spring Security 配置。配置管理控制台的安全过滤器链、
 * 基于表单的登录/登出，并创建用户账户管理、密码编码和用户详情加载等核心 Bean。</p>
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class JNimbleSecurityConfiguration {

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * <p>Public paths (login, error, health, assets) are permitted without authentication.
     * Admin paths require authentication. Form login and logout are configured.</p>
     *
     * @param http the HttpSecurity to configure
     * @return the built SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    SecurityFilterChain jnimbleSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/actuator/health", "/assets/system/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .build();
    }

    /**
     * Creates the user account service bean.
     *
     * @param userMapper       the MyBatis mapper for user table
     * @param passwordEncoder  the password encoder for credential hashing
     * @return a new MybatisUserAccountService instance
     */
    @Bean
    UserAccountService jnimbleUserAccountService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        return new MybatisUserAccountService(userMapper, passwordEncoder);
    }

    /**
     * Creates the default user initializer bean if no custom one is defined.
     *
     * @param properties           the authentication properties
     * @param userAccountService   the user account service
     * @return a new DefaultUserInitializer instance
     */
    @Bean
    @ConditionalOnMissingBean(DefaultUserInitializer.class)
    DefaultUserInitializer jnimbleDefaultUserInitializer(
            AuthProperties properties,
            UserAccountService userAccountService
    ) {
        return new DefaultUserInitializer(properties, userAccountService);
    }

    /**
     * Creates the platform user details service if no custom one is defined.
     *
     * @param userAccountService the user account service
     * @return a new PlatformUserDetailsService instance
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    UserDetailsService jnimblePlatformUserDetailsService(UserAccountService userAccountService) {
        return new PlatformUserDetailsService(userAccountService);
    }

    /**
     * Creates a delegating password encoder that supports multiple encoding schemes.
     *
     * @return a PasswordEncoder instance
     */
    @Bean
    PasswordEncoder jnimblePasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
