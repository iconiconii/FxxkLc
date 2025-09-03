"use client"

import { useState } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Lightbulb, AlertCircle } from "lucide-react"
import { reviewApi, type SubmitReviewRequest } from "@/lib/review-api"
import { ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth-context"

interface ReviewAssessmentModalProps {
  isOpen: boolean
  onClose: () => void
  problemId: number
  problemTitle: string
  onReviewComplete?: (problemId: number) => void
}

export default function ReviewAssessmentModal({
  isOpen,
  onClose,
  problemId,
  problemTitle,
  onReviewComplete,
}: ReviewAssessmentModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { isAuthenticated, loading } = useAuth()

  const reviewOptions = [
    {
      id: 1,
      title: "再来一次",
      description: "完全忘记了，需要重新学习",
      emoji: "😵",
      bgColor: "bg-red-50 dark:bg-red-900/20",
      borderColor: "border-red-200 dark:border-red-800",
      hoverColor: "hover:bg-red-100 dark:hover:bg-red-900/30",
    },
    {
      id: 2,
      title: "困难",
      description: "想起来了但很困难",
      emoji: "😰",
      bgColor: "bg-orange-50 dark:bg-orange-900/20",
      borderColor: "border-orange-200 dark:border-orange-800",
      hoverColor: "hover:bg-orange-100 dark:hover:bg-orange-900/30",
    },
    {
      id: 3,
      title: "良好",
      description: "正确回忆，稍有犹豫",
      emoji: "😊",
      bgColor: "bg-yellow-50 dark:bg-yellow-900/20",
      borderColor: "border-yellow-200 dark:border-yellow-800",
      hoverColor: "hover:bg-yellow-100 dark:hover:bg-yellow-900/30",
    },
    {
      id: 4,
      title: "容易",
      description: "轻松回忆，完全掌握",
      emoji: "😄",
      bgColor: "bg-green-50 dark:bg-green-900/20",
      borderColor: "border-green-200 dark:border-green-800",
      hoverColor: "hover:bg-green-100 dark:hover:bg-green-900/30",
    },
  ]

  const handleReviewSubmit = async (rating: 1 | 2 | 3 | 4) => {
    setIsSubmitting(true)
    setError(null)
    
    // 等待认证状态加载完成
    if (loading) {
      setIsSubmitting(false)
      setError("正在验证登录状态，请稍候...")
      return
    }
    
    // 仅依赖全局认证状态
    if (!isAuthenticated) {
      console.log('Authentication check failed:', { isAuthenticated, loading })
      setError("请先登录后再进行操作")
      setTimeout(() => {
        window.location.href = '/login'
      }, 2000)
      setIsSubmitting(false)
      return
    }

    try {
      // Submit FSRS review
      const request: SubmitReviewRequest = {
        problemId,
        rating,
        reviewType: 'MANUAL' // User manually initiated review
      }
      
      const result = await reviewApi.submitReview(request)

      console.log("FSRS review submitted:", {
        problemId,
        problemTitle,
        rating,
        result
      })

      // Notify parent component that review is complete
      onReviewComplete?.(problemId)
      
      // Close the modal
      onClose()
    } catch (error) {
      console.error("Failed to submit FSRS review:", error)
      
      let errorMessage = "提交失败，请稍后重试"
      
      if (error instanceof ApiError) {
        if (error.status === 401) {
          errorMessage = "登录已过期，请重新登录"
          setTimeout(() => {
            window.location.href = '/login'
          }, 2000)
        } else if (error.status === 403) {
          errorMessage = "您没有权限执行此操作"
        } else if (error.status === 404) {
          errorMessage = "题目不存在或未加入FSRS系统"
        } else {
          errorMessage = error.message || errorMessage
        }
      }
      
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader className="text-center pb-6">
          <DialogTitle className="text-xl font-bold flex items-center justify-center gap-2">
            这道题复习得怎么样？
            <span className="text-2xl">🤔</span>
          </DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {reviewOptions.map((option) => (
            <button
              key={option.id}
              onClick={() => handleReviewSubmit(option.id as 1 | 2 | 3 | 4)}
              disabled={isSubmitting}
              className={`
                p-6 rounded-xl border-2 transition-all duration-200 text-center
                ${option.bgColor} ${option.borderColor} ${option.hoverColor}
                hover:scale-102 disabled:opacity-50 disabled:cursor-not-allowed
              `}
            >
              <div className="text-4xl mb-3">{option.emoji}</div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{option.title}</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">{option.description}</p>
            </button>
          ))}
        </div>

        {error ? (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 mt-0.5 flex-shrink-0" />
              <p className="text-sm text-red-800 dark:text-red-200">
                {error}
              </p>
            </div>
          </div>
        ) : (
          <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <Lightbulb className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
              <p className="text-sm text-blue-800 dark:text-blue-200">
                {isSubmitting ? "正在提交复习结果..." : "选择符合你实际情况的选项，这将影响下次复习的时间安排"}
              </p>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
