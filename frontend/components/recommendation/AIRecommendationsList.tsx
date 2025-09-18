'use client'

import { useState, useCallback, useEffect, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import AIRecommendationCard from './AIRecommendationCard'
import { 
  getAIRecommendations 
} from '@/lib/recommendation-api'
import { 
  AIRecommendationRequest, 
  FeedbackAction 
} from '@/types/recommendation'
import { getABGroup } from '@/lib/ab'
import { useOptionalRecommendationContext } from '@/lib/recommendation-context'

interface AIRecommendationsListProps {
  limit?: number
  params?: Omit<AIRecommendationRequest, 'limit' | 'ab_group'>
  onFeedback?: (action: FeedbackAction, success: boolean, problemId: number) => void
  className?: string
  enableInfiniteLoading?: boolean // Future feature flag
}

export default function AIRecommendationsList({ 
  limit = 10, 
  params = {},
  onFeedback,
  className,
  enableInfiniteLoading = false
}: AIRecommendationsListProps) {
  const [toastMessage, setToastMessage] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const recommendationContext = useOptionalRecommendationContext()

  // Memoize query parameters for stability
  const queryParams = useMemo((): AIRecommendationRequest => ({
    limit,
    ab_group: getABGroup(),
    ...params,
  }), [limit, JSON.stringify(params)])

  // React Query for data fetching
  // TODO: Replace with useInfiniteQuery when backend supports pagination
  // const {
  //   data: infiniteData,
  //   fetchNextPage,
  //   hasNextPage,
  //   isFetchingNextPage,
  // } = useInfiniteQuery({
  //   queryKey: ['ai-recommendations-infinite', queryParams],
  //   queryFn: ({ pageParam = 0 }) => getAIRecommendations({ ...queryParams, offset: pageParam }),
  //   getNextPageParam: (lastPage) => lastPage.meta.nextOffset || undefined,
  //   enabled: enableInfiniteLoading,
  // })

  const {
    data: response,
    isLoading,
    error,
    refetch,
    isRefetching
  } = useQuery({
    queryKey: ['ai-recommendations', queryParams],
    queryFn: () => getAIRecommendations(queryParams),
    staleTime: 60 * 1000, // 1 minute
    retry: 1,
    refetchOnWindowFocus: false,
  })

  const items = response?.data?.items || []
  const meta = response?.data?.meta
  const headers = response?.headers

  // Handle feedback with toast messages
  const handleFeedback = useCallback((action: FeedbackAction, success: boolean, problemId: number) => {
    if (success) {
      setToastMessage({ 
        message: '感谢反馈，已记录。', 
        type: 'success' 
      })
    } else {
      setToastMessage({ 
        message: '提交失败，请稍后重试。', 
        type: 'error' 
      })
    }

    // Auto-hide toast after 3 seconds
    setTimeout(() => setToastMessage(null), 3000)

    onFeedback?.(action, success, problemId)
  }, [onFeedback])

  // Clear toast when query params change - use stable dependencies
  useEffect(() => {
    setToastMessage(null)
  }, [queryParams])

  // Extract problem IDs with memoization to prevent unnecessary re-renders
  const currentProblemIds = useMemo(() => {
    return items.map(item => item.problemId)
  }, [items])

  // Update recommendation context with current problem IDs
  // Only depend on the stable addRecommendedProblems function, not the entire context
  const addRecommendedProblems = recommendationContext?.addRecommendedProblems
  useEffect(() => {
    if (addRecommendedProblems && currentProblemIds.length > 0) {
      addRecommendedProblems(currentProblemIds)
    }
  }, [currentProblemIds, addRecommendedProblems])

  // Loading skeleton
  if (isLoading) {
    return (
      <div className={`space-y-4 ${className}`}>
        {Array.from({ length: Math.min(limit, 5) }).map((_, index) => (
          <Card key={index} className="p-4 animate-pulse">
            <div className="space-y-3">
              {/* Title and badges */}
              <div className="flex justify-between items-start">
                <div className="space-y-2 flex-1">
                  <div className="h-5 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                  <div className="flex gap-2">
                    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-12"></div>
                    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                  </div>
                </div>
                <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
              </div>
              
              {/* Reason */}
              <div className="space-y-2">
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-2/3"></div>
              </div>
              
              {/* Progress and buttons */}
              <div className="space-y-3">
                <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded"></div>
                <div className="flex gap-2">
                  <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                  <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                  <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    )
  }

  // Error state
  if (error && !items.length) {
    return (
      <div className={`${className}`}>
        <Card className="p-6 text-center">
          <div className="text-red-600 dark:text-red-400 mb-4">
            <svg className="w-12 h-12 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            获取推荐失败
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            {error instanceof Error ? error.message : '网络错误，请检查连接'}
          </p>
          <Button
            onClick={() => refetch()}
            disabled={isRefetching}
            className="flex items-center gap-2"
          >
            {isRefetching && (
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            )}
            重试
          </Button>
        </Card>
      </div>
    )
  }

  // Empty state
  if (!isLoading && items.length === 0) {
    return (
      <div className={`${className}`}>
        <Card className="p-6 text-center">
          <div className="text-gray-400 dark:text-gray-500 mb-4">
            <svg className="w-12 h-12 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            暂无推荐
          </h3>
          <p className="text-gray-600 dark:text-gray-400">
            暂无可用推荐，稍后再试或调整偏好。
          </p>
        </Card>
      </div>
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Busy/fallback notice */}
      {(meta?.busy || headers?.recSource === 'DEFAULT') && (
        <Card className="p-4 bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800">
          <div className="flex items-center gap-3">
            <svg className="w-5 h-5 text-yellow-600 dark:text-yellow-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div className="text-sm">
              <p className="text-yellow-800 dark:text-yellow-200 font-medium">
                系统繁忙，推荐已回退基础策略
              </p>
              <p className="text-yellow-700 dark:text-yellow-300">
                结果可能较为保守，稍后可重新获取推荐。
              </p>
            </div>
          </div>
        </Card>
      )}

      {/* Recommendation cards */}
      {items.map((item, index) => (
        <AIRecommendationCard
          key={`${item.problemId}-${index}`}
          item={item}
          traceId={meta?.traceId || ''}
          cacheHit={headers?.cacheHit || false}
          onFeedback={(action, success) => handleFeedback(action, success, item.problemId)}
        />
      ))}

      {/* Refresh button (instead of misleading "load more") */}
      {items.length > 0 && (
        <div className="text-center pt-4">
          <Button
            variant="outline"
            onClick={() => refetch()}
            disabled={isRefetching}
            className="flex items-center gap-2"
          >
            {isRefetching && (
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            )}
            {isRefetching ? '刷新中...' : '刷新推荐'}
          </Button>
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">
            刷新以获取新的推荐结果
          </p>
        </div>
      )}

      {/* Toast notification */}
      {toastMessage && (
        <div className="fixed bottom-4 right-4 z-50">
          <Card className={`p-4 shadow-lg ${
            toastMessage.type === 'success' 
              ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800' 
              : 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
          }`}>
            <div className="flex items-center gap-3">
              <svg 
                className={`w-5 h-5 ${
                  toastMessage.type === 'success' 
                    ? 'text-green-600 dark:text-green-400' 
                    : 'text-red-600 dark:text-red-400'
                }`} 
                fill="none" 
                viewBox="0 0 24 24" 
                stroke="currentColor"
              >
                {toastMessage.type === 'success' ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                )}
              </svg>
              <span className={`text-sm font-medium ${
                toastMessage.type === 'success' 
                  ? 'text-green-800 dark:text-green-200' 
                  : 'text-red-800 dark:text-red-200'
              }`}>
                {toastMessage.message}
              </span>
            </div>
          </Card>
        </div>
      )}

      {/* Debug info in development */}
      {process.env.NODE_ENV === 'development' && meta && (
        <Card className="p-4 bg-gray-50 dark:bg-gray-900/50">
          <details>
            <summary className="cursor-pointer text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              调试信息
            </summary>
            <div className="text-xs text-gray-600 dark:text-gray-400 space-y-1">
              <div className="grid grid-cols-2 gap-2">
                <div>TraceId: {meta.traceId}</div>
                <div>来源: {headers?.recSource || 'Unknown'}</div>
                <div>缓存命中: {headers?.cacheHit ? '是' : '否'}</div>
                <div>结果数量: {items.length}</div>
                {headers?.providerChain && <div className="col-span-2">Provider链: {headers.providerChain}</div>}
                <div className="col-span-2">生成时间: {new Date(meta.generatedAt).toLocaleString()}</div>
                {items.length > 0 && items[0].latencyMs && (
                  <div className="col-span-2">平均延迟: {Math.round(items.reduce((acc, item) => acc + (item.latencyMs || 0), 0) / items.length)}ms</div>
                )}
              </div>
            </div>
          </details>
        </Card>
      )}
    </div>
  )
}