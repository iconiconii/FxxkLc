'use client'

import React from 'react'
import { Badge } from '@/components/ui/badge'
import { useOptionalRecommendationContext } from '@/lib/recommendation-context'

interface AIBadgeProps {
  problemId: number
  className?: string
  size?: 'sm' | 'default'
}

export default function AIBadge({ problemId, className, size = 'sm' }: AIBadgeProps) {
  const recommendationContext = useOptionalRecommendationContext()
  
  // If no context is available or problem is not recommended, don't show badge
  if (!recommendationContext || !recommendationContext.isRecommended(problemId)) {
    return null
  }

  return (
    <Badge 
      variant="default" 
      className={`${size === 'sm' ? 'text-xs px-1.5 py-0.5' : ''} bg-blue-600 dark:bg-blue-500 text-white hover:bg-blue-700 dark:hover:bg-blue-600 ${className}`}
      title="AI推荐题目"
    >
      AI
    </Badge>
  )
}