'use client'

import React, { useEffect, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAIRecommendations } from '@/lib/recommendation-api'
import { useOptionalRecommendationContext } from '@/lib/recommendation-context'
import { getABGroup, shouldShowAIRecommendations } from '@/lib/ab'

interface PrefetchOptions {
  enabled?: boolean
  limit?: number
}

/**
 * Hook to prefetch a small number of AI recommendations for badge display
 * This ensures badges appear immediately on problem lists without requiring
 * users to visit the recommendations page first
 * 
 * Only runs when enabled=true and user is authenticated
 */
export function useRecommendationsPrefetch({ 
  enabled = true, 
  limit = 10 
}: PrefetchOptions = {}) {
  const recommendationContext = useOptionalRecommendationContext()
  
  // Only prefetch if explicitly enabled, has context, and user should see AI recommendations
  // This ensures we don't make API calls for unauthenticated users
  const shouldPrefetch = enabled && 
    !!recommendationContext && 
    shouldShowAIRecommendations()
  
  const { data: response } = useQuery({
    queryKey: ['ai-recommendations-prefetch', limit],
    queryFn: () => getAIRecommendations({
      limit,
      ab_group: getABGroup(),
      recommendation_type: 'hybrid', // Use hybrid for best results
    }),
    enabled: shouldPrefetch,
    staleTime: 5 * 60 * 1000, // 5 minutes - longer than regular queries
    gcTime: 10 * 60 * 1000, // 10 minutes
    refetchOnWindowFocus: false,
    refetchOnMount: false, // Don't refetch if already cached
  })

  // Memoize problem IDs to prevent unnecessary re-renders
  const problemIds = useMemo(() => {
    return response?.data?.items?.map(item => item.problemId) || []
  }, [response?.data?.items])

  // Update context with prefetched problem IDs
  useEffect(() => {
    if (recommendationContext && problemIds.length > 0) {
      recommendationContext.addRecommendedProblems(problemIds)
    }
  }, [problemIds, recommendationContext?.addRecommendedProblems])

  return {
    isPrefetching: shouldPrefetch && !response,
    prefetchedCount: response?.data?.items?.length || 0,
  }
}