"use client"

import { useState } from "react"
import DifficultyModal from "./difficulty-modal"
import { userApi } from "@/lib/user-api"
import { ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth-context"

interface ProblemAssessmentModalProps {
  isOpen: boolean
  onClose: () => void
  problemId: number
  problemTitle: string
  onStatusUpdate?: () => void
}

export default function ProblemAssessmentModal({
  isOpen,
  onClose,
  problemId,
  problemTitle,
  onStatusUpdate,
}: ProblemAssessmentModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { isAuthenticated, user } = useAuth()

  const handleDifficultySubmit = async (difficulty: string) => {
    setIsSubmitting(true)
    setError(null)
    
    // 更稳健的登录判断：仅依赖 isAuthenticated，避免用户对象延迟导致误判
    if (!isAuthenticated) {
      console.log('Authentication check failed:', { isAuthenticated, user })
      setError("请先登录后再进行操作")
      setTimeout(() => {
        window.location.href = '/login'
      }, 2000)
      setIsSubmitting(false)
      return
    }

    try {
      // Map difficulty to mastery level
      const masteryLevel = getDifficultyToMasteryLevel(difficulty)
      
      // Update problem status to done
      await userApi.updateProblemStatus(problemId, {
        status: 'done',
        mastery: masteryLevel,
        notes: `难度评估: ${getDifficultyLabel(difficulty)}`
      })

      console.log("Assessment submitted:", {
        problemId,
        problemTitle,
        difficulty,
        masteryLevel
      })

      // Notify parent component to refresh data
      onStatusUpdate?.()
      
      // Close the modal
      onClose()
    } catch (error) {
      console.error("Failed to update problem status:", error)
      
      let errorMessage = "提交失败，请稍后重试"
      
      if (error instanceof ApiError) {
        if (error.status === 401) {
          errorMessage = "请先登录后再进行操作"
          // Optionally redirect to login
          setTimeout(() => {
            window.location.href = '/login'
          }, 2000)
        } else if (error.status === 403) {
          errorMessage = "您没有权限执行此操作"
        } else if (error.status === 404) {
          errorMessage = "题目不存在"
        } else {
          errorMessage = error.message || errorMessage
        }
      }
      
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  // Map difficulty selection to mastery level (0-3)
  const getDifficultyToMasteryLevel = (difficulty: string): number => {
    switch (difficulty) {
      case "very-hard": return 0
      case "somewhat-hard": return 1
      case "moderate": return 2
      case "easy": return 3
      default: return 1
    }
  }

  // Get difficulty label for notes
  const getDifficultyLabel = (difficulty: string): string => {
    switch (difficulty) {
      case "very-hard": return "非常困难"
      case "somewhat-hard": return "有些困难"
      case "moderate": return "适中正常"
      case "easy": return "很容易"
      default: return "未知"
    }
  }

  return (
    <DifficultyModal
      isOpen={isOpen}
      onClose={onClose}
      onNext={handleDifficultySubmit}
      problemTitle={problemTitle}
      isSubmitting={isSubmitting}
      error={error}
    />
  )
}
