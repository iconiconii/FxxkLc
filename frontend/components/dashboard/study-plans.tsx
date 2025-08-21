'use client'

import { cn } from "@/lib/utils"
import {
  Target,
  ArrowRight,
  CheckCircle2,
  Timer,
  AlertCircle,
  Code,
  TrendingUp,
  Trophy,
} from "lucide-react"
import React from "react"
import type { AnalyticsOverview } from "@/lib/analytics-api"
import type { ReviewQueueCard } from "@/lib/review-api"

interface StudyPlansProps {
  analyticsData: AnalyticsOverview | null
  reviewQueue: ReviewQueueCard[]
  className?: string
}

interface StudyPlan {
  id: string
  title: string
  subtitle: string
  icon: React.ComponentType<{ className?: string }>
  iconStyle: string
  deadline: string
  progress: number
  status: "pending" | "in-progress" | "completed"
  difficulty: "beginner" | "intermediate" | "advanced"
  estimatedTime: string
}

const iconStyles = {
  algorithm: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
  datastructure: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
  interview: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
}

const statusConfig = {
  pending: {
    icon: Timer,
    class: "text-amber-600 dark:text-amber-400",
    bg: "bg-amber-100 dark:bg-amber-900/30",
    label: "待开始",
  },
  "in-progress": {
    icon: AlertCircle,
    class: "text-blue-600 dark:text-blue-400",
    bg: "bg-blue-100 dark:bg-blue-900/30",
    label: "进行中",
  },
  completed: {
    icon: CheckCircle2,
    class: "text-emerald-600 dark:text-emerald-400",
    bg: "bg-emerald-100 dark:bg-emerald-900/30",
    label: "已完成",
  },
}

const difficultyColors = {
  beginner: "text-green-600 dark:text-green-400",
  intermediate: "text-orange-600 dark:text-orange-400",
  advanced: "text-red-600 dark:text-red-400",
}

const difficultyLabels = {
  beginner: "初级",
  intermediate: "中级",
  advanced: "高级",
}

