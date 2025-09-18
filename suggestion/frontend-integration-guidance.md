# AI Recommendation Frontend Integration — Implementation Guide (Next.js + TS)

This guide distills the “Frontend Integration” requirements into concrete, actionable steps for implementation in our Next.js (App Router) + TypeScript codebase. It covers UI style, component structure, data flow, interactions, and testing. The goal is a clean MVP that’s easy to iterate on while meeting performance and UX expectations.

## Scope
- New UI for AI recommendations on the dashboard and problem lists.
- Toggle between FSRS and AI recommendations with user preference.
- Feedback interactions (helpful/not helpful/skip/solved/hidden) with backend reporting.
- Loading/error UX that gracefully degrades on timeouts, 429, and default-provider fallback.

Non-goals: full design overhaul; complex visualization beyond scores/badges.

## Tech + Style Conventions
- Use existing UI kit in `frontend/components/ui` (Card, Button, Badge, Tabs, Select, Spinner, Separator, Progress). Tailwind utility classes with our existing dark theme.
- Components PascalCase; files kebab-case if utilities. 2-space indent. Keep consistent with existing dashboard cards.
- Client components for interactive parts (`'use client'`). Fetch via `frontend/lib/api.ts:apiRequest`.

## API Contract (from backend spec)
- GET `/api/v1/problems/ai-recommendations`
  - Query: `limit` (1..50, default 10), `difficulty_preference`, `topic_filter[]`, `recommendation_type`, `ab_group?`.
  - Returns `AIRecommendationResponse` with `items: RecommendationItemDTO[]` and `meta { cached, traceId, generatedAt, busy?, strategy? }`.
  - Headers: `X-Trace-Id`, `X-Rec-Source` (LLM|FSRS|HYBRID|DEFAULT), `X-Cache-Hit` (true/false), `X-Provider-Chain` (e.g., `openai>azure>default`).
- POST `/api/v1/problems/{id}/recommendation-feedback`
  - Body: `FeedbackRequest { recommendationId, problemId, action: accepted|skipped|solved|hidden, helpful?: boolean, rating?: 1..5, comment?: string }`
  - Returns `{ status: "ok", recordedAt }`.

If the backend returns `meta.busy=true` or `X-Rec-Source=DEFAULT`, show a gentle “系统繁忙，已回退基础推荐” notice. If backend uses FSRS fallback, still render items normally with subdued indicator.

## Types and Client
Create shared types aligned with DTOs and a small API client. Keep it colocated for now and promote later if needed.

- File: `frontend/types/recommendation.ts`
  - `RecommendationSource = 'LLM' | 'FSRS' | 'HYBRID' | 'DEFAULT'`
  - `RecommendationItem` with fields: `problemId: number`, `title?: string`, `reason: string`, `confidence: number`, `strategy: string`, `source: RecommendationSource`, `explanations?: string[]`, `model?: string`, `promptVersion?: string`, `latencyMs?: number`, `score?: number`, `matchScore?: number`, `difficulty?: string`, `topics?: string[]`.
  - `AIRecommendationMeta` with `cached: boolean`, `traceId: string`, `generatedAt: string`, `busy?: boolean`, `strategy?: string`.
  - `AIRecommendationResponse` with `items: RecommendationItem[]`, `meta: AIRecommendationMeta`.
  - `FeedbackAction = 'accepted' | 'skipped' | 'solved' | 'hidden'`.

- File: `frontend/lib/recommendation-api.ts`
  - `getAIRecommendations(params)` → returns `{ data, headersMeta }` capturing JSON and relevant headers (`X-Trace-Id`, `X-Rec-Source`, `X-Cache-Hit`, `X-Provider-Chain`). Implementation: use `fetch` via `apiRequest` pattern; if needed, add a helper `apiRequestWithHeaders` (wrapper that returns both parsed JSON and headers) or directly use `fetch` for this module to read headers once, while preserving cookies.
  - `postRecommendationFeedback(problemId, body)`.
  - Pass `ab_group` from a small util (see A/B below).

Note: Our current `apiRequest` returns parsed JSON only; to read headers we can either:
- Add `apiRequestWithHeaders<T>(endpoint, options): Promise<{ body: T; headers: Headers }>` in `frontend/lib/api.ts` OR
- In `recommendation-api.ts`, call `fetch(`${API_BASE_URL}/...`, { credentials: 'include', headers: { ... }})` mirroring `apiRequest` options to access headers, then JSON-parse locally. Keep changes minimal and localized.

