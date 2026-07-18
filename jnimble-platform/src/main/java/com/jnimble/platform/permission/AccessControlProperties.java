package com.jnimble.platform.permission;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the access control system.
 *
 * <p>Currently a placeholder for future access control configuration options.
 * Bound to the {@code jnimble.access-control} prefix.</p>
 *
 * <p>访问控制系统的配置属性。当前为预留的空配置类，绑定到 {@code jnimble.access-control} 前缀。</p>
 */
@ConfigurationProperties(prefix = "jnimble.access-control")
public class AccessControlProperties {


}
