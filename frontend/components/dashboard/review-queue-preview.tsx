'use client'

import { cn } from "@/lib/utils"
import { 
  Clock, 
  AlertTriangle, 
  CheckCircle,
  ArrowRight,
  Brain,
  Timer,
  Star,
  TrendingDown,
  TrendingUp
} from "lucide-react"
import { useState } from "react"
import { reviewApi } from "@/lib/review-api"
import type { AnalyticsOverview } from "@/lib/analytics-api"

interface ReviewQueuePreviewProps {
  analyticsData: AnalyticsOverview | null
  className?: string
}

interface PreviewCard {
  id: number
  title: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  state: 'NEW' | 'LEARNING' | 'REVIEW' | 'RELEARNING'
  priority: number
  intervalDays: number
  isOverdue: boolean
  stability: number
  difficultyScore: number
}

export default function ReviewQueuePreview({ 
  analyticsData, 
  className 
}: ReviewQueuePreviewProps) {
  const [queueCards, setQueueCards] = useState<PreviewCard[]>([])
  const [loading, setLoading] = useState(false)
  const [showPreview, setShowPreview] = useState(false)

  const loadPreviewCards = async () => {
    if (!analyticsData || analyticsData.dueCards === 0) return
    
    try {
      setLoading(true)
      const cards = await reviewApi.getAllDueProblems(5) // Get top 5 due cards
      
      const previewCards: PreviewCard[] = cards.slice(0, 4).map(card => ({
        id: card.id,
        title: card.problemTitle,
        difficulty: card.problemDifficulty,
        state: card.state,
        priority: card.priority,
        intervalDays: card.intervalDays,
        isOverdue: card.overdue,
        stability: card.stability,
        difficultyScore: card.difficulty
      }))
      
      setQueueCards(previewCards)
    } catch (error) {
      console.error('Failed to load preview cards:', error)
    } finally {
      setLoading(false)
    }
  }

  const handlePreviewHover = () => {
    if (!showPreview && queueCards.length === 0 && !loading) {
      setShowPreview(true)
      loadPreviewCards()
    }
  }

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY':
        return 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20'
      case 'MEDIUM':
        return 'text-orange-600 dark:text-orange-400 bg-orange-50 dark:bg-orange-900/20'
      case 'HARD':
        return 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20'
      default:
        return 'text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-900/20'
    }
  }

  const getStateInfo = (state: string, isOverdue: boolean) => {
    if (isOverdue) {
      return {
        icon: AlertTriangle,
        label: '逾期',
        color: 'text-red-600 dark:text-red-400',
        urgent: true
      }
    }
    
    switch (state) {
      case 'NEW':
        return {
          icon: Star,
          label: '新题',
          color: 'text-blue-600 dark:text-blue-400',
          urgent: false
        }
      case 'LEARNING':
        return {
          icon: Brain,
          label: '学习中',
          color: 'text-purple-600 dark:text-purple-400',
          urgent: false
        }
      case 'REVIEW':
        return {
          icon: CheckCircle,
          label: '复习',
          color: 'text-green-600 dark:text-green-400',
          urgent: false
        }
      case 'RELEARNING':
        return {
          icon: TrendingDown,
          label: '重学',
          color: 'text-orange-600 dark:text-orange-400',
          urgent: true
        }
      default:
        return {
          icon: Clock,
          label: '待处理',
          color: 'text-gray-600 dark:text-gray-400',
          urgent: false
        }
    }
  }

  const getStabilityIndicator = (stability: number) => {
    if (stability > 10) {
      return {
        icon: TrendingUp,
        label: '强',
        color: 'text-green-600 dark:text-green-400'
      }
    } else if (stability > 5) {
      return {
        icon: TrendingUp,
        label: '中',
        color: 'text-yellow-600 dark:text-yellow-400'
      }
    } else {
      return {
        icon: TrendingDown,
        label: '弱',
        color: 'text-red-600 dark:text-red-400'
      }
    }
  }

  // Don't render if no due cards
  if (!analyticsData || analyticsData.dueCards === 0) {
    return (
      <div className={cn(
        "bg-white dark:bg-[#0F0F12] rounded-xl p-6",
        "border border-gray-200 dark:border-[#1F1F23]",
        className
      )}>
        <div className="text-center py-8">
          <CheckCircle className="w-12 h-12 text-green-600 dark:text-green-400 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            全部完成！
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            今天的复习任务已经完成了
          </p>
          <button
            onClick={() => window.location.href = '/codetop'}
            className="px-4 py-2 bg-gray-900 dark:bg-white text-white dark:text-gray-900 rounded-lg hover:bg-gray-800 dark:hover:bg-gray-100 transition-colors"
          >
            学习新题目
          </button>
        </div>
      </div>
    )
  }

  return (
    <div 
      className={cn(
        "bg-white dark:bg-[#0F0F12] rounded-xl p-6",
        "border border-gray-200 dark:border-[#1F1F23]",
        "transition-all duration-200 hover:shadow-md",
        className
      )}
      onMouseEnter={handlePreviewHover}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-red-50 dark:bg-red-900/20 rounded-lg">
            <Clock className="w-5 h-5 text-red-600 dark:text-red-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              复习队列预览
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {analyticsData.dueCards} 道题目等待复习
            </p>
          </div>
        </div>
        
        <div className="text-right">
          <div className="text-2xl font-bold text-red-600 dark:text-red-400">
            {analyticsData.dueCards}
          </div>
          <div className="text-xs text-gray-600 dark:text-gray-400">
            待复习
          </div>
        </div>
      </div>

      {/* Preview Cards */}
      <div className="space-y-3 mb-6">
        {loading ? (
          // Loading state
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg animate-pulse">
              <div className="w-8 h-8 bg-gray-200 dark:bg-gray-700 rounded"></div>
              <div className="flex-1">
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
              </div>
            </div>
          ))
        ) : queueCards.length > 0 ? (
          // Actual preview cards
          queueCards.map((card) => {
            const stateInfo = getStateInfo(card.state, card.isOverdue)
            const stabilityInfo = getStabilityIndicator(card.stability)
            const difficultyColor = getDifficultyColor(card.difficulty)
            
            return (
              <div
                key={card.id}
                className={cn(
                  "flex items-center gap-3 p-3 rounded-lg",
                  "border-l-4 transition-all duration-200",
                  "hover:bg-gray-50 dark:hover:bg-gray-800/50",
                  stateInfo.urgent 
                    ? "border-red-500 bg-red-50/50 dark:bg-red-900/10" 
                    : "border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-800/30"
                )}
              >
                <div className="flex items-center gap-2">
                  <stateInfo.icon className={cn("w-4 h-4", stateInfo.color)} />
                  {stateInfo.urgent && (
                    <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
                  )}
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <h4 className="font-medium text-gray-900 dark:text-white truncate">
                      {card.title}
                    </h4>
                    <span className={cn("px-2 py-0.5 rounded text-xs font-medium", difficultyColor)}>
                      {card.difficulty === 'EASY' ? '简单' : 
                       card.difficulty === 'MEDIUM' ? '中等' : '困难'}
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-gray-600 dark:text-gray-400">
                    <span>{stateInfo.label}</span>
                    <span>间隔: {card.intervalDays}天</span>
                    <div className="flex items-center gap-1">
                      <stabilityInfo.icon className={cn("w-3 h-3", stabilityInfo.color)} />
                      <span className={stabilityInfo.color}>记忆: {stabilityInfo.label}</span>
                    </div>
                  </div>
                </div>
                
                <div className="text-right">
                  <div className="text-sm font-medium text-gray-900 dark:text-white">
                    #{card.priority}
                  </div>
                  <div className="text-xs text-gray-600 dark:text-gray-400">
                    优先级
                  </div>
                </div>
              </div>
            )
          })
        ) : showPreview ? (
          // No cards message
          <div className="text-center py-6 text-gray-600 dark:text-gray-400">
            <Clock className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p>暂无可预览的复习卡片</p>
          </div>
        ) : (
          // Hover prompt
          <div className="text-center py-6 text-gray-600 dark:text-gray-400">
            <Timer className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p>悬停查看即将复习的题目</p>
          </div>
        )}
      </div>

      {/* Action Button */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-200 dark:border-gray-700">
        <div className="text-sm text-gray-600 dark:text-gray-400">
          {queueCards.length > 0 ? `显示前 ${queueCards.length} 个` : ''}
        </div>
        
        <button
          onClick={() => window.location.href = '/review'}
          className={cn(
            "flex items-center gap-2 px-4 py-2 rounded-lg",
            "font-medium text-sm transition-all duration-200",
            "transform hover:-translate-y-0.5 active:translate-y-0",
            analyticsData.dueCards > 10
              ? "bg-red-600 hover:bg-red-700 text-white shadow-sm hover:shadow-md"
              : "bg-gray-900 dark:bg-white text-white dark:text-gray-900 hover:bg-gray-800 dark:hover:bg-gray-100"
          )}
        >
          开始复习
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}