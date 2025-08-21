'use client'

import { cn } from "@/lib/utils"
import { TrendingUp, Target, Code, Trophy, Plus, ArrowRight, CheckCircle, Clock } from "lucide-react"
import type { AnalyticsOverview } from "@/lib/analytics-api"
import type { UserProblemStatus } from "@/lib/problems-api"

interface LearningProgressProps {
  analyticsData: AnalyticsOverview | null
  userProgress: UserProblemStatus[]
  className?: string
}

export default function LearningProgress({
  analyticsData,
  userProgress,
  className,
}: LearningProgressProps) {
  // 计算总体进度百分比
  const calculateOverallProgress = () => {
    if (!analyticsData) return 0
    const total = analyticsData.totalCards
    if (total === 0) return 0
    const completed = analyticsData.reviewCards + analyticsData.learningCards
    return Math.round((completed / total) * 100)
  }

  // 根据用户进度计算学习模块数据
  const getProgressModules = () => {
    if (!userProgress.length && !analyticsData) {
      return []
    }

    // 如果有真实数据，基于实际进度计算
    const progressByType = userProgress.reduce((acc, problem) => {
      // 根据题目类型分组 - 这里需要根据实际的问题标签来分类
      const category = getCategory(problem.title || '')
      if (!acc[category]) {
        acc[category] = { completed: 0, total: 0 }
      }
      acc[category].total++
      if (problem.status === 'done' || problem.status === 'reviewed') {
        acc[category].completed++
      }
      return acc
    }, {} as Record<string, { completed: number; total: number }>)

    const modules = [
      {
        id: "1",
        title: "数组与字符串",
        description: "基础算法题目",
        type: "algorithm" as const,
        ...getModuleStats('array', progressByType),
      },
      {
        id: "2", 
        title: "链表操作",
        description: "链表相关题目",
        type: "data-structure" as const,
        ...getModuleStats('linkedlist', progressByType),
      },
      {
        id: "3",
        title: "动态规划", 
        description: "DP经典题目",
        type: "algorithm" as const,
        ...getModuleStats('dp', progressByType),
      },
      {
        id: "4",
        title: "系统设计",
        description: "高级面试题目", 
        type: "system-design" as const,
        ...getModuleStats('design', progressByType),
      },
      {
        id: "5",
        title: "复习计划",
        description: "FSRS智能复习",
        type: "interview" as const,
        completed: analyticsData?.reviewCards || 0,
        total: Math.max(analyticsData?.totalCards || 1, 1),
        progress: analyticsData ? `${Math.round(((analyticsData.reviewCards || 0) / Math.max(analyticsData.totalCards, 1)) * 100)}%` : "0%",
      },
    ]

    return modules
  }

  // 根据题目标题推断分类
  const getCategory = (title: string): string => {
    const lower = title.toLowerCase()
    if (lower.includes('数组') || lower.includes('字符串') || lower.includes('array') || lower.includes('string')) return 'array'
    if (lower.includes('链表') || lower.includes('linked')) return 'linkedlist'
    if (lower.includes('动态规划') || lower.includes('dp') || lower.includes('dynamic')) return 'dp'
    if (lower.includes('设计') || lower.includes('design')) return 'design'
    return 'array' // 默认分类
  }

  // 获取模块统计信息
  const getModuleStats = (category: string, progressByType: Record<string, { completed: number; total: number }>) => {
    const stats = progressByType[category] || { completed: 0, total: 1 }
    const progress = Math.round((stats.completed / Math.max(stats.total, 1)) * 100)
    return {
      completed: stats.completed,
      total: stats.total,
      progress: `${progress}%`,
    }
  }

  const overallProgress = calculateOverallProgress()
  const progressModules = getProgressModules()

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
      {/* Total Progress Section */}
      <div className="p-4 border-b border-zinc-100 dark:border-zinc-800">
        <p className="text-xs text-zinc-600 dark:text-zinc-400">学习统计</p>
        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
          总体进度 {overallProgress}%
        </h1>
        <div className="mt-2 w-full bg-zinc-200 dark:bg-zinc-700 rounded-full h-2">
          <div 
            className="bg-blue-600 h-2 rounded-full" 
            style={{ width: `${overallProgress}%` }}
          ></div>
        </div>
        {analyticsData && (
          <div className="mt-2 flex items-center gap-4 text-xs text-zinc-600 dark:text-zinc-400">
            <span>总卡片: {analyticsData.totalCards}</span>
            <span>待复习: {analyticsData.dueCards}</span>
            <span>新卡片: {analyticsData.newCards}</span>
          </div>
        )}
      </div>

      {/* Progress List */}
      <div className="p-3">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-xs font-medium text-zinc-900 dark:text-zinc-100">学习模块</h2>
        </div>

        <div className="space-y-1">
          {progressModules.map((item) => (
            <div
              key={item.id}
              className={cn(
                "group flex items-center justify-between",
                "p-2 rounded-lg",
                "hover:bg-zinc-100 dark:hover:bg-zinc-800/50",
                "transition-all duration-200",
              )}
            >
              <div className="flex items-center gap-2">
                <div
                  className={cn("p-1.5 rounded-lg", {
                    "bg-blue-100 dark:bg-blue-900/30": item.type === "algorithm",
                    "bg-green-100 dark:bg-green-900/30": item.type === "data-structure",
                    "bg-purple-100 dark:bg-purple-900/30": item.type === "system-design",
                    "bg-orange-100 dark:bg-orange-900/30": item.type === "interview",
                  })}
                >
                  {item.type === "algorithm" && <Code className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />}
                  {item.type === "data-structure" && (
                    <Target className="w-3.5 h-3.5 text-green-600 dark:text-green-400" />
                  )}
                  {item.type === "system-design" && (
                    <TrendingUp className="w-3.5 h-3.5 text-purple-600 dark:text-purple-400" />
                  )}
                  {item.type === "interview" && <Trophy className="w-3.5 h-3.5 text-orange-600 dark:text-orange-400" />}
                </div>
                <div>
                  <h3 className="text-xs font-medium text-zinc-900 dark:text-zinc-100">{item.title}</h3>
                  {item.description && (
                    <p className="text-[11px] text-zinc-600 dark:text-zinc-400">{item.description}</p>
                  )}
                  <div className="flex items-center gap-2 mt-1">
                    <div className="w-16 bg-zinc-200 dark:bg-zinc-700 rounded-full h-1">
                      <div
                        className="bg-zinc-900 dark:bg-zinc-100 h-1 rounded-full"
                        style={{ width: item.progress }}
                      ></div>
                    </div>
                    <span className="text-[10px] text-zinc-600 dark:text-zinc-400">
                      {item.completed}/{item.total}
                    </span>
                  </div>
                </div>
              </div>

              <div className="text-right">
                <span className="text-xs font-medium text-zinc-900 dark:text-zinc-100">{item.progress}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Action buttons */}
      <div className="p-2 border-t border-zinc-100 dark:border-zinc-800">
        <div className="grid grid-cols-4 gap-2">
          <button
            type="button"
            className={cn(
              "flex items-center justify-center gap-2",
              "py-2 px-3 rounded-lg",
              "text-xs font-medium",
              "bg-zinc-900 dark:bg-zinc-50",
              "text-zinc-50 dark:text-zinc-900",
              "hover:bg-zinc-800 dark:hover:bg-zinc-200",
              "shadow-sm hover:shadow",
              "transition-all duration-200",
            )}
            onClick={() => window.location.href = '/codetop'}
          >
            <Plus className="w-3.5 h-3.5" />
            <span>开始</span>
          </button>
          <button
            type="button"
            className={cn(
              "flex items-center justify-center gap-2",
              "py-2 px-3 rounded-lg",
              "text-xs font-medium",
              "bg-zinc-900 dark:bg-zinc-50",
              "text-zinc-50 dark:text-zinc-900",
              "hover:bg-zinc-800 dark:hover:bg-zinc-200",
              "shadow-sm hover:shadow",
              "transition-all duration-200",
            )}
            onClick={() => window.location.href = '/review'}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>复习</span>
          </button>
          <button
            type="button"
            className={cn(
              "flex items-center justify-center gap-2",
              "py-2 px-3 rounded-lg",
              "text-xs font-medium",
              "bg-zinc-900 dark:bg-zinc-50",
              "text-zinc-50 dark:text-zinc-900",
              "hover:bg-zinc-800 dark:hover:bg-zinc-200",
              "shadow-sm hover:shadow",
              "transition-all duration-200",
            )}
            onClick={() => window.location.href = '/analysis'}
          >
            <CheckCircle className="w-3.5 h-3.5" />
            <span>分析</span>
          </button>
          <button
            type="button"
            className={cn(
              "flex items-center justify-center gap-2",
              "py-2 px-3 rounded-lg",
              "text-xs font-medium",
              "bg-zinc-900 dark:bg-zinc-50",
              "text-zinc-50 dark:text-zinc-900",
              "hover:bg-zinc-800 dark:hover:bg-zinc-200",
              "shadow-sm hover:shadow",
              "transition-all duration-200",
            )}
            onClick={() => window.location.href = '/leaderboard'}
          >
            <ArrowRight className="w-3.5 h-3.5" />
            <span>更多</span>
          </button>
        </div>
      </div>
    </div>
  )
}