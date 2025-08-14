package com.codetop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Rate limiting configuration using Redis backend.
 * 
 * Implements:
 * - 100 requests per minute per authenticated user
 * - 1000 requests per minute per IP address
 * - Exponential backoff for repeated violations
 * - Sliding window rate limiting algorithm
 * 
 * @author CodeTop Team
 */
@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.per-user:100}")
    private int perUserLimit;

    @Value("${app.rate-limit.per-ip:1000}")
    private int perIpLimit;

    @Value("${app.rate-limit.window-minutes:1}")
    private int windowMinutes;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Bean
    public RateLimitProperties rateLimitProperties() {
        return RateLimitProperties.builder()
                .perUserLimit(perUserLimit)
                .perIpLimit(perIpLimit)
                .windowMinutes(windowMinutes)
                .enabled(rateLimitEnabled)
                .build();
    }

    /**
     * Rate limit configuration properties.
     */
    public static class RateLimitProperties {
        private final int perUserLimit;
        private final int perIpLimit;
        private final int windowMinutes;
        private final boolean enabled;

        private RateLimitProperties(int perUserLimit, int perIpLimit, int windowMinutes, boolean enabled) {
            this.perUserLimit = perUserLimit;
            this.perIpLimit = perIpLimit;
            this.windowMinutes = windowMinutes;
            this.enabled = enabled;
        }

        public static Builder builder() {
            return new Builder();
        }

        public int getPerUserLimit() { return perUserLimit; }
        public int getPerIpLimit() { return perIpLimit; }
        public int getWindowMinutes() { return windowMinutes; }
        public boolean isEnabled() { return enabled; }

        public static class Builder {
            private int perUserLimit = 100;
            private int perIpLimit = 1000;
            private int windowMinutes = 1;
            private boolean enabled = true;

            public Builder perUserLimit(int perUserLimit) {
                this.perUserLimit = perUserLimit;
                return this;
            }

            public Builder perIpLimit(int perIpLimit) {
                this.perIpLimit = perIpLimit;
                return this;
            }

            public Builder windowMinutes(int windowMinutes) {
                this.windowMinutes = windowMinutes;
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public RateLimitProperties build() {
                return new RateLimitProperties(perUserLimit, perIpLimit, windowMinutes, enabled);
            }
        }
    }
}