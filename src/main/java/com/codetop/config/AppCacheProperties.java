package com.codetop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {
    /**
     * Delay for second-phase cache invalidation (double delete), milliseconds.
     */
    private long doubleDeleteDelayMillis = 500;

    public long getDoubleDeleteDelayMillis() {
        return doubleDeleteDelayMillis;
    }

    public void setDoubleDeleteDelayMillis(long doubleDeleteDelayMillis) {
        this.doubleDeleteDelayMillis = doubleDeleteDelayMillis;
    }
}

