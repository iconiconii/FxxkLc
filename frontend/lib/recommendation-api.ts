/**
 * AI Recommendation API Client
 * Provides methods for fetching recommendations and submitting feedback
 * with header capture for telemetry and debugging
 */

import { apiRequestWithHeaders } from '@/lib/api'
import { 
  AIRecommendationRequest, 
  AIRecommendationResponse, 
  FeedbackRequest, 
  FeedbackResponse,
  RecommendationHeaders,
  RecommendationSource
} from '@/types/recommendation'

/**
 * Map backend recommendation source to frontend format
 * Backend: 'LLM'/'AI'/'FSRS'/'DEFAULT' vs Frontend UI: 'ai'/'fsrs'
 */
function mapRecSourceFromBackend(backendSource: string | null): RecommendationSource {
  if (!backendSource) return 'DEFAULT'
  
  const upperSource = backendSource.toUpperCase()
  switch (upperSource) {
    case 'LLM':
    case 'AI':
      return 'LLM'
    case 'FSRS':
      return 'FSRS'
    case 'HYBRID':
      return 'HYBRID'
    case 'DEFAULT':
    default:
      return 'DEFAULT'
  }
}

interface ApiResponseWithHeaders<T> {
  data: T
  headers: RecommendationHeaders
}

/**
 * Get AI recommendations with request parameters
 */
export async function getAIRecommendations(
  params: AIRecommendationRequest = {}
): Promise<ApiResponseWithHeaders<AIRecommendationResponse>> {
  const searchParams = new URLSearchParams()
  
  if (params.limit) {
    searchParams.append('limit', params.limit.toString())
  }
  // Use aligned parameter names
  if (params.difficulty) {
    searchParams.append('difficulty', params.difficulty)
  }
  if (params.domains) {
    params.domains.forEach(domain => 
      searchParams.append('domains', domain)
    )
  }
  if (params.recommendation_type) {
    searchParams.append('recommendation_type', params.recommendation_type)
  }
  if (params.ab_group) {
    searchParams.append('ab_group', params.ab_group)
  }
  // Add support for forceRefresh parameter
  if (params.forceRefresh) {
    searchParams.append('forceRefresh', params.forceRefresh.toString())
  }
  if (params.cursor) {
    searchParams.append('cursor', params.cursor)
  }
  if (params.page) {
    searchParams.append('page', params.page.toString())
  }
  if (params.offset) {
    searchParams.append('offset', params.offset.toString())
  }

  const endpoint = `/problems/ai-recommendations${searchParams.toString() ? `?${searchParams.toString()}` : ''}`
  
  const response = await apiRequestWithHeaders<AIRecommendationResponse>(endpoint, {
    method: 'GET',
  })

  // Extract relevant headers
  const headers: RecommendationHeaders = {
    traceId: response.headers.get('X-Trace-Id') || '',
    recSource: mapRecSourceFromBackend(response.headers.get('X-Rec-Source')),
    cacheHit: response.headers.get('X-Cache-Hit') === 'true',
    providerChain: response.headers.get('X-Provider-Chain') || undefined,
  }

  return { data: response.body, headers }
}

/**
 * Submit recommendation feedback
 */
export async function postRecommendationFeedback(
  problemId: number,
  body: Omit<FeedbackRequest, 'problemId'>
): Promise<FeedbackResponse> {
  const response = await apiRequestWithHeaders<FeedbackResponse>(`/problems/${problemId}/recommendation-feedback`, {
    method: 'POST',
    body: JSON.stringify({ ...body, problemId }),
  })

  return response.body
}

/**
 * Generate recommendation ID for feedback correlation
 */
export function generateRecommendationId(traceId: string, problemId: number): string {
  return `${traceId}:${problemId}`
}
