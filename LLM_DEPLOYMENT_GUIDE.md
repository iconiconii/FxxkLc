# LLM Recommendation System - Deployment Guide

## Overview

The LLM recommendation system supports segmented routing and feature toggles for different user tiers, A/B testing groups, and API routes. This guide covers safe deployment strategies and operational considerations.

## Key Features

### 1. Segmented Chain Routing
- **Multiple Chains**: Configure different LLM provider chains for different user segments
- **Dynamic Selection**: Route users to appropriate chains based on tier, AB group, and route
- **Graceful Fallback**: Automatic fallback to FSRS recommendations when LLM fails

### 2. Feature Toggles  
- **Global Toggle**: Master switch for all LLM functionality
- **Per-Segment Toggles**: Fine-grained control by tier, AB group, and route
- **User Override Lists**: Allow/deny specific users regardless of other rules

### 3. Observability
- **Response Headers**: `X-Chain-Id`, `X-Provider-Chain`, `X-Fallback-Reason`
- **Metrics**: Micrometer counters for routing decisions, fallbacks, and cache hits
- **Logging**: Structured logging with trace IDs for debugging

## Safe Deployment Strategy

### Phase 1: Enable for Small Cohort
```yaml
llm:
  enabled: true
  toggles:
    byTier:
      GOLD: true      # Start with premium users only
      PLATINUM: true
    allowUserIds: [1001, 1002, 1003]  # Add test users
```

### Phase 2: A/B Testing
```yaml
llm:
  routing:
    rules:
      - when: {abGroup: [experimental]}
        useChain: experimental
  toggles:
    byAbGroup:
      experimental: true  # Enable for experiment group
```

### Phase 3: Gradual Rollout
```yaml
llm:
  toggles:
    byTier:
      BRONZE: true   # Expand to more users
      SILVER: true
```

### Phase 4: Full Deployment
```yaml
llm:
  toggles:
    byTier:
      FREE: true     # Enable for all users
```

## Configuration Reference

### Chain Configuration
```yaml
chains:
  main:                    # Chain ID (referenced in routing rules)
    nodes:
      - name: openai       # Provider name (must exist in provider catalog)
        enabled: true
        timeoutMs: 1800    # Request timeout
        retry:
          attempts: 1      # Retry count (0 = no retry)
        rateLimit:
          rps: 5          # Global requests per second
          perUserRps: 1   # Per-user requests per second
    defaultProvider:
      strategy: fsrs_fallback  # busy_message | fsrs_fallback | empty
```

### Routing Rules
```yaml
routing:
  rules:
    - when:
        tier: [GOLD, PLATINUM]     # Case-insensitive tier matching
        abGroup: [experimental]    # Case-sensitive AB group matching
        route: [ai-recommendations] # Case-sensitive route matching
      useChain: premium            # Must exist in chains
```

### Feature Toggles
```yaml
toggles:
  byTier:
    FREE: false        # Tier-based toggles (case-insensitive)
  byAbGroup:
    A: true           # AB group toggles (case-sensitive)
  byRoute:
    ai-recommendations: true  # Route-based toggles (case-sensitive)
  allowUserIds: [1001]        # Override: always allow (after deny check)
  denyUserIds: [9999]         # Override: always deny (highest priority)
```

## Toggle Semantics

The system evaluates toggles in this order:

1. **Global Check**: If `llm.enabled = false`, deny all requests
2. **Deny List**: If user in `denyUserIds`, deny (highest priority)
3. **Allow List**: If user in `allowUserIds`, allow (override mode)
4. **Route Toggle**: Check `byRoute` setting
5. **Tier Toggle**: Check `byTier` setting  
6. **AB Group Toggle**: Check `byAbGroup` setting
7. **Default**: Allow if no explicit deny

**Important**: Allow list works in "override mode" - users in the allow list bypass other toggle checks but still respect the deny list.

## Monitoring and Alerts

