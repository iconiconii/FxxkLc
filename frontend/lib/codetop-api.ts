/**
 * CodeTop filtering API client functions
 */

import { apiRequest } from './api'

export interface ProblemRankingDTO {
  problemId: number
  title: string
  difficulty: string
  problemUrl: string
  leetcodeId: string
  
  // Frequency statistics
  frequencyScore: number
  interviewCount: number
  frequencyRank: number
  percentile: number
  lastAskedDate: string
  trend: string
  
  // Analysis scores
  recencyScore: number
  isHotProblem: boolean
  isTrending: boolean
  isTopPercentile: boolean
  
  // Category information
  primaryCategory: string
  allCategories: string[]
  relevanceScore: number
  isPrimary: boolean
  
  // Similar problems data
  sharedCategories: number
  sharedCategoryNames: string
  
  // Company-specific data
  companyName: string
  departmentName: string
  positionName: string
  statsScope: string
  
  // Quality indicators
  credibilityScore: number
  verificationLevel: string
  communityScore: number
  
  // Additional metadata
  addedDate: string
  isPremium: boolean
  tags: string
}

export interface CodeTopFilterRequest {
  companyId?: number
  departmentId?: number
  positionId?: number
  categoryIds?: number[]
  difficultyLevels?: string[]
  frequencyRange?: {
    min: number
    max: number
  }
  dateRange?: {
    startDate: string
    endDate: string
  }
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
  page?: number
  size?: number
}

export interface CodeTopFilterResponse {
  problems: ProblemRankingDTO[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
  // Note: filterOptions removed - use getFilterOptions() API separately as needed
  summary: {
    totalProblems: number
    hotProblems: number
    trendingProblems: number
    avgFrequencyScore: number
    mostCommonDifficulty: string
    mostActiveCompany: string
  }
}

export interface FilterOptions {
  availableCompanies: Array<{
    id: number
    name: string
    problemCount: number
  }>
  availableDepartments: Array<{
    id: number
    name: string
    problemCount: number
  }>
  availablePositions: Array<{
    id: number
    name: string
    problemCount: number
  }>
  availableCategories: Array<{
    id: number
    name: string
    problemCount: number
  }>
}

export interface CompanyProblemBreakdownDTO {
  departmentName: string
  positionName: string
  problemCount: number
  averageFrequency: number
  topCategories: string[]
}

export interface CategoryUsageStatsDTO {
  categoryId: number
  categoryName: string
  problemCount: number
  averageFrequency: number
  topCompanies: string[]
}

export const codeTopApi = {
  /**
   * Get filtered problems with CodeTop-style ranking
   */
  async getFilteredProblems(request: CodeTopFilterRequest): Promise<CodeTopFilterResponse> {
    return await apiRequest<CodeTopFilterResponse>('/codetop/filter', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get trending problems
   */
  async getTrendingProblems(
    limit: number = 20,
    days: number = 30
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
      days: days.toString(),
    })

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/trending?${params}`)
  },

  /**
   * Get hot problems
   */
  async getHotProblems(
    companyId?: number,
    limit: number = 15
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })
    
    if (companyId) {
      params.append('companyId', companyId.toString())
    }

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/hot?${params}`)
  },

  /**
   * Get top problems by frequency
   */
  async getTopProblemsByFrequency(
    scope: string,
    companyId?: number,
    departmentId?: number,
    positionId?: number,
    limit: number = 20
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      scope,
      limit: limit.toString(),
    })
    
    if (companyId) params.append('companyId', companyId.toString())
    if (departmentId) params.append('departmentId', departmentId.toString())
    if (positionId) params.append('positionId', positionId.toString())

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/top-frequency?${params}`)
  },

  /**
   * Get company problem breakdown
   */
  async getCompanyProblemBreakdown(companyId: number): Promise<CompanyProblemBreakdownDTO[]> {
    return await apiRequest<CompanyProblemBreakdownDTO[]>(`/codetop/company/${companyId}/breakdown`)
  },

  /**
   * Get problems by category
   */
  async getProblemsByCategory(
    categoryId: number,
    page: number = 1,
    size: number = 20
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/category/${categoryId}/problems?${params}`)
  },

  /**
   * Get similar problems
   */
  async getSimilarProblems(
    problemId: number,
    limit: number = 10
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/problems/${problemId}/similar?${params}`)
  },

  /**
   * Get global problem rankings (simple list)
   */
  async getGlobalRankings(
    page: number = 1,
    size: number = 50
  ): Promise<ProblemRankingDTO[]> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    return await apiRequest<ProblemRankingDTO[]>(`/codetop/rankings/global?${params}`)
  },

  /**
   * Get global problems with full pagination info (recommended for simple queries)
   */
  async getGlobalProblems(
    page: number = 1,
    size: number = 20,
    sortBy: string = 'frequency_score',
    sortOrder: string = 'desc'
  ): Promise<CodeTopFilterResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortOrder,
    })

    return await apiRequest<CodeTopFilterResponse>(`/codetop/problems/global?${params}`)
  },

  /**
   * Get category usage statistics
   */
  async getCategoryUsageStatistics(): Promise<CategoryUsageStatsDTO[]> {
    return await apiRequest<CategoryUsageStatsDTO[]>('/codetop/categories/stats')
  },

  /**
   * Get filter options
   */
  async getFilterOptions(
    companyId?: number,
    departmentId?: number
  ): Promise<FilterOptions> {
    const params = new URLSearchParams()
    
    if (companyId) params.append('companyId', companyId.toString())
    if (departmentId) params.append('departmentId', departmentId.toString())

    return await apiRequest<FilterOptions>(`/codetop/filter-options?${params}`)
  },

  /**
   * Health check for CodeTop filtering system
   */
  async healthCheck(): Promise<string> {
    return await apiRequest<string>('/codetop/health')
  },
}