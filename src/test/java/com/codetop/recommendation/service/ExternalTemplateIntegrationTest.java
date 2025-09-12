package com.codetop.recommendation.service;

import com.codetop.recommendation.config.PromptTemplateProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify external template system works with actual files.
 */
@SpringBootTest(classes = {
    ExternalPromptTemplateService.class,
    PromptTemplateProperties.class
})
@EnableConfigurationProperties(PromptTemplateProperties.class)
@TestPropertySource(properties = {
    "llm.prompt.templates.defaultVersion=v1",
    "llm.prompt.templates.cacheEnabled=false"
})
@ActiveProfiles("test")
public class ExternalTemplateIntegrationTest {

    @Autowired
    private ExternalPromptTemplateService externalTemplateService;

    @Autowired 
    private PromptTemplateProperties templateProperties;

    @Test
    void testExternalTemplateLoading() {
        // Test that we can load actual template files
        String systemMessage = externalTemplateService.buildSystemMessage("v1", null);
        
        assertThat(systemMessage)
            .contains("recommendation re-ranking engine")
            .contains("JSON");
    }

    @Test
    void testTemplatePropertiesBinding() {
        // Test that configuration properties are properly bound
        assertThat(templateProperties.getDefaultVersion()).isEqualTo("v1");
        assertThat(templateProperties.isCacheEnabled()).isFalse();
    }

    @Test
    void testVersionSelection() {
        // Test version selection logic
        String version = externalTemplateService.getCurrentPromptVersion(null);
        assertThat(version).isEqualTo("v1"); // Should use configured default
    }
}