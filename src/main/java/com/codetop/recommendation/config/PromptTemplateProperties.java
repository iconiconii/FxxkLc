package com.codetop.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for external prompt templates.
 * Supports A/B testing by allowing different templates for different versions.
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.prompt.templates")
public class PromptTemplateProperties {
    
    /**
     * Default prompt version to use when not specified.
     */
    private String defaultVersion = "v3";
    
    /**
     * Whether to enable template caching for performance.
     */
    private boolean cacheEnabled = true;
    
    /**
     * Cache TTL in minutes for loaded templates.
     */
    private int cacheTimeoutMinutes = 60;
    
    /**
     * Base path for template files in resources.
     */
    private String templateBasePath = "prompts";
    
    /**
     * Template file mappings for different versions and types.
     * Key format: "{version}.{type}" (e.g., "v1.system", "v2.user")
     */
    private Map<String, String> templateFiles = new HashMap<>();
    
    /**
     * A/B test configuration for template selection based on user segments.
     */
    private Map<String, VersionMapping> abTests = new HashMap<>();
    
    @Data
    public static class VersionMapping {
        /**
         * User tiers that should use this version.
         */
        private String[] tiers = {};
        
        /**
         * AB groups that should use this version.
         */
        private String[] abGroups = {};
        
        /**
         * Routes that should use this version.
         */
        private String[] routes = {};
        
        /**
         * Prompt version to use for matching segments.
         */
        private String version;
        
        /**
         * Weight for weighted random selection (0-100).
         */
        private int weight = 100;
    }
    
    /**
     * Get template file path for a specific version and type.
     */
    public String getTemplateFile(String version, String type) {
        String key = version + "." + type;
        return templateFiles.getOrDefault(key, getDefaultTemplateFile(version, type));
    }
    
    private String getDefaultTemplateFile(String version, String type) {
        return templateBasePath + "/" + type + "-" + version + ".txt";
    }
    
    /**
     * Determine which prompt version to use based on user context.
     */
    public String selectVersion(String userTier, String abGroup, String route) {
        // Check A/B test configurations
        for (Map.Entry<String, VersionMapping> entry : abTests.entrySet()) {
            VersionMapping mapping = entry.getValue();
            
            if (matches(mapping.getTiers(), userTier) ||
                matches(mapping.getAbGroups(), abGroup) ||
                matches(mapping.getRoutes(), route)) {
                return mapping.getVersion();
            }
        }
        
        return defaultVersion;
    }
    
    private boolean matches(String[] values, String target) {
        if (values == null || values.length == 0 || target == null) {
            return false;
        }
        
        for (String value : values) {
            if (value.equals(target) || "*".equals(value)) {
                return true;
            }
        }
        return false;
    }
}