/**
 * AI Recommendation System Types
 * Aligned with backend DTOs for consistent data contracts
 */

export type RecommendationSource = 'LLM' | 'FSRS' | 'HYBRID' | 'DEFAULT'

export type FeedbackAction = 'accepted' | 'skipped' | 'solved' | 'hidden'

export interface RecommendationItem {
  problemId: number
  title?: string
  reason: string
  confidence: number
  strategy: string
  source: RecommendationSource
  explanations?: string[]
  model?: string
  promptVersion?: string
  latencyMs?: number
  score?: number
  matchScore?: number
  difficulty?: string
  topics?: string[]
}

export interface AIRecommendationMeta {
  cached: boolean
  traceId: string
  generatedAt: string
  busy?: boolean
  strategy?: string
  // Pagination support
  nextCursor?: string
  nextPage?: number
  hasMore?: boolean
  totalItems?: number
}

export interface AIRecommendationResponse {
  items: RecommendationItem[]
  meta: AIRecommendationMeta
}

export interface AIRecommendationRequest {
  limit?: number
  difficulty?: string  // Aligned with backend parameter name
  domains?: string[]   // Aligned with backend parameter name (was topic_filter)
  recommendation_type?: 'hybrid' | 'llm_only' | 'fsrs_fallback'
  ab_group?: string
  forceRefresh?: boolean  // Force bypass cache
  // Pagination support
  cursor?: string
  page?: number
  offset?: number
}

export interface FeedbackRequest {
  recommendationId: string
  problemId: number
  action: FeedbackAction
  helpful?: boolean
  rating?: number // 1-5
  comment?: string
}

export interface FeedbackResponse {
  status: 'ok'
  recordedAt: string
}

export interface RecommendationHeaders {
  traceId: string
  recSource: RecommendationSource
  cacheHit: boolean
  providerChain?: string
}