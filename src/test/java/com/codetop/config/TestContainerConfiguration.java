package com.codetop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * TestContainers configuration for integration tests.
 * 
 * Provides containerized services:
 * - MySQL 8.0 database for realistic database testing
 * - Redis 7 for caching and rate limiting tests
 * - Proper lifecycle management and resource cleanup
 * 
 * @author CodeTop Team
 */
@TestConfiguration
@Testcontainers
@Slf4j
public class TestContainerConfiguration {

    // MySQL TestContainer
    @Container
    public static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("fsrs_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .withEnv("MYSQL_ROOT_PASSWORD", "root_password")
            .withEnv("MYSQL_INNODB_BUFFER_POOL_SIZE", "128M")
            .withEnv("MYSQL_INNODB_LOG_FILE_SIZE", "64M")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1));

    // Redis TestContainer
    @Container
    public static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--appendonly", "yes", "--maxmemory", "128mb", "--maxmemory-policy", "allkeys-lru")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

    static {
        // Start containers
        MYSQL_CONTAINER.start();
        REDIS_CONTAINER.start();
        
        log.info("TestContainers started successfully:");
        log.info("MySQL - URL: {}, Username: {}", MYSQL_CONTAINER.getJdbcUrl(), MYSQL_CONTAINER.getUsername());
        log.info("Redis - Host: {}, Port: {}", REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getFirstMappedPort());
    }

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL properties
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis properties
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getFirstMappedPort().toString());

        // JPA properties for MySQL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
        registry.add("spring.jpa.show-sql", () -> "true");

        // Flyway properties
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-on-validation-error", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }

    @Bean
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create()
                .url(MYSQL_CONTAINER.getJdbcUrl())
                .username(MYSQL_CONTAINER.getUsername())
                .password(MYSQL_CONTAINER.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    /**
     * Utility methods for test data setup and cleanup.
     */
    public static class TestContainerUtils {
        
        /**
         * Execute SQL script in MySQL container.
         */
        public static void executeSqlScript(String script) {
            try {
                MYSQL_CONTAINER.execInContainer("mysql", 
                        "-u", MYSQL_CONTAINER.getUsername(),
                        "-p" + MYSQL_CONTAINER.getPassword(),
                        "-D", MYSQL_CONTAINER.getDatabaseName(),
                        "-e", script);
            } catch (Exception e) {
                log.error("Failed to execute SQL script", e);
                throw new RuntimeException("Failed to execute SQL script", e);
            }
        }

        /**
         * Clean Redis data for test isolation.
         */
        public static void cleanRedisData() {
            try {
                REDIS_CONTAINER.execInContainer("redis-cli", "FLUSHALL");
            } catch (Exception e) {
                log.error("Failed to clean Redis data", e);
                throw new RuntimeException("Failed to clean Redis data", e);
            }
        }

        /**
         * Get MySQL container JDBC URL for manual connections.
         */
        public static String getMySQLJdbcUrl() {
            return MYSQL_CONTAINER.getJdbcUrl();
        }

        /**
         * Get Redis connection string for manual connections.
         */
        public static String getRedisConnectionString() {
            return String.format("redis://%s:%d", 
                    REDIS_CONTAINER.getHost(), 
                    REDIS_CONTAINER.getFirstMappedPort());
        }

        /**
         * Wait for containers to be fully ready.
         */
        public static void waitForContainers() {
            // Additional health checks if needed
            try {
                // Test MySQL connection
                MYSQL_CONTAINER.execInContainer("mysqladmin", 
                        "-u", MYSQL_CONTAINER.getUsername(),
                        "-p" + MYSQL_CONTAINER.getPassword(),
                        "ping");
                
                // Test Redis connection
                REDIS_CONTAINER.execInContainer("redis-cli", "ping");
                
                log.info("All test containers are ready");
            } catch (Exception e) {
                log.error("Container health check failed", e);
                throw new RuntimeException("Container health check failed", e);
            }
        }
    }
}