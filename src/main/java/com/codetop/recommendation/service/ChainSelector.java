package com.codetop.recommendation.service;

import com.codetop.recommendation.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for selecting the appropriate LLM provider chain based on request context (tier, AB group, route).
 * Implements the routing logic defined in llm.routing configuration.
 */
@Service
public class ChainSelector {
    private static final Logger log = LoggerFactory.getLogger(ChainSelector.class);

    /**
     * Selects the appropriate chain based on request context and routing rules.
     * 
     * @param ctx The request context containing tier, abGroup, route
     * @param props The LLM configuration properties
     * @return The selected chain, or null if no suitable chain found
     */
    public LlmProperties.Chain selectChain(RequestContext ctx, LlmProperties props) {
        if (props == null) {
            log.warn("LlmProperties is null, cannot select chain for userId={}", ctx.getUserId());
            return null;
        }

        // Use multiple chains if configured, otherwise fall back to legacy single chain
        Map<String, LlmProperties.Chain> chains = props.getChains();
        if (chains == null || chains.isEmpty()) {
            log.debug("No multiple chains configured, using legacy single chain for userId={}", ctx.getUserId());
            return props.getChain();
        }

        String selectedChainId = null;
        String matchedRuleInfo = null;

        // Evaluate routing rules in order
        if (props.getRouting() != null && props.getRouting().getRules() != null) {
            for (LlmProperties.Rule rule : props.getRouting().getRules()) {
                if (matchesRule(rule, ctx)) {
                    selectedChainId = rule.getUseChain();
                    matchedRuleInfo = formatRuleMatch(rule, ctx);
                    break; // First match wins
                }
            }
        }

        // If no rule matched, use default chain
        if (selectedChainId == null) {
            selectedChainId = props.getDefaultChainId();
            if (selectedChainId == null || selectedChainId.isEmpty()) {
                selectedChainId = chains.keySet().iterator().next(); // Use first available chain
                log.warn("No defaultChainId configured and no routing rule matched for userId={}, using first chain: {}", 
                    ctx.getUserId(), selectedChainId);
            }
            matchedRuleInfo = "default";
        }

        // Retrieve the selected chain
        LlmProperties.Chain selectedChain = chains.get(selectedChainId);
        if (selectedChain == null) {
            log.error("Chain '{}' not found in configuration for userId={}, falling back to first available chain", 
                selectedChainId, ctx.getUserId());
            selectedChain = chains.values().iterator().next(); // Fallback to first available
            selectedChainId = chains.keySet().iterator().next();
        }

        log.debug("Selected chain '{}' for userId={}, tier={}, abGroup={}, route={} (matched: {})", 
            selectedChainId, ctx.getUserId(), ctx.getTier(), ctx.getAbGroup(), ctx.getRoute(), matchedRuleInfo);

        return selectedChain;
    }

    /**
     * Gets the ID of the selected chain (for telemetry purposes).
     */
    public String getSelectedChainId(RequestContext ctx, LlmProperties props) {
        if (props == null || props.getChains() == null || props.getChains().isEmpty()) {
            return "legacy";
        }

        // Evaluate routing rules to find chain ID
        if (props.getRouting() != null && props.getRouting().getRules() != null) {
            for (LlmProperties.Rule rule : props.getRouting().getRules()) {
                if (matchesRule(rule, ctx)) {
                    return rule.getUseChain();
                }
            }
        }

        // Return default chain ID
        String defaultId = props.getDefaultChainId();
        return (defaultId != null && !defaultId.isEmpty()) ? defaultId : props.getChains().keySet().iterator().next();
    }

    /**
 * Checks if a routing rule matches the given request context.
 * Performs case-insensitive matching for robustness.
 */
private boolean matchesRule(LlmProperties.Rule rule, RequestContext ctx) {
    if (rule.getWhen() == null) {
        return false;
    }

    Map<String, List<String>> conditions = rule.getWhen();

    // Check tier condition (case-insensitive, normalized to uppercase)
    if (conditions.containsKey("tier")) {
        List<String> allowedTiers = conditions.get("tier");
        if (allowedTiers != null && !allowedTiers.isEmpty()) {
            String contextTier = ctx.getTier();
            if (contextTier == null) {
                return false;
            }
            // Both configuration and context are normalized to uppercase for comparison
            String normalizedContextTier = contextTier.toUpperCase();
            boolean tierMatches = allowedTiers.contains(normalizedContextTier);
            if (!tierMatches) {
                return false;
            }
        }
    }

    // Check abGroup condition (case-sensitive for AB group codes)
    if (conditions.containsKey("abGroup")) {
        List<String> allowedAbGroups = conditions.get("abGroup");
        if (allowedAbGroups != null && !allowedAbGroups.isEmpty()) {
            String contextAbGroup = ctx.getAbGroup();
            if (contextAbGroup == null || !allowedAbGroups.contains(contextAbGroup)) {
                return false;
            }
        }
    }

    // Check route condition (case-sensitive for route names)
    if (conditions.containsKey("route")) {
        List<String> allowedRoutes = conditions.get("route");
        if (allowedRoutes != null && !allowedRoutes.isEmpty()) {
            String contextRoute = ctx.getRoute();
            if (contextRoute == null || !allowedRoutes.contains(contextRoute)) {
                return false;
            }
        }
    }

    return true; // All conditions matched (or no conditions specified)
}

    /**
     * Formats rule match information for logging.
     */
    private String formatRuleMatch(LlmProperties.Rule rule, RequestContext ctx) {
        if (rule.getWhen() == null) {
            return "empty-rule";
        }
        
        StringBuilder sb = new StringBuilder();
        Map<String, List<String>> conditions = rule.getWhen();
        
        if (conditions.containsKey("tier")) {
            sb.append("tier:").append(ctx.getTier()).append(" ");
        }
        if (conditions.containsKey("abGroup")) {
            sb.append("abGroup:").append(ctx.getAbGroup()).append(" ");
        }
        if (conditions.containsKey("route")) {
            sb.append("route:").append(ctx.getRoute()).append(" ");
        }
        
        return sb.toString().trim();
    }
}