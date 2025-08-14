import { cn } from "@/lib/utils"
import { CheckCircle, XCircle, Clock, Code, type LucideIcon, ArrowRight } from "lucide-react"

interface PracticeRecord {
  id: string
  title: string
  difficulty: "easy" | "medium" | "hard"
  status: "solved" | "attempted" | "reviewing"
  timestamp: string
  timeSpent: string
  category: string
  icon: LucideIcon
}

interface List02Props {
  records?: PracticeRecord[]
  className?: string
}

const categoryStyles = {
  array: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
  linkedlist: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
  tree: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
  dp: "bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100",
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

const PRACTICE_RECORDS: PracticeRecord[] = [
  {
    id: "1",
    title: "两数之和",
    difficulty: "easy",
    status: "solved",
    timestamp: "刚刚",
    timeSpent: "15分钟",
    category: "array",
    icon: Code,
  },
  {
    id: "2",
    title: "无重复字符的最长子串",
    difficulty: "medium",
    status: "solved",
    timestamp: "1小时前",
    timeSpent: "32分钟",
    category: "array",
    icon: Code,
  },
  {
    id: "3",
    title: "反转链表",
    difficulty: "easy",
    status: "reviewing",
    timestamp: "2小时前",
    timeSpent: "25分钟",
    category: "linkedlist",
    icon: Code,
  },
  {
    id: "4",
    title: "LRU缓存机制",
    difficulty: "medium",
    status: "attempted",
    timestamp: "昨天",
    timeSpent: "45分钟",
    category: "design",
    icon: Code,
  },
  {
    id: "5",
    title: "最大子序和",
    difficulty: "easy",
    status: "solved",
    timestamp: "昨天",
    timeSpent: "20分钟",
    category: "dp",
    icon: Code,
  },
  {
    id: "6",
    title: "三数之和",
    difficulty: "medium",
    status: "reviewing",
    timestamp: "2天前",
    timeSpent: "38分钟",
    category: "array",
    icon: Code,
  },
]

export default function List02({ records = PRACTICE_RECORDS, className }: List02Props) {
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
            <span className="text-xs font-normal text-zinc-600 dark:text-zinc-400 ml-1">(最近{records.length}题)</span>
          </h2>
          <span className="text-xs text-zinc-600 dark:text-zinc-400">本周</span>
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
                  <record.icon className="w-4 h-4 text-zinc-900 dark:text-zinc-100" />
                </div>

                <div className="flex-1 flex items-center justify-between min-w-0">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <h3 className="text-xs font-medium text-zinc-900 dark:text-zinc-100 truncate">{record.title}</h3>
                      <span className={cn("text-[10px] font-medium", difficultyColors[record.difficulty])}>
                        {record.difficulty === "easy" ? "简单" : record.difficulty === "medium" ? "中等" : "困难"}
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
        >
          <span>查看全部练习记录</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  )
}
