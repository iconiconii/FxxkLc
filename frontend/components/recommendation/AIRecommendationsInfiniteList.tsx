'use client'

import { useState, useCallback, useEffect, useMemo, useRef } from 'react'
import { useInfiniteQuery } from '@tanstack/react-query'
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

interface AIRecommendationsInfiniteListProps {
  pageSize?: number
  params?: Omit<AIRecommendationRequest, 'limit' | 'ab_group' | 'cursor' | 'page' | 'offset'>
  onFeedback?: (action: FeedbackAction, success: boolean, problemId: number) => void
  className?: string
  enableAutoLoad?: boolean // Auto-load when near bottom
}

export default function AIRecommendationsInfiniteList({ 
  pageSize = 10, 
  params = {},
  onFeedback,
  className,
  enableAutoLoad = true
}: AIRecommendationsInfiniteListProps) {
  const [toastMessage, setToastMessage] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const loadMoreRef = useRef<HTMLDivElement>(null)
  const recommendationContext = useOptionalRecommendationContext()

  // Memoize query parameters for stability
  const baseParams = useMemo((): Omit<AIRecommendationRequest, 'cursor' | 'page' | 'offset'> => ({
    limit: pageSize,
    ab_group: getABGroup(),
    ...params,
  }), [pageSize, JSON.stringify(params)])

  // Infinite Query for data fetching
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    error,
    refetch,
    isRefetching
  } = useInfiniteQuery({
    queryKey: ['ai-recommendations-infinite', baseParams],
    initialPageParam: null,
    queryFn: ({ pageParam }: { pageParam: string | null }) => {
      if (!pageParam) {
        return getAIRecommendations(baseParams)
      }
      
      // If pageParam is a number, treat as page-based pagination
      if (typeof pageParam === 'string' && /^\d+$/.test(pageParam)) {
        return getAIRecommendations({ ...baseParams, page: parseInt(pageParam) })
      }
      
      // Otherwise treat as cursor-based pagination
      return getAIRecommendations({ ...baseParams, cursor: pageParam })
    },
    getNextPageParam: (lastPage) => {
      // Prefer cursor-based pagination
      if (lastPage.data.meta.nextCursor) {
        return lastPage.data.meta.nextCursor
      }
      
      // Fallback to page-based pagination
      if (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage) {
        return lastPage.data.meta.nextPage.toString()
      }
      
      // No more pages
      return undefined
    },
    staleTime: 60 * 1000, // 1 minute
    retry: 1,
    refetchOnWindowFocus: false,
  })

  // Flatten all pages into a single array
  const allItems = useMemo(() => {
    return data?.pages?.flatMap(page => page.data.items) || []
  }, [data])

  // Get metadata from the first page
  const firstPage = data?.pages?.[0]
  const meta = firstPage?.data?.meta
  const headers = firstPage?.headers

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

  // Clear toast when query params change
  useEffect(() => {
    setToastMessage(null)
  }, [baseParams])

  // Update recommendation context with current problem IDs
  useEffect(() => {
    if (recommendationContext && allItems.length > 0) {
      const problemIds = allItems.map(item => item.problemId)
      recommendationContext.addRecommendedProblems(problemIds)
    }
  }, [allItems, recommendationContext])

  // IntersectionObserver for auto-loading
  useEffect(() => {
    if (!enableAutoLoad || !loadMoreRef.current || !hasNextPage || isFetchingNextPage) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries
        if (entry.isIntersecting) {
          fetchNextPage()
        }
      },
      {
        threshold: 0.1,
        rootMargin: '100px', // Start loading 100px before reaching the element
      }
    )

    observer.observe(loadMoreRef.current)

    return () => {
      observer.disconnect()
    }
  }, [enableAutoLoad, hasNextPage, isFetchingNextPage, fetchNextPage])

  // Loading skeleton
  if (isLoading) {
    return (
      <div className={`space-y-4 ${className}`}>
        {Array.from({ length: Math.min(pageSize, 5) }).map((_, index) => (
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
  if (error && !allItems.length) {
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
  if (!isLoading && allItems.length === 0) {
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

  // Aggregate busy/default across all loaded pages
  const anyBusy = (data?.pages?.some(p => p?.data?.meta?.busy) ?? false) || !!meta?.busy
  const anyDefault = (data?.pages?.some(p => p?.headers?.recSource === 'DEFAULT') ?? false) || headers?.recSource === 'DEFAULT'

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Busy/fallback notice */}
      {(anyBusy || anyDefault) && (
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
      {allItems.map((item, index) => (
        <AIRecommendationCard
          key={`${item.problemId}-${index}`}
          item={item}
          traceId={meta?.traceId || ''}
          cacheHit={headers?.cacheHit || false}
          onFeedback={(action, success) => handleFeedback(action, success, item.problemId)}
        />
      ))}

      {/* Load more section */}
      {hasNextPage && (
        <div 
          ref={loadMoreRef}
          className="text-center pt-4"
        >
          {enableAutoLoad ? (
            // Auto-loading indicator
            <div className="flex items-center justify-center gap-2 text-gray-500 dark:text-gray-400">
              {isFetchingNextPage ? (
                <>
                  <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span className="text-sm">正在加载更多...</span>
                </>
              ) : (
                <span className="text-sm">滚动到底部加载更多</span>
              )}
            </div>
          ) : (
            // Manual load more button
            <Button
              variant="outline"
              onClick={() => fetchNextPage()}
              disabled={isFetchingNextPage}
              className="flex items-center gap-2"
            >
              {isFetchingNextPage && (
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              )}
              {isFetchingNextPage ? '加载中...' : '加载更多推荐'}
            </Button>
          )}
        </div>
      )}

      {/* End indicator */}
      {!hasNextPage && allItems.length > 0 && (
        <div className="text-center pt-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            已显示全部 {allItems.length} 个推荐
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
              调试信息 (无限加载)
            </summary>
            <div className="text-xs text-gray-600 dark:text-gray-400 space-y-1">
              <div className="grid grid-cols-2 gap-2">
                <div>TraceId: {meta.traceId}</div>
                <div>来源: {headers?.recSource || 'Unknown'}</div>
                <div>缓存命中: {headers?.cacheHit ? '是' : '否'}</div>
                <div>已加载页数: {data?.pages?.length || 0}</div>
                <div>总结果数量: {allItems.length}</div>
                <div>有更多: {hasNextPage ? '是' : '否'}</div>
                <div className="col-span-2">任一页Busy: {anyBusy ? '是' : '否'}，任一页默认源: {anyDefault ? '是' : '否'}</div>
                {headers?.providerChain && <div className="col-span-2">Provider链: {headers.providerChain}</div>}
                <div className="col-span-2">生成时间: {new Date(meta.generatedAt).toLocaleString()}</div>
                {allItems.length > 0 && allItems[0].latencyMs && (
                  <div className="col-span-2">平均延迟: {Math.round(allItems.reduce((acc, item) => acc + (item.latencyMs || 0), 0) / allItems.length)}ms</div>
                )}
              </div>
            </div>
          </details>
        </Card>
      )}
    </div>
  )
}
