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
  masteryScore: number
  streak: number
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
  streakRank: number
  overallRank?: number
  percentile?: number
  badge?: string
}

export interface TopPerformersSummary {
  topByVolume: LeaderboardEntry[]
  topByStreak: StreakLeaderboardEntry[]
}

export interface LeaderboardStats {
  totalUsers: number
  weeklyActiveUsers: number
  usersWithStreak: number
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