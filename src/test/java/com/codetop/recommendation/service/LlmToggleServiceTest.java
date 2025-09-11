package com.codetop.recommendation.service;

import com.codetop.recommendation.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LlmToggleServiceTest {

    private LlmToggleService llmToggleService;
    private LlmProperties llmProperties;

    @BeforeEach
    void setUp() {
        llmToggleService = new LlmToggleService();
        llmProperties = new LlmProperties();
        llmProperties.setEnabled(true); // Global enabled by default
        
        // Set up feature toggles
        LlmProperties.FeatureToggles toggles = new LlmProperties.FeatureToggles();
        
        // Configure tier toggles
        Map<String, Boolean> byTier = new HashMap<>();
        byTier.put("FREE", false);
        byTier.put("BRONZE", true);
        byTier.put("SILVER", true);
        byTier.put("GOLD", true);
        byTier.put("PLATINUM", true);
        toggles.setByTier(byTier);
        
        // Configure AB group toggles
        Map<String, Boolean> byAbGroup = new HashMap<>();
        byAbGroup.put("A", true);
        byAbGroup.put("B", false);
        byAbGroup.put("default", true);
        toggles.setByAbGroup(byAbGroup);
        
        // Configure route toggles
        Map<String, Boolean> byRoute = new HashMap<>();
        byRoute.put("ai-recommendations", true);
        byRoute.put("disabled-route", false);
        toggles.setByRoute(byRoute);
        
        // Allow and deny lists (empty by default)
        Set<Long> allowUserIds = new HashSet<>(); // Empty means allow list is not active
        toggles.setAllowUserIds(allowUserIds);
        
        Set<Long> denyUserIds = new HashSet<>();
        denyUserIds.add(9999L);
        toggles.setDenyUserIds(denyUserIds);
        
        llmProperties.setToggles(toggles);
    }

    @Test
    @DisplayName("Should return false when LLM is globally disabled")
    void shouldReturnFalseWhenGloballyDisabled() {
        llmProperties.setEnabled(false);
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(123L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("GLOBAL_DISABLED", reason);
    }

    @Test
    @DisplayName("Should return false for users in deny list")
    void shouldReturnFalseForDenyListUsers() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(9999L); // In deny list
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("USER_DENIED", reason);
    }

    @Test
    @DisplayName("Should return true for users in allow list regardless of other toggles")
    void shouldReturnTrueForAllowListUsers() {
        // Set up allow list for this test
        Set<Long> allowUserIds = new HashSet<>();
        allowUserIds.add(1001L);
        allowUserIds.add(1002L);
        llmProperties.getToggles().setAllowUserIds(allowUserIds);
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(1001L); // In allow list
        ctx.setTier("FREE"); // Would normally be disabled
        ctx.setAbGroup("B"); // Would normally be disabled
        ctx.setRoute("disabled-route"); // Would normally be disabled
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertNull(reason);
    }

    @Test
    @DisplayName("Should return false for FREE tier users")
    void shouldReturnFalseForFreeTierUsers() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(123L);
        ctx.setTier("FREE");
        ctx.setAbGroup("A");
        ctx.setRoute("ai-recommendations");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("TIER_DISABLED:FREE", reason);
    }

    @Test
    @DisplayName("Should return true for BRONZE tier users")
    void shouldReturnTrueForBronzeTierUsers() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(456L);
        ctx.setTier("BRONZE");
        ctx.setAbGroup("A");
        ctx.setRoute("ai-recommendations");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertNull(reason);
    }

    @Test
    @DisplayName("Should return false for AB group B users")
    void shouldReturnFalseForAbGroupBUsers() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(789L);
        ctx.setTier("SILVER");
        ctx.setAbGroup("B");
        ctx.setRoute("ai-recommendations");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("ABGROUP_DISABLED:B", reason);
    }

    @Test
    @DisplayName("Should return false for disabled routes")
    void shouldReturnFalseForDisabledRoutes() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(111L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        ctx.setRoute("disabled-route");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("ROUTE_DISABLED:disabled-route", reason);
    }

    @Test
    @DisplayName("Should return true when all conditions pass")
    void shouldReturnTrueWhenAllConditionsPass() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(222L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        ctx.setRoute("ai-recommendations");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertNull(reason);
    }

    @Test
    @DisplayName("Should inherit global setting when no specific toggles configured")
    void shouldInheritGlobalSettingWhenNoTogglesConfigured() {
        llmProperties.setToggles(null);
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(333L);
        ctx.setTier("UNKNOWN_TIER");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled); // Should inherit global enabled=true
    }

    @Test
    @DisplayName("Should return true for unknown tier/abGroup/route when not explicitly configured")
    void shouldReturnTrueForUnknownSegments() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(444L);
        ctx.setTier("UNKNOWN_TIER"); // Not in tier toggles
        ctx.setAbGroup("UNKNOWN_GROUP"); // Not in AB group toggles
        ctx.setRoute("unknown-route"); // Not in route toggles
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled); // Should inherit global setting
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertNull(reason);
    }

    @Test
    @DisplayName("Should handle null properties gracefully")
    void shouldHandleNullPropertiesGracefully() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(555L);
        
        boolean enabled = llmToggleService.isEnabled(ctx, null);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, null);
        assertEquals("GLOBAL_DISABLED", reason);
    }

    @Test
    @DisplayName("Should handle null context fields gracefully")
    void shouldHandleNullContextFieldsGracefully() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(666L);
        // tier, abGroup, route are null
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled); // Should pass since no explicit disabling rules match
    }

    @Test
    @DisplayName("Should respect deny list over allow list")
    void shouldRespectDenyListOverAllowList() {
        // Add user to both allow and deny lists
        llmProperties.getToggles().getAllowUserIds().add(9999L);
        // 9999L is already in deny list
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(9999L);
        ctx.setTier("GOLD");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled); // Deny list should take precedence
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        assertEquals("USER_DENIED", reason);
    }

    @Test
    @DisplayName("Should check all conditions in hierarchy: deny > allow > route > tier > abGroup")
    void shouldCheckConditionsInCorrectOrder() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(777L);
        ctx.setTier("FREE"); // Would disable
        ctx.setAbGroup("B"); // Would disable
        ctx.setRoute("disabled-route"); // Would disable
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertFalse(enabled);
        
        String reason = llmToggleService.getDisabledReason(ctx, llmProperties);
        // Should report the first disabling condition encountered
        assertEquals("ROUTE_DISABLED:disabled-route", reason);
    }

    @Test
    @DisplayName("Should handle empty allow list correctly")
    void shouldHandleEmptyAllowListCorrectly() {
        // Clear allow list (already empty by default)
        llmProperties.getToggles().setAllowUserIds(new HashSet<>());
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(888L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        
        boolean enabled = llmToggleService.isEnabled(ctx, llmProperties);
        assertTrue(enabled); // Should not be affected by empty allow list
    }
    
    @Test
    @DisplayName("Should use override mode for allow list - users evaluate other toggles when not in allow list")  
    void shouldUseOverrideModeForAllowList() {
        // Set up allow list for this test
        Set<Long> allowUserIds = new HashSet<>();
        allowUserIds.add(1001L);
        allowUserIds.add(1002L);
        llmProperties.getToggles().setAllowUserIds(allowUserIds);
        
        // User in allow list should bypass other toggles
        RequestContext ctx1 = new RequestContext();
        ctx1.setUserId(1001L); // In allow list  
        ctx1.setTier("FREE"); // Would normally be disabled
        ctx1.setAbGroup("B"); // Would normally be disabled
        ctx1.setRoute("disabled-route"); // Would normally be disabled
        
        assertTrue(llmToggleService.isEnabled(ctx1, llmProperties));
        assertNull(llmToggleService.getDisabledReason(ctx1, llmProperties));
        
        // User NOT in allow list should evaluate other toggles normally
        RequestContext ctx2 = new RequestContext();
        ctx2.setUserId(555L); // NOT in allow list
        ctx2.setTier("GOLD"); // Enabled
        ctx2.setAbGroup("A"); // Enabled  
        ctx2.setRoute("ai-recommendations"); // Enabled
        
        assertTrue(llmToggleService.isEnabled(ctx2, llmProperties)); // Should be enabled by tier/ab/route
        assertNull(llmToggleService.getDisabledReason(ctx2, llmProperties));
    }
}