## Components
Place UI under `frontend/components/recommendation/`.

- `AIRecommendationCard.tsx`
  - Purpose: Display a single recommended problem with reasoning and controls.
  - UI: `Card` with sections: title + difficulty + badges (source/strategy/cache-hit), reason/explanations (collapsible), confidence/score indicator, action buttons.
  - Controls: Buttons — Helpful (thumbs-up), Not helpful (thumbs-down), Skip, Mark solved; secondary “隐藏” in overflow menu if needed.
  - Visual indicators:
    - Source badge: LLM (blue), FSRS (green), HYBRID (purple), DEFAULT (gray) using `Badge` variants.
    - Confidence/progress: `Progress` or a compact meter with text (e.g., “置信度 0.82”).
    - Cache hit: subtle icon or tooltip if `X-Cache-Hit=true`.
  - Accessibility: `aria-label` on action buttons; keyboard focus rings; `sr-only` text for icons.

- `AIRecommendationsList.tsx`
  - Purpose: Render a paginated list of `AIRecommendationCard`s with loading, empty, and error states.
  - Data: Uses React Query `useQuery` or `useInfiniteQuery` with params (limit, filters, type, abGroup). Include retry=1, staleTime ~60s for cache.
  - UX states:
    - Loading: skeleton card shimmers (3–5 items) using neutral bg in both light/dark.
    - Error: compact error with retry.
    - Busy/default fallback: render items with a top inline notice: “系统繁忙，推荐已回退基础策略，结果可能较为保守”。
  - Interactions: Wire feedback actions; optimistic UI (e.g., after “已解决”, gray out card or move to end).

- `RecommendationToggle.tsx`
  - Purpose: Switch between FSRS and AI recommendations, storing user preference.
  - UI: `Tabs` or segmented control with two options: “FSRS 推荐” | “AI 推荐（实验）”。
  - Persistence: `localStorage.setItem('pref_recommendation_source', 'fsrs' | 'ai')`; read on mount and default to AI only for users in AB allowlist.

- `RecommendationFilters.tsx`
  - Optional: `Select` for difficulty, `Dropdown` for topics, sort by score/confidence; bubble selected chips with `Badge`.

- `RecommendationQualityIndicator.tsx` (small subcomponent)
  - Compact meter showing `score` and `confidence` with tooltip text for explanation.

## Pages + Integration

- Dashboard
  - File: `frontend/app/dashboard/recommendations/page.tsx` (already planned in task) or embed into existing dashboard grid via a new component:
    - Add `components/dashboard/ai-recommendations.tsx` and include in `optimized-dashboard-content.tsx` right column below `ReviewQueuePreview`.
  - Layout: one card-wide list of top N (e.g., 5–10) with “查看全部推荐” CTA navigating to a full-page list.
  - Toggle: surface `RecommendationToggle` in the section header; value chooses source and query param.

- Problem List Enhancement
  - Add an “AI” badge next to problem titles when they appear in the current recommendation set; use a local set of recommended `problemId`s to mark items.
  - Provide sorting “推荐度（默认）/ 置信度 / 频次 / 难度” and quick filters.

- Settings
  - File: `frontend/app/settings/recommendations/page.tsx` (simple form)
  - Controls: default recommendation source, default limit, difficulty preference, topics (multi-select), frequency; save to localStorage first, optionally POST to backend when available.

