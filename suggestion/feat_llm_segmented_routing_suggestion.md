# Feature Suggestions — Segmented Chain Routing & Per‑Segment Toggles

Goal: Finish the two remaining Task 1 checkboxes by adding (1) dynamic chain selection by user tier/AB/route; (2) per‑segment feature toggles to gate LLM usage.

## 1) Segmented Chain Routing (tier/AB/route)

Outcome
- Select different responsibility chains at runtime based on `RequestContext` (tier, abGroup, route).
- Keep a safe default chain and robust fallback to avoid outages on misconfiguration.

Proposed Config (YAML)
```yaml
llm:
  defaultChainId: main
  chains:
    main:                           # existing chain moved here
      nodes:
        - name: openai
          enabled: true
          timeoutMs: 1800
          retry: { attempts: 1 }
          onErrorsToNext: [TIMEOUT, HTTP_429, HTTP_5XX, PARSING_ERROR]
          rateLimit: { rps: 5, perUserRps: 1 }
        - name: mock
          enabled: false
      defaultProvider: { strategy: busy_message, message: "系统繁忙，请稍后重试", httpStatus: 503 }
    premium:
      nodes:
        - name: openai
          enabled: true
          timeoutMs: 1200
          retry: { attempts: 2 }
          rateLimit: { rps: 10, perUserRps: 2 }
      defaultProvider: { strategy: fsrs_fallback }
    experimentA:
      nodes:
        - name: openai
          enabled: true
          timeoutMs: 1500
          retry: { attempts: 1 }
      defaultProvider: { strategy: busy_message }

  routing:
    # Simple rule routing; first match wins
    rules:
      - when: { tier: [GOLD, PLATINUM] }
        useChain: premium
      - when: { abGroup: [A] }
        useChain: experimentA
      - when: { route: [ai-recommendations] }
        useChain: main
```

Implementation Plan
- Model
  - Extend `LlmProperties`:
    - `Map<String, Chain> chains` and `String defaultChainId`.
    - `Routing` with `List<Rule> rules` where `Rule` contains `when` (maps: `tier`, `abGroup`, `route`) and `useChain`.
  - Keep existing `Chain`/`Node` model unchanged for reuse.
- Selection
  - Add `ChainSelector` service:
    - `Chain select(RequestContext ctx, LlmProperties props)`.
    - Evaluate `routing.rules` in order; if none match, return `chains[defaultChainId]`.
    - Validate missing `defaultChainId` or chain not found → log warn, fallback to the first defined chain.
  - Add `LlmOrchestrator` (new) or evolve current wiring:
    - `Result execute(ctx, candidates, options)`:
      1) Select chain via `ChainSelector`.
      2) Instantiate/Reuse `ProviderChain` with the selected `Chain` (pass chain instance instead of global props.chain).
      3) Execute (sync/async) and return `Result`.
- Wiring
  - `LlmConfiguration`:
    - Provide beans for `ChainSelector` and `LlmOrchestrator`.
    - Build a provider catalog once; `ProviderChain` can be constructed per call with selected chain, or reuse a single `ProviderChain` that accepts `Chain` parameter on `execute`.
  - `AIRecommendationService`:
    - Populate `RequestContext` with `tier` (from user profile/JWT claim), `abGroup` (from feature flags or rollout), and `route`.
    - Call orchestrator instead of the fixed `providerChain.execute(...)`.
- Telemetry
  - Fill `AIRecommendationResponse.Meta.chainId` with the selected chain id; `policyId` with a short hash of matched rule.
  - Continue to emit `X-Provider-Chain`, add `X-Chain-Id`, `X-Policy-Id` headers.
- Safety
  - On misconfig or empty chain, fallback to `defaultProvider` or FSRS fallback with a clear `fallbackReason`.

Testing
- Unit tests for `ChainSelector` covering precedence, missing chains, unknown tiers.
- Integration test with 2 chains and different `RequestContext`s.
- Config validation test: duplicate chain ids, no default defined.

