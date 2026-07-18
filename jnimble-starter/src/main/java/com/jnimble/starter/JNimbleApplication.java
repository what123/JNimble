package com.jnimble.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the JNimble platform.
 * Configures auto-configuration exclusions for DataSource, transaction management,
 * and Flyway to allow plugins to provide their own configurations.
 */
@SpringBootApplication(scanBasePackages = {
        "com.jnimble.starter",
        "com.jnimble.admin",
        "com.jnimble.kernel",
        "com.jnimble.platform"
})
@MapperScan(
        basePackages = "com.jnimble.platform.persistence.mapper",
        annotationClass = Mapper.class
)
public class JNimbleApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(JNimbleApplication.class, args);
    }
}
