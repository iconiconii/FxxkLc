'use client'

import { useState, useCallback } from 'react'
import Link from 'next/link'
import { Card, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import RecommendationToggle, { RecommendationSource } from '../recommendation/RecommendationToggle'
import AIRecommendationsList from '../recommendation/AIRecommendationsList'
import { FeedbackAction } from '@/types/recommendation'

interface AIRecommendationsSectionProps {
  userId?: string | number
  className?: string
}

export default function AIRecommendationsSection({ 
  userId,
  className 
}: AIRecommendationsSectionProps) {
  const [recommendationSource, setRecommendationSource] = useState<RecommendationSource>('ai')
  const [feedbackCount, setFeedbackCount] = useState(0)

  const handleSourceChange = useCallback((source: RecommendationSource) => {
    setRecommendationSource(source)
  }, [])

  const handleFeedback = useCallback((action: FeedbackAction, success: boolean, problemId: number) => {
    if (success) {
      setFeedbackCount(prev => prev + 1)
    }
  }, [])

  // Don't show if user selected FSRS only
  if (recommendationSource === 'fsrs') {
    return null
  }

  return (
    <Card className={`${className}`}>
      <CardHeader className="border-b border-gray-100 dark:border-gray-800">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              智能推荐
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              基于AI分析的个性化题目推荐
            </p>
          </div>
          
          <Link href="/dashboard/recommendations">
            <Button variant="ghost" size="sm" className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300">
              查看全部
              <svg className="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Button>
          </Link>
        </div>

        {/* Toggle */}
        <div className="mt-4">
          <RecommendationToggle
            value={recommendationSource}
            onValueChange={handleSourceChange}
            userId={userId}
          />
        </div>
      </CardHeader>

      <div className="p-6">
        {recommendationSource === 'ai' ? (
          <AIRecommendationsList
            limit={5} // Show fewer items in dashboard
            params={{
              recommendation_type: 'hybrid', // Use hybrid approach for better results
            }}
            onFeedback={handleFeedback}
          />
        ) : (
          <div className="text-center py-6">
            <div className="text-gray-400 dark:text-gray-500 mb-3">
              <svg className="w-10 h-10 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <h4 className="font-medium text-gray-900 dark:text-white mb-2">
              FSRS 传统模式
            </h4>
            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              当前使用间隔重复算法。尝试 AI 推荐获得更智能的个性化建议！
            </p>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setRecommendationSource('ai')}
              className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
            >
              体验 AI 推荐
              <svg className="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </Button>
          </div>
        )}
      </div>

      {/* Feedback stats in development */}
      {process.env.NODE_ENV === 'development' && feedbackCount > 0 && (
        <div className="px-6 pb-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-900 rounded px-2 py-1">
            已收集 {feedbackCount} 个反馈
          </div>
        </div>
      )}
    </Card>
  )
}