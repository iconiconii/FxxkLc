'use client'

import { cn } from "@/lib/utils"
import { 
  BarChart3, 
  TrendingUp, 
  Brain, 
  Zap,
  Target,
  AlertCircle,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Settings,
  Lightbulb,
  Clock
} from "lucide-react"
import { useState } from "react"
import type { AnalyticsOverview } from "@/lib/analytics-api"

interface LearningInsightsProps {
  analyticsData: AnalyticsOverview | null
  className?: string
}

interface Insight {
  id: string
  title: string
  value: string | number
  description: string
  type: 'positive' | 'warning' | 'info' | 'suggestion'
  icon: React.ComponentType<{ className?: string }>
  action?: {
    label: string
    onClick: () => void
  }
}

export default function LearningInsights({ 
  analyticsData, 
  className 
}: LearningInsightsProps) {
  const [isExpanded, setIsExpanded] = useState(false)

  const generateInsights = (): Insight[] => {
    if (!analyticsData) return []

    const insights: Insight[] = []
    const {
      totalCards,
      newCards,
      learningCards,
      reviewCards,
      relearningCards,
      dueCards,
      avgReviews,
      avgDifficulty,
      avgStability,
      totalLapses
    } = analyticsData

    // FSRS Algorithm Performance
    if (avgStability !== undefined && avgStability > 0) {
      insights.push({
        id: 'stability',
        title: '记忆稳定性',
        value: avgStability.toFixed(1),
        description: avgStability > 10 
          ? '记忆稳定性优秀，长期保持效果很好'
          : avgStability > 5
          ? '记忆稳定性良好，可以适当延长复习间隔'
          : '记忆稳定性较低，建议增加复习频率',
        type: avgStability > 10 ? 'positive' : avgStability > 5 ? 'info' : 'warning',
        icon: Brain
      })
    }

    // Difficulty Analysis
    if (avgDifficulty !== undefined && avgDifficulty > 0) {
      insights.push({
        id: 'difficulty',
        title: '平均难度系数',
        value: avgDifficulty.toFixed(2),
        description: avgDifficulty < 5
          ? '题目掌握程度较好，可以挑战更难的题目'
          : avgDifficulty < 7
          ? '难度适中，保持当前学习节奏'
          : '题目难度较高，建议多做巩固练习',
        type: avgDifficulty < 5 ? 'positive' : avgDifficulty < 7 ? 'info' : 'warning',
        icon: Target,
        action: avgDifficulty > 7 ? {
          label: '优化参数',
          onClick: () => window.location.href = '/analysis'
        } : undefined
      })
    }

    // Lapse Rate Analysis  
    if (totalCards > 0 && totalLapses !== undefined) {
      const lapseRate = (totalLapses / totalCards) * 100
      insights.push({
        id: 'lapse_rate',
        title: '错误率',
        value: `${lapseRate.toFixed(1)}%`,
        description: lapseRate < 10
          ? '错误率很低，学习效果优秀'
          : lapseRate < 20
          ? '错误率正常，继续保持'
          : '错误率偏高，建议调整学习策略',
        type: lapseRate < 10 ? 'positive' : lapseRate < 20 ? 'info' : 'warning',
        icon: BarChart3
      })
    }

    // Learning Progress
    if (totalCards > 0) {
      const progressRate = ((reviewCards + learningCards) / totalCards) * 100
      insights.push({
        id: 'progress',
        title: '学习进度',
        value: `${progressRate.toFixed(1)}%`,
        description: progressRate > 80
          ? '学习进度优秀，即将完成当前学习计划'
          : progressRate > 50
          ? '学习进度良好，保持当前节奏'
          : '学习进度较慢，建议增加学习时间',
        type: progressRate > 80 ? 'positive' : progressRate > 50 ? 'info' : 'suggestion',
        icon: TrendingUp
      })
    }

    // Review Efficiency
    if (avgReviews !== undefined && avgReviews > 0) {
      insights.push({
        id: 'efficiency',
        title: '复习效率',
        value: avgReviews.toFixed(1),
        description: avgReviews < 2
          ? '复习效率很高，一次就能掌握大部分内容'
          : avgReviews < 3
          ? '复习效率良好，学习方法有效'
          : '需要多次复习才能掌握，建议优化学习方法',
        type: avgReviews < 2 ? 'positive' : avgReviews < 3 ? 'info' : 'suggestion',
        icon: Zap,
        action: avgReviews >= 3 ? {
          label: '查看建议',
          onClick: () => window.location.href = '/analysis'
        } : undefined
      })
    }

    // Workload Balance
    if (dueCards > 0) {
      const workloadStatus = dueCards > 20 ? 'heavy' : dueCards > 10 ? 'moderate' : 'light'
      insights.push({
        id: 'workload',
        title: '当前工作量',
        value: workloadStatus === 'heavy' ? '繁重' : workloadStatus === 'moderate' ? '适中' : '轻松',
        description: workloadStatus === 'heavy'
          ? '待复习题目较多，建议分批完成避免疲劳'
          : workloadStatus === 'moderate'
          ? '工作量适中，可以一次性完成'
          : '工作量较轻，可以学习一些新题目',
        type: workloadStatus === 'heavy' ? 'warning' : workloadStatus === 'moderate' ? 'info' : 'positive',
        icon: Clock,
        action: workloadStatus === 'heavy' ? {
          label: '开始复习',
          onClick: () => window.location.href = '/review'
        } : workloadStatus === 'light' ? {
          label: '学习新题',
          onClick: () => window.location.href = '/codetop'
        } : undefined
      })
    }

    return insights
  }

  const getTypeStyles = (type: string) => {
    switch (type) {
      case 'positive':
        return {
          bg: 'bg-green-50 dark:bg-green-900/20',
          border: 'border-green-200 dark:border-green-800',
          icon: 'text-green-600 dark:text-green-400',
          title: 'text-green-800 dark:text-green-200'
        }
      case 'warning':
        return {
          bg: 'bg-red-50 dark:bg-red-900/20',
          border: 'border-red-200 dark:border-red-800',
          icon: 'text-red-600 dark:text-red-400',
          title: 'text-red-800 dark:text-red-200'
        }
      case 'suggestion':
        return {
          bg: 'bg-blue-50 dark:bg-blue-900/20',
          border: 'border-blue-200 dark:border-blue-800',
          icon: 'text-blue-600 dark:text-blue-400',
          title: 'text-blue-800 dark:text-blue-200'
        }
      default:
        return {
          bg: 'bg-gray-50 dark:bg-gray-800/50',
          border: 'border-gray-200 dark:border-gray-700',
          icon: 'text-gray-600 dark:text-gray-400',
          title: 'text-gray-800 dark:text-gray-200'
        }
    }
  }

  const insights = generateInsights()

  return (
    <div className={cn(
      "bg-white dark:bg-[#0F0F12] rounded-xl",
      "border border-gray-200 dark:border-[#1F1F23]",
      "transition-all duration-200",
      className
    )}>
      {/* Header */}
      <div 
        className="flex items-center justify-between p-6 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="flex items-center gap-3">
          <div className="p-2 bg-purple-50 dark:bg-purple-900/20 rounded-lg">
            <BarChart3 className="w-5 h-5 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              学习洞察
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              FSRS 算法分析与建议
            </p>
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          {insights.length > 0 && (
            <div className="px-2 py-1 bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 text-xs font-medium rounded-full">
              {insights.length} 项洞察
            </div>
          )}
          {isExpanded ? (
            <ChevronDown className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          ) : (
            <ChevronRight className="w-5 h-5 text-gray-600 dark:text-gray-400" />
          )}
        </div>
      </div>

      {/* Expanded Content */}
      {isExpanded && (
        <div className="px-6 pb-6 border-t border-gray-200 dark:border-gray-700">
          {insights.length > 0 ? (
            <div className="space-y-4 mt-4">
              {insights.map((insight) => {
                const styles = getTypeStyles(insight.type)
                return (
                  <div
                    key={insight.id}
                    className={cn(
                      "p-4 rounded-lg border",
                      styles.bg,
                      styles.border
                    )}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3 flex-1">
                        <insight.icon className={cn("w-5 h-5 mt-0.5", styles.icon)} />
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <h4 className={cn("font-medium", styles.title)}>
                              {insight.title}
                            </h4>
                            <span className="text-lg font-bold text-gray-900 dark:text-white">
                              {insight.value}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {insight.description}
                          </p>
                        </div>
                      </div>
                      
                      {insight.action && (
                        <button
                          onClick={insight.action.onClick}
                          className={cn(
                            "px-3 py-1.5 text-xs font-medium rounded-lg",
                            "transition-all duration-200 hover:shadow-sm",
                            "transform hover:-translate-y-0.5 active:translate-y-0",
                            insight.type === 'positive'
                              ? "bg-green-600 hover:bg-green-700 text-white"
                              : insight.type === 'warning'
                              ? "bg-red-600 hover:bg-red-700 text-white"
                              : "bg-blue-600 hover:bg-blue-700 text-white"
                          )}
                        >
                          {insight.action.label}
                        </button>
                      )}
                    </div>
                  </div>
                )
              })}
              
              {/* Additional Actions */}
              <div className="flex items-center justify-between pt-4 border-t border-gray-200 dark:border-gray-700">
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <Lightbulb className="w-4 h-4" />
                  <span>基于 FSRS v4.5+ 算法分析</span>
                </div>
                
                <div className="flex gap-2">
                  <button
                    onClick={() => window.location.href = '/analysis'}
                    className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
                  >
                    <Settings className="w-4 h-4" />
                    详细分析
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-600 dark:text-gray-400">
              <Brain className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p className="mb-2">暂无学习洞察</p>
              <p className="text-sm">完成一些复习后将显示个性化分析</p>
            </div>
          )}
        </div>
      )}
      
      {/* Collapsed Preview */}
      {!isExpanded && insights.length > 0 && (
        <div className="px-6 pb-6">
          <div className="flex items-center gap-4 text-sm">
            {insights.slice(0, 3).map((insight) => {
              const styles = getTypeStyles(insight.type)
              return (
                <div key={insight.id} className="flex items-center gap-2">
                  <insight.icon className={cn("w-4 h-4", styles.icon)} />
                  <span className="text-gray-600 dark:text-gray-400">
                    {insight.title}: <span className="font-medium text-gray-900 dark:text-white">{insight.value}</span>
                  </span>
                </div>
              )
            })}
            {insights.length > 3 && (
              <span className="text-gray-500 dark:text-gray-500">
                +{insights.length - 3} 更多
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  )
}