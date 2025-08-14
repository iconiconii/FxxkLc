/**
 * Review and FSRS API client functions
 */

import { apiRequest } from './api'

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
  problemId: number
  title: string
  difficulty: string
  due: string
  state: string
  intervalDays: number
  stability: number
  cardDifficulty: number
  reps: number
  lapses: number
}

export interface ReviewQueue {
  newCards: ReviewQueueCard[]
  learningCards: ReviewQueueCard[]
  reviewCards: ReviewQueueCard[]
  relearningCards: ReviewQueueCard[]
  totalCount: number
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
  reviewType: 'LEARNING' | 'REVIEW' | 'CRAM'
}

export interface OptimizationResult {
  success: boolean
  message: string
  parameters: any
}

export const reviewApi = {
  /**
   * Get review queue for user
   */
  async getReviewQueue(limit: number = 20): Promise<ReviewQueue> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<ReviewQueue>(`/review/queue?${params}`)
  },

  /**
   * Submit review for a problem
   */
  async submitReview(request: SubmitReviewRequest): Promise<ReviewResult> {
    return await apiRequest<ReviewResult>('/review/submit', {
      method: 'POST',
      body: JSON.stringify(request),
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
    return await apiRequest<OptimizationResult>('/review/optimize-parameters', {
      method: 'POST',
    })
  },
}