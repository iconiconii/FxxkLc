package com.codetop.recommendation.service;

import com.codetop.recommendation.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Service for determining if LLM recommendations are enabled for a specific user segment.
 * Implements feature toggle logic based on user tier, AB group, route, and allow/deny lists.
 */
@Service
public class LlmToggleService {
    private static final Logger log = LoggerFactory.getLogger(LlmToggleService.class);

    public boolean isEnabled(RequestContext ctx, LlmProperties props) {
    if (props == null || !props.isEnabled()) {
        log.debug("LLM globally disabled for userId={}", ctx.getUserId());
        return false;
    }

    LlmProperties.FeatureToggles toggles = props.getToggles();
    if (toggles == null) {
        // No toggle configuration means inherit global setting
        return true;
    }

    Long userId = ctx.getUserId();

    // Check deny list first (highest priority)
    Set<Long> denyUserIds = toggles.getDenyUserIds();
    if (denyUserIds != null && denyUserIds.contains(userId)) {
        log.debug("User {} is in deny list, LLM disabled", userId);
        return false;
    }

    // Check allow list (behavior depends on allowListMode)
    Set<Long> allowUserIds = toggles.getAllowUserIds();
    if (allowUserIds != null && !allowUserIds.isEmpty()) {
        String allowListMode = toggles.getAllowListMode() != null ? toggles.getAllowListMode() : "override";
        boolean userInAllowList = allowUserIds.contains(userId);
        
        if ("whitelist".equals(allowListMode)) {
            // Whitelist mode: only users in allowUserIds are allowed
            if (!userInAllowList) {
                log.debug("User {} not in allow list (whitelist mode), LLM disabled", userId);
                return false;
            } else {
                log.debug("User {} is in allow list (whitelist mode), LLM enabled", userId);
                return true;
            }
        } else {
            // Override mode (default): users in allowUserIds bypass other checks
            if (userInAllowList) {
                log.debug("User {} is in allow list (override mode), LLM enabled", userId);
                return true;
            }
            // Continue to other toggle evaluations for users not in allow list
        }
    }

    // Check route-level toggle
    if (ctx.getRoute() != null) {
        Map<String, Boolean> byRoute = toggles.getByRoute();
        if (byRoute != null && byRoute.containsKey(ctx.getRoute())) {
            boolean routeEnabled = byRoute.get(ctx.getRoute());
            if (!routeEnabled) {
                log.debug("Route '{}' disabled for userId={}", ctx.getRoute(), userId);
                return false;
            }
        }
    }

    // Check tier-level toggle (case-insensitive)
    if (ctx.getTier() != null) {
        Map<String, Boolean> byTier = toggles.getByTier();
        if (byTier != null) {
            String normalizedTier = ctx.getTier().toUpperCase();
            // Find matching tier key (case-insensitive)
            String matchingTierKey = byTier.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(normalizedTier))
                .findFirst()
                .orElse(null);
            
            if (matchingTierKey != null) {
                boolean tierEnabled = byTier.get(matchingTierKey);
                if (!tierEnabled) {
                    log.debug("Tier '{}' (normalized: '{}') disabled for userId={}", 
                        ctx.getTier(), normalizedTier, userId);
                    return false;
                }
            }
        }
    }

    // Check AB group toggle
    if (ctx.getAbGroup() != null) {
        Map<String, Boolean> byAbGroup = toggles.getByAbGroup();
        if (byAbGroup != null && byAbGroup.containsKey(ctx.getAbGroup())) {
            boolean abGroupEnabled = byAbGroup.get(ctx.getAbGroup());
            if (!abGroupEnabled) {
                log.debug("AB group '{}' disabled for userId={}", ctx.getAbGroup(), userId);
                return false;
            }
        }
    }

    // If no explicit toggle found, inherit global setting
    log.debug("No specific toggle found for userId={}, tier={}, abGroup={}, route={}, inheriting global setting (enabled)", 
        userId, ctx.getTier(), ctx.getAbGroup(), ctx.getRoute());
    return true;
}

    public String getDisabledReason(RequestContext ctx, LlmProperties props) {
    if (props == null || !props.isEnabled()) {
        return "GLOBAL_DISABLED";
    }

    LlmProperties.FeatureToggles toggles = props.getToggles();
    if (toggles == null) {
        return null; // enabled
    }

    Long userId = ctx.getUserId();

    // Check deny list first
    if (toggles.getDenyUserIds() != null && toggles.getDenyUserIds().contains(userId)) {
        return "USER_DENIED";
    }

    // Check allow list - behavior depends on allowListMode
    Set<Long> allowUserIds = toggles.getAllowUserIds();
    if (allowUserIds != null && !allowUserIds.isEmpty()) {
        String allowListMode = toggles.getAllowListMode() != null ? toggles.getAllowListMode() : "override";
        boolean userInAllowList = allowUserIds.contains(userId);
        
        if ("whitelist".equals(allowListMode)) {
            // Whitelist mode: users not in allowUserIds are denied
            if (!userInAllowList) {
                return "ALLOWLIST_DENIED"; // User not in whitelist
            } else {
                return null; // User is allowed via whitelist
            }
        } else {
            // Override mode: users in allowUserIds are enabled
            if (userInAllowList) {
                return null; // User is allowed via override
            }
            // Continue evaluation for users not in allow list
        }
    }

    // Check route toggle
    if (ctx.getRoute() != null) {
        Map<String, Boolean> byRoute = toggles.getByRoute();
        if (byRoute != null && byRoute.containsKey(ctx.getRoute()) && !byRoute.get(ctx.getRoute())) {
            return "ROUTE_DISABLED:" + ctx.getRoute();
        }
    }

    // Check tier toggle (case-insensitive)
    if (ctx.getTier() != null) {
        Map<String, Boolean> byTier = toggles.getByTier();
        if (byTier != null) {
            String normalizedTier = ctx.getTier().toUpperCase();
            // Find matching tier key (case-insensitive)
            String matchingTierKey = byTier.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(normalizedTier))
                .findFirst()
                .orElse(null);
                
            if (matchingTierKey != null && !byTier.get(matchingTierKey)) {
                return "TIER_DISABLED:" + normalizedTier;
            }
        }
    }

    // Check AB group toggle
    if (ctx.getAbGroup() != null) {
        Map<String, Boolean> byAbGroup = toggles.getByAbGroup();
        if (byAbGroup != null && byAbGroup.containsKey(ctx.getAbGroup()) && !byAbGroup.get(ctx.getAbGroup())) {
            return "ABGROUP_DISABLED:" + ctx.getAbGroup();
        }
    }

    return null; // enabled
}
}