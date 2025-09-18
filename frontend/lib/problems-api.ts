/**
 * Problems API client functions
 */

import { apiRequest } from './api'

export interface Problem {
  id: number
  title: string
  description?: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  tags?: string[]
  isPremium: boolean
  solutionCount: number
  acceptanceRate?: number
  frequency?: number
  problemUrl?: string
  leetcodeId?: string
  createdAt: string
  updatedAt: string
}

export interface PaginatedResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface HotProblem {
  problemId: number
  title: string
  difficulty: string
  companyCount: number
  companies: string[]
}

export interface ProblemStatistics {
  totalProblems: number
  easyCount: number
  mediumCount: number
  hardCount: number
  premiumCount: number
  avgSolutionCount: number
}

export interface TagUsage {
  tag: string
  count: number
}

export interface UserProblemStatus {
  problemId: number
  title: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  status: 'not_done' | 'done' | 'reviewed'
  mastery: number
  lastAttemptDate?: string
  lastConsideredDate?: string
  attemptCount: number
  masteryScore?: number
  notes?: string
}

export interface AdvancedSearchRequest {
  keyword?: string
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
  tag?: string
  isPremium?: boolean
  
  // Enhanced fields for comprehensive filtering
  difficulties?: string[]
  statuses?: string[]
  tags?: string[]
  userId?: number
}

export interface EnhancedSearchRequest {
  search?: string
  difficulties?: string[]
  statuses?: string[]
  tags?: string[]
  userId?: number
  sort?: string
}

export const problemsApi = {
  /**
   * Get problem by ID
   */
  async getProblem(id: number): Promise<Problem> {
    return await apiRequest<Problem>(`/problems/${id}`)
  },

  /**
   * Search problems with pagination
   */
  async searchProblems(
    keyword?: string,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })
    
    if (keyword) {
      params.append('keyword', keyword)
    }

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/search?${params}`)
  },

  /**
   * Enhanced search with comprehensive filters
   */
  async enhancedSearch(
    request: EnhancedSearchRequest,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    // Add search parameter
    if (request.search) {
      params.append('search', request.search)
    }

    // Add array parameters
    if (request.difficulties) {
      request.difficulties.forEach(diff => params.append('difficulties', diff))
    }
    if (request.statuses) {
      request.statuses.forEach(status => params.append('statuses', status))
    }
    if (request.tags) {
      request.tags.forEach(tag => params.append('tags', tag))
    }
    if (request.userId) {
      params.append('userId', request.userId.toString())
    }
    if (request.sort) {
      params.append('sort', request.sort)
    }

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/search/enhanced?${params}`)
  },

  /**
   * Advanced search with filters
   */
  async advancedSearch(
    request: AdvancedSearchRequest,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/search/advanced?${params}`, {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get problems by difficulty
   */
  async getProblemsByDifficulty(
    difficulty: 'EASY' | 'MEDIUM' | 'HARD',
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/difficulty/${difficulty}?${params}`)
  },

  /**
   * Get problems by tag
   */
  async getProblemsByTag(
    tag: string,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/tag/${encodeURIComponent(tag)}?${params}`)
  },

  /**
   * Get problems by company ID
   */
  async getProblemsByCompany(
    companyId: number,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/company/${companyId}?${params}`)
  },

  /**
   * Get problems by company name
   */
  async getProblemsByCompanyName(
    companyName: string,
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<PaginatedResponse<Problem>>(`/problems/company-name/${encodeURIComponent(companyName)}?${params}`)
  },

  /**
   * Get hot problems
   */
  async getHotProblems(
    minCompanies: number = 3,
    limit: number = 50
  ): Promise<HotProblem[]> {
    const params = new URLSearchParams({
      minCompanies: minCompanies.toString(),
      limit: limit.toString(),
    })

    return await apiRequest<HotProblem[]>(`/problems/hot?${params}`)
  },

  /**
   * Get recent problems
   */
  async getRecentProblems(limit: number = 20): Promise<Problem[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<Problem[]>(`/problems/recent?${params}`)
  },

  /**
   * Get trending problems (Note: Use codeTopApi.getTrendingProblems() for OLIVER-style trending)
   */
  async getTrendingProblems(
    days: number = 7,
    limit: number = 20
  ): Promise<Problem[]> {
    // This endpoint doesn't exist in backend - use recent problems instead
    return await this.getRecentProblems(limit)
  },

  /**
   * Get most popular problems (Note: Use codeTopApi.getHotProblems() for frequency-based popular)
   */
  async getMostPopular(limit: number = 20): Promise<Problem[]> {
    // This endpoint doesn't exist in backend - use recent problems instead
    return await this.getRecentProblems(limit)
  },

  /**
   * Get problem statistics
   */
  async getStatistics(): Promise<ProblemStatistics> {
    return await apiRequest<ProblemStatistics>('/problems/statistics')
  },

  /**
   * Get tag usage statistics
   */
  async getTagStatistics(): Promise<TagUsage[]> {
    return await apiRequest<TagUsage[]>('/problems/tags/statistics')
  },

  /**
   * Get user's problem progress
   */
  async getUserProgress(): Promise<UserProblemStatus[]> {
    return await apiRequest<UserProblemStatus[]>('/problems/user-progress')
  },

  /**
   * Filter problems by company, department, and position IDs
   */
  async filterProblems(
    filters: {
      companyId?: number
      departmentId?: number
      positionId?: number
    },
    page: number = 1,
    size: number = 20
  ): Promise<PaginatedResponse<Problem>> {
    const body = {
      ...filters,
      page,
      size
    }

    return await apiRequest<PaginatedResponse<Problem>>('/codetop/filter', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
}
