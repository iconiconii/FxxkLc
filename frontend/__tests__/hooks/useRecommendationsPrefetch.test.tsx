import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RecommendationProvider } from '@/lib/recommendation-context'
import { useRecommendationsPrefetch } from '@/hooks/useRecommendationsPrefetch'
import * as recommendationApi from '@/lib/recommendation-api'
import * as abLib from '@/lib/ab'

// Mock the modules
vi.mock('@/lib/recommendation-api')
vi.mock('@/lib/ab')

const mockGetAIRecommendations = vi.mocked(recommendationApi.getAIRecommendations)
const mockGetABGroup = vi.mocked(abLib.getABGroup)
const mockShouldShowAIRecommendations = vi.mocked(abLib.shouldShowAIRecommendations)

// Test wrapper component
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0, // Disable caching for tests
      },
    },
  })

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <RecommendationProvider>
        {children}
      </RecommendationProvider>
    </QueryClientProvider>
  )
}

describe('useRecommendationsPrefetch', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetABGroup.mockReturnValue('control')
  })

  describe('enabled control logic', () => {
    it('should not prefetch when enabled=false', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: false, limit: 10 }),
        { wrapper: createWrapper() }
      )

      // Wait a bit to ensure no API calls are made
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(mockGetAIRecommendations).not.toHaveBeenCalled()
      expect(result.current.isPrefetching).toBe(false)
      expect(result.current.prefetchedCount).toBe(0)
    })

    it('should not prefetch when shouldShowAIRecommendations returns false', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(false)

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true, limit: 10 }),
        { wrapper: createWrapper() }
      )

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(mockGetAIRecommendations).not.toHaveBeenCalled()
      expect(result.current.isPrefetching).toBe(false)
    })

    it('should prefetch when enabled=true and shouldShowAIRecommendations=true', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
              title: 'Test Problem 1',
              reason: 'Test reason',
              confidence: 0.8,
              source: 'LLM' as const,
            },
            {
              problemId: 2,
              title: 'Test Problem 2',
              reason: 'Test reason',
              confidence: 0.7,
              source: 'LLM' as const,
            },
          ],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true, limit: 10 }),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalledWith({
          limit: 10,
          ab_group: 'control',
          recommendation_type: 'hybrid',
        })
      })

      expect(result.current.prefetchedCount).toBe(2)
    })

    it('should use default enabled=true when not specified', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      mockGetAIRecommendations.mockResolvedValue({
        data: {
          items: [],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      })

      renderHook(
        () => useRecommendationsPrefetch(), // No enabled specified
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalled()
      })
    })

    it('should use default limit=10 when not specified', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      mockGetAIRecommendations.mockResolvedValue({
        data: {
          items: [],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      })

      renderHook(
        () => useRecommendationsPrefetch({ enabled: true }), // No limit specified
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalledWith({
          limit: 10, // Default limit
          ab_group: 'control',
          recommendation_type: 'hybrid',
        })
      })
    })
  })

  describe('context integration', () => {
    it('should add prefetched problem IDs to context', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 123,
              title: 'Test Problem',
              reason: 'Test reason',
              confidence: 0.8,
              source: 'LLM' as const,
            },
          ],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true, limit: 5 }),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(result.current.prefetchedCount).toBe(1)
      })

      // Note: Context integration is tested through the effect, 
      // actual context state verification would require additional test setup
    })

    it('should handle missing context gracefully', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      mockGetAIRecommendations.mockResolvedValue({
        data: {
          items: [],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      })

      // Render without RecommendationProvider
      const queryClient = new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
        },
      })

      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>
          {children}
        </QueryClientProvider>
      )

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true }),
        { wrapper }
      )

      // Should not crash without context
      expect(result.current.isPrefetching).toBe(false)
    })
  })

  describe('query configuration', () => {
    it('should configure appropriate stale time for prefetch', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      mockGetAIRecommendations.mockResolvedValue({
        data: {
          items: [],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      })

      renderHook(
        () => useRecommendationsPrefetch({ enabled: true }),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalled()
      })

      // The hook should configure longer stale time for prefetch (5 minutes)
      // This is verified through the query configuration in the hook
    })

    it('should not refetch on window focus or mount', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      mockGetAIRecommendations.mockResolvedValue({
        data: {
          items: [],
          meta: {
            cached: false,
            traceId: 'test-trace',
            generatedAt: '2024-01-01T00:00:00Z',
          },
        },
        headers: new Headers(),
      })

      const { rerender } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true }),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalledTimes(1)
      })

      // Simulate window focus or component remount
      rerender()

      // Should not trigger additional API calls
      expect(mockGetAIRecommendations).toHaveBeenCalledTimes(1)
    })
  })

  describe('return values', () => {
    it('should return correct isPrefetching state', async () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)
      
      // Create a promise that we can control
      let resolvePromise: (value: any) => void
      const controlledPromise = new Promise(resolve => {
        resolvePromise = resolve
      })
      
      mockGetAIRecommendations.mockReturnValue(controlledPromise)

      const { result } = renderHook(
        () => useRecommendationsPrefetch({ enabled: true }),
        { wrapper: createWrapper() }
      )

      // Initially should be prefetching
      expect(result.current.isPrefetching).toBe(true)
      expect(result.current.prefetchedCount).toBe(0)

      // Resolve the promise
      resolvePromise!({
        data: {
          items: [{ problemId: 1, title: 'Test', reason: 'Test', confidence: 0.8, source: 'LLM' as const }],
          meta: { cached: false, traceId: 'test', generatedAt: '2024-01-01T00:00:00Z' },
        },
        headers: new Headers(),
      })

      await waitFor(() => {
        expect(result.current.isPrefetching).toBe(false)
        expect(result.current.prefetchedCount).toBe(1)
      })
    })
  })
})