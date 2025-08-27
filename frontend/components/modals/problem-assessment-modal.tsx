"use client"

import { useState } from "react"
import DifficultyModal from "./difficulty-modal"
import { userApi } from "@/lib/user-api"
import { ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth-context"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { NoteEditor, NoteViewer, PublicNotesList } from '@/components/notes'
import { useNotes } from '@/hooks/use-notes'

interface ProblemAssessmentModalProps {
  isOpen: boolean
  onClose: () => void
  problemId: number
  problemTitle: string
  onStatusUpdate?: (problemId: number, newStatus: string, newMastery: number) => void
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
  const [activeTab, setActiveTab] = useState('assessment')
  const [editingNote, setEditingNote] = useState(false)
  const { isAuthenticated, user, loading } = useAuth()
  
  // Notes functionality
  const { 
    userNote, 
    isLoading: isNotesLoading, 
    createOrUpdateNote, 
    deleteNote,
    isCreating
  } = useNotes(problemId)

  const handleDifficultySubmit = async (difficulty: string) => {
    setIsSubmitting(true)
    setError(null)
    
    // 等待认证状态加载完成
    if (loading) {
      setIsSubmitting(false)
      setError("正在验证登录状态，请稍候...")
      return
    }
    
    // 更稳健的登录判断：同时检查token和认证状态
    const hasToken = typeof window !== 'undefined' && !!localStorage.getItem('accessToken')
    
    if (!isAuthenticated && !hasToken) {
      console.log('Authentication check failed:', { isAuthenticated, hasToken, user, loading })
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
      const result = await userApi.updateProblemStatus(problemId, {
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

      // Notify parent component to update local state with actual result
      onStatusUpdate?.(problemId, result.status, result.mastery)
      
      // Close the modal
      onClose()
    } catch (error) {
      console.error("Failed to update problem status:", error)
      
      let errorMessage = "提交失败，请稍后重试"
      
      if (error instanceof ApiError) {
        if (error.status === 401) {
          errorMessage = "登录已过期，请重新登录"
          // Clear local tokens on 401
          if (typeof window !== 'undefined') {
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('userInfo')
          }
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
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{problemTitle}</DialogTitle>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="assessment">题目评估</TabsTrigger>
            <TabsTrigger value="notes">我的笔记</TabsTrigger>
            <TabsTrigger value="public-notes">社区笔记</TabsTrigger>
          </TabsList>
          
          <TabsContent value="assessment" className="mt-4">
            <div className="space-y-6">
              <div className="text-center pb-4">
                <h3 className="text-xl font-bold flex items-center justify-center gap-2">
                  这道题对你来说有多难？
                  <span className="text-2xl">🤔</span>
                </h3>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[
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
                ].map((option) => (
                  <button
                    key={option.id}
                    onClick={() => handleDifficultySubmit(option.id)}
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
                <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                  <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
                </div>
              ) : (
                <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
                  <p className="text-sm text-blue-800 dark:text-blue-200">
                    {isSubmitting ? "正在提交评估结果..." : "点击任意选项即可完成评估并标记为已完成"}
                  </p>
                </div>
              )}
            </div>
          </TabsContent>
          
          <TabsContent value="notes" className="mt-4 space-y-4">
            {editingNote ? (
              <NoteEditor
                initialNote={userNote || undefined}
                problemId={problemId}
                onSave={async (noteData) => {
                  await createOrUpdateNote(noteData);
                  setEditingNote(false);
                }}
                onCancel={() => setEditingNote(false)}
              />
            ) : userNote ? (
              <div className="space-y-4">
                <NoteViewer
                  note={userNote}
                  showEdit={true}
                  onEdit={() => setEditingNote(true)}
                  onShare={() => {
                    // TODO: Implement share functionality
                    navigator.clipboard.writeText(window.location.href);
                  }}
                />
                <div className="flex justify-end">
                  <Button 
                    variant="outline" 
                    onClick={() => deleteNote(userNote.id)}
                    size="sm"
                  >
                    删除笔记
                  </Button>
                </div>
              </div>
            ) : (
              <div className="text-center py-8">
                <p className="text-gray-500 dark:text-gray-400 mb-4">还没有笔记</p>
                <Button onClick={() => setEditingNote(true)}>
                  创建笔记
                </Button>
              </div>
            )}
            
            {isNotesLoading && (
              <div className="text-center py-4">
                <p className="text-gray-500">加载中...</p>
              </div>
            )}
          </TabsContent>
          
          <TabsContent value="public-notes" className="mt-4">
            <PublicNotesList problemId={problemId} />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  )
}
