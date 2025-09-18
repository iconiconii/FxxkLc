"use client"

import { useState, useEffect, useCallback } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Button } from "@/components/ui/button"
import { CheckCircle, Clock, Trophy, Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface CompletedProblem {
  problemId: number
  title: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  status: string
  mastery: number
  lastAttemptDate: string | null
  attemptCount: number
  accuracy: number
  masteryScore?: number
}

interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

const difficultyColors = {
  EASY: "bg-green-100 text-green-800 dark:bg-green-800 dark:text-green-100",
  MEDIUM: "bg-yellow-100 text-yellow-800 dark:bg-yellow-800 dark:text-yellow-100", 
  HARD: "bg-red-100 text-red-800 dark:bg-red-800 dark:text-red-100"
}

const difficultyLabels = {
  EASY: "简单",
  MEDIUM: "中等",
  HARD: "困难"
}

export default function CompletedProblems() {
  const [problems, setProblems] = useState<CompletedProblem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  
  // Filters
  const [sortBy, setSortBy] = useState("date")
  const [searchTerm, setSearchTerm] = useState("")

  const fetchCompletedProblems = useCallback(async (pageNum: number, isLoadMore = false) => {
    try {
      if (!isLoadMore) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }

      // Mock data for testing when backend is not available
      const mockData: PagedResponse<CompletedProblem> = {
        content: [
          {
            problemId: 1,
            title: "两数之和",
            difficulty: 'EASY',
            status: "done",
            mastery: 2,
            lastAttemptDate: "2025-09-01T10:30:00",
            attemptCount: 3,
            accuracy: 0.85
          },
          {
            problemId: 2,
            title: "三数之和",
            difficulty: 'MEDIUM',
            status: "reviewed",
            mastery: 3,
            lastAttemptDate: "2025-08-30T15:20:00",
            attemptCount: 5,
            accuracy: 0.92
          },
          {
            problemId: 3,
            title: "合并两个有序链表",
            difficulty: 'EASY',
            status: "done",
            mastery: 1,
            lastAttemptDate: "2025-08-28T09:45:00",
            attemptCount: 2,
            accuracy: 0.75
          },
          {
            problemId: 4,
            title: "最大子数组和",
            difficulty: 'HARD',
            status: "reviewed",
            mastery: 2,
            lastAttemptDate: "2025-08-25T14:15:00",
            attemptCount: 7,
            accuracy: 0.88
          },
          {
            problemId: 5,
            title: "爬楼梯",
            difficulty: 'EASY',
            status: "done",
            mastery: 3,
            lastAttemptDate: "2025-08-22T11:30:00",
            attemptCount: 4,
            accuracy: 0.95
          }
        ],
        page: pageNum,
        size: 10,
        totalElements: 12,
        totalPages: 2,
        hasNext: pageNum < 1
      }

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 500))

      try {
        // Try to fetch from real API first
        const params = new URLSearchParams({
          page: pageNum.toString(),
          size: "10",
          sortBy: sortBy
        })

        const response = await fetch(`/api/v1/problems/user/completed?${params}`, {
          credentials: 'include'
        })

        if (response.ok) {
          const data: PagedResponse<CompletedProblem> = await response.json()
          
          if (isLoadMore) {
            setProblems(prev => [...prev, ...data.content])
          } else {
            setProblems(data.content)
          }
          
          setHasNext(data.hasNext)
          setPage(pageNum)
          setError(null)
          return
        }
      } catch {
        console.log('API not available, using mock data')
      }

      // Use mock data if API fails
      if (isLoadMore) {
        // For demo: add more mock data on load more
        const moreData = mockData.content.map(item => ({
          ...item,
          problemId: item.problemId + 10
        }))
        setProblems(prev => [...prev, ...moreData])
      } else {
        setProblems(mockData.content)
      }
      
      setHasNext(mockData.hasNext)
      setPage(pageNum)
      setError(null)
      
    } catch (err) {
      console.error('Failed to fetch completed problems:', err)
      setError('获取完成题目失败，请稍后重试')
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [sortBy])

  // Initial load
  useEffect(() => {
    fetchCompletedProblems(0)
  }, [fetchCompletedProblems])

  // Load more function
  const handleLoadMore = useCallback(() => {
    if (!loadingMore && hasNext) {
      fetchCompletedProblems(page + 1, true)
    }
  }, [fetchCompletedProblems, page, hasNext, loadingMore])

  // Filter problems by search term
  const filteredProblems = problems.filter(problem =>
    problem.title.toLowerCase().includes(searchTerm.toLowerCase())
  )

  // Format date
  const formatDate = (dateString: string | null) => {
    if (!dateString) return '未知'
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }

  // Get mastery level text and color
  const getMasteryInfo = (mastery: number) => {
    if (mastery >= 3) return { text: "专家", color: "text-purple-600 dark:text-purple-400" }
    if (mastery >= 2) return { text: "熟练", color: "text-blue-600 dark:text-blue-400" }
    if (mastery >= 1) return { text: "基础", color: "text-green-600 dark:text-green-400" }
    return { text: "学习中", color: "text-orange-600 dark:text-orange-400" }
  }

  if (loading && problems.length === 0) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">完成记录</h1>
          <p className="text-gray-600 dark:text-gray-300">查看您已完成的所有算法题目</p>
        </div>
        
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <Card>
          <CardContent className="p-6 text-center">
            <p className="text-red-600 dark:text-red-400">{error}</p>
            <Button 
              onClick={() => fetchCompletedProblems(0)} 
              className="mt-4"
              variant="outline"
            >
              重新加载
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center gap-2 mb-2">
          <CheckCircle className="h-8 w-8 text-green-600 dark:text-green-400" />
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">完成记录</h1>
        </div>
        <p className="text-gray-600 dark:text-gray-300">
          已完成 {problems.length} 道算法题目
        </p>
      </div>

      {/* Filters and Search */}
      <div className="mb-6 flex flex-col sm:flex-row gap-4">
        <div className="flex-1">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="搜索题目标题..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>
        
        <div className="flex gap-2">
          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="date">按日期</SelectItem>
              <SelectItem value="difficulty">按难度</SelectItem>
              <SelectItem value="mastery">按掌握度</SelectItem>
              <SelectItem value="title">按标题</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Problems List */}
      {filteredProblems.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center">
            <Trophy className="h-16 w-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
            <h3 className="text-xl font-medium text-gray-900 dark:text-white mb-2">
              {searchTerm ? "未找到匹配的题目" : "还没有完成的题目"}
            </h3>
            <p className="text-gray-500 dark:text-gray-400">
              {searchTerm ? "尝试修改搜索关键词" : "开始练习算法题目吧！"}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredProblems.map((problem) => {
            const masteryInfo = getMasteryInfo(problem.mastery)
            return (
              <Card key={problem.problemId} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                    {/* Problem Info */}
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                          {problem.title}
                        </h3>
                        <Badge className={difficultyColors[problem.difficulty]}>
                          {difficultyLabels[problem.difficulty]}
                        </Badge>
                      </div>
                      
                      <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                        <div className="flex items-center gap-1">
                          <Clock className="h-4 w-4" />
                          <span>完成于 {formatDate(problem.lastAttemptDate)}</span>
                        </div>
                        <div>复习 {problem.attemptCount} 次</div>
                        <div>
                          掌握度 {
                            (() => {
                              const value = problem.masteryScore ?? problem.accuracy
                              const percent = value <= 1 ? Math.round(value * 100) : Math.round(value)
                              return `${percent}%`
                            })()
                          }
                        </div>
                      </div>
                    </div>

                    {/* Mastery Progress */}
                    <div className="lg:w-48">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium text-gray-600 dark:text-gray-300">掌握程度</span>
                        <span className={`text-sm font-medium ${masteryInfo.color}`}>
                          {masteryInfo.text}
                        </span>
                      </div>
                      <Progress value={(problem.mastery / 3) * 100} className="h-2" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            )
          })}

          {/* Load More Button */}
          {hasNext && (
            <div className="text-center pt-6">
              <Button
                onClick={handleLoadMore}
                disabled={loadingMore}
                variant="outline"
                size="lg"
              >
                {loadingMore ? "加载中..." : "加载更多"}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
