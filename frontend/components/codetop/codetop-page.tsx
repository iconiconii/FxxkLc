"use client"

import { useState, useEffect, useId, useCallback } from "react"
import { Search, Star, BookOpen, Filter, RotateCcw, X, CircleX, Tag, Signal, CircleDotDashed } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Checkbox } from "@/components/ui/checkbox"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import ProblemAssessmentModal from "@/components/modals/problem-assessment-modal"
import { filterApi, type Company, type Department, type Position } from "@/lib/filter-api"
import { problemsApi, type EnhancedSearchRequest } from "@/lib/problems-api"
import { codeTopApi, type ProblemRankingDTO, type OliverFilterRequest, type OliverFilterResponse } from "@/lib/codetop-api"
import { useAuth } from "@/lib/auth-context"

interface DisplayProblem extends Omit<ProblemRankingDTO, 'difficulty'> {
  difficulty: "easy" | "medium" | "hard"
  mastery: number
  status: "not_done" | "done" | "reviewed" | "attempted"
}

const difficultyColors = {
  easy: "bg-green-500 text-white",
  medium: "bg-orange-500 text-white",
  hard: "bg-red-500 text-white",
}

const difficultyLabels = {
  easy: "容易",
  medium: "中等",
  hard: "困难",
}

export default function OliverPage() {
  const id = useId()
  const { isAuthenticated, loading: authLoading } = useAuth()
  const [searchTerm, setSearchTerm] = useState("")
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedProblem, setSelectedProblem] = useState<DisplayProblem | null>(null)
  
  // Advanced filters state
  const [difficultyFilters, setDifficultyFilters] = useState<string[]>([])
  const [statusFilters, setStatusFilters] = useState<string[]>([])
  const [tagFilters, setTagFilters] = useState<string[]>([])
  
  // Progressive filter state
  const [companies, setCompanies] = useState<Company[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [positions, setPositions] = useState<Position[]>([])
  
  // Filter selection state
  const [selectedCompany, setSelectedCompany] = useState<number | null>(null)
  const [selectedDepartment, setSelectedDepartment] = useState<number | null>(null)
  const [selectedPosition, setSelectedPosition] = useState<number | null>(null)
  
  // Applied filters (for actual API calls)
  const [appliedFilters, setAppliedFilters] = useState<{
    companyId?: number
    departmentId?: number
    positionId?: number
  }>({})
  
  // Control states
  const [departmentEnabled, setDepartmentEnabled] = useState(false)
  const [positionEnabled, setPositionEnabled] = useState(false)
  const [filterLoading, setFilterLoading] = useState(false)
  
  // API state
  const [problems, setProblems] = useState<DisplayProblem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pagination, setPagination] = useState({
    current: 1,
    size: 20,
    total: 0,
    pages: 0,
  })

  // Transform API Problem to DisplayProblem
  const transformProblem = useCallback((apiProblem: any): DisplayProblem => {
    return {
      ...apiProblem,
      problemId: apiProblem.id || apiProblem.problemId,
      difficulty: (apiProblem.difficulty || 'MEDIUM').toLowerCase() as "easy" | "medium" | "hard",
      mastery: apiProblem.mastery || 0,
      status: apiProblem.status || "not_done",
      frequencyScore: apiProblem.frequency || apiProblem.frequencyScore || 0,
      lastAskedDate: apiProblem.lastAskedDate || apiProblem.updatedAt?.split('T')[0] || '2024-01-01',
      problemUrl: apiProblem.problemUrl || `https://leetcode.cn/problems/problem-${apiProblem.id || apiProblem.problemId}/`,
    } as DisplayProblem
  }, [])

  // Load companies on component mount
  const loadCompanies = async () => {
    try {
      const companiesData = await filterApi.getCompanies()
      setCompanies(companiesData)
    } catch (error) {
      console.error('Failed to load companies:', error)
      setError('加载公司列表失败')
    }
  }

  // Handle company selection
  const handleCompanyChange = async (companyId: string) => {
    const id = companyId === 'null' ? null : Number(companyId)
    setSelectedCompany(id)
    
    // Clear dependent selections
    setSelectedDepartment(null)
    setSelectedPosition(null)
    setDepartments([])
    setPositions([])
    setDepartmentEnabled(false)
    setPositionEnabled(false)
    
    if (id) {
      try {
        setFilterLoading(true)
        const departmentsData = await filterApi.getDepartmentsByCompany(id)
        setDepartments(departmentsData)
        setDepartmentEnabled(true)
      } catch (error) {
        console.error('Failed to load departments:', error)
        setError('加载部门列表失败')
      } finally {
        setFilterLoading(false)
      }
    }
  }

  // Handle department selection
  const handleDepartmentChange = async (departmentId: string) => {
    const id = departmentId === 'null' ? null : Number(departmentId)
    setSelectedDepartment(id)
    
    // Clear dependent selections
    setSelectedPosition(null)
    setPositions([])
    setPositionEnabled(false)
    
    if (id) {
      try {
        setFilterLoading(true)
        const positionsData = await filterApi.getPositionsByDepartment(id)
        setPositions(positionsData)
        setPositionEnabled(true)
      } catch (error) {
        console.error('Failed to load positions:', error)
        setError('加载岗位列表失败')
      } finally {
        setFilterLoading(false)
      }
    }
  }

  // Handle position selection
  const handlePositionChange = (positionId: string) => {
    const id = positionId === 'null' ? null : Number(positionId)
    setSelectedPosition(id)
  }

  // Enhanced search with difficulty, status, and tag filters
  const executeEnhancedSearch = useCallback(async (page: number = 1) => {
    try {
      setLoading(true)
      setError(null)
      
      // Check if we have any advanced filters to apply
      const hasAdvancedFilters = difficultyFilters.length > 0 || 
                                 statusFilters.length > 0 || 
                                 tagFilters.length > 0 || 
                                 (searchTerm && searchTerm.trim())
      
      if (hasAdvancedFilters) {
        // Use enhanced search API with advanced filters
        const request: EnhancedSearchRequest = {
          search: searchTerm || undefined,
          difficulties: difficultyFilters.length > 0 ? difficultyFilters : undefined,
          statuses: statusFilters.length > 0 ? statusFilters : undefined,
          tags: tagFilters.length > 0 ? tagFilters : undefined,
          userId: isAuthenticated ? 1 : undefined, // TODO: Get actual user ID
        }
        
        const response = await problemsApi.enhancedSearch(request, page, pagination.size)
        
        // Transform problems directly (enhanced search already includes user status)
        const displayProblems: DisplayProblem[] = response.records.map(transformProblem)
        
        setProblems(displayProblems)
        setPagination({
          current: response.current,
          size: response.size,
          total: response.total,
          pages: response.pages,
        })
      } else {
        // Fall back to basic company/department/position filtering using CodeTop API
        try {
          const hasFilters = appliedFilters.companyId || appliedFilters.departmentId || appliedFilters.positionId
          let response: any
          
          if (hasFilters) {
            // Use complex filter API for specific filtering
            const request = {
              companyId: appliedFilters.companyId as number,
              departmentId: appliedFilters.departmentId as number,
              positionId: appliedFilters.positionId as number,
              page: page,
              size: pagination.size,
            }
            response = await codeTopApi.getFilteredProblems(request)
          } else {
            // Use new unified API based on authentication status
            if (isAuthenticated) {
              response = await codeTopApi.getGlobalProblemsWithUserStatus(
                page,
                pagination.size,
                'frequency_score',
                'desc'
              )
            } else {
              response = await codeTopApi.getGlobalProblems(
                page,
                pagination.size,
                'frequency_score',
                'desc'
              )
            }
          }
          
          // Transform problems directly (no need for merging anymore)
          const displayProblems: DisplayProblem[] = response.problems.map(transformProblem)
          
          setProblems(displayProblems)
          setPagination({
            current: response.currentPage,
            size: response.pageSize,
            total: response.totalElements,
            pages: response.totalPages,
          })
          
        } catch (fallbackErr) {
          setError(fallbackErr instanceof Error ? fallbackErr.message : '加载题目失败')
          setProblems([])
        }
      }
      
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '筛选搜索失败')
      setProblems([])
    } finally {
      setLoading(false)
    }
  }, [difficultyFilters, statusFilters, tagFilters, searchTerm, isAuthenticated, pagination.size, 
      appliedFilters, transformProblem])

  // Apply advanced filters
  const handleApplyAdvancedFilters = async () => {
    await executeEnhancedSearch(1)
  }

  // Clear advanced filters
  const handleClearAdvancedFilters = async () => {
    setDifficultyFilters([])
    setStatusFilters([])
    setTagFilters([])
    setSearchTerm("")
    // After clearing advanced filters, show results based on current company/department/position filters
    // If no company/department/position filters are applied, show all problems
    await fetchProblems(1, appliedFilters)
  }

  // Apply filters and fetch problems
  const handleApplyFilters = async () => {
    const filters = {
      companyId: selectedCompany || undefined,
      departmentId: selectedDepartment || undefined,
      positionId: selectedPosition || undefined
    }
    
    setAppliedFilters(filters)
    await fetchProblems(1, filters)
  }

  // Reset all filters
  const handleResetFilters = async () => {
    setSelectedCompany(null)
    setSelectedDepartment(null)
    setSelectedPosition(null)
    setDepartments([])
    setPositions([])
    setDepartmentEnabled(false)
    setPositionEnabled(false)
    setAppliedFilters({})
    
    // Fetch all problems after resetting filters
    await fetchProblems(1, {})
  }

  // Fetch problems data using CodeTop API
  const fetchProblems = useCallback(async (page: number = 1, filters: Record<string, unknown> = {}) => {
    try {
      setLoading(true)
      setError(null)
      
      // Check if any filters are applied
      const hasFilters = filters.companyId || filters.departmentId || filters.positionId
      
      let response: OliverFilterResponse
      
      if (hasFilters) {
        // Use complex filter API for specific filtering
        const request: OliverFilterRequest = {
          companyId: filters.companyId as number,
          departmentId: filters.departmentId as number,
          positionId: filters.positionId as number,
          page: page,
          size: pagination.size,
        }
        response = await codeTopApi.getFilteredProblems(request)
      } else {
        // Use new unified API based on authentication status
        if (isAuthenticated) {
          // For authenticated users, use API with integrated user status
          response = await codeTopApi.getGlobalProblemsWithUserStatus(
            page,
            pagination.size,
            'frequency_score',
            'desc'
          )
        } else {
          // For anonymous users, use standard API
          response = await codeTopApi.getGlobalProblems(
            page,
            pagination.size,
            'frequency_score',
            'desc'
          )
        }
      }
      
      // Transform problems directly (no need for merging anymore)
      const displayProblems: DisplayProblem[] = response.problems.map(transformProblem)
      
      setProblems(displayProblems)
      setPagination({
        current: response.currentPage,
        size: response.pageSize,
        total: response.totalElements,
        pages: response.totalPages,
      })
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '加载题目失败')
      setProblems([])
    } finally {
      setLoading(false)
    }
  }, [pagination.size, transformProblem, isAuthenticated])

  // Load companies on component mount (only once)
  useEffect(() => {
    loadCompanies()
  }, []) // No dependencies - only run once on mount

  // Load initial problems on component mount and when auth state stabilizes
  useEffect(() => {
    if (!authLoading) {
      fetchProblems(1, {}) // Load all problems initially
    }
  }, [fetchProblems, authLoading]) // Only run when auth loading completes

  // Handle search with debounce
  useEffect(() => {
    // Only trigger search if there are advanced filters or search term
    // Company/department/position filters are handled separately through appliedFilters
    if (!searchTerm && difficultyFilters.length === 0 && statusFilters.length === 0 && tagFilters.length === 0) {
      return // Don't search when no advanced filters are applied
    }
    
    const timeoutId = setTimeout(() => {
      executeEnhancedSearch(1)
    }, 500)

    return () => clearTimeout(timeoutId)
  }, [searchTerm, difficultyFilters, statusFilters, tagFilters, executeEnhancedSearch])

  // Helper functions for filter display
  const getSelectedCompanyName = () => {
    const company = companies.find(c => c.id === selectedCompany)
    return company?.displayName || company?.name
  }

  const getSelectedDepartmentName = () => {
    const department = departments.find(d => d.id === selectedDepartment)
    return department?.displayName || department?.name
  }

  const getSelectedPositionName = () => {
    const position = positions.find(p => p.id === selectedPosition)
    return position?.displayName || position?.name
  }

  const getFilterCount = () => {
    let count = 0
    if (selectedCompany) count++
    if (selectedDepartment) count++
    if (selectedPosition) count++
    return count
  }

  const getFilterButtonText = () => {
    const count = getFilterCount()
    if (count === 0) return "筛选"
    
    const parts = []
    if (selectedCompany) parts.push(getSelectedCompanyName())
    if (selectedDepartment) parts.push(getSelectedDepartmentName())
    if (selectedPosition) parts.push(getSelectedPositionName())
    
    return `筛选 (${parts.join(' > ')})`
  }

  const filteredProblems = problems // Already transformed in fetchProblems

  const completedCount = filteredProblems.filter((p) => p.status === "done").length
  const easyCompletedCount = filteredProblems.filter((p) => p.difficulty === "easy" && p.status === "done").length
  const mediumCompletedCount = filteredProblems.filter((p) => p.difficulty === "medium" && p.status === "done").length
  const hardCompletedCount = filteredProblems.filter((p) => p.difficulty === "hard" && p.status === "done").length
  const totalCount = filteredProblems.length

  const handleNotDoneClick = (problem: DisplayProblem) => {
    // 只允许"未做"状态的题目点击
    if (problem.status === "not_done") {
      setSelectedProblem(problem)
      setModalOpen(true)
    }
  }

  // 本地更新题目状态，避免重新请求API
  const updateLocalProblemStatus = (problemId: number, newStatus: string, newMastery: number) => {
    setProblems(prevProblems => 
      prevProblems.map(problem => 
        problem.problemId === problemId 
          ? { ...problem, status: newStatus as "not_done" | "done" | "reviewed" | "attempted", mastery: newMastery }
          : problem
      )
    )
  }

  const handleModalClose = () => {
    setModalOpen(false)
    setSelectedProblem(null)
  }

  const handlePageChange = (page: number) => {
    // 根据当前状态选择合适的数据获取方法
    const hasAdvancedFilters = difficultyFilters.length > 0 || 
                               statusFilters.length > 0 || 
                               tagFilters.length > 0 || 
                               (searchTerm && searchTerm.trim())
    
    if (hasAdvancedFilters) {
      executeEnhancedSearch(page)
    } else {
      fetchProblems(page, appliedFilters)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">OLIVER</h1>
          <span className="text-2xl">🔥</span>
        </div>
        <div className="flex items-center gap-4">
          {/* 登录状态指示器 */}
          {!authLoading && (
            <div className="flex items-center gap-2">
              {isAuthenticated ? (
                <Badge className="bg-green-500 hover:bg-green-600">
                  ✓ 已登录
                </Badge>
              ) : (
                <Badge variant="outline" className="text-gray-500">
                  未登录
                </Badge>
              )}
            </div>
          )}
          {pagination.total > 0 && (
            <div className="text-sm text-gray-500 dark:text-gray-400">
              共 {pagination.total} 道题目
            </div>
          )}
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
          <button 
            onClick={() => fetchProblems()} 
            className="ml-2 underline hover:no-underline"
          >
            重试
          </button>
        </div>
      )}

      {/* Modern Filter Section */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-6">
        {/* Primary Filters */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Company Selection */}
          <div className="min-w-[180px] flex-1 max-w-[240px]">
            <Select 
              value={selectedCompany?.toString() || 'null'} 
              onValueChange={handleCompanyChange}
            >
              <SelectTrigger>
                <SelectValue placeholder="选择公司" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="null">不限公司</SelectItem>
                {companies.map((company) => (
                  <SelectItem key={company.id} value={company.id.toString()}>
                    {company.displayName || company.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Department Selection */}
          <div className="min-w-[160px] flex-1 max-w-[200px]">
            <Select 
              value={selectedDepartment?.toString() || 'null'} 
              onValueChange={handleDepartmentChange}
              disabled={!departmentEnabled}
            >
              <SelectTrigger>
                <SelectValue placeholder={departmentEnabled ? "选择部门" : "请先选择公司"} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="null">不限部门</SelectItem>
                {departments.map((department) => (
                  <SelectItem key={department.id} value={department.id.toString()}>
                    {department.displayName || department.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Position Selection */}
          <div className="min-w-[160px] flex-1 max-w-[200px]">
            <Select 
              value={selectedPosition?.toString() || 'null'} 
              onValueChange={handlePositionChange}
              disabled={!positionEnabled}
            >
              <SelectTrigger>
                <SelectValue placeholder={positionEnabled ? "选择岗位" : "请先选择部门"} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="null">不限岗位</SelectItem>
                {positions.map((position) => (
                  <SelectItem key={position.id} value={position.id.toString()}>
                    {position.displayName || position.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Filter Actions */}
          <div className="flex gap-2 ml-auto">
            <Button 
              onClick={handleApplyFilters}
              disabled={filterLoading || !selectedCompany}
              className="px-6"
            >
              {filterLoading ? (
                <div className="flex items-center gap-2">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                  加载中
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Filter className="h-4 w-4" />
                  {getFilterButtonText()}
                </div>
              )}
            </Button>
            <Button 
              variant="outline" 
              onClick={handleResetFilters}
              disabled={getFilterCount() === 0}
              className="px-3"
              title="清除所有筛选条件"
            >
              <RotateCcw className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Advanced Filters Row */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Difficulty Filter */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" className="h-9">
                <CircleDotDashed className="w-4 h-4 mr-2" />
                难度
                {difficultyFilters.length > 0 && (
                  <Badge variant="secondary" className="ml-2 h-5 px-1.5 text-xs">
                    {difficultyFilters.length}
                  </Badge>
                )}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-48 p-3" align="start">
              <div className="space-y-3">
                <div className="text-xs font-medium text-muted-foreground">选择难度</div>
                <div className="space-y-2">
                  {[{key: 'easy', label: '容易', color: 'text-green-600'}, {key: 'medium', label: '中等', color: 'text-orange-600'}, {key: 'hard', label: '困难', color: 'text-red-600'}].map((difficulty) => (
                    <div key={difficulty.key} className="flex items-center gap-2">
                      <Checkbox 
                        id={`${id}-diff-${difficulty.key}`}
                        checked={difficultyFilters.includes(difficulty.key)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            setDifficultyFilters([...difficultyFilters, difficulty.key])
                          } else {
                            setDifficultyFilters(difficultyFilters.filter(f => f !== difficulty.key))
                          }
                        }}
                      />
                      <Label htmlFor={`${id}-diff-${difficulty.key}`} className={`font-normal ${difficulty.color}`}>
                        {difficulty.label}
                      </Label>
                    </div>
                  ))}
                </div>
                {difficultyFilters.length > 0 && (
                  <>
                    <div className="border-t border-border my-3" />
                    <Button size="sm" variant="outline" className="w-full h-8" onClick={() => setDifficultyFilters([])}>
                      清除
                    </Button>
                  </>
                )}
              </div>
            </PopoverContent>
          </Popover>

          {/* Status Filter */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" className="h-9">
                <Signal className="w-4 h-4 mr-2" />
                状态
                {statusFilters.length > 0 && (
                  <Badge variant="secondary" className="ml-2 h-5 px-1.5 text-xs">
                    {statusFilters.length}
                  </Badge>
                )}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-48 p-3" align="start">
              <div className="space-y-3">
                <div className="text-xs font-medium text-muted-foreground">选择状态</div>
                <div className="space-y-2">
                  {[{key: 'not_done', label: '未做', color: 'text-gray-600'}, {key: 'done', label: '已完成', color: 'text-green-600'}, {key: 'reviewed', label: '已复习', color: 'text-blue-600'}, {key: 'attempted', label: '尝试过', color: 'text-orange-600'}].map((status) => (
                    <div key={status.key} className="flex items-center gap-2">
                      <Checkbox 
                        id={`${id}-status-${status.key}`}
                        checked={statusFilters.includes(status.key)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            setStatusFilters([...statusFilters, status.key])
                          } else {
                            setStatusFilters(statusFilters.filter(f => f !== status.key))
                          }
                        }}
                      />
                      <Label htmlFor={`${id}-status-${status.key}`} className={`font-normal ${status.color}`}>
                        {status.label}
                      </Label>
                    </div>
                  ))}
                </div>
                {statusFilters.length > 0 && (
                  <>
                    <div className="border-t border-border my-3" />
                    <Button size="sm" variant="outline" className="w-full h-8" onClick={() => setStatusFilters([])}>
                      清除
                    </Button>
                  </>
                )}
              </div>
            </PopoverContent>
          </Popover>

          {/* Tags Filter */}
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" className="h-9">
                <Tag className="w-4 h-4 mr-2" />
                标签
                {tagFilters.length > 0 && (
                  <Badge variant="secondary" className="ml-2 h-5 px-1.5 text-xs">
                    {tagFilters.length}
                  </Badge>
                )}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-56 p-3" align="start">
              <div className="space-y-3">
                <div className="text-xs font-medium text-muted-foreground">选择标签</div>
                <div className="space-y-2">
                  {['数组', '链表', '栈', '队列', '哈希表', '字符串', '二叉树', '动态规划', '贪心', '回溯'].map((tag) => (
                    <div key={tag} className="flex items-center gap-2">
                      <Checkbox 
                        id={`${id}-tag-${tag}`}
                        checked={tagFilters.includes(tag)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            setTagFilters([...tagFilters, tag])
                          } else {
                            setTagFilters(tagFilters.filter(f => f !== tag))
                          }
                        }}
                      />
                      <Label htmlFor={`${id}-tag-${tag}`} className="font-normal text-sm">
                        {tag}
                      </Label>
                    </div>
                  ))}
                </div>
                {tagFilters.length > 0 && (
                  <>
                    <div className="border-t border-border my-3" />
                    <Button size="sm" variant="outline" className="w-full h-8" onClick={() => setTagFilters([])}>
                      清除所有标签
                    </Button>
                  </>
                )}
              </div>
            </PopoverContent>
          </Popover>

          {/* Clear All Advanced Filters */}
          {(difficultyFilters.length > 0 || statusFilters.length > 0 || tagFilters.length > 0) && (
            <>
              <Button 
                variant="default" 
                size="sm" 
                className="bg-blue-600 hover:bg-blue-700 text-white h-9"
                onClick={handleApplyAdvancedFilters}
              >
                应用筛选
              </Button>
              <Button 
                variant="ghost" 
                size="sm" 
                className="text-muted-foreground h-9"
                onClick={handleClearAdvancedFilters}
              >
                <X className="w-4 h-4 mr-1" />
                清除筛选
              </Button>
            </>
          )}
        </div>

        {/* Active Filters Display */}
        {(Object.keys(appliedFilters).length > 0 || difficultyFilters.length > 0 || statusFilters.length > 0 || tagFilters.length > 0) && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm text-muted-foreground">已应用：</span>
            {/* Applied Company/Department/Position filters */}
            {appliedFilters.companyId && (
              <Badge variant="outline" className="gap-1">
                {companies.find(c => c.id === appliedFilters.companyId)?.displayName || 
                 companies.find(c => c.id === appliedFilters.companyId)?.name}
                <X className="w-3 h-3 cursor-pointer" onClick={async () => {
                  setAppliedFilters(prev => {
                    const newFilters = { ...prev }
                    delete newFilters.companyId
                    return newFilters
                  })
                  // Also clear the selected company
                  setSelectedCompany(null)
                  setSelectedDepartment(null)
                  setSelectedPosition(null)
                  setDepartments([])
                  setPositions([])
                  setDepartmentEnabled(false)
                  setPositionEnabled(false)
                  // Fetch with updated filters
                  const newFilters = { ...appliedFilters }
                  delete newFilters.companyId
                  await fetchProblems(1, newFilters)
                }} />
              </Badge>
            )}
            {appliedFilters.departmentId && (
              <Badge variant="outline" className="gap-1">
                {departments.find(d => d.id === appliedFilters.departmentId)?.displayName || 
                 departments.find(d => d.id === appliedFilters.departmentId)?.name}
                <X className="w-3 h-3 cursor-pointer" onClick={async () => {
                  setAppliedFilters(prev => {
                    const newFilters = { ...prev }
                    delete newFilters.departmentId
                    return newFilters
                  })
                  // Also clear the selected department
                  setSelectedDepartment(null)
                  setSelectedPosition(null)
                  setPositions([])
                  setPositionEnabled(false)
                  // Fetch with updated filters
                  const newFilters = { ...appliedFilters }
                  delete newFilters.departmentId
                  await fetchProblems(1, newFilters)
                }} />
              </Badge>
            )}
            {appliedFilters.positionId && (
              <Badge variant="outline" className="gap-1">
                {positions.find(p => p.id === appliedFilters.positionId)?.displayName || 
                 positions.find(p => p.id === appliedFilters.positionId)?.name}
                <X className="w-3 h-3 cursor-pointer" onClick={async () => {
                  setAppliedFilters(prev => {
                    const newFilters = { ...prev }
                    delete newFilters.positionId
                    return newFilters
                  })
                  // Also clear the selected position
                  setSelectedPosition(null)
                  // Fetch with updated filters
                  const newFilters = { ...appliedFilters }
                  delete newFilters.positionId
                  await fetchProblems(1, newFilters)
                }} />
              </Badge>
            )}
            {/* Difficulty filters */}
            {difficultyFilters.map(diff => (
              <Badge key={diff} variant="outline" className="gap-1">
                {diff === 'easy' ? '容易' : diff === 'medium' ? '中等' : '困难'}
                <X className="w-3 h-3 cursor-pointer" onClick={() => setDifficultyFilters(difficultyFilters.filter(f => f !== diff))} />
              </Badge>
            ))}
            {/* Status filters */}
            {statusFilters.map(status => (
              <Badge key={status} variant="outline" className="gap-1">
                {status === 'not_done' ? '未做' : status === 'done' ? '已完成' : status === 'reviewed' ? '已复习' : '尝试过'}
                <X className="w-3 h-3 cursor-pointer" onClick={() => setStatusFilters(statusFilters.filter(f => f !== status))} />
              </Badge>
            ))}
            {/* Tag filters */}
            {tagFilters.map(tag => (
              <Badge key={tag} variant="outline" className="gap-1">
                {tag}
                <X className="w-3 h-3 cursor-pointer" onClick={() => setTagFilters(tagFilters.filter(f => f !== tag))} />
              </Badge>
            ))}
          </div>
        )}
        {/* Search and Progress Section */}
        <div className="space-y-4">
          {/* Search Bar */}
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
            <Input
              placeholder="搜索题目名称或编号..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 h-10"
            />
            {searchTerm && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1 h-8 w-8"
                onClick={() => setSearchTerm("")}
              >
                <CircleX className="h-4 w-4" />
              </Button>
            )}
          </div>

          {/* Progress Statistics */}
          <div className="flex flex-wrap items-center gap-3">
            {isAuthenticated ? (
              <div className="flex items-center gap-2">
                <Badge className="bg-blue-500 hover:bg-blue-600 px-3 py-1">
                  总计 {completedCount}/{totalCount}
                </Badge>
                <Badge className="bg-green-500 hover:bg-green-600 px-3 py-1">
                  简单 {easyCompletedCount}
                </Badge>
                <Badge className="bg-orange-500 hover:bg-orange-600 px-3 py-1">
                  中等 {mediumCompletedCount}
                </Badge>
                <Badge className="bg-red-500 hover:bg-red-600 px-3 py-1">
                  困难 {hardCompletedCount}
                </Badge>
              </div>
            ) : (
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <span>💡 登录后查看个人进度统计</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modern Problems Table */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden shadow-sm">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="text-gray-500 dark:text-gray-400">加载中...</div>
          </div>
        ) : filteredProblems.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <div className="text-gray-500 dark:text-gray-400">
              {searchTerm ? '未找到相关题目' : '暂无题目数据'}
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
            <thead className="bg-gray-50/50 dark:bg-gray-700/50 backdrop-blur-sm">
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                  题目
                </th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                  难度
                </th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                  最近考察
                </th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                  频度
                </th>
                {isAuthenticated && (
                  <>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                      掌握程度
                    </th>
                    <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                      操作
                      <div className="text-xs font-normal text-gray-500 dark:text-gray-400 mt-1">
                        复习请到复习页面
                      </div>
                    </th>
                  </>
                )}
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800">
              {filteredProblems.map((problem) => (
                <tr key={problem.problemId} className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50/50 dark:hover:bg-gray-700/50 transition-colors">
                  <td className="px-6 py-5">
                    <div className="flex items-center gap-3">
                      <div className="font-medium text-gray-900 dark:text-gray-100">
                        <a 
                          href={problem.problemUrl || `https://leetcode.cn/problems/problem-${problem.problemId}/`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 transition-colors"
                        >
                          {problem.problemId}. {problem.title}
                        </a>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-5">
                    <Badge
                      className={`${difficultyColors[problem.difficulty]} border-0 font-medium`}
                    >
                      {difficultyLabels[problem.difficulty]}
                    </Badge>
                  </td>
                  <td className="px-6 py-5 text-sm text-gray-600 dark:text-gray-400">
                    {problem.lastAskedDate}
                  </td>
                  <td className="px-6 py-5">
                    <Badge variant="outline" className="font-normal">
                      {problem.frequencyScore}
                    </Badge>
                  </td>
                  {isAuthenticated && (
                    <>
                      <td className="px-6 py-5">
                        <div className="flex items-center gap-0.5">
                          {[1, 2, 3].map((star) => (
                            <Star
                              key={star}
                              className={`h-4 w-4 ${
                                star <= problem.mastery
                                  ? "text-yellow-500 fill-yellow-500"
                                  : "text-gray-300 dark:text-gray-600"
                              }`}
                            />
                          ))}
                        </div>
                      </td>
                      <td className="px-6 py-5">
                        <div className="flex items-center gap-2">
                          {problem.status === "not_done" ? (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 hover:border-blue-400 h-8"
                              onClick={() => handleNotDoneClick(problem)}
                              title="点击记录做题状态"
                            >
                              练习
                            </Button>
                          ) : problem.status === "done" ? (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled
                              className="text-green-600 dark:text-green-400 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 h-8 opacity-75 cursor-not-allowed"
                              title="已完成 - 复习请到复习页面"
                            >
                              已完成
                            </Button>
                          ) : problem.status === "attempted" ? (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled
                              className="text-orange-600 dark:text-orange-400 border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-900/20 h-8 opacity-75 cursor-not-allowed"
                              title="尝试过 - 复习请到复习页面"
                            >
                              尝试过
                            </Button>
                          ) : (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled
                              className="text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-900/20 h-8 opacity-75 cursor-not-allowed"
                              title="已复习 - 复习请到复习页面"
                            >
                              已复习
                            </Button>
                          )}
                          <Button variant="ghost" size="sm" className="text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 h-8">
                            <BookOpen className="h-4 w-4 mr-1" />
                            笔记
                          </Button>
                        </div>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        )}
      </div>

      {/* Pagination */}
      {pagination.pages > 1 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center justify-between">
            <div className="text-sm text-gray-500 dark:text-gray-400">
              第 {pagination.current} 页，共 {pagination.pages} 页（总计 {pagination.total} 道题目）
            </div>
            <div className="flex gap-2 items-center">
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.current <= 1}
                onClick={() => handlePageChange(1)}
                className="px-3 py-1 text-xs border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700 transition-colors"
              >
                首页
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.current <= 1}
                onClick={() => handlePageChange(pagination.current - 1)}
                className="px-3 py-1 text-xs border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700 transition-colors"
              >
                上一页
              </Button>
              
              {/* 页码显示 */}
              <div className="flex gap-1">
                {Array.from({ length: Math.min(5, pagination.pages) }, (_, i) => {
                  let pageNum: number;
                  if (pagination.pages <= 5) {
                    pageNum = i + 1;
                  } else if (pagination.current <= 3) {
                    pageNum = i + 1;
                  } else if (pagination.current >= pagination.pages - 2) {
                    pageNum = pagination.pages - 4 + i;
                  } else {
                    pageNum = pagination.current - 2 + i;
                  }
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === pagination.current ? "default" : "outline"}
                      size="sm"
                      onClick={() => handlePageChange(pageNum)}
                      className={`w-8 h-8 p-0 text-xs ${
                        pageNum === pagination.current 
                          ? "bg-blue-500 text-white hover:bg-blue-600" 
                          : "border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700"
                      } transition-colors`}
                    >
                      {pageNum}
                    </Button>
                  );
                })}
              </div>
              
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.current >= pagination.pages}
                onClick={() => handlePageChange(pagination.current + 1)}
                className="px-3 py-1 text-xs border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700 transition-colors"
              >
                下一页
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.current >= pagination.pages}
                onClick={() => handlePageChange(pagination.pages)}
                className="px-3 py-1 text-xs border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700 transition-colors"
              >
                末页
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Assessment Modal */}
      {selectedProblem && (
        <ProblemAssessmentModal
          isOpen={modalOpen}
          onClose={handleModalClose}
          problemId={selectedProblem.problemId}
          problemTitle={selectedProblem.title}
          onStatusUpdate={(problemId, newStatus, newMastery) => {
            updateLocalProblemStatus(problemId, newStatus, newMastery)
          }}
        />
      )}
    </div>
  )
}
