"use client"

import { useState } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Lightbulb, AlertCircle } from "lucide-react"

interface DifficultyModalProps {
  isOpen: boolean
  onClose: () => void
  onNext: (difficulty: string) => void
  problemTitle: string
  isSubmitting?: boolean
  error?: string | null
}

const difficultyOptions = [
  {
    id: "very-hard",
    title: "非常困难",
    description: "完全不会，需要大量学习",
    emoji: "😵",
    bgColor: "bg-red-50 dark:bg-red-900/20",
    borderColor: "border-red-200 dark:border-red-800",
    hoverColor: "hover:bg-red-100 dark:hover:bg-red-900/30",
  },
  {
    id: "somewhat-hard",
    title: "有些困难",
    description: "有一定理解，但仍有困难",
    emoji: "😰",
    bgColor: "bg-orange-50 dark:bg-orange-900/20",
    borderColor: "border-orange-200 dark:border-orange-800",
    hoverColor: "hover:bg-orange-100 dark:hover:bg-orange-900/30",
  },
  {
    id: "moderate",
    title: "适中正常",
    description: "能够理解和解答",
    emoji: "😊",
    bgColor: "bg-yellow-50 dark:bg-yellow-900/20",
    borderColor: "border-yellow-200 dark:border-yellow-800",
    hoverColor: "hover:bg-yellow-100 dark:hover:bg-yellow-900/30",
  },
  {
    id: "easy",
    title: "很容易",
    description: "很容易解答，掌握很好",
    emoji: "😄",
    bgColor: "bg-green-50 dark:bg-green-900/20",
    borderColor: "border-green-200 dark:border-green-800",
    hoverColor: "hover:bg-green-100 dark:hover:bg-green-900/30",
  },
]

export default function DifficultyModal({ isOpen, onClose, onNext, problemTitle, isSubmitting, error }: DifficultyModalProps) {
  const [selectedDifficulty, setSelectedDifficulty] = useState<string>("")

  const handleDifficultySelect = (difficulty: string) => {
    setSelectedDifficulty(difficulty)
    // 直接提交选择的难度
    onNext(difficulty)
  }

  const handleNext = () => {
    if (selectedDifficulty) {
      onNext(selectedDifficulty)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader className="text-center pb-6">
          <DialogTitle className="text-xl font-bold flex items-center justify-center gap-2">
            这道题对你来说有多难？
            <span className="text-2xl">🤔</span>
          </DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {difficultyOptions.map((option) => (
            <button
              key={option.id}
              onClick={() => handleDifficultySelect(option.id)}
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
                {isSubmitting ? "正在提交评估结果..." : "点击任意选项即可完成评估并标记为已完成"}
              </p>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
