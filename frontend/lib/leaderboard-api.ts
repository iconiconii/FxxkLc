/**
 * Leaderboard API client functions
 */

import { apiRequest } from './api'

export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  avatarUrl?: string
  totalReviews: number
  correctReviews: number
  accuracy: number
  streak: number
  badge?: string
}

export interface AccuracyLeaderboardEntry {
  rank: number
  userId: number
  username: string
  avatarUrl?: string
  totalReviews: number
  correctReviews: number
  accuracy: number
  badge?: string
}

export interface StreakLeaderboardEntry {
  rank: number
  userId: number
  username: string
  avatarUrl?: string
  currentStreak: number
  longestStreak: number
  totalActiveDays: number
  badge?: string
}

export interface UserRankInfo {
  globalRank: number
  weeklyRank: number
  monthlyRank: number
  accuracyRank: number
  streakRank: number
  overallRank?: number
  percentile?: number
  badge?: string
}

export interface TopPerformersSummary {
  topByVolume: LeaderboardEntry[]
  topByAccuracy: AccuracyLeaderboardEntry[]
  topByStreak: StreakLeaderboardEntry[]
}

export interface LeaderboardStats {
  totalUsers: number
  weeklyActiveUsers: number
  usersWithStreak: number
  averageAccuracy: number
  maxCurrentStreak: number
  maxLongestStreak: number
  totalReviews: number
}

export const leaderboardApi = {
  /**
   * Get global leaderboard
   */
  async getGlobalLeaderboard(limit: number = 50): Promise<LeaderboardEntry[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<LeaderboardEntry[]>(`/leaderboard?${params}`)
  },

  /**
   * Get weekly leaderboard
   */
  async getWeeklyLeaderboard(limit: number = 50): Promise<LeaderboardEntry[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<LeaderboardEntry[]>(`/leaderboard/weekly?${params}`)
  },

  /**
   * Get monthly leaderboard
   */
  async getMonthlyLeaderboard(limit: number = 50): Promise<LeaderboardEntry[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<LeaderboardEntry[]>(`/leaderboard/monthly?${params}`)
  },

  /**
   * Get accuracy leaderboard
   */
  async getAccuracyLeaderboard(
    limit: number = 50, 
    days: number = 30
  ): Promise<AccuracyLeaderboardEntry[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
      days: days.toString(),
    })

    return await apiRequest<AccuracyLeaderboardEntry[]>(`/leaderboard/accuracy?${params}`)
  },

  /**
   * Get streak leaderboard
   */
  async getStreakLeaderboard(limit: number = 50): Promise<StreakLeaderboardEntry[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<StreakLeaderboardEntry[]>(`/leaderboard/streak?${params}`)
  },

  /**
   * Get user's rank information
   */
  async getUserRank(userId: number): Promise<UserRankInfo> {
    return await apiRequest<UserRankInfo>(`/leaderboard/user/${userId}/rank`)
  },

  /**
   * Get leaderboard summary statistics
   */
  async getLeaderboardStats(): Promise<TopPerformersSummary> {
    return await apiRequest<TopPerformersSummary>('/leaderboard/stats')
  },

  }