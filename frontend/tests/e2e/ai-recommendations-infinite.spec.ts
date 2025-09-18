import { test, expect, Page } from '@playwright/test'
import { AuthHelper } from './fixtures/auth-helper'

test.describe('AI Recommendations Infinite Loading', () => {
  let authHelper: AuthHelper

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page)
    
    // Mock the AI recommendations API with pagination support
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const url = new URL(route.request().url())
      const cursor = url.searchParams.get('cursor')
      const page_num = url.searchParams.get('page')
      const limit = parseInt(url.searchParams.get('limit') || '10')

      // Simulate different pagination scenarios
      let response
      if (cursor === 'cursor-page-2') {
        // Second page with nextCursor
        response = {
          data: {
            items: Array.from({ length: limit }, (_, i) => ({
              problemId: 100 + i,
              title: `Problem ${100 + i}`,
              reason: `This problem helps with advanced algorithms (page 2, item ${i + 1})`,
              confidence: 0.75 + (i * 0.01),
              source: 'LLM',
              difficulty: 'medium',
              topics: ['Dynamic Programming', 'Trees']
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-2',
              generatedAt: new Date().toISOString(),
              nextCursor: 'cursor-page-3',
              hasMore: true,
              totalItems: 50
            }
          }
        }
      } else if (cursor === 'cursor-page-3') {
        // Third page - final page
        response = {
          data: {
            items: Array.from({ length: 5 }, (_, i) => ({
              problemId: 200 + i,
              title: `Problem ${200 + i}`,
              reason: `Final batch of recommendations (page 3, item ${i + 1})`,
              confidence: 0.65 + (i * 0.02),
              source: 'LLM',
              difficulty: 'hard',
              topics: ['Graph Theory', 'Backtracking']
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-3',
              generatedAt: new Date().toISOString(),
              // No nextCursor - end of results
              hasMore: false,
              totalItems: 50
            }
          }
        }
      } else if (page_num === '2') {
        // Fallback to page-based pagination
        response = {
          data: {
            items: Array.from({ length: limit }, (_, i) => ({
              problemId: 300 + i,
              title: `Problem ${300 + i} (Page-based)`,
              reason: `Problem using page-based pagination (page 2, item ${i + 1})`,
              confidence: 0.80 + (i * 0.01),
              source: 'HYBRID',
              difficulty: 'easy',
              topics: ['Arrays', 'Strings']
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-based-2',
              generatedAt: new Date().toISOString(),
              hasMore: true,
              nextPage: 3,
              totalItems: 30
            }
          }
        }
      } else {
        // First page
        response = {
          data: {
            items: Array.from({ length: limit }, (_, i) => ({
              problemId: i + 1,
              title: `Problem ${i + 1}`,
              reason: `This problem is recommended based on your learning progress (item ${i + 1})`,
              confidence: 0.85 + (i * 0.001),
              source: 'LLM',
              difficulty: i % 3 === 0 ? 'easy' : i % 3 === 1 ? 'medium' : 'hard',
              topics: ['Arrays', 'Two Pointers', 'Hash Table'],
              latencyMs: 150 + Math.random() * 100
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-1',
              generatedAt: new Date().toISOString(),
              nextCursor: 'cursor-page-2',
              hasMore: true,
              totalItems: 50
            }
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'LLM',
          'cache-hit': 'false'
        },
        body: JSON.stringify(response)
      })
    })

    await authHelper.loginWithValidCredentials()
  })

  test('should load more items when scrolling to bottom (nextCursor strategy)', async ({ page }) => {
    // Navigate to recommendations page with infinite loading enabled
    await page.goto('/dashboard/recommendations')
    
    // Wait for initial load and verify first page items
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    await expect(page.getByText('Problem 1')).toBeVisible()
    
    // Enable infinite loading mode
    await page.click('[data-testid="infinite-toggle"]')
    await expect(page.getByText('无限滚动模式')).toBeVisible()

    // Scroll to trigger next page load
    await page.evaluate(() => {
      const trigger = document.querySelector('[data-testid="auto-load-trigger"]')
      if (trigger) {
        trigger.scrollIntoView({ behavior: 'smooth' })
      }
    })

    // Wait for second page to load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(20)
    await expect(page.getByText('Problem 100')).toBeVisible()
    await expect(page.getByText('This problem helps with advanced algorithms (page 2, item 1)')).toBeVisible()

    // Scroll again to load final page
    await page.evaluate(() => {
      const trigger = document.querySelector('[data-testid="auto-load-trigger"]')
      if (trigger) {
        trigger.scrollIntoView({ behavior: 'smooth' })
      }
    })

    // Wait for third page to load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(25) // 10 + 10 + 5
    await expect(page.getByText('Problem 200')).toBeVisible()
    await expect(page.getByText('Final batch of recommendations')).toBeVisible()

    // Verify no more loading trigger exists
    await expect(page.locator('[data-testid="auto-load-trigger"]')).not.toBeVisible()
    await expect(page.getByText('已加载全部推荐')).toBeVisible()
  })

  test('should handle page-based pagination fallback', async ({ page }) => {
    // Mock API to only return page-based pagination
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const url = new URL(route.request().url())
      const page_num = parseInt(url.searchParams.get('page') || '1')
      const limit = parseInt(url.searchParams.get('limit') || '10')

      const response = {
        data: {
          items: Array.from({ length: limit }, (_, i) => ({
            problemId: (page_num - 1) * limit + i + 1,
            title: `Page-based Problem ${(page_num - 1) * limit + i + 1}`,
            reason: `Using page-based pagination (page ${page_num}, item ${i + 1})`,
            confidence: 0.75,
            source: 'FSRS',
            difficulty: 'medium',
            topics: ['Math', 'Logic']
          })),
          meta: {
            cached: false,
            traceId: `trace-page-${page_num}`,
            generatedAt: new Date().toISOString(),
            // No nextCursor - only page-based
            hasMore: page_num < 3,
            nextPage: page_num < 3 ? page_num + 1 : undefined,
            totalItems: 25
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Enable infinite loading
    await page.click('[data-testid="infinite-toggle"]')
    
    // Wait for initial load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    await expect(page.getByText('Page-based Problem 1')).toBeVisible()

    // Scroll to load page 2
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })

    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(20)
    await expect(page.getByText('Page-based Problem 11')).toBeVisible()

    // Scroll to load final page
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })

    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(25) // 10 + 10 + 5
    await expect(page.getByText('Page-based Problem 21')).toBeVisible()
  })

  test('should handle API errors during infinite loading', async ({ page }) => {
    let requestCount = 0
    
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      requestCount++
      
      if (requestCount === 1) {
        // First request succeeds
        await route.fulfill({
          status: 200,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            data: {
              items: Array.from({ length: 10 }, (_, i) => ({
                problemId: i + 1,
                title: `Problem ${i + 1}`,
                reason: 'Test problem',
                confidence: 0.8,
                source: 'LLM'
              })),
              meta: {
                cached: false,
                traceId: 'trace-1',
                generatedAt: new Date().toISOString(),
                nextCursor: 'cursor-fail',
                hasMore: true
              }
            }
          })
        })
      } else {
        // Second request fails
        await route.fulfill({
          status: 500,
          body: JSON.stringify({ error: 'Internal server error' })
        })
      }
    })

    await page.goto('/dashboard/recommendations')
    await page.click('[data-testid="infinite-toggle"]')
    
    // Wait for initial load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)

    // Scroll to trigger error
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })

    // Should show error message but keep existing items
    await expect(page.getByText('加载更多推荐时出错')).toBeVisible()
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10) // Still shows first page
  })

  test('should show loading spinner during infinite loading', async ({ page }) => {
    // Add delay to API response
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const url = new URL(route.request().url())
      const cursor = url.searchParams.get('cursor')
      
      if (cursor) {
        // Delay second page response
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
      
      const response = {
        data: {
          items: Array.from({ length: 5 }, (_, i) => ({
            problemId: cursor ? 100 + i : i + 1,
            title: `Problem ${cursor ? 100 + i : i + 1}`,
            reason: 'Test problem',
            confidence: 0.8,
            source: 'LLM'
          })),
          meta: {
            cached: false,
            traceId: cursor ? 'trace-2' : 'trace-1',
            generatedAt: new Date().toISOString(),
            nextCursor: cursor ? undefined : 'cursor-2',
            hasMore: !cursor
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    await page.click('[data-testid="infinite-toggle"]')
    
    // Wait for initial load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(5)

    // Scroll to trigger loading
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })

    // Should show loading spinner
    await expect(page.locator('[data-testid="loading-more"]')).toBeVisible()
    await expect(page.getByText('加载更多推荐中...')).toBeVisible()

    // Wait for loading to complete
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    await expect(page.locator('[data-testid="loading-more"]')).not.toBeVisible()
  })
})