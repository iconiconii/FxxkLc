'use client'

import { useState, useEffect } from 'react'
import { 
  getRecommendationPreference, 
  setRecommendationPreference,
  shouldShowAIRecommendations 
} from '@/lib/ab'

// UI layer recommendation source type - user-friendly lowercase
// Different from API layer RecommendationSource ('LLM'|'FSRS'|'HYBRID'|'DEFAULT')
export type RecommendationSource = 'fsrs' | 'ai'

interface RecommendationToggleProps {
  value?: RecommendationSource
  onValueChange?: (value: RecommendationSource) => void
  className?: string
  userId?: string | number
}

export default function RecommendationToggle({ 
  value,
  onValueChange,
  className,
  userId
}: RecommendationToggleProps) {
  const [currentValue, setCurrentValue] = useState<RecommendationSource>('fsrs')
  const [isClient, setIsClient] = useState(false)

  // Check if AI recommendations should be available for this user
  const aiEnabled = shouldShowAIRecommendations(userId)

  useEffect(() => {
    setIsClient(true)
    
    // Initialize value from props or localStorage
    const initialValue = value || getRecommendationPreference()
    setCurrentValue(initialValue)
  }, [value])

  const handleValueChange = (newValue: string) => {
    const source = newValue as RecommendationSource
    
    // Only allow AI if user is in the experiment group
    if (source === 'ai' && !aiEnabled) {
      return
    }

    setCurrentValue(source)
    setRecommendationPreference(source)
    onValueChange?.(source)
  }

  // Don't render until client-side to avoid hydration mismatch
  if (!isClient) {
    return (
      <div className={`${className}`}>
        <div className="flex h-9 bg-gray-100 dark:bg-gray-800 rounded-md p-1 animate-pulse">
          <div className="flex-1 h-7 bg-gray-200 dark:bg-gray-700 rounded-sm mx-0.5"></div>
          <div className="flex-1 h-7 bg-gray-200 dark:bg-gray-700 rounded-sm mx-0.5"></div>
        </div>
      </div>
    )
  }

  return (
    <div className={`${className}`}>
      <div className="flex bg-gray-100 dark:bg-gray-800 rounded-lg p-1">
        <button
          onClick={() => handleValueChange('fsrs')}
          className={`flex-1 px-3 py-1.5 text-sm font-medium rounded-md transition-all ${
            currentValue === 'fsrs'
              ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
          }`}
        >
          FSRS 推荐
        </button>
        
        <button
          onClick={() => handleValueChange('ai')}
          disabled={!aiEnabled}
          className={`flex-1 px-3 py-1.5 text-sm font-medium rounded-md transition-all ${
            currentValue === 'ai'
              ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
              : aiEnabled
                ? 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
                : 'text-gray-400 dark:text-gray-500 cursor-not-allowed'
          }`}
          title={!aiEnabled ? '该功能仅对部分用户开放' : undefined}
        >
          <div className="flex items-center justify-center gap-1">
            AI 推荐
            {aiEnabled && (
              <span className="text-xs bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 px-1.5 py-0.5 rounded">
                实验
              </span>
            )}
          </div>
        </button>
      </div>
      
      {!aiEnabled && (
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-2 text-center">
          AI 推荐功能正在实验中，仅对部分用户开放
        </p>
      )}
    </div>
  )
}