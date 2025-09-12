package com.codetop.recommendation.service;

import com.codetop.recommendation.config.PromptTemplateProperties;
import com.codetop.recommendation.provider.LlmProvider.ProblemCandidate;
import com.codetop.recommendation.provider.LlmProvider.PromptOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExternalPromptTemplateServiceTest {
    
    @Mock
    private PromptTemplateProperties templateProperties;
    
    private ExternalPromptTemplateService service;
    
    @BeforeEach
    void setUp() {
        service = new ExternalPromptTemplateService(templateProperties);
        
        // Mock default properties with lenient stubbing
        lenient().when(templateProperties.getDefaultVersion()).thenReturn("v3");
        lenient().when(templateProperties.isCacheEnabled()).thenReturn(false); // Disable cache for tests
        lenient().when(templateProperties.getAbTests()).thenReturn(new HashMap<>());
    }
    
    @Test
    void testGetCurrentPromptVersion_withoutContext() {
        String version = service.getCurrentPromptVersion(null);
        assertThat(version).isEqualTo("v3");
    }
    
    @Test
    void testSelectPromptVersion_withABTesting() {
        // Setup A/B test configuration
        Map<String, PromptTemplateProperties.VersionMapping> abTests = new HashMap<>();
        PromptTemplateProperties.VersionMapping mapping = new PromptTemplateProperties.VersionMapping();
        mapping.setTiers(new String[]{"GOLD", "PLATINUM"});
        mapping.setVersion("v3");
        abTests.put("premium-users", mapping);
        
        lenient().when(templateProperties.getAbTests()).thenReturn(abTests);
        lenient().when(templateProperties.selectVersion("GOLD", null, null)).thenReturn("v3");
        lenient().when(templateProperties.getDefaultVersion()).thenReturn("v2"); // Override for this test
        
        RequestContext context = new RequestContext();
        ReflectionTestUtils.setField(context, "tier", "GOLD");
        
        String version = service.getCurrentPromptVersion(context);
        assertThat(version).isEqualTo("v3");
    }
    
    @Test
    void testBuildSystemMessage_fallback() {
        // Test fallback when template loading fails
        lenient().when(templateProperties.getTemplateFile("v1", "system")).thenReturn("nonexistent.txt");
        lenient().when(templateProperties.selectVersion(null, null, null)).thenReturn("v1");
        
        String systemMessage = service.buildSystemMessage("v1", null);
        
        assertThat(systemMessage)
            .contains("recommendation re-ranking engine")
            .contains("JSON");
    }
    
    @Test
    void testBuildUserMessage_basic() {
        RequestContext context = new RequestContext();
        ReflectionTestUtils.setField(context, "userId", 123L);
        
        List<ProblemCandidate> candidates = List.of(createTestCandidate());
        PromptOptions options = new PromptOptions();
        options.limit = 5;
        
        // Mock template file loading to trigger fallback
        lenient().when(templateProperties.getTemplateFile("v3", "user-advanced")).thenReturn("nonexistent.txt");
        lenient().when(templateProperties.selectVersion(null, null, null)).thenReturn("v3");
        
        String userMessage = service.buildUserMessage(context, candidates, options);
        
        assertThat(userMessage)
            .contains("5 items")
            .contains("123")
            .contains("JSON");
    }
    
    @Test
    void testTemplateVariableSubstitution() {
        // Test the template variable substitution logic
        String template = "Hello {{name}}, you have {{count}} items.";
        Map<String, Object> variables = Map.of(
            "name", "John",
            "count", 5
        );
        
        // Use reflection to test the private method
        String result = (String) ReflectionTestUtils.invokeMethod(
            service, "substituteVariables", template, variables
        );
        
        assertThat(result).isEqualTo("Hello John, you have 5 items.");
    }
    
    @Test
    void testBooleanVariableHandling() {
        String template = "{{#condition}}This is shown{{/condition}} always shown";
        Map<String, Object> variables = Map.of("condition", true);
        
        String result = (String) ReflectionTestUtils.invokeMethod(
            service, "substituteVariables", template, variables
        );
        
        // Simple boolean handling - just removes the placeholder
        assertThat(result).contains("always shown");
    }
    
    private ProblemCandidate createTestCandidate() {
        ProblemCandidate candidate = new ProblemCandidate();
        candidate.id = 1L;
        candidate.topic = "arrays";
        candidate.difficulty = "MEDIUM";
        candidate.tags = List.of("array", "sorting");
        candidate.recentAccuracy = 0.75;
        candidate.attempts = 3;
        candidate.urgencyScore = 0.6;
        candidate.daysOverdue = 2;
        return candidate;
    }
}