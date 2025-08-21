'use client'

import { cn } from "@/lib/utils"
import { CheckCircle, XCircle, Clock, Code, ArrowRight } from "lucide-react"
import type { DailyReviewActivity } from "@/lib/analytics-api"
import type { ReviewQueueCard } from "@/lib/review-api"

interface RecentPracticeProps {
  recentActivity: DailyReviewActivity[]
  reviewQueue: ReviewQueueCard[]
  className?: string
}

interface PracticeRecord {
  id: string
  title: string
  difficulty: "easy" | "medium" | "hard"
  status: "solved" | "attempted" | "reviewing"
  timestamp: string
  timeSpent: string
  category: string
}

const difficultyColors = {
  easy: "text-green-600 dark:text-green-400",
  medium: "text-orange-600 dark:text-orange-400", 
  hard: "text-red-600 dark:text-red-400",
}

const statusIcons = {
  solved: CheckCircle,
  attempted: XCircle,
  reviewing: Clock,
}

const statusColors = {
  solved: "text-green-600 dark:text-green-400",
  attempted: "text-red-600 dark:text-red-400",
  reviewing: "text-orange-600 dark:text-orange-400",
}

export default function RecentPractice({ 
  recentActivity, 
  reviewQueue, 
  className 
}: RecentPracticeProps) {
  
  // 将API数据转换为组件需要的格式
  const generatePracticeRecords = (): PracticeRecord[] => {
    const records: PracticeRecord[] = []
    
    // 从复习队列生成记录
    reviewQueue.slice(0, 4).forEach((card, index) => {
      const difficultyMap: Record<string, "easy" | "medium" | "hard"> = {
        'EASY': 'easy',
        'MEDIUM': 'medium', 
        'HARD': 'hard'
      }
      
      const statusMap: Record<string, "solved" | "attempted" | "reviewing"> = {
        'NEW': 'attempted',
        'LEARNING': 'reviewing',
        'REVIEW': 'solved',
        'RELEARNING': 'reviewing'
      }
      
      records.push({
        id: `queue-${card.id}`,
        title: card.problemTitle,
        difficulty: difficultyMap[card.problemDifficulty] || 'medium',
        status: statusMap[card.state] || 'attempted',
        timestamp: formatTimestamp(card.dueDate),
        timeSpent: `${Math.max(card.intervalDays, 1)} 天间隔`,
        category: getCategoryFromTitle(card.problemTitle)
      })
    })

    // 从最近活动生成记录
    recentActivity.slice(0, 3).forEach((activity, index) => {
      records.push({
        id: `activity-${index}`,
        title: `每日练习 ${activity.reviewCount} 题`,
        difficulty: activity.correctCount / Math.max(activity.reviewCount, 1) > 0.8 ? 'easy' : 
                   activity.correctCount / Math.max(activity.reviewCount, 1) > 0.6 ? 'medium' : 'hard',
        status: activity.correctCount === activity.reviewCount ? 'solved' : 
                activity.correctCount > 0 ? 'reviewing' : 'attempted',
        timestamp: formatDate(activity.date),
        timeSpent: `${activity.totalTimeMinutes || 0}分钟`,
        category: 'practice'
      })
    })

    // 如果没有真实数据，返回一些示例数据
    if (records.length === 0) {
      return [
        {
          id: "fallback-1",
          title: "开始第一道题目",
          difficulty: "easy",
          status: "attempted",
          timestamp: "等待开始",
          timeSpent: "-",
          category: "algorithm",
        },
        {
          id: "fallback-2", 
          title: "建立学习计划",
          difficulty: "medium",
          status: "reviewing",
          timestamp: "建议",
          timeSpent: "-",
          category: "planning",
        }
      ]
    }

    return records.slice(0, 6) // 最多显示6条记录
  }

  // 获取题目分类
  const getCategoryFromTitle = (title: string): string => {
    const lower = title.toLowerCase()
    if (lower.includes('数组') || lower.includes('array')) return 'array'
    if (lower.includes('链表') || lower.includes('linked')) return 'linkedlist'
    if (lower.includes('树') || lower.includes('tree')) return 'tree'
    if (lower.includes('动态规划') || lower.includes('dp')) return 'dp'
    return 'algorithm'
  }

  // 格式化时间戳
  const formatTimestamp = (dateStr: string): string => {
    try {
      const date = new Date(dateStr)
      const now = new Date()
      const diffMs = now.getTime() - date.getTime()
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
      const diffDays = Math.floor(diffHours / 24)

      if (diffDays > 0) {
        return `${diffDays}天前`
      } else if (diffHours > 0) {
        return `${diffHours}小时前`
      } else {
        return '刚刚'
      }
    } catch {
      return '最近'
    }
  }

  // 格式化日期
  const formatDate = (dateStr: string): string => {
    try {
      const date = new Date(dateStr)
      const now = new Date()
      const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24))
      
      if (diffDays === 0) return '今天'
      if (diffDays === 1) return '昨天'
      if (diffDays <= 7) return `${diffDays}天前`
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
    } catch {
      return '最近'
    }
  }

  const records = generatePracticeRecords()

  return (
    <div
      className={cn(
        "w-full max-w-xl mx-auto",
        "bg-white dark:bg-zinc-900/70",
        "border border-zinc-100 dark:border-zinc-800",
        "rounded-xl shadow-sm backdrop-blur-xl",
        className,
      )}
    >
      <div className="p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            练习记录
            <span className="text-xs font-normal text-zinc-600 dark:text-zinc-400 ml-1">
              (最近{records.length}项)
            </span>
          </h2>
          <span className="text-xs text-zinc-600 dark:text-zinc-400">
            {recentActivity.length > 0 ? '本周' : '开始学习'}
          </span>
        </div>

        <div className="space-y-1">
          {records.map((record) => {
            const StatusIcon = statusIcons[record.status]
            return (
              <div
                key={record.id}
                className={cn(
                  "group flex items-center gap-3",
                  "p-2 rounded-lg",
                  "hover:bg-zinc-100 dark:hover:bg-zinc-800/50",
                  "transition-all duration-200",
                )}
              >
                <div
                  className={cn(
                    "p-2 rounded-lg",
                    "bg-zinc-100 dark:bg-zinc-800",
                    "border border-zinc-200 dark:border-zinc-700",
                  )}
                >
                  <Code className="w-4 h-4 text-zinc-900 dark:text-zinc-100" />
                </div>

                <div className="flex-1 flex items-center justify-between min-w-0">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <h3 className="text-xs font-medium text-zinc-900 dark:text-zinc-100 truncate">
                        {record.title}
                      </h3>
                      <span className={cn("text-[10px] font-medium", difficultyColors[record.difficulty])}>
                        {record.difficulty === "easy" ? "简单" : 
                         record.difficulty === "medium" ? "中等" : "困难"}
                      </span>
                    </div>
                    <p className="text-[11px] text-zinc-600 dark:text-zinc-400">
                      {record.timestamp} · 用时 {record.timeSpent}
                    </p>
                  </div>

                  <div className="flex items-center gap-1.5 pl-3">
                    <StatusIcon className={cn("w-3.5 h-3.5", statusColors[record.status])} />
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      <div className="p-2 border-t border-zinc-100 dark:border-zinc-800">
        <button
          type="button"
          className={cn(
            "w-full flex items-center justify-center gap-2",
            "py-2 px-3 rounded-lg",
            "text-xs font-medium",
            "bg-gradient-to-r from-zinc-900 to-zinc-800",
            "dark:from-zinc-50 dark:to-zinc-200",
            "text-zinc-50 dark:text-zinc-900",
            "hover:from-zinc-800 hover:to-zinc-700",
            "dark:hover:from-zinc-200 dark:hover:to-zinc-300",
            "shadow-sm hover:shadow",
            "transform transition-all duration-200",
            "hover:-translate-y-0.5",
            "active:translate-y-0",
            "focus:outline-none focus:ring-2",
            "focus:ring-zinc-500 dark:focus:ring-zinc-400",
            "focus:ring-offset-2 dark:focus:ring-offset-zinc-900",
          )}
          onClick={() => window.location.href = '/review'}
        >
          <span>查看全部练习记录</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  )
}