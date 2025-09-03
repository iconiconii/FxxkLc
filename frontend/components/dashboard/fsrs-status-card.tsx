'use client'

import { cn } from "@/lib/utils"
import { 
  Brain, 
  Clock, 
  TrendingUp, 
  PlayCircle, 
  BookOpen, 
  Zap,
  CheckCircle,
  AlertCircle
} from "lucide-react"
import type { AnalyticsOverview } from "@/lib/analytics-api"

interface FSRSStatusCardProps {
  analyticsData: AnalyticsOverview | null
  loading?: boolean
  className?: string
}

export default function FSRSStatusCard({ 
  analyticsData, 
  loading = false, 
  className 
}: FSRSStatusCardProps) {
  
  const getRetentionRate = () => {
    if (!analyticsData) return 0
    const total = analyticsData.totalCards
    if (total === 0) return 0
    const mastered = analyticsData.reviewCards + analyticsData.learningCards
    return Math.round((mastered / total) * 100)
  }

  const getMemoryStrength = () => {
    if (!analyticsData) return 0
    // Use average stability as memory strength indicator
    return Math.min(100, Math.round((analyticsData.avgStability || 0) * 10))
  }

  const getPriorityAction = () => {
    if (!analyticsData) return null
    
    const { dueCards, newCards, learningCards, relearningCards } = analyticsData
    
    if (dueCards > 0) {
      return {
        type: 'review',
        title: '开始复习',
        count: dueCards,
        description: '有题目需要复习',
        icon: Clock,
        color: 'text-red-600 dark:text-red-400',
        bg: 'bg-red-50 dark:bg-red-900/20',
        urgent: dueCards > 10
      }
    }
    
    if (relearningCards > 0) {
      return {
        type: 'relearn',
        title: '重新学习',
        count: relearningCards,
        description: '需要加强练习',
        icon: AlertCircle,
        color: 'text-orange-600 dark:text-orange-400',
        bg: 'bg-orange-50 dark:bg-orange-900/20',
        urgent: false
      }
    }
    
    if (learningCards > 0) {
      return {
        type: 'continue',
        title: '继续学习',
        count: learningCards,
        description: '正在学习中',
        icon: BookOpen,
        color: 'text-blue-600 dark:text-blue-400',
        bg: 'bg-blue-50 dark:bg-blue-900/20',
        urgent: false
      }
    }
    
    if (newCards > 0) {
      return {
        type: 'new',
        title: '学习新题',
        count: newCards,
        description: '开始新的学习',
        icon: PlayCircle,
        color: 'text-green-600 dark:text-green-400',
        bg: 'bg-green-50 dark:bg-green-900/20',
        urgent: false
      }
    }
    
    return {
      type: 'complete',
      title: '全部完成',
      count: 0,
      description: '今日学习完成',
      icon: CheckCircle,
      color: 'text-green-600 dark:text-green-400',
      bg: 'bg-green-50 dark:bg-green-900/20',
      urgent: false
    }
  }

  const handleActionClick = () => {
    const action = getPriorityAction()
    if (!action) return
    
    switch (action.type) {
      case 'review':
      case 'relearn':
      case 'continue':
        window.location.href = '/review'
        break
      case 'new':
        window.location.href = '/codetop'
        break
      case 'complete':
        window.location.href = '/analysis'
        break
    }
  }

  if (loading) {
    return (
      <div className={cn(
        "bg-white dark:bg-[#0F0F12] rounded-xl p-6",
        "border border-gray-200 dark:border-[#1F1F23]",
        "animate-pulse",
        className
      )}>
        <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded mb-4"></div>
        <div className="space-y-3">
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded"></div>
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
          <div className="h-12 bg-gray-200 dark:bg-gray-700 rounded"></div>
        </div>
      </div>
    )
  }

  const retentionRate = getRetentionRate()
  const memoryStrength = getMemoryStrength()
  const priorityAction = getPriorityAction()

  return (
    <div className={cn(
      "bg-white dark:bg-[#0F0F12] rounded-xl p-6",
      "border border-gray-200 dark:border-[#1F1F23]",
      "shadow-sm transition-all duration-200 hover:shadow-md",
      className
    )}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
            <Brain className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              FSRS 学习状态
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              智能间隔重复学习系统
            </p>
          </div>
        </div>
        
        {analyticsData && (
          <div className="text-right">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">
              {analyticsData.totalCards}
            </div>
            <div className="text-xs text-gray-600 dark:text-gray-400">
              总卡片数
            </div>
          </div>
        )}
      </div>

      {/* FSRS Metrics */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <div className="p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-4 h-4 text-green-600 dark:text-green-400" />
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
              保持率
            </span>
          </div>
          <div className="text-xl font-bold text-gray-900 dark:text-white">
            {retentionRate}%
          </div>
        </div>
        
        <div className="p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
              记忆强度
            </span>
          </div>
          <div className="text-xl font-bold text-gray-900 dark:text-white">
            {memoryStrength}%
          </div>
        </div>
      </div>

      {/* Card Distribution */}
      {analyticsData && (
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
              卡片分布
            </span>
            <span className="text-xs text-gray-600 dark:text-gray-400">
              共 {analyticsData.totalCards} 张
            </span>
          </div>
          
          <div className="grid grid-cols-4 gap-2 text-center">
            <div className="p-2 bg-green-50 dark:bg-green-900/20 rounded">
              <div className="text-sm font-bold text-green-700 dark:text-green-300">
                {analyticsData.newCards}
              </div>
              <div className="text-xs text-green-600 dark:text-green-400">新</div>
            </div>
            
            <div className="p-2 bg-blue-50 dark:bg-blue-900/20 rounded">
              <div className="text-sm font-bold text-blue-700 dark:text-blue-300">
                {analyticsData.learningCards}
              </div>
              <div className="text-xs text-blue-600 dark:text-blue-400">学习</div>
            </div>
            
            <div className="p-2 bg-purple-50 dark:bg-purple-900/20 rounded">
              <div className="text-sm font-bold text-purple-700 dark:text-purple-300">
                {analyticsData.reviewCards}
              </div>
              <div className="text-xs text-purple-600 dark:text-purple-400">复习</div>
            </div>
            
            <div className="p-2 bg-orange-50 dark:bg-orange-900/20 rounded">
              <div className="text-sm font-bold text-orange-700 dark:text-orange-300">
                {analyticsData.relearningCards}
              </div>
              <div className="text-xs text-orange-600 dark:text-orange-400">重学</div>
            </div>
          </div>
        </div>
      )}

      {/* Priority Action */}
      {priorityAction && (
        <div className={cn(
          "p-4 rounded-lg border-2 transition-all duration-200",
          priorityAction.bg,
          priorityAction.urgent 
            ? "border-red-200 dark:border-red-800 ring-1 ring-red-200 dark:ring-red-800" 
            : "border-transparent"
        )}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <priorityAction.icon className={cn("w-5 h-5", priorityAction.color)} />
              <div>
                <div className="font-medium text-gray-900 dark:text-white">
                  {priorityAction.title}
                  {priorityAction.count > 0 && (
                    <span className={cn("ml-2 text-sm", priorityAction.color)}>
                      ({priorityAction.count})
                    </span>
                  )}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-400">
                  {priorityAction.description}
                </div>
              </div>
            </div>
            
            <button
              onClick={handleActionClick}
              className={cn(
                "px-4 py-2 rounded-lg font-medium text-sm",
                "transition-all duration-200",
                "hover:shadow-sm transform hover:-translate-y-0.5",
                "active:translate-y-0",
                priorityAction.urgent
                  ? "bg-red-600 hover:bg-red-700 text-white"
                  : "bg-gray-900 dark:bg-white text-white dark:text-gray-900 hover:bg-gray-800 dark:hover:bg-gray-100"
              )}
            >
              开始
            </button>
          </div>
        </div>
      )}

      {/* Quick Stats */}
      {analyticsData && (
        <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div>
              <div className="text-lg font-bold text-gray-900 dark:text-white">
                {analyticsData.dueCards}
              </div>
              <div className="text-xs text-gray-600 dark:text-gray-400">
                待复习
              </div>
            </div>
            
            <div>
              <div className="text-lg font-bold text-gray-900 dark:text-white">
                {analyticsData.avgReviews?.toFixed(1) || '0.0'}
              </div>
              <div className="text-xs text-gray-600 dark:text-gray-400">
                平均复习次数
              </div>
            </div>
            
            <div>
              <div className="text-lg font-bold text-gray-900 dark:text-white">
                {analyticsData.avgDifficulty?.toFixed(1) || '0.0'}
              </div>
              <div className="text-xs text-gray-600 dark:text-gray-400">
                平均难度
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}