export default function StudyPlans({ 
  analyticsData, 
  reviewQueue, 
  className 
}: StudyPlansProps) {
  
  // 根据真实数据生成学习计划
  const generateStudyPlans = (): StudyPlan[] => {
    const plans: StudyPlan[] = []
    
    // 基于复习队列的智能计划
    if (reviewQueue.length > 0) {
      const dueCount = reviewQueue.filter(card => new Date(card.dueDate) <= new Date()).length
      const learningCount = reviewQueue.filter(card => card.state === 'LEARNING').length
      const newCount = reviewQueue.filter(card => card.state === 'NEW').length
      
      // 复习计划
      if (dueCount > 0) {
        plans.push({
          id: "review-plan",
          title: "今日复习计划",
          subtitle: `${dueCount} 道题目等待复习`,
          icon: Trophy,
          iconStyle: "interview",
          deadline: "今天完成",
          progress: Math.max(0, Math.round(((reviewQueue.length - dueCount) / reviewQueue.length) * 100)),
          status: dueCount > 5 ? "pending" : "in-progress",
          difficulty: dueCount > 10 ? "advanced" : dueCount > 5 ? "intermediate" : "beginner",
          estimatedTime: `${Math.ceil(dueCount * 8 / 60)}小时`,
        })
      }

      // 学习新题计划  
      if (newCount > 0) {
        plans.push({
          id: "new-learning",
          title: "新题学习计划",
          subtitle: `${newCount} 道新题目待学习`,
          icon: Code,
          iconStyle: "algorithm",
          deadline: "本周完成",
          progress: analyticsData ? Math.round(((analyticsData.learningCards || 0) / Math.max(analyticsData.totalCards, 1)) * 100) : 0,
          status: "in-progress",
          difficulty: "beginner",
          estimatedTime: `${Math.ceil(newCount * 15 / 60)}小时`,
        })
      }

      // 正在学习的题目
      if (learningCount > 0) {
        plans.push({
          id: "learning-progress",
          title: "学习进度跟进",
          subtitle: `${learningCount} 道题目学习中`,
          icon: TrendingUp,
          iconStyle: "datastructure",
          deadline: "持续进行",
          progress: Math.round((learningCount / (learningCount + newCount + 1)) * 100),
          status: "in-progress",
          difficulty: "intermediate",
          estimatedTime: `${Math.ceil(learningCount * 10 / 60)}小时`,
        })
      }
    }

    // 基于分析数据的建议计划
    if (analyticsData) {
      // 如果准确率较低，建议强化练习
      const totalCards = analyticsData.totalCards || 0
      const reviewCards = analyticsData.reviewCards || 0
      const masteryRate = totalCards > 0 ? (reviewCards / totalCards) : 0

      if (masteryRate < 0.7 && totalCards > 0) {
        plans.push({
          id: "mastery-improvement",
          title: "掌握度提升计划",
          subtitle: "针对薄弱环节强化练习",
          icon: Target,
          iconStyle: "algorithm", 
          deadline: "两周内完成",
          progress: Math.round(masteryRate * 100),
          status: "pending",
          difficulty: "intermediate",
          estimatedTime: "10小时",
        })
      }

      // 如果有很多新卡片，建议制定学习计划
      const newCards = analyticsData.newCards || 0
      if (newCards > 20) {
        plans.push({
          id: "systematic-learning",
          title: "系统化学习计划",
          subtitle: `${newCards} 道新题目系统学习`,
          icon: Code,
          iconStyle: "algorithm",
          deadline: "一个月内完成", 
          progress: 0,
          status: "pending",
          difficulty: "beginner",
          estimatedTime: `${Math.ceil(newCards * 12 / 60)}小时`,
        })
      }
    }

    // 默认计划（如果没有数据）
    if (plans.length === 0) {
      plans.push(
        {
          id: "start-journey",
          title: "开始学习之旅",
          subtitle: "创建您的第一个学习计划",
          icon: Code,
          iconStyle: "algorithm",
          deadline: "立即开始",
          progress: 0,
          status: "pending",
          difficulty: "beginner",
          estimatedTime: "1小时",
        },
        {
          id: "daily-practice",
          title: "每日练习习惯",
          subtitle: "建立持续的学习节奏",
          icon: Target,
          iconStyle: "interview",
          deadline: "养成习惯",
          progress: 0,
          status: "pending",
          difficulty: "intermediate",
          estimatedTime: "30分钟/天",
        }
      )
    }

    return plans.slice(0, 3) // 最多显示3个计划
  }

  const studyPlans = generateStudyPlans()

  return (
    <div className={cn("w-full overflow-x-auto scrollbar-none", className)}>
      <div className="flex gap-3 min-w-full p-1">
        {studyPlans.map((plan) => (
          <div
            key={plan.id}
            className={cn(
              "flex flex-col",
              "w-[280px] shrink-0",
              "bg-white dark:bg-zinc-900/70",
              "rounded-xl",
              "border border-zinc-100 dark:border-zinc-800",
              "hover:border-zinc-200 dark:hover:border-zinc-700",
              "transition-all duration-200",
              "shadow-sm backdrop-blur-xl",
            )}
          >
            <div className="p-4 space-y-3">
              <div className="flex items-start justify-between">
                <div className={cn("p-2 rounded-lg", iconStyles[plan.iconStyle as keyof typeof iconStyles])}>
                  <plan.icon className="w-4 h-4" />
                </div>
                <div
                  className={cn(
                    "px-2 py-1 rounded-full text-xs font-medium flex items-center gap-1.5",
                    statusConfig[plan.status].bg,
                    statusConfig[plan.status].class,
                  )}
                >
                  {React.createElement(statusConfig[plan.status].icon, { className: "w-3.5 h-3.5" })}
                  {statusConfig[plan.status].label}
                </div>
              </div>

              <div>
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{plan.title}</h3>
                  <span className={cn("text-xs font-medium", difficultyColors[plan.difficulty])}>
                    {difficultyLabels[plan.difficulty]}
                  </span>
                </div>
                <p className="text-xs text-zinc-600 dark:text-zinc-400 line-clamp-2">{plan.subtitle}</p>
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-zinc-600 dark:text-zinc-400">完成进度</span>
                  <span className="text-zinc-900 dark:text-zinc-100">{plan.progress}%</span>
                </div>
                <div className="h-1.5 bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-zinc-900 dark:bg-zinc-100 rounded-full transition-all duration-300"
                    style={{ width: `${plan.progress}%` }}
                  />
                </div>
              </div>

              <div className="flex items-center justify-between text-xs text-zinc-600 dark:text-zinc-400">
                <div className="flex items-center gap-1.5">
                  <Target className="w-3.5 h-3.5" />
                  <span>{plan.deadline}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Timer className="w-3.5 h-3.5" />
                  <span>{plan.estimatedTime}</span>
                </div>
              </div>
            </div>

            <div className="mt-auto border-t border-zinc-100 dark:border-zinc-800">
              <button
                className={cn(
                  "w-full flex items-center justify-center gap-2",
                  "py-2.5 px-3",
                  "text-xs font-medium",
                  "text-zinc-600 dark:text-zinc-400",
                  "hover:text-zinc-900 dark:hover:text-zinc-100",
                  "hover:bg-zinc-100 dark:hover:bg-zinc-800/50",
                  "transition-colors duration-200",
                )}
                onClick={() => {
                  // 根据计划类型跳转到相应页面
                  if (plan.id.includes('review')) {
                    window.location.href = '/review'
                  } else if (plan.id.includes('learning') || plan.id.includes('new')) {
                    window.location.href = '/codetop'
                  } else {
                    window.location.href = '/analysis'
                  }
                }}
              >
                {plan.status === "pending" ? "开始学习" : 
                 plan.status === "completed" ? "查看详情" : "继续学习"}
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}