Rollout
- Start with `rules` matching a small AB cohort (e.g., abGroup A = 10%).
- Gradually add `tier` rules (e.g., GOLD/PLATINUM) after monitoring success/error rates.

## 2) Per‑Segment Feature Toggles (on/off)

Outcome
- Gate LLM recommendation by user segment to control exposure and cost.
- Support allow/deny lists for emergency kill‑switch.

Proposed Config (YAML)
```yaml
llm:
  enabled: true                 # global gate
  toggles:
    byTier:                     # default true unless explicitly set
      FREE: false
      BRONZE: true
      SILVER: true
      GOLD: true
      PLATINUM: true
    byAbGroup:
      A: true
      B: false
      default: true
    byRoute:
      ai-recommendations: true
    allowUserIds: [1001, 1002]  # always on
    denyUserIds:  []            # force off
```

Implementation Plan
- Model
  - Extend `LlmProperties` with `FeatureToggles`:
    - Maps: `byTier`, `byAbGroup`, `byRoute`; sets: `allowUserIds`, `denyUserIds`.
- Evaluator
  - Add `LlmToggleService.isEnabled(ctx: RequestContext): boolean`:
    1) If global `llm.enabled=false` → return false.
    2) If `userId` in `denyUserIds` → false.
    3) If `userId` in `allowUserIds` → true.
    4) Check `byRoute`, `byTier`, `byAbGroup` with sensible defaults (missing key = inherit global true).
- Usage
  - `AIRecommendationService`: early exit if toggle off:
    - Return FSRS fallback (deterministic) and set `meta.strategy=fsrs_fallback` and `fallbackReason=TOGGLE_OFF`.
    - Emit header `X-Fallback-Reason: TOGGLE_OFF`.
- Caching
  - Include segment dimensions in cache key to avoid mixing (e.g., `seg_tier_BRONZE_ab_A`).
- Observability
  - Add counters for `toggle_on/off`, by segment; add short logs for toggle decisions.

Testing
- Unit tests for `LlmToggleService` covering allow/deny, and map lookups.
- Integration test ensuring FSRS fallback when toggled off.

Rollout
- Start with FREE users off, BRONZE+ on.
- Flip AB group B off for experiments; allowlist staff accounts.

## Suggested Minimal Diffs (High‑Level)
- Config
  - `LlmProperties` add:
    - `Map<String, Chain> chains`, `String defaultChainId`, `Routing`, `FeatureToggles`.
  - `application.yml` add `llm.chains`, `llm.routing`, `llm.toggles` with prod defaults (conservative).
- Code
  - New: `ChainSelector`, `LlmToggleService`, `LlmOrchestrator`.
  - Adapt: `AIRecommendationService` to call toggle+selector prior to execution and to extend cache keys with segment suffix.
  - Adapt: `ProviderChain` to accept a `Chain` instance per call, or build per‑call from the selector.
- Telemetry
  - Fill `meta.chainId`, `meta.policyId` and export `X-Chain-Id`, `X-Policy-Id`.

## Acceptance Criteria
- Routing selects expected chain across tiers and AB groups with correct headers (`X-Chain-Id`, `X-Provider-Chain`).
- Toggles correctly gate LLM per segment; disabled segments return FSRS fallback with `fallbackReason=TOGGLE_OFF`.
- Cache keys and results are isolated by prompt version and segment.
- Misconfiguration does not break traffic; requests cleanly fallback with clear reasons.

## Risks & Mitigations
- Misconfig causing empty chains → detect on startup and log errors; runtime fallback to default chain.
- Segment explosion → limit the dimensions included in the cache key; keep segment schema small (tier, abGroup, route).
- Operational complexity → add simple admin endpoints (or config reload) to view current routing and toggles.

## Rollout Plan
- Phase 1: Implement toggles, ship with FREE off; verify metrics.
- Phase 2: Add routing rules for small AB cohort; observe error rates and costs.
- Phase 3: Enable premium chain for GOLD/PLATINUM; compare quality metrics; iterate.

