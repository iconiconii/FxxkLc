/**
 * User API client functions
 */

import { apiRequest } from './api'

export interface UserProfile {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
  timezone?: string
  authProvider: string
  emailVerified: boolean
  createdAt: string
  lastLoginAt?: string
}

export interface UpdateProfileRequest {
  firstName?: string
  lastName?: string
  timezone?: string
  avatarUrl?: string
}

export interface UserPreferences {
  theme: 'light' | 'dark' | 'system'
  language: string
  emailNotifications: boolean
  pushNotifications: boolean
  dailyGoal: number
  difficultyPreference: 'easy' | 'medium' | 'hard' | 'mixed'
}

export interface UpdatePreferencesRequest {
  theme?: 'light' | 'dark' | 'system'
  language?: string
  emailNotifications?: boolean
  pushNotifications?: boolean
  dailyGoal?: number
  difficultyPreference?: 'easy' | 'medium' | 'hard' | 'mixed'
}

export interface UserStatistics {
  totalProblemsAttempted: number
  totalProblemsSolved: number
  easyProblemsSolved: number
  mediumProblemsSolved: number
  hardProblemsSolved: number
  overallAccuracy: number
  currentStreak: number
  longestStreak: number
  totalReviewTime: number
  totalReviews: number
  averageRating: number
}

export interface AuthStatus {
  isAuthenticated: boolean
  userId?: number
  username?: string
}

export interface UserProblemStatus {
  problemId: number
  title: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  status: 'not_done' | 'done' | 'reviewed'
  mastery: number // 0-3 stars
  lastAttemptDate?: string
  lastConsideredDate?: string
  attemptCount: number
  accuracy: number
  notes?: string
}

export interface UpdateProblemStatusRequest {
  status: 'not_done' | 'done' | 'reviewed'
  mastery?: number
  notes?: string
}

export interface ProblemMastery {
  problemId: number
  masteryLevel: number // 0-3
  attemptCount: number
  accuracy: number
  lastAttemptDate?: string
  nextReviewDate?: string
  difficulty: string
  notes?: string
}

export const userApi = {
  /**
   * Get current user profile
   */
  async getCurrentUserProfile(): Promise<UserProfile> {
    return await apiRequest<UserProfile>('/user/profile')
  },

  /**
   * Update user profile
   */
  async updateUserProfile(request: UpdateProfileRequest): Promise<UserProfile> {
    return await apiRequest<UserProfile>('/user/profile', {
      method: 'PUT',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get user preferences
   */
  async getUserPreferences(): Promise<UserPreferences> {
    return await apiRequest<UserPreferences>('/user/preferences')
  },

  /**
   * Update user preferences
   */
  async updateUserPreferences(request: UpdatePreferencesRequest): Promise<UserPreferences> {
    return await apiRequest<UserPreferences>('/user/preferences', {
      method: 'PUT',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get user statistics
   */
  async getUserStatistics(): Promise<UserStatistics> {
    return await apiRequest<UserStatistics>('/user/statistics')
  },

  /**
   * Get authentication status
   */
  async getAuthStatus(): Promise<AuthStatus> {
    return await apiRequest<AuthStatus>('/user/status')
  },

  /**
   * Get user's problem progress
   */
  async getUserProblemProgress(): Promise<UserProblemStatus[]> {
    return await apiRequest<UserProblemStatus[]>('/problems/user-progress')
  },

  /**
   * Get problem status
   */
  async getProblemStatus(problemId: number): Promise<UserProblemStatus> {
    return await apiRequest<UserProblemStatus>(`/problems/${problemId}/status`)
  },

  /**
   * Update problem status
   */
  async updateProblemStatus(
    problemId: number, 
    request: UpdateProblemStatusRequest
  ): Promise<UserProblemStatus> {
    return await apiRequest<UserProblemStatus>(`/problems/${problemId}/status`, {
      method: 'PUT',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get problem mastery
   */
  async getProblemMastery(problemId: number): Promise<ProblemMastery> {
    return await apiRequest<ProblemMastery>(`/problems/${problemId}/mastery`)
  },
}