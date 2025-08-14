"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { RefreshCw, AlertTriangle, Clock, CheckCircle, BookOpen } from "lucide-react"
import { reviewApi, type ReviewQueue, type ReviewQueueCard, type SubmitReviewRequest } from "@/lib/review-api"

interface DisplayReviewProblem {
  id: number
  title: string
  difficulty: "easy" | "medium" | "hard"
  lastSolved: string
  reviewStatus: "urgent" | "due" | "upcoming"
  category: string
  mistakes: number
  notes?: string
  dueDate: string
  state: string
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
  urgent: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300",
  due: "bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300",
  upcoming: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
}

const statusLabels = {
  urgent: "紧急复习",
  due: "需要复习",
  upcoming: "即将到期",
}

const statusIcons = {
  urgent: <AlertTriangle className="h-4 w-4" />,
  due: <Clock className="h-4 w-4" />,
  upcoming: <RefreshCw className="h-4 w-4" />,
}

function ReviewProblemCard({ 
  problem, 
  onComplete 
}: { 
  problem: DisplayReviewProblem
  onComplete: (problemId: number) => void
}) {
  const [isCompleted, setIsCompleted] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleComplete = async () => {
    setSubmitting(true)
    try {
      // Submit review with "Good" rating (3)
      await reviewApi.submitReview({
        problemId: problem.id,
        rating: 3,
        reviewType: 'REVIEW',
      })
      setIsCompleted(true)
      onComplete(problem.id)
    } catch (error) {
      console.error('Failed to submit review:', error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className={`transition-all duration-200 ${isCompleted ? "opacity-50" : "hover:shadow-md"}`}>
      <CardContent className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <span className="font-medium text-gray-900 dark:text-gray-100">
                {problem.id}. {problem.title}
              </span>
              <Badge className={difficultyColors[problem.difficulty]}>{difficultyLabels[problem.difficulty]}</Badge>
            </div>
            <div className="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400 mb-2">
              <span>{problem.category}</span>
              <span>上次练习: {problem.lastSolved}</span>
              <span>错误次数: {problem.mistakes}</span>
            </div>
            {problem.notes && (
              <div className="flex items-start gap-2 mt-2 p-2 bg-gray-50 dark:bg-gray-800 rounded">
                <BookOpen className="h-4 w-4 mt-0.5 text-gray-400" />
                <span className="text-sm text-gray-600 dark:text-gray-300">{problem.notes}</span>
              </div>
            )}
          </div>
          <div className="flex flex-col items-end gap-2">
            <Badge className={`${statusColors[problem.reviewStatus]} flex items-center gap-1`}>
              {statusIcons[problem.reviewStatus]}
              {statusLabels[problem.reviewStatus]}
            </Badge>
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            onClick={isCompleted ? () => setIsCompleted(false) : handleComplete}
            variant={isCompleted ? "outline" : "default"}
            className="flex items-center gap-1"
            disabled={submitting}
          >
            {isCompleted ? (
              <>
                <RefreshCw className="h-4 w-4" />
                重新复习
              </>
            ) : (
              <>
                <CheckCircle className="h-4 w-4" />
                {submitting ? '提交中...' : '标记完成'}
              </>
            )}
          </Button>
          <Button size="sm" variant="outline">
            <BookOpen className="h-4 w-4 mr-1" />
            查看笔记
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

export default function ReviewPage() {
  const [reviewQueue, setReviewQueue] = useState<ReviewQueue | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Transform API data to display format
  const transformReviewCard = (card: ReviewQueueCard): DisplayReviewProblem => {
    const dueDate = new Date(card.due)
    const now = new Date()
    const diffDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    
    let reviewStatus: "urgent" | "due" | "upcoming"
    if (diffDays < 0) {
      reviewStatus = "urgent"
    } else if (diffDays <= 1) {
      reviewStatus = "due"
    } else {
      reviewStatus = "upcoming"
    }

    return {
      id: card.problemId,
      title: card.title,
      difficulty: card.difficulty.toLowerCase() as "easy" | "medium" | "hard",
      lastSolved: "从未练习", // ReviewQueueCard doesn't have lastReview property
      reviewStatus,
      category: "算法题", // TODO: Add category to ReviewQueueCard interface
      mistakes: card.lapses,
      dueDate: card.due,
      state: card.state,
      notes: card.lapses > 0 ? `已重复 ${card.lapses} 次` : undefined,
    }
  }

  const fetchReviewQueue = async () => {
    try {
      setLoading(true)
      setError(null)
      const queue = await reviewApi.getReviewQueue(50)
      setReviewQueue(queue)
    } catch (err: any) {
      setError(err.message || '加载复习队列失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchReviewQueue()
  }, [])

  const handleComplete = (problemId: number) => {
    // Refresh the review queue after completion
    setTimeout(() => {
      fetchReviewQueue()
    }, 1000)
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

  const urgentProblems = allProblems.filter((p) => p.reviewStatus === "urgent")
  const dueProblems = allProblems.filter((p) => p.reviewStatus === "due")
  const upcomingProblems = allProblems.filter((p) => p.reviewStatus === "upcoming")

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">复习计划</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">基于遗忘曲线的智能复习提醒</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">紧急复习</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{urgentProblems.length}</div>
            <p className="text-xs text-muted-foreground">需要立即复习</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">今日复习</CardTitle>
            <Clock className="h-4 w-4 text-orange-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{dueProblems.length}</div>
            <p className="text-xs text-muted-foreground">今日需要复习</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">即将到期</CardTitle>
            <RefreshCw className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{upcomingProblems.length}</div>
            <p className="text-xs text-muted-foreground">未来几天需复习</p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="urgent" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="urgent" className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            紧急复习 ({urgentProblems.length})
          </TabsTrigger>
          <TabsTrigger value="due" className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            今日复习 ({dueProblems.length})
          </TabsTrigger>
          <TabsTrigger value="upcoming" className="flex items-center gap-2">
            <RefreshCw className="h-4 w-4" />
            即将到期 ({upcomingProblems.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="urgent" className="space-y-4">
          {urgentProblems.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              暂无紧急复习题目
            </div>
          ) : (
            urgentProblems.map((problem) => (
              <ReviewProblemCard key={problem.id} problem={problem} onComplete={handleComplete} />
            ))
          )}
        </TabsContent>

        <TabsContent value="due" className="space-y-4">
          {dueProblems.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              暂无今日复习题目
            </div>
          ) : (
            dueProblems.map((problem) => (
              <ReviewProblemCard key={problem.id} problem={problem} onComplete={handleComplete} />
            ))
          )}
        </TabsContent>

        <TabsContent value="upcoming" className="space-y-4">
          {upcomingProblems.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              暂无即将到期题目
            </div>
          ) : (
            upcomingProblems.map((problem) => (
              <ReviewProblemCard key={problem.id} problem={problem} onComplete={handleComplete} />
            ))
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
