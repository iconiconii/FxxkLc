"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { RefreshCw, AlertTriangle, Clock, CheckCircle, BookOpen, CircleCheckBig, Filter, StickyNote } from "lucide-react"
import { reviewApi, type ReviewQueue, type ReviewQueueCard, type SubmitReviewRequest } from "@/lib/review-api"
import ReviewAssessmentModal from "@/components/modals/review-assessment-modal"
import { NoteViewer } from "@/components/notes"
import { useNotes } from "@/hooks/use-notes"

interface DisplayReviewProblem {
  id: number
  title: string
  difficulty: "easy" | "medium" | "hard"
  lastSolved: string
  reviewStatus: "overdue" | "due" | "upcoming"
  category: string
  mistakes: number
  notes?: string
  dueDate: string
  state: string
  priorityScore: number // 用于排序的优先级分数
}

const difficultyColors = {
  easy: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
  medium: "bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300",
  hard: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300",
}

const difficultyLabels = {
  easy: "简单",
  medium: "中等",
  hard: "困难",
}

const statusColors = {
  overdue: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300",
  due: "bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300",
  upcoming: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
}

const statusLabels = {
  overdue: "已过期",
  due: "今日复习",
  upcoming: "即将复习",
}

const statusIcons = {
  overdue: <AlertTriangle className="h-4 w-4" />,
  due: <Clock className="h-4 w-4" />,
  upcoming: <RefreshCw className="h-4 w-4" />,
}

