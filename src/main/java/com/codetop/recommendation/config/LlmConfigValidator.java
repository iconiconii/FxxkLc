package com.codetop.recommendation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates LLM configuration on application startup to catch misconfigurations early.
 */
@Component
public class LlmConfigValidator {
    private static final Logger log = LoggerFactory.getLogger(LlmConfigValidator.class);
    
    private final LlmProperties llmProperties;

    public LlmConfigValidator(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        if (llmProperties == null || !llmProperties.isEnabled()) {
            log.debug("LLM is disabled, skipping configuration validation");
            return;
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateChainConfiguration(errors, warnings);
        validateRoutingRules(errors, warnings);
        validateFeatureToggles(errors, warnings);
        validateAllowListMode(errors, warnings);
        
        // Log all findings
        if (!warnings.isEmpty()) {
            for (String warning : warnings) {
                log.warn("LLM Config Warning: {}", warning);
            }
        }
        
        if (!errors.isEmpty()) {
            for (String error : errors) {
                log.error("LLM Config Error: {}", error);
            }
            throw new IllegalStateException("LLM configuration validation failed. Errors: " + 
                String.join("; ", errors));
        }
        
        log.info("LLM configuration validation passed with {} warnings", warnings.size());
    }

    private void validateChainConfiguration(List<String> errors, List<String> warnings) {
        Map<String, LlmProperties.Chain> chains = llmProperties.getChains();
        
        if (chains == null || chains.isEmpty()) {
            warnings.add("No multiple chains configured, using legacy single chain mode");
            return;
        }
        
        // Validate default chain exists
        String defaultChainId = llmProperties.getDefaultChainId();
        if (defaultChainId == null || defaultChainId.isEmpty()) {
            warnings.add("No defaultChainId specified, will use first available chain");
        } else if (!chains.containsKey(defaultChainId)) {
            errors.add(String.format("defaultChainId '%s' does not exist in chains", defaultChainId));
        }
        
        // Validate each chain has at least one enabled node
        for (Map.Entry<String, LlmProperties.Chain> entry : chains.entrySet()) {
            String chainId = entry.getKey();
            LlmProperties.Chain chain = entry.getValue();
            
            if (chain.getNodes() == null || chain.getNodes().isEmpty()) {
                warnings.add(String.format("Chain '%s' has no nodes configured", chainId));
                continue;
            }
            
            boolean hasEnabledNode = chain.getNodes().stream().anyMatch(LlmProperties.Node::isEnabled);
            if (!hasEnabledNode) {
                warnings.add(String.format("Chain '%s' has no enabled nodes", chainId));
            }
        }
    }

    private void validateRoutingRules(List<String> errors, List<String> warnings) {
        if (llmProperties.getRouting() == null || llmProperties.getRouting().getRules() == null) {
            warnings.add("No routing rules configured");
            return;
        }
        
        Map<String, LlmProperties.Chain> chains = llmProperties.getChains();
        if (chains == null || chains.isEmpty()) {
            return; // Already warned about missing chains
        }
        
        Set<String> availableChains = chains.keySet();
        
        for (int i = 0; i < llmProperties.getRouting().getRules().size(); i++) {
            LlmProperties.Rule rule = llmProperties.getRouting().getRules().get(i);
            String useChain = rule.getUseChain();
            
            if (useChain == null || useChain.isEmpty()) {
                errors.add(String.format("Routing rule[%d] has no useChain specified", i));
                continue;
            }
            
            if (!availableChains.contains(useChain)) {
                errors.add(String.format("Routing rule[%d] references unknown chain '%s'. Available chains: %s", 
                    i, useChain, availableChains.stream().sorted().collect(Collectors.joining(", "))));
            }
            
            if (rule.getWhen() == null || rule.getWhen().isEmpty()) {
                warnings.add(String.format("Routing rule[%d] has no conditions, will never match", i));
            }
        }
    }

    private void validateFeatureToggles(List<String> errors, List<String> warnings) {
        LlmProperties.FeatureToggles toggles = llmProperties.getToggles();
        if (toggles == null) {
            return; // No toggles configured is valid
        }

        // Check for potentially conflicting configurations
        if (toggles.getAllowUserIds() != null && !toggles.getAllowUserIds().isEmpty() && 
            toggles.getDenyUserIds() != null && !toggles.getDenyUserIds().isEmpty()) {
            
            Set<Long> intersection = toggles.getAllowUserIds().stream()
                .filter(toggles.getDenyUserIds()::contains)
                .collect(Collectors.toSet());
            
            if (!intersection.isEmpty()) {
                warnings.add(String.format("Users %s appear in both allowUserIds and denyUserIds. Deny list takes precedence.", 
                    intersection));
            }
        }

        // Validate tier names are consistent with routing rules
        if (toggles.getByTier() != null && !toggles.getByTier().isEmpty() &&
            llmProperties.getRouting() != null && llmProperties.getRouting().getRules() != null) {
            
            Set<String> tierToggles = toggles.getByTier().keySet().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
            
            Set<String> routingTiers = llmProperties.getRouting().getRules().stream()
                .filter(rule -> rule.getWhen() != null && rule.getWhen().containsKey("tier"))
                .flatMap(rule -> rule.getWhen().get("tier").stream())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

            Set<String> unusedToggles = tierToggles.stream()
                .filter(tier -> !routingTiers.contains(tier))
                .collect(Collectors.toSet());
            
            if (!unusedToggles.isEmpty() && !routingTiers.isEmpty()) {
                warnings.add(String.format("Tier toggles %s are not referenced in routing rules", unusedToggles));
            }
        }
    }

    private void validateAllowListMode(List<String> errors, List<String> warnings) {
        LlmProperties.FeatureToggles toggles = llmProperties.getToggles();
        if (toggles == null) {
            return;
        }

        String allowListMode = toggles.getAllowListMode();
        if (allowListMode != null && 
            !allowListMode.equals("override") && 
            !allowListMode.equals("whitelist")) {
            errors.add(String.format("Invalid allowListMode '%s'. Must be 'override' or 'whitelist'", allowListMode));
        }

        // Warn about whitelist mode in production if it might be overly restrictive
        if ("whitelist".equals(allowListMode) && 
            toggles.getAllowUserIds() != null && 
            toggles.getAllowUserIds().size() < 10) {
            warnings.add("allowListMode 'whitelist' with small allowUserIds list may be overly restrictive in production");
        }
    }
}