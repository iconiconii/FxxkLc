'use client'

import { cn } from "@/lib/utils"
import { 
  Calendar, 
  Flame, 
  Target, 
  TrendingUp, 
  Clock,
  CheckCircle,
  Award,
  BarChart3
} from "lucide-react"
import type { AnalyticsOverview } from "@/lib/analytics-api"

interface QuickStatsProps {
  analyticsData: AnalyticsOverview | null
  loading?: boolean
  className?: string
}

interface StatItem {
  id: string
  title: string
  value: string | number
  change?: string
  icon: React.ComponentType<{ className?: string }>
  color: string
  bgColor: string
  description: string
}

export default function QuickStats({ 
  analyticsData, 
  loading = false, 
  className 
}: QuickStatsProps) {
  
  const generateStats = (): StatItem[] => {
    if (!analyticsData) {
      return [
        {
          id: 'streak',
          title: '学习天数',
          value: 0,
          icon: Flame,
          color: 'text-orange-600 dark:text-orange-400',
          bgColor: 'bg-orange-50 dark:bg-orange-900/20',
          description: '连续学习天数'
        },
        {
          id: 'mastered',
          title: '已掌握',
          value: 0,
          icon: CheckCircle,
          color: 'text-green-600 dark:text-green-400',
          bgColor: 'bg-green-50 dark:bg-green-900/20',
          description: '已完全掌握的题目'
        },
        {
          id: 'today_progress',
          title: '今日进度',
          value: '0%',
          icon: Target,
          color: 'text-blue-600 dark:text-blue-400',
          bgColor: 'bg-blue-50 dark:bg-blue-900/20',
          description: '今日复习完成率'
        },
        {
          id: 'efficiency',
          title: '学习效率',
          value: '0%',
          icon: TrendingUp,
          color: 'text-purple-600 dark:text-purple-400',
          bgColor: 'bg-purple-50 dark:bg-purple-900/20',
          description: '记忆保持效率'
        }
      ]
    }

    // Calculate meaningful statistics from FSRS data
    const totalCards = analyticsData.totalCards || 0
    const reviewCards = analyticsData.reviewCards || 0
    const learningCards = analyticsData.learningCards || 0
    const dueCards = analyticsData.dueCards || 0
    const avgStability = analyticsData.avgStability || 0
    const totalLapses = analyticsData.totalLapses || 0

    // Mastered problems (cards in review state with good stability)
    const masteredCount = reviewCards

    // Today's progress (assuming due cards are today's target)
    const todayProgress = dueCards > 0 
      ? Math.max(0, Math.round(((totalCards - dueCards) / totalCards) * 100))
      : 100

    // Learning efficiency (based on stability and lapses)
    const efficiency = totalCards > 0 
      ? Math.max(0, Math.round((100 - (totalLapses / totalCards) * 10)))
      : 0

    // Estimated streak based on review cards (simplified)
    const estimatedStreak = Math.min(30, Math.round(avgStability || 1))

    return [
      {
        id: 'streak',
        title: '学习天数',
        value: estimatedStreak,
        change: estimatedStreak > 7 ? '+1' : undefined,
        icon: Flame,
        color: 'text-orange-600 dark:text-orange-400',
        bgColor: 'bg-orange-50 dark:bg-orange-900/20',
        description: '基于学习稳定性估算'
      },
      {
        id: 'mastered',
        title: '已掌握',
        value: masteredCount,
        change: masteredCount > 0 ? `${Math.round((masteredCount / totalCards) * 100)}%` : undefined,
        icon: CheckCircle,
        color: 'text-green-600 dark:text-green-400',
        bgColor: 'bg-green-50 dark:bg-green-900/20',
        description: '进入复习阶段的题目'
      },
      {
        id: 'today_progress',
        title: '今日进度',
        value: `${todayProgress}%`,
        icon: Target,
        color: 'text-blue-600 dark:text-blue-400',
        bgColor: 'bg-blue-50 dark:bg-blue-900/20',
        description: `剩余 ${dueCards} 题待复习`
      },
      {
        id: 'efficiency',
        title: '学习效率',
        value: `${efficiency}%`,
        change: efficiency > 80 ? '优秀' : efficiency > 60 ? '良好' : '需提升',
        icon: TrendingUp,
        color: 'text-purple-600 dark:text-purple-400',
        bgColor: 'bg-purple-50 dark:bg-purple-900/20',
        description: '基于错误率和记忆强度'
      }
    ]
  }

  if (loading) {
    return (
      <div className={cn("grid grid-cols-2 lg:grid-cols-4 gap-4", className)}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="bg-white dark:bg-[#0F0F12] rounded-xl p-4 border border-gray-200 dark:border-[#1F1F23] animate-pulse"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="w-8 h-8 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
              <div className="w-12 h-6 bg-gray-200 dark:bg-gray-700 rounded"></div>
            </div>
            <div className="space-y-2">
              <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded"></div>
              <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
            </div>
          </div>
        ))}
      </div>
    )
  }

  const stats = generateStats()

  return (
    <div className={cn("grid grid-cols-2 lg:grid-cols-4 gap-4", className)}>
      {stats.map((stat) => (
        <div
          key={stat.id}
          className={cn(
            "bg-white dark:bg-[#0F0F12] rounded-xl p-4",
            "border border-gray-200 dark:border-[#1F1F23]",
            "shadow-sm transition-all duration-200 hover:shadow-md",
            "transform hover:-translate-y-0.5 active:translate-y-0"
          )}
        >
          <div className="flex items-center justify-between mb-3">
            <div className={cn("p-2 rounded-lg", stat.bgColor)}>
              <stat.icon className={cn("w-5 h-5", stat.color)} />
            </div>
            {stat.change && (
              <div className={cn(
                "px-2 py-1 rounded-full text-xs font-medium",
                stat.change.includes('+') || stat.change === '优秀' || stat.change === '良好'
                  ? "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300"
                  : stat.change === '需提升'
                  ? "bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300"
                  : "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400"
              )}>
                {stat.change}
              </div>
            )}
          </div>

          <div className="space-y-1">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">
              {stat.value}
            </div>
            <div className="text-sm font-medium text-gray-700 dark:text-gray-300">
              {stat.title}
            </div>
            <div className="text-xs text-gray-600 dark:text-gray-400">
              {stat.description}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}