function ReviewProblemRow({ 
  problem, 
  onComplete 
}: { 
  problem: DisplayReviewProblem
  onComplete: (problemId: number) => void
}) {
  const [isCompleted, setIsCompleted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [showNotes, setShowNotes] = useState(false)
  
  // Get user notes for this problem
  const { userNote, isLoading: isNotesLoading } = useNotes(problem.id)

  const handleComplete = () => {
    // Open the assessment modal instead of directly submitting
    setModalOpen(true)
  }

  const handleReviewComplete = (problemId: number) => {
    setIsCompleted(true)
    setModalOpen(false)
    // 不立即调用 onComplete 来刷新页面，只是标记为已完成
  }

  // 计算到期时间显示
  const getDueDisplay = () => {
    const dueDate = new Date(problem.dueDate)
    const now = new Date()
    const diffDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    
    if (diffDays < 0) {
      return `已过期 ${Math.abs(diffDays)} 天`
    } else if (diffDays === 0) {
      const diffHours = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60))
      if (diffHours <= 0) {
        return "现在到期"
      } else {
        return `${diffHours}小时后到期`
      }
    } else {
      return `${diffDays}天后到期`
    }
  }

  return (
    <>
      <tr className={`border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50/50 dark:hover:bg-gray-700/50 transition-colors ${isCompleted ? "opacity-50" : ""}`}>
        <td className="px-4 md:px-6 py-3 md:py-4">
          <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3">
            <div className="font-medium text-gray-900 dark:text-gray-100 text-sm md:text-base">
              {problem.id}. {problem.title}
            </div>
            <Badge className={difficultyColors[problem.difficulty]}>{difficultyLabels[problem.difficulty]}</Badge>
          </div>
          {/* 移动端显示更多信息 */}
          <div className="sm:hidden mt-2 space-y-1 text-xs text-gray-500 dark:text-gray-400">
            <div>{getDueDisplay()}</div>
            <div>复习次数: {problem.lastSolved} 次</div>
          </div>
          {problem.notes && (
            <div className="flex items-start gap-2 mt-2 text-sm text-gray-600 dark:text-gray-400">
              <BookOpen className="h-3 w-3 mt-0.5" />
              <span className="text-xs">{problem.notes}</span>
            </div>
          )}
        </td>
        <td className="hidden sm:table-cell px-6 py-4 text-sm">
          <div className="text-gray-600 dark:text-gray-400">
            {getDueDisplay()}
          </div>
          <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
            {problem.dueDate.split('T')[0]}
          </div>
        </td>
        <td className="hidden md:table-cell px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
          {problem.lastSolved} 次
        </td>
        <td className="px-4 md:px-6 py-3 md:py-4">
          <Badge 
            className={`${statusColors[problem.reviewStatus]} flex items-center gap-1 w-fit text-xs`}
            variant="outline"
          >
            {statusIcons[problem.reviewStatus]}
            <span className="hidden sm:inline">{statusLabels[problem.reviewStatus]}</span>
          </Badge>
        </td>
        <td className="px-4 md:px-6 py-3 md:py-4">
          <div className="flex items-center gap-1 md:gap-2">
            <Button
              size="sm"
              onClick={isCompleted ? () => setIsCompleted(false) : handleComplete}
              variant={isCompleted ? "outline" : "default"}
              className="flex items-center gap-1 h-8 text-xs md:text-sm px-2 md:px-3"
              title="点击记录做题状态"
            >
              {isCompleted ? (
                <>
                  <CheckCircle className="h-4 w-4" />
                  <span className="hidden sm:inline">今日已复习</span>
                </>
              ) : (
                <>
                  <CircleCheckBig className="h-4 w-4" />
                  <span className="hidden sm:inline">复习</span>
                </>
              )}
            </Button>
            <Button 
              size="sm" 
              variant={showNotes ? "default" : "ghost"} 
              className="h-8 px-2"
              onClick={() => setShowNotes(!showNotes)}
              disabled={isNotesLoading}
              title={userNote ? "查看笔记" : "暂无笔记"}
            >
              {userNote ? (
                <StickyNote className="h-4 w-4" />
              ) : (
                <BookOpen className="h-4 w-4" />
              )}
              <span className="sr-only">
                {userNote ? "查看笔记" : "暂无笔记"}
              </span>
            </Button>
          </div>
        </td>
      </tr>
      
      {/* Notes row - shows when showNotes is true */}
      {showNotes && userNote && (
        <tr className="border-b border-gray-100 dark:border-gray-800">
          <td colSpan={5} className="px-4 md:px-6 py-4">
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-3">
                <StickyNote className="w-4 h-4 text-blue-600" />
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">我的笔记</span>
                <Badge variant="outline" className="text-xs">
                  {userNote.isPublic ? "公开" : "私有"}
                </Badge>
              </div>
              
              <div className="max-h-96 overflow-y-auto">
                <NoteViewer 
                  note={userNote} 
                  showEdit={false} 
                  showVoting={false}
                />
              </div>
            </div>
          </td>
        </tr>
      )}
      
      {/* Loading state for notes */}
      {showNotes && isNotesLoading && (
        <tr className="border-b border-gray-100 dark:border-gray-800">
          <td colSpan={5} className="px-4 md:px-6 py-4">
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4 text-center">
              <div className="text-sm text-gray-500">加载笔记中...</div>
            </div>
          </td>
        </tr>
      )}
      
      {/* No notes state */}
      {showNotes && !isNotesLoading && !userNote && (
        <tr className="border-b border-gray-100 dark:border-gray-800">
          <td colSpan={5} className="px-4 md:px-6 py-4">
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4 text-center">
              <div className="text-sm text-gray-500 mb-2">此题目暂无笔记</div>
              <Button 
                size="sm" 
                variant="outline"
                onClick={() => {
                  // Could open assessment modal to create a note
                  setModalOpen(true);
                }}
              >
                创建笔记
              </Button>
            </div>
          </td>
        </tr>
      )}
      
      <ReviewAssessmentModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        problemId={problem.id}
        problemTitle={problem.title}
        onReviewComplete={handleReviewComplete}
      />
    </>
  )
}

