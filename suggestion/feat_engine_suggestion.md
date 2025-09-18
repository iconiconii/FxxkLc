Where We Are
  - LLM pipeline exists: AIRecommendationService → ProviderChain → providers (OpenAiProvider, DefaultProvider), toggles
  (LlmToggleService), routing (ChainSelector), caching (user/chain/prompt-version segmented), telemetry headers.
  - User profiling exists: UserProfilingService → UserProfile (domains, difficulty, learning pattern, tag affinity).
  - Candidate generation exists: CandidateBuilder (FSRS-driven) + CandidateEnhancer (domain/similarity enrichment).
  - Prompt templates exist: PromptTemplateService (v2 system/user messages with profile + candidates, JSON-only schema).
  - Fallback/FSRS-only path solid: FSRSService, FSRSAlgorithmImpl, FSRSCardMapper (rich signals: stability, difficulty,
  lapses, nextReview etc.).
  - Configs ready: application.yml for multiple chains, toggles, similarity weights; dev/test/prod profiles already
  wired.

  Below are targeted suggestions for each subtask.

  Prompt Templates

  - Add learning objectives to request context and prompts.
      - Code: extend RequestContext with optional fields:
          - List<String> targetDomains, String objective (enum: weakness_focus, progressive_difficulty, topic_coverage,
  exam_prep, refresh_mastered), String desiredDifficulty (EASY|MEDIUM|HARD), Integer timeboxMinutes (optional).
      - API: update ProblemRecommendationController.getAiRecommendations to accept query params objective, domains[],
  difficulty, timebox and populate RequestContext.
      - Prompt: extend PromptTemplateService (add v3):
          - System: keep strict JSON schema (no markdown, items[] with problemId|reason|confidence|strategy|score),
  enumerate valid strategies, and forbid new IDs.
          - User: include “Goals” section (learning objective, target domains, desired difficulty, timebox if present)
  and a slimmer “Candidates” JSON (id, topic, difficulty, tags, attempts, recentAccuracy, optional urgency).
      - Versioning and cache: bump getCurrentPromptVersion() to v3 and ensure cache keys include pv_v3 and objective
  hash (already segmented by promptVersion + chainId; add objective hash to segmentSuffix or the value passed to
  CacheKeyBuilder.buildUserKey).
      - Safety: keep PII out of prompts; only pass summarized profile and candidate features.

  Hybrid Algorithm (FSRS + LLM Content)

  - Represent FSRS “urgency” explicitly and use it both pre- and post-LLM.
      - Pre-LLM:
          - Enrich CandidateBuilder.toCandidate(...) to compute and attach features used in prompt (no interface change
  needed): retentionProbability (from FSRSCard.calculateRetentionProbability()), daysOverdue, maybe normalized urgency =
  clamp(0..1) from 1-retentionProbability + overdue bonus.
          - In PromptTemplateService.buildCandidateArray, include the new fields (as read-only features for LLM).
      - Post-LLM (true hybrid ranking):
          - Add a HybridRankingService that merges LLM-ranked items with FSRS/Similarity/UserProfile signals for final
  scoring and reordering:
              - Inputs: LLM items, original candidate map (id → features), UserProfile, config weights.
              - Signals: score_llm (from provider), score_fsrs (urgency: 1-retention, daysOverdue, lapses),
  score_similarity (from CandidateEnhancer or compute Jaccard quickly via SimilarityScorer), score_personalization
  (domainAffinity, difficulty match to DifficultyPref).
              - Output: updated RecommendationItemDTO list with source=HYBRID, re-ordered by weighted score; preserve
  original LLM reasons but augment with hybrid “why” if needed.
          - Config: add rec.hybrid weights in application.yml (e.g., llm=0.45 fsrs=0.3 similarity=0.15
  personalization=0.1).
          - Fallback: if LLM fails, keep current FSRS fallback intact.
      - Replace placeholder randomness in CandidateEnhancer.applyHybridScoring with real FSRS/similarity signals or move
  hybridization to post-LLM only (clear separation and testability).

  Multi-Dimensional Strategies

  - Strategy-aware selection with quotas and backfills.
      - Ensure each item has a strategy:
          - Prefer LLM-provided strategy (already part of schema).
          - If missing, infer server-side:
              - weakness_focus: candidate domains intersect UserProfile.getWeakDomains().
              - progressive_difficulty: difficulty aligned with DifficultyPref trend and “slightly above” current
  mastery.
              - topic_coverage: introduces new domains/tags (diversity from SimilarityScorer).
              - review_reinforcement: high urgency (overdue or low retention).
      - Mixer: add RecommendationMixer (or integrate in HybridRankingService) to enforce a blend, e.g., 40% weakness, 40%
  progressive, 20% coverage; backfill from next-best strategy if a bucket is short.
      - Configurable quotas: rec.strategy.mix (percentages per strategy) in application.yml.
      - Determinism: use deterministic tie-breaking (problemId/userId hashing already exists in CandidateEnhancer) to
  keep results stable for caching and A/B.

  Confidence Scoring

  - Calibrated confidence beyond LLM self-report.
      - Add ConfidenceCalibrator:
          - Inputs per item: llm_conf, fsrs_urgency (inverse retention, daysOverdue), personalization_fit (domain
  affinity, difficulty match), similarity_fit (tags/category), data_quality (from UserProfile.getDataQuality()),
  provider/chain status (fallback or parsing errors).
          - Proposed formula (configurable weights):
              - base = 0.2llm_conf + 0.25personalization_fit + 0.25fsrs_urgency + 0.15similarity_fit
              - adjust = base * (0.6 + 0.4*data_quality)
              - penalties: -0.1..-0.2 if provider_chain fallback occurred or model parsing struggled; clamp to [0,1].
          - Set RecommendationItemDTO.setConfidence(final) and align score to final ranking score for transparency.
      - Telemetry: record average confidence, source, and fallback reasons via LlmMetricsCollector for dashboards.

  API & Config Changes

  - Controller: GET /problems/ai-recommendations
      - New params: objective, domains[], difficulty, timebox, limit.
      - Validate/sanitize inputs; default objective to profile-driven focus when omitted.
  - Context: extend RequestContext with the objective fields and pass into prompt building and hybrid ranking.
  - Config:
      - rec.hybrid: weights for llm/fsrs/similarity/personalization.
      - rec.strategy.mix: per-strategy quota percentages and enable/disable switch.
      - Keep existing rec.similarity.integration and llm settings; ensure prompt versioning and chain segmentation are in
  cache keys (already implemented).

  Pitfalls To Watch

  - JSON-only responses: keep strict parsing and schema enforcement; guard against hallucinated problemIds (already
  done).
  - Cache correctness: include promptVersion, chainId, segment, and objective hash in keys; invalidate on feedback or
  profile changes (feedback service already evicts keys and profile cache).
  - Cost/latency: keep candidate arrays compact; cap candidate count; avoid heavy per-item DB hits in post-LLM hybrid
  step (hydrate signals in one query or reuse CandidateBuilder map).
  - Determinism and A/B: maintain stable selection ordering to make cache and experimentation meaningful; avoid non-
  deterministic randomness.
  - Safety: do not log API keys or prompt content with PII; keep telemetry to IDs and versions only.
  - Backward compatibility: FSRS-only path must still work when llm.enabled=false; ensure default strategy/meta remains
  consistent.

  High-Value Test Coverage

  - Prompt v3 tests: verify objectives/domains/difficulty/timebox are embedded and JSON schema remains unchanged.
  - Hybrid ranking unit tests: given synthetic candidates with FSRS/similarity/profile signals and LLM outputs, verify
  final ordering and confidence calibration.
  - Controller contract tests: inputs → headers (X-Trace-Id, X-Provider-Chain, X-Chain-Id, X-User-Profile) and payload
  shape remain correct; cache hit path exercises segmentation.
  - Degradation tests: llm.enabled=false, retry/timeouts, DefaultProvider strategies; ensure FSRS fallback path still
  sets clear reasons and confidence behavior.