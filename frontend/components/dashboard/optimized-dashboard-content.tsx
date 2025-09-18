'use client'

import { useEffect, useState, useCallback } from "react"
import { analyticsApi, type AnalyticsOverview } from "@/lib/analytics-api"
import { cachedFetch, CACHE_KEYS, CACHE_TTL, invalidateCache } from "@/lib/cache-utils"
import FSRSStatusCard from "./fsrs-status-card"
import QuickStats from "./quick-stats"
import ReviewQueuePreview from "./review-queue-preview"
import LearningInsights from "./learning-insights"
import AIRecommendationsSection from "./ai-recommendations"

export default function OptimizedDashboardContent() {
  const [analyticsData, setAnalyticsData] = useState<AnalyticsOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)

  const fetchAnalyticsData = useCallback(async (force: boolean = false) => {
    try {
      if (force) {
        setRefreshing(true)
        invalidateCache([CACHE_KEYS.ANALYTICS_OVERVIEW])
      } else {
        setLoading(true)
      }
      
      // Single API call with caching
      const overview = await cachedFetch(
        CACHE_KEYS.ANALYTICS_OVERVIEW,
        () => analyticsApi.getOverview(),
        CACHE_TTL.ANALYTICS_OVERVIEW
      )

      setAnalyticsData(overview)
      setError(null)
    } catch (err) {
      console.error('Failed to fetch analytics data:', err)
      setError('获取数据失败，请稍后重试')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  // Initial data load
  useEffect(() => {
    fetchAnalyticsData()
  }, [fetchAnalyticsData])

  // Auto-refresh every 5 minutes if dashboard is active
  useEffect(() => {
    let refreshInterval: NodeJS.Timeout | null = null
    
    const startAutoRefresh = () => {
      refreshInterval = setInterval(() => {
        // Only refresh if document is visible to save resources
        if (!document.hidden && analyticsData) {
          fetchAnalyticsData(true)
        }
      }, 5 * 60 * 1000) // 5 minutes
    }

    // Start auto-refresh after initial load
    if (analyticsData) {
      startAutoRefresh()
    }

    // Handle visibility change
    const handleVisibilityChange = () => {
      if (document.hidden) {
        if (refreshInterval) {
          clearInterval(refreshInterval)
          refreshInterval = null
        }
      } else {
        // Refresh when tab becomes active
        if (analyticsData) {
          fetchAnalyticsData(true)
          startAutoRefresh()
        }
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      if (refreshInterval) {
        clearInterval(refreshInterval)
      }
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [analyticsData, fetchAnalyticsData])

  // Handle manual refresh
  const handleRefresh = useCallback(() => {
    fetchAnalyticsData(true)
  }, [fetchAnalyticsData])

  // Loading state
  if (loading && !analyticsData) {
    return (
      <div className="space-y-6">
        {/* Loading Header */}
        <div className="flex items-center justify-between">
          <div>
            <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-48 animate-pulse"></div>
            <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-64 mt-2 animate-pulse"></div>
          </div>
          <div className="w-16 h-8 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
        </div>

        {/* Loading Quick Stats */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="bg-white dark:bg-[#0F0F12] rounded-xl p-4 border border-gray-200 dark:border-[#1F1F23] animate-pulse"
            >
              <div className="h-16"></div>
            </div>
          ))}
        </div>

        {/* Loading Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white dark:bg-[#0F0F12] rounded-xl p-6 border border-gray-200 dark:border-[#1F1F23] animate-pulse">
            <div className="h-64"></div>
          </div>
          <div className="bg-white dark:bg-[#0F0F12] rounded-xl p-6 border border-gray-200 dark:border-[#1F1F23] animate-pulse">
            <div className="h-64"></div>
          </div>
        </div>
      </div>
    )
  }

  // Error state
  if (error && !analyticsData) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="text-red-600 dark:text-red-400 mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            无法加载数据
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            {error}
          </p>
          <button
            onClick={handleRefresh}
            className="px-4 py-2 bg-gray-900 dark:bg-white text-white dark:text-gray-900 rounded-lg hover:bg-gray-800 dark:hover:bg-gray-100 transition-colors"
          >
            重试
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header with refresh */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            学习仪表板
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            基于 FSRS 算法的智能学习系统
          </p>
        </div>
        
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <svg 
            className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} 
            fill="none" 
            viewBox="0 0 24 24" 
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {refreshing ? '更新中...' : '刷新'}
        </button>
      </div>

      {/* Quick Stats Row */}
      <QuickStats 
        analyticsData={analyticsData}
        loading={loading}
      />

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column - Primary Actions */}
        <div className="space-y-6">
          <FSRSStatusCard
            analyticsData={analyticsData}
            loading={loading}
          />
        </div>

        {/* Right Column - Secondary Info */}
        <div className="space-y-6">
          <ReviewQueuePreview
            analyticsData={analyticsData}
          />
          <AIRecommendationsSection />
        </div>
      </div>

      {/* Bottom Section - Expandable Insights */}
      <LearningInsights
        analyticsData={analyticsData}
      />

      {/* Debug Info (Development Only) */}
      {process.env.NODE_ENV === 'development' && (
        <div className="mt-8 p-4 bg-gray-100 dark:bg-gray-800 rounded-lg text-xs text-gray-600 dark:text-gray-400">
          <details>
            <summary className="cursor-pointer font-medium mb-2">调试信息</summary>
            <div className="space-y-1">
              <div>数据加载: {loading ? '加载中' : '完成'}</div>
              <div>刷新状态: {refreshing ? '刷新中' : '空闲'}</div>
              <div>错误状态: {error || '无'}</div>
              <div>总卡片数: {analyticsData?.totalCards || '未知'}</div>
              <div>待复习: {analyticsData?.dueCards || '未知'}</div>
              <div>最后更新: {new Date().toLocaleTimeString()}</div>
            </div>
          </details>
        </div>
      )}
    </div>
  )
}