export default function ReviewPage() {
  const [reviewQueue, setReviewQueue] = useState<ReviewQueue | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [upcomingFilter, setUpcomingFilter] = useState<string>("all") // all, 1day, 3days, 7days
  
  // 分页状态 - 现在使用服务端分页
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize] = useState(10) // 每页10题
  const [serverPagination, setServerPagination] = useState(true) // 是否使用服务端分页

  // Transform API data to display format
  const transformReviewCard = (card: ReviewQueueCard): DisplayReviewProblem => {
    const dueDate = new Date(card.dueDate)
    const now = new Date()
    const diffDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    const diffHours = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60))
    
    let reviewStatus: "overdue" | "due" | "upcoming"
    let priorityScore: number
    
    if (card.overdue || diffDays < 0) {
      // 过期题目：最高优先级
      reviewStatus = "overdue"
      priorityScore = 1000 + Math.abs(diffDays) // 过期越久优先级越高
    } else if (card.due || diffDays === 0) {
      // 今日题目：高优先级
      reviewStatus = "due"
      priorityScore = 500 + (24 - Math.max(diffHours, 0)) // 今日内越早到期优先级越高
    } else {
      // 未来题目：按时间远近排序
      reviewStatus = "upcoming"
      priorityScore = Math.max(0, 100 - diffDays) // 越近的优先级越高
    }

    return {
      id: card.problemId,
      title: card.problemTitle,
      difficulty: card.problemDifficulty.toLowerCase() as "easy" | "medium" | "hard",
      lastSolved: `${card.reviewCount}`, // 使用复习次数
      reviewStatus,
      category: "算法题",
      mistakes: card.lapses,
      dueDate: card.dueDate,
      state: card.state,
      priorityScore,
      notes: card.lapses > 0 ? `已重复 ${card.lapses} 次` : undefined,
    }
  }

  const fetchReviewQueue = async () => {
    try {
      setLoading(true)
      setError(null)
      // 使用服务端分页，传递当前页和页面大小，默认 showAll=true 获取所有需要复习的题目
      const queue = await reviewApi.getReviewQueue(100, currentPage, pageSize, true)
      setReviewQueue(queue)
    } catch (err: any) {
      setError(err.message || '加载复习队列失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchReviewQueue()
  }, [currentPage]) // 当页面改变时重新获取数据

  const handleComplete = async (problemId: number) => {
    // 复习完成后记录日志，但不立即刷新页面
    console.log(`Review completed for problem ${problemId}`)
    // 移除自动刷新，让用户控制何时刷新数据
    // await fetchReviewQueue()
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-gray-500 dark:text-gray-400">加载复习队列中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">复习计划</h1>
        <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
          <button 
            onClick={fetchReviewQueue} 
            className="ml-2 underline hover:no-underline"
          >
            重试
          </button>
        </div>
      </div>
    )
  }

  const allProblems = [
    ...reviewQueue?.newCards || [],
    ...reviewQueue?.learningCards || [],
    ...reviewQueue?.reviewCards || [],
    ...reviewQueue?.relearningCards || [],
  ].map(transformReviewCard)

  // 如果使用服务端分页，直接使用返回的数据
  let displayProblems = allProblems
  let totalPages = reviewQueue?.totalPages || 1
  let totalCount = reviewQueue?.totalCount || 0

  // 根据筛选条件过滤题目（客户端筛选）
  if (upcomingFilter !== "all") {
    const filteredProblems = allProblems.filter((p) => {
      const dueDate = new Date(p.dueDate)
      const now = new Date()
      const diffDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
      
      switch (upcomingFilter) {
        case "1day":
          return diffDays <= 1
        case "3days":
          return diffDays <= 3
        case "7days":
          return diffDays <= 7
        default:
          return true
      }
    })
    
    // 筛选后切换为客户端分页
    displayProblems = filteredProblems.sort((a, b) => b.priorityScore - a.priorityScore)
    totalCount = displayProblems.length
    totalPages = Math.ceil(totalCount / pageSize)
    
    // 应用客户端分页
    const startIndex = (currentPage - 1) * pageSize
    const endIndex = startIndex + pageSize
    displayProblems = displayProblems.slice(startIndex, endIndex)
  } else {
    // 按优先级排序：过期 > 今日 > 即将到期（服务端已排序）
    displayProblems = allProblems.sort((a, b) => b.priorityScore - a.priorityScore)
  }
  
  // 统计各类题目数量
  const overdueCount = allProblems.filter((p) => p.reviewStatus === "overdue").length
  const dueCount = allProblems.filter((p) => p.reviewStatus === "due").length
  const upcomingCount = allProblems.filter((p) => p.reviewStatus === "upcoming").length
  
  // 分页处理函数
  const handlePageChange = (page: number) => {
    setCurrentPage(page)
  }
  
  // 重置分页当筛选条件改变时
  const handleFilterChange = (newFilter: string) => {
    setUpcomingFilter(newFilter)
    setCurrentPage(1) // 重置到第一页
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">复习计划</h1>
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchReviewQueue}
            disabled={loading}
            className="flex items-center gap-2"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            刷新数据
          </Button>
          <p className="text-sm text-gray-500 dark:text-gray-400">基于遗忘曲线的智能复习提醒</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">已过期</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{overdueCount}</div>
            <p className="text-xs text-muted-foreground">需优先复习</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">今日复习</CardTitle>
            <Clock className="h-4 w-4 text-orange-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{dueCount}</div>
            <p className="text-xs text-muted-foreground">今日计划复习</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">即将复习</CardTitle>
            <RefreshCw className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{upcomingCount}</div>
            <p className="text-xs text-muted-foreground">未来几天</p>
          </CardContent>
        </Card>
      </div>

      {/* 筛选控件 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-gray-500" />
          <span className="text-sm text-gray-600 dark:text-gray-400">筛选范围:</span>
          <Select value={upcomingFilter} onValueChange={handleFilterChange}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部</SelectItem>
              <SelectItem value="1day">1天内</SelectItem>
              <SelectItem value="3days">3天内</SelectItem>
              <SelectItem value="7days">7天内</SelectItem>
            </SelectContent>
          </Select>
        </div>
        {upcomingFilter !== "all" && (
          <Button 
            variant="outline" 
            size="sm" 
            onClick={() => handleFilterChange("all")}
          >
            清除筛选
          </Button>
        )}
      </div>

      {/* 统一复习列表 */}
      <div className="space-y-4">
        {displayProblems.length === 0 ? (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            {upcomingFilter === "all" ? "暂无复习题目" : "该时间范围内暂无复习题目"}
          </div>
        ) : (
          <>
            {/* 显示总数统计和分页信息 */}
            <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
              <span>共 {totalCount} 道题目 · 按复习优先级排序</span>
              {totalPages > 1 && <span>第 {currentPage} 页，共 {totalPages} 页</span>}
            </div>
            
            {/* 分页题目表格 */}
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50/50 dark:bg-gray-700/50 backdrop-blur-sm">
                    <tr className="border-b border-gray-200 dark:border-gray-700">
                      <th className="px-4 md:px-6 py-3 md:py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                        题目
                      </th>
                      <th className="hidden sm:table-cell px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                        到期时间
                      </th>
                      <th className="hidden md:table-cell px-6 py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                        复习次数
                      </th>
                      <th className="px-4 md:px-6 py-3 md:py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                        状态
                      </th>
                      <th className="px-4 md:px-6 py-3 md:py-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-200">
                        操作
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white dark:bg-gray-800">
                    {displayProblems.map((problem) => (
                      <ReviewProblemRow key={problem.id} problem={problem} onComplete={handleComplete} />
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>

      {/* 分页组件 */}
      {displayProblems.length > 0 && totalPages > 1 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            {/* 分页信息 */}
            <div className="text-sm text-gray-500 dark:text-gray-400">
              显示第 {((currentPage - 1) * pageSize) + 1} - {Math.min(currentPage * pageSize, totalCount)} 条，共 {totalCount} 条记录
            </div>
            
            {/* 分页控件 */}
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage <= 1}
                onClick={() => handlePageChange(1)}
                className="px-3 py-1 text-xs"
              >
                首页
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage <= 1}
                onClick={() => handlePageChange(currentPage - 1)}
                className="px-3 py-1 text-xs"
              >
                上一页
              </Button>
              
              {/* 页码显示 */}
              <div className="flex gap-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let pageNum: number;
                  if (totalPages <= 5) {
                    pageNum = i + 1;
                  } else if (currentPage <= 3) {
                    pageNum = i + 1;
                  } else if (currentPage >= totalPages - 2) {
                    pageNum = totalPages - 4 + i;
                  } else {
                    pageNum = currentPage - 2 + i;
                  }
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === currentPage ? "default" : "outline"}
                      size="sm"
                      onClick={() => handlePageChange(pageNum)}
                      className={`w-8 h-8 p-0 text-xs ${
                        pageNum === currentPage 
                          ? "bg-blue-600 text-white hover:bg-blue-700" 
                          : "border border-gray-300 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700"
                      }`}
                    >
                      {pageNum}
                    </Button>
                  );
                })}
              </div>
              
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage >= totalPages}
                onClick={() => handlePageChange(currentPage + 1)}
                className="px-3 py-1 text-xs"
              >
                下一页
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage >= totalPages}
                onClick={() => handlePageChange(totalPages)}
                className="px-3 py-1 text-xs"
              >
                末页
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
