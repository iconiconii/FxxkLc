/**
 * Analytics API client functions
 */

import { apiRequest } from './api'

export interface AnalyticsOverview {
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

export interface DailyReviewActivity {
  date: string
  reviewCount: number
  correctCount: number
  newCards: number
  totalTimeMinutes: number
}

export interface UserReviewStats {
  totalReviews: number
  correctReviews: number
  averageRating: number
  totalTimeMinutes: number
  streakDays: number
  accuracy: number
}

export interface MonthlyProgress {
  month: string
  year: number
  reviewCount: number
  newCardsLearned: number
  accuracy: number
  totalTimeMinutes: number
}

export interface DifficultyPerformance {
  difficulty: string
  totalReviews: number
  correctReviews: number
  accuracy: number
  averageRating: number
}

export interface SystemFSRSMetrics {
  totalUsers: number
  totalCards: number
  totalReviews: number
  averageRetention: number
  averageStability: number
  averageDifficulty: number
  optimizedUsers: number
}

export const analyticsApi = {
  /**
   * Get user learning overview
   */
  async getOverview(): Promise<AnalyticsOverview> {
    return await apiRequest<AnalyticsOverview>('/analytics/overview')
  },

  /**
   * Get daily review activity
   */
  async getDailyActivity(days: number = 30): Promise<DailyReviewActivity[]> {
    const params = new URLSearchParams({
      days: days.toString(),
    })

    return await apiRequest<DailyReviewActivity[]>(`/analytics/daily?${params}`)
  },

  /**
   * Get weekly review statistics
   */
  async getWeeklyStats(): Promise<UserReviewStats> {
    return await apiRequest<UserReviewStats>('/analytics/weekly')
  },

  /**
   * Get monthly progress
   */
  async getMonthlyProgress(): Promise<MonthlyProgress[]> {
    return await apiRequest<MonthlyProgress[]>('/analytics/monthly')
  },

  /**
   * Get difficulty performance breakdown
   */
  async getDifficultyPerformance(days: number = 30): Promise<DifficultyPerformance[]> {
    const params = new URLSearchParams({
      days: days.toString(),
    })

    return await apiRequest<DifficultyPerformance[]>(`/analytics/difficulty-performance?${params}`)
  },

  /**
   * Get system-wide metrics (for admin users)
   */
  async getSystemMetrics(days: number = 30): Promise<SystemFSRSMetrics> {
    const params = new URLSearchParams({
      days: days.toString(),
    })

    return await apiRequest<SystemFSRSMetrics>(`/analytics/system?${params}`)
  },
}