### Key Metrics to Monitor
```
# Toggle decisions
llm.toggle.decision{tier,ab_group,route,enabled,disabled_reason}

# Chain selections  
llm.routing.chain_selected{chain_id,tier,ab_group,route}

# Provider execution
llm.provider.execution{chain_id,hops_count,success,fallback_reason}

# Fallback events
llm.fallback.event{reason,chain_id,strategy}

# Cache performance
llm.cache.access{tier,ab_group,chain_id,result}
```

### Recommended Alerts
- High fallback rate (>20% in 5 minutes)
- Chain timeout rate increasing
- Cache hit rate dropping significantly
- Configuration validation errors on startup

### Response Headers for Debugging
```
X-Chain-Id: premium
X-Provider-Chain: openai>mock
X-Fallback-Reason: TIMEOUT
X-Cache-Hit: false
X-Trace-Id: uuid
```

## Production Environment Variables

### Required
```bash
# LLM Configuration
LLM_ENABLED=true
DEEPSEEK_API_KEY=sk-xxx

# Security (if different from dev)
JWT_SECRET=your_production_secret_256_bits
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Optional Overrides
```bash
# Override default chain
LLM_DEFAULT_CHAIN_ID=main

# Override rate limits
LLM_GLOBAL_CONCURRENCY=20
LLM_PER_USER_CONCURRENCY=3
```

## Troubleshooting

### Configuration Validation Errors
The system validates configuration on startup. Check logs for:
- Missing chains referenced in routing rules
- Invalid defaultChainId
- Empty chains with no enabled nodes

### Common Issues

**Issue**: Users getting FSRS fallback instead of LLM
- **Check**: Feature toggles for user's tier/AB group
- **Check**: Chain configuration and enabled nodes
- **Check**: Provider API keys and network connectivity

**Issue**: Cache not working across deployments
- **Solution**: Cache keys include segment info and chain ID
- **Note**: Changing routing rules invalidates relevant cache entries

**Issue**: Inconsistent AB group assignment
- **Solution**: Current implementation uses user ID hash for consistency
- **Note**: Deploy new AB logic carefully to avoid user flapping

### Emergency Procedures

**Disable LLM Globally**:
```bash
# Via environment variable
export LLM_ENABLED=false

# Or via application properties
llm.enabled=false
```

**Disable for Specific Segment**:
```yaml
llm:
  toggles:
    byTier:
      GOLD: false  # Disable for GOLD users only
```

**Emergency Fallback**:
```yaml
llm:
  chains:
    main:
      defaultProvider:
        strategy: fsrs_fallback  # Ensure FSRS fallback works
```

## Cache Isolation

Cache keys include segment information to prevent cross-contamination:
```
Format: codetop:rec-ai:userId{userId}:limit_{limit}_pv_{version}_t_{tier}_ab_{abGroup}_chain_{chainId}
```

This ensures:
- Different tiers don't share cache
- AB groups maintain isolation  
- Chain changes invalidate cache
- Prompt version changes invalidate cache

## Security Considerations

1. **API Keys**: Store in environment variables, never in config files
2. **User Data**: Tier/AB group assignments should come from authenticated sources
3. **Rate Limiting**: Per-user limits prevent abuse
4. **Input Validation**: All user inputs are sanitized before LLM calls

## Performance Tuning

### Recommended Settings by Scale

**Small Scale (< 1000 DAU)**:
```yaml
asyncLimits:
  globalConcurrency: 10
  perUserConcurrency: 2
chains:
  main:
    nodes:
      - rateLimit: {rps: 5, perUserRps: 1}
```

**Medium Scale (1000-10000 DAU)**:
```yaml  
asyncLimits:
  globalConcurrency: 50
  perUserConcurrency: 3
chains:
  main:
    nodes:
      - rateLimit: {rps: 20, perUserRps: 2}
```

**Large Scale (> 10000 DAU)**:
```yaml
asyncLimits:
  globalConcurrency: 100  
  perUserConcurrency: 5
chains:
  main:
    nodes:
      - rateLimit: {rps: 50, perUserRps: 3}
```

### Cache Tuning
- Cache TTL: 1 hour (balance freshness vs hit rate)
- Cache warming: Consider pre-computing for high-value users
- Cache eviction: Monitor memory usage and adjust TTL accordingly