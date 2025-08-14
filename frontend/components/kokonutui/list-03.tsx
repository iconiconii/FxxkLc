import { cn } from "@/lib/utils"
import {
  Target,
  type LucideIcon,
  ArrowRight,
  CheckCircle2,
  Timer,
  AlertCircle,
  Code,
  TrendingUp,
  Trophy,
} from "lucide-react"
import React from "react"

interface StudyPlan {
  id: string
  title: string
  subtitle: string
  icon: LucideIcon
  iconStyle: string
  deadline: string
  progress: number
  status: "pending" | "in-progress" | "completed"
  difficulty: "beginner" | "intermediate" | "advanced"
  estimatedTime: string
}

interface List03Props {
  plans?: StudyPlan[]
  className?: string
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

const STUDY_PLANS: StudyPlan[] = [
  {
    id: "1",
    title: "数组基础专项",
    subtitle: "掌握数组操作的核心技巧",
    icon: Code,
    iconStyle: "algorithm",
    deadline: "本周内完成",
    progress: 75,
    status: "in-progress",
    difficulty: "beginner",
    estimatedTime: "8小时",
  },
  {
    id: "2",
    title: "链表进阶训练",
    subtitle: "深入理解链表操作与优化",
    icon: TrendingUp,
    iconStyle: "datastructure",
    deadline: "下周完成",
    progress: 30,
    status: "in-progress",
    difficulty: "intermediate",
    estimatedTime: "12小时",
  },
  {
    id: "3",
    title: "面试模拟冲刺",
    subtitle: "大厂面试真题集训",
    icon: Trophy,
    iconStyle: "interview",
    deadline: "月底前完成",
    progress: 0,
    status: "pending",
    difficulty: "advanced",
    estimatedTime: "20小时",
  },
]

export default function List03({ plans = STUDY_PLANS, className }: List03Props) {
  return (
    <div className={cn("w-full overflow-x-auto scrollbar-none", className)}>
      <div className="flex gap-3 min-w-full p-1">
        {plans.map((plan) => (
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
              >
                {plan.status === "pending" ? "开始学习" : plan.status === "completed" ? "查看详情" : "继续学习"}
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
