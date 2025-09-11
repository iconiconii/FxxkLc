package com.codetop.recommendation.service;

import com.codetop.recommendation.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChainSelectorTest {

    private ChainSelector chainSelector;
    private LlmProperties llmProperties;

    @BeforeEach
    void setUp() {
        chainSelector = new ChainSelector();
        llmProperties = new LlmProperties();
        
        // Set up multiple chains
        Map<String, LlmProperties.Chain> chains = new HashMap<>();
        
        // Main chain
        LlmProperties.Chain mainChain = new LlmProperties.Chain();
        List<LlmProperties.Node> mainNodes = new ArrayList<>();
        LlmProperties.Node openaiNode = new LlmProperties.Node();
        openaiNode.setName("openai");
        openaiNode.setEnabled(true);
        mainNodes.add(openaiNode);
        mainChain.setNodes(mainNodes);
        chains.put("main", mainChain);
        
        // Premium chain
        LlmProperties.Chain premiumChain = new LlmProperties.Chain();
        List<LlmProperties.Node> premiumNodes = new ArrayList<>();
        LlmProperties.Node fastNode = new LlmProperties.Node();
        fastNode.setName("openai");
        fastNode.setEnabled(true);
        premiumNodes.add(fastNode);
        premiumChain.setNodes(premiumNodes);
        chains.put("premium", premiumChain);
        
        // Experiment chain
        LlmProperties.Chain expChain = new LlmProperties.Chain();
        chains.put("experimentA", expChain);
        
        llmProperties.setChains(chains);
        llmProperties.setDefaultChainId("main");
        
        // Set up routing rules
        LlmProperties.Routing routing = new LlmProperties.Routing();
        List<LlmProperties.Rule> rules = new ArrayList<>();
        
        // Rule 1: GOLD/PLATINUM users get premium chain
        LlmProperties.Rule rule1 = new LlmProperties.Rule();
        Map<String, List<String>> conditions1 = new HashMap<>();
        conditions1.put("tier", Arrays.asList("GOLD", "PLATINUM"));
        rule1.setWhen(conditions1);
        rule1.setUseChain("premium");
        rules.add(rule1);
        
        // Rule 2: AB group A gets experiment chain
        LlmProperties.Rule rule2 = new LlmProperties.Rule();
        Map<String, List<String>> conditions2 = new HashMap<>();
        conditions2.put("abGroup", Arrays.asList("A"));
        rule2.setWhen(conditions2);
        rule2.setUseChain("experimentA");
        rules.add(rule2);
        
        routing.setRules(rules);
        llmProperties.setRouting(routing);
    }

    @Test
    @DisplayName("Should select premium chain for GOLD tier user")
    void shouldSelectPremiumChainForGoldUser() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(123L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("B");
        ctx.setRoute("ai-recommendations");
        
        LlmProperties.Chain selectedChain = chainSelector.selectChain(ctx, llmProperties);
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        
        assertNotNull(selectedChain);
        assertEquals("premium", chainId);
    }

    @Test
    @DisplayName("Should select premium chain for PLATINUM tier user")
    void shouldSelectPremiumChainForPlatinumUser() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(456L);
        ctx.setTier("PLATINUM");
        ctx.setAbGroup("B");
        
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        assertEquals("premium", chainId);
    }

    @Test
    @DisplayName("Should select experiment chain for AB group A")
    void shouldSelectExperimentChainForGroupA() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(789L);
        ctx.setTier("SILVER");
        ctx.setAbGroup("A");
        
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        assertEquals("experimentA", chainId);
    }

    @Test
    @DisplayName("Should select default chain when no rules match")
    void shouldSelectDefaultChainWhenNoRulesMatch() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(999L);
        ctx.setTier("BRONZE");
        ctx.setAbGroup("B");
        ctx.setRoute("ai-recommendations");
        
        LlmProperties.Chain selectedChain = chainSelector.selectChain(ctx, llmProperties);
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        
        assertNotNull(selectedChain);
        assertEquals("main", chainId);
    }

    @Test
    @DisplayName("Should handle first rule precedence when multiple rules match")
    void shouldHandleRulePrecedence() {
        // This user matches both rules (GOLD tier AND AB group A)
        // But rule1 (tier-based) should win because it comes first
        RequestContext ctx = new RequestContext();
        ctx.setUserId(555L);
        ctx.setTier("GOLD");
        ctx.setAbGroup("A");
        
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        assertEquals("premium", chainId); // First rule wins, not experimentA
    }

    @Test
    @DisplayName("Should fallback to legacy single chain when no multiple chains configured")
    void shouldFallbackToLegacySingleChain() {
        LlmProperties legacyProps = new LlmProperties();
        LlmProperties.Chain legacyChain = new LlmProperties.Chain();
        legacyProps.setChain(legacyChain);
        // No chains map set
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(100L);
        
        LlmProperties.Chain selectedChain = chainSelector.selectChain(ctx, legacyProps);
        assertEquals(legacyChain, selectedChain);
        
        String chainId = chainSelector.getSelectedChainId(ctx, legacyProps);
        assertEquals("legacy", chainId);
    }

    @Test
    @DisplayName("Should handle missing chain gracefully")
    void shouldHandleMissingChainGracefully() {
        // Set up a rule that points to non-existent chain
        LlmProperties.Rule badRule = new LlmProperties.Rule();
        Map<String, List<String>> conditions = new HashMap<>();
        conditions.put("tier", Arrays.asList("FREE"));
        badRule.setWhen(conditions);
        badRule.setUseChain("nonexistent");
        
        llmProperties.getRouting().getRules().add(0, badRule); // Add at front to test
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(111L);
        ctx.setTier("FREE");
        
        // Should gracefully fallback to first available chain
        LlmProperties.Chain selectedChain = chainSelector.selectChain(ctx, llmProperties);
        assertNotNull(selectedChain);
    }

    @Test
    @DisplayName("Should handle null properties gracefully")
    void shouldHandleNullPropertiesGracefully() {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(222L);
        
        LlmProperties.Chain selectedChain = chainSelector.selectChain(ctx, null);
        assertNull(selectedChain);
        
        String chainId = chainSelector.getSelectedChainId(ctx, null);
        assertEquals("legacy", chainId);
    }

    @Test
    @DisplayName("Should handle empty routing rules")
    void shouldHandleEmptyRoutingRules() {
        llmProperties.getRouting().setRules(new ArrayList<>());
        
        RequestContext ctx = new RequestContext();
        ctx.setUserId(333L);
        ctx.setTier("SILVER");
        
        String chainId = chainSelector.getSelectedChainId(ctx, llmProperties);
        assertEquals("main", chainId); // Should use default chain
    }

    @Test
    @DisplayName("Should handle partial condition matches correctly")
    void shouldHandlePartialConditionMatches() {
        // Add a rule with multiple conditions that must ALL match
        LlmProperties.Rule strictRule = new LlmProperties.Rule();
        Map<String, List<String>> conditions = new HashMap<>();
        conditions.put("tier", Arrays.asList("SILVER"));
        conditions.put("abGroup", Arrays.asList("A"));
        conditions.put("route", Arrays.asList("special-route"));
        strictRule.setWhen(conditions);
        strictRule.setUseChain("experimentA");
        
        // Insert at beginning to test first
        llmProperties.getRouting().getRules().add(0, strictRule);
        
        // Test case where only tier matches
        RequestContext ctx1 = new RequestContext();
        ctx1.setTier("SILVER");
        ctx1.setAbGroup("B"); // doesn't match
        ctx1.setRoute("ai-recommendations"); // doesn't match
        String chainId1 = chainSelector.getSelectedChainId(ctx1, llmProperties);
        assertNotEquals("experimentA", chainId1); // Should not match strict rule
        
        // Test case where all conditions match
        RequestContext ctx2 = new RequestContext();
        ctx2.setTier("SILVER");
        ctx2.setAbGroup("A");
        ctx2.setRoute("special-route");
        String chainId2 = chainSelector.getSelectedChainId(ctx2, llmProperties);
        assertEquals("experimentA", chainId2); // Should match strict rule
    }
}