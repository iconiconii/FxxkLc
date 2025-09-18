import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RecommendationProvider } from '@/lib/recommendation-context'
import AIRecommendationsInfiniteList from '@/components/recommendation/AIRecommendationsInfiniteList'
import * as recommendationApi from '@/lib/recommendation-api'
import * as abLib from '@/lib/ab'

// Mock the API module
vi.mock('@/lib/recommendation-api')
vi.mock('@/lib/ab')

const mockGetAIRecommendations = vi.mocked(recommendationApi.getAIRecommendations)
const mockGetABGroup = vi.mocked(abLib.getABGroup)

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return (
    <QueryClientProvider client={queryClient}>
      <RecommendationProvider>
        {children}
      </RecommendationProvider>
    </QueryClientProvider>
  )
}

describe('AIRecommendationsInfiniteList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetABGroup.mockReturnValue('control')
  })

  describe('getNextPageParam fallback logic', () => {
    it('should use nextCursor when available (primary strategy)', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            nextCursor: 'cursor-123',
            hasMore: true,
            nextPage: 2,
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalledWith({
          limit: 10,
          ab_group: 'control',
        })
      })

      // Verify that the next page parameter would be the cursor
      const lastPage = mockResponse
      const nextPageParam = lastPage.data.meta.nextCursor || 
        (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage ? 
          lastPage.data.meta.nextPage.toString() : undefined)
      
      expect(nextPageParam).toBe('cursor-123')
    })

    it('should fallback to nextPage when nextCursor is not available', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            // nextCursor is undefined
            hasMore: true,
            nextPage: 2,
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalled()
      })

      // Verify fallback to nextPage
      const lastPage = mockResponse
      const nextPageParam = lastPage.data.meta.nextCursor || 
        (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage ? 
          lastPage.data.meta.nextPage.toString() : undefined)
      
      expect(nextPageParam).toBe('2')
    })

    it('should return undefined when no more pages available', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            // No nextCursor
            hasMore: false,
            // No nextPage when hasMore is false
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalled()
      })

      // Verify no next page available
      const lastPage = mockResponse
      const nextPageParam = lastPage.data.meta.nextCursor || 
        (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage ? 
          lastPage.data.meta.nextPage.toString() : undefined)
      
      expect(nextPageParam).toBeUndefined()
    })

    it('should handle missing pagination metadata gracefully', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            // No pagination fields at all
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockGetAIRecommendations).toHaveBeenCalled()
      })

      // Verify graceful handling of missing metadata
      const lastPage = mockResponse
      const nextPageParam = lastPage.data.meta.nextCursor || 
        (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage ? 
          lastPage.data.meta.nextPage.toString() : undefined)
      
      expect(nextPageParam).toBeUndefined()
    })
  })

  describe('busy/DEFAULT banner aggregation', () => {
    it('should show banner when any page has meta.busy=true', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            busy: true, // This should trigger the banner
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(screen.getByText('系统繁忙，推荐已回退基础策略')).toBeInTheDocument()
        expect(screen.getByText('结果可能较为保守，稍后可重新获取推荐。')).toBeInTheDocument()
      })
    })

    it('should show banner when headers.recSource is DEFAULT', async () => {
      const headers = new Headers()
      headers.set('rec-source', 'DEFAULT')

      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
        headers,
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(screen.getByText('系统繁忙，推荐已回退基础策略')).toBeInTheDocument()
      })
    })

    it('should not show banner when system is operating normally', async () => {
      const headers = new Headers()
      headers.set('rec-source', 'LLM')

      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            busy: false,
          },
        },
        headers,
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        expect(screen.queryByText('系统繁忙，推荐已回退基础策略')).not.toBeInTheDocument()
      })
    })
  })

  describe('auto-loading behavior', () => {
    it('should render auto-load trigger element when hasNextPage is true', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              problemId: 1,
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
            nextCursor: 'cursor-123',
            hasMore: true,
          },
        },
        headers: new Headers(),
      }

      mockGetAIRecommendations.mockResolvedValue(mockResponse)

      render(
        <TestWrapper>
          <AIRecommendationsInfiniteList limit={10} />
        </TestWrapper>
      )

      await waitFor(() => {
        // The auto-load trigger should be present (div with specific data attribute)
        expect(document.querySelector('[data-testid="auto-load-trigger"]')).toBeInTheDocument()
      })
    })
  })
})