## Data Flow and Request Details
- Prefer React Query for caching and refetch logic.
- Parameters: derive from user preferences and UI filters. Use QS: `limit`, `difficulty_preference`, `topic_filter[]=...`, `recommendation_type` (fsrs|llm_only|hybrid), `ab_group`.
- Headers: capture `X-Trace-Id`, `X-Rec-Source`, `X-Cache-Hit`, `X-Provider-Chain` for telemetry display and logging.
- Trace: display the last 6 chars of `traceId` in dev tooltip for debugging.
- A/B Group: create `lib/ab.ts` that deterministically assigns group based on userId hash if available, else sticky random stored in localStorage as `ab_group_reco`.
- RecommendationId: if backend doesn’t provide it, synthesize `recommendationId = `${meta.traceId}:${item.problemId}` to correlate feedback.

## Interactions and State
- Feedback
  - Actions: helpful/not helpful/skip/solved/hidden map to `FeedbackAction` enum.
  - Call `POST /api/v1/problems/{id}/recommendation-feedback` with `{ recommendationId, problemId, action, helpful?: boolean, rating?, comment? }`.
  - Optimistic UX: immediately reflect states (e.g., dim card on “已解决”), and toast “已记录反馈”。On failure, revert and toast error.

- Loading/Error/Busy
  - Loading: 3–5 skeleton cards (visual parity with other dashboard loaders).
  - Error: inline error with retry button; do not block rest of dashboard.
  - Busy/default: render items, but show subdued banner and set source badge accordingly.

- Pagination/Lazy Loading
  - Use `useInfiniteQuery` with `getNextPageParam` reading `items.length === limit`.
  - “加载更多” button; or IntersectionObserver auto-load when near bottom.

## Visual Design
- Cards
  - Use `Card` with consistent paddings (`px-6 py-4`), subtle borders (`border-gray-200 dark:border-[#1F1F23]`), rounded corners.
  - Title row: problem title (link), difficulty badge, source/strategy badges right-aligned.
  - Body: reason (clamp to 2–3 lines), “展开解释” to show `explanations` list.
  - Footer: confidence/progress + action buttons.

- Badges
  - LLM → primary/blue; FSRS → green; HYBRID → purple; DEFAULT → gray.
  - Cache hit (if true) → light gray dot with tooltip “命中缓存，加速响应”。

- Dark Mode
  - Respect existing theme tokens; test readability for all badges and progress colors.

Copy suggestions (ZH):
- Section title: “AI 推荐（实验）”
- Busy banner: “系统繁忙，推荐已回退基础策略，结果可能较为保守。”
- Empty: “暂无可用推荐，稍后再试或调整偏好。”
- Feedback toasts: “感谢反馈，已记录。” / “提交失败，请稍后重试。”

## File Plan (suggested new files)
- `frontend/types/recommendation.ts`
- `frontend/lib/recommendation-api.ts`
- `frontend/lib/ab.ts` (AB group util)
- `frontend/components/recommendation/AIRecommendationCard.tsx`
- `frontend/components/recommendation/AIRecommendationsList.tsx`
- `frontend/components/recommendation/RecommendationToggle.tsx`
- `frontend/components/recommendation/RecommendationFilters.tsx` (optional)
- `frontend/components/dashboard/ai-recommendations.tsx` (dashboard section wrapper)
- `frontend/app/dashboard/recommendations/page.tsx` (full-page view)

## Implementation Steps (MVP → polish)
1) Types + API client with headers capture
2) Card component with basic layout + buttons
3) List component with fetch, loading, error, busy states
4) Dashboard section integration with toggle and CTA
5) Problem list badge integration
6) Feedback wiring with optimistic updates
7) Filters + pagination (optional in MVP)
8) Settings page for preferences (optional in MVP)

## Testing
- Unit
  - Render `AIRecommendationCard` with different sources; verify badges and confidence meter.
  - Simulate button clicks; ensure correct payload to feedback API.
  - List component: loading skeleton, error fallback, busy banner.

- E2E (Playwright)
  - Load dashboard: recommendations visible with skeleton → content.
  - Toggle FSRS/AI switches list request params.
  - Click feedback buttons; server returns 200; UI shows toast and card state updates.
  - Error injection (429/timeout) gracefully shows fallback message but keeps page usable.

## Performance + Reliability
- Keep limit small on dashboard (5–10); paginate in full-page view.
- Cache results for ~60s (React Query `staleTime`), refetch on tab focus.
- Defer heavy lists with dynamic import if needed.
- Avoid layout shift: fixed skeleton heights; use `min-h` for cards.

## Telemetry and Debug
- Log `traceId`, `source`, `providerChain`, and `cacheHit` to dev console in development builds only.
- Add a small “i” tooltip in dev to display headers and `meta.*` for troubleshooting.

## Integration Notes with Existing Codebase
- Follow the dashboard grid patterns in `components/dashboard/optimized-dashboard-content.tsx`.
- Reuse UI kit in `components/ui` for consistent look and feel.
- Use `frontend/lib/api.ts` for consistent credentials/cookies; add a small wrapper to access headers when needed.
- Keep strings localized consistently with existing Chinese copy.

---

If you want, I can scaffold the files as stubs to accelerate development, leaving TODOs for API wiring and exact UI polish.

