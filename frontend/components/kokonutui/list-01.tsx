import { cn } from "@/lib/utils"
import { TrendingUp, Target, Code, Trophy, Plus, ArrowRight, CheckCircle, Clock } from "lucide-react"

interface ProgressItem {
  id: string
  title: string
  description?: string
  progress: string
  type: "algorithm" | "data-structure" | "system-design" | "interview"
  completed: number
  total: number
}

interface List01Props {
  totalProgress?: string
  progressItems?: ProgressItem[]
  className?: string
}

const PROGRESS_ITEMS: ProgressItem[] = [
  {
    id: "1",
    title: "数组与字符串",
    description: "基础算法题目",
    progress: "75%",
    type: "algorithm",
    completed: 45,
    total: 60,
  },
  {
    id: "2",
    title: "链表操作",
    description: "链表相关题目",
    progress: "60%",
    type: "data-structure",
    completed: 18,
    total: 30,
  },
  {
    id: "3",
    title: "动态规划",
    description: "DP经典题目",
    progress: "40%",
    type: "algorithm",
    completed: 12,
    total: 30,
  },
  {
    id: "4",
    title: "系统设计",
    description: "高级面试题目",
    progress: "25%",
    type: "system-design",
    completed: 5,
    total: 20,
  },
  {
    id: "5",
    title: "面试模拟",
    description: "模拟面试练习",
    progress: "80%",
    type: "interview",
    completed: 8,
    total: 10,
  },
]

export default function List01({
  totalProgress = "总体进度 65%",
  progressItems = PROGRESS_ITEMS,
  className,
}: List01Props) {
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
        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">{totalProgress}</h1>
        <div className="mt-2 w-full bg-zinc-200 dark:bg-zinc-700 rounded-full h-2">
          <div className="bg-blue-600 h-2 rounded-full" style={{ width: "65%" }}></div>
        </div>
      </div>

      {/* Progress List */}
      <div className="p-3">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-xs font-medium text-zinc-900 dark:text-zinc-100">学习模块</h2>
        </div>

        <div className="space-y-1">
          {progressItems.map((item) => (
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
          >
            <CheckCircle className="w-3.5 h-3.5" />
            <span>测试</span>
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
          >
            <ArrowRight className="w-3.5 h-3.5" />
            <span>更多</span>
          </button>
        </div>
      </div>
    </div>
  )
}
