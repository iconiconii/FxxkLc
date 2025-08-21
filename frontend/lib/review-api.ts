/**
 * Review and FSRS API client functions
 */

import { apiRequest } from './api'
import { withIdempotency } from './idempotency-utils'

export interface FSRSCard {
  id: number
  userId: number
  problemId: number
  due: string
  stability: number
  difficulty: number
  elapsedDays: number
  scheduledDays: number
  reps: number
  lapses: number
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING'
  lastReview?: string
  createdAt: string
  updatedAt: string
}

export interface ReviewQueueCard {
  id: number
  userId: number
  problemId: number
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING'
  difficulty: number
  stability: number
  reviewCount: number
  lapses: number
  dueDate: string
  intervalDays: number
  problemTitle: string
  problemDifficulty: 'EASY' | 'MEDIUM' | 'HARD'
  priority: number
  new: boolean
  learning: boolean
  review: boolean
  relearning: boolean
  due: boolean
  overdue: boolean
}

export interface ReviewQueueResponse {
  cards: ReviewQueueCard[]
  totalCount: number
  stats: {
    totalCards: number
    newCards: number
    learningCards: number
    reviewCards: number
    relearningCards: number
    dueCards: number
    avgReviews: number
    avgDifficulty: number
    avgStability: number
    totalLapses: number
  }
  generatedAt: string
  // Pagination fields
  currentPage?: number
  pageSize?: number
  totalPages?: number
}

export interface ReviewQueue {
  newCards: ReviewQueueCard[]
  learningCards: ReviewQueueCard[]
  reviewCards: ReviewQueueCard[]
  relearningCards: ReviewQueueCard[]
  totalCount: number
  // Pagination fields
  currentPage?: number
  pageSize?: number
  totalPages?: number
}

export interface ReviewResult {
  success: boolean
  message: string
  nextReviewDate: string
  newState: string
  intervals: number[]
}

export interface UserLearningStats {
  totalCards: number
  newCards: number
  learningCards: number
  reviewCards: number
  relearningCards: number
  dueCards: number
  avgReviews: number
  avgDifficulty: number
  avgStability: number
  totalLapses: number
}

export interface SubmitReviewRequest {
  problemId: number
  rating: 1 | 2 | 3 | 4 // 1=Again, 2=Hard, 3=Good, 4=Easy
  reviewType: 'SCHEDULED' | 'EXTRA' | 'CRAM' | 'MANUAL' | 'BULK'
  requestId?: string // 幂等性请求ID
}

export interface OptimizationResult {
  success: boolean
  message: string
  parameters: any
}

export const reviewApi = {
  /**
   * Get review queue for user with pagination support
   */
  async getReviewQueue(
    limit: number = 20, 
    page: number = 1, 
    pageSize: number = 10,
    showAll: boolean = false
  ): Promise<ReviewQueue> {
    const params = new URLSearchParams({
      limit: limit.toString(),
      page: page.toString(),
      pageSize: pageSize.toString(),
      showAll: showAll.toString(),
    })

    const response = await apiRequest<ReviewQueueResponse>(`/review/queue?${params}`)
    
    // Transform the flat cards array into categorized arrays
    const newCards = response.cards.filter(card => card.state === 'NEW')
    const learningCards = response.cards.filter(card => card.state === 'LEARNING')
    const reviewCards = response.cards.filter(card => card.state === 'REVIEW')
    const relearningCards = response.cards.filter(card => card.state === 'RELEARNING')

    return {
      newCards,
      learningCards,
      reviewCards,
      relearningCards,
      totalCount: response.totalCount,
      currentPage: response.currentPage,
      pageSize: response.pageSize,
      totalPages: response.totalPages,
    }
  },

  /**
   * Get all problems that need review (due today or overdue, not reviewed today)
   * Ordered by next review time ascending
   */
  async getAllDueProblems(limit: number = 50): Promise<ReviewQueueCard[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
      showAll: 'true',
    })

    const response = await apiRequest<ReviewQueueResponse>(`/review/queue?${params}`)
    return response.cards
  },

  /**
   * Submit review for a problem
   */
  async submitReview(request: SubmitReviewRequest): Promise<ReviewResult> {
    // 为复习提交添加幂等性requestId
    const requestWithId = withIdempotency(request)
    
    return await apiRequest<ReviewResult>('/review/submit', {
      method: 'POST',
      body: JSON.stringify(requestWithId),
    })
  },

  /**
   * Get due cards for immediate review
   */
  async getDueCards(limit: number = 10): Promise<ReviewQueueCard[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<ReviewQueueCard[]>(`/review/due?${params}`)
  },

  /**
   * Get new cards for learning
   */
  async getNewCards(limit: number = 10): Promise<ReviewQueueCard[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<ReviewQueueCard[]>(`/review/new?${params}`)
  },

  /**
   * Get overdue cards
   */
  async getOverdueCards(limit: number = 10): Promise<ReviewQueueCard[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<ReviewQueueCard[]>(`/review/overdue?${params}`)
  },

  /**
   * Get user's learning statistics
   */
  async getLearningStats(): Promise<UserLearningStats> {
    return await apiRequest<UserLearningStats>('/review/stats')
  },

  /**
   * Get all user cards
   */
  async getUserCards(): Promise<FSRSCard[]> {
    return await apiRequest<FSRSCard[]>('/review/cards')
  },

  /**
   * Get card intervals for a problem
   */
  async getCardIntervals(problemId: number): Promise<number[]> {
    return await apiRequest<number[]>(`/review/intervals/${problemId}`)
  },

  /**
   * Optimize FSRS parameters for user
   */
  async optimizeParameters(): Promise<OptimizationResult> {
    // 为参数优化添加幂等性requestId
    const requestWithId = withIdempotency({})
    
    return await apiRequest<OptimizationResult>('/review/optimize-parameters', {
      method: 'POST',
      body: JSON.stringify(requestWithId),
    })
  },
}