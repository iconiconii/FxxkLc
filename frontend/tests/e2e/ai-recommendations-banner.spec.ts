import { test, expect, Page } from '@playwright/test'
import { AuthHelper } from './fixtures/auth-helper'

test.describe('AI Recommendations Busy/DEFAULT Banner', () => {
  let authHelper: AuthHelper

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page)
    await authHelper.loginWithValidCredentials()
  })

  test('should show banner when meta.busy=true in any page', async ({ page }) => {
    let pageCount = 0
    
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      pageCount++
      
      const response = {
        data: {
          items: Array.from({ length: 5 }, (_, i) => ({
            problemId: (pageCount - 1) * 5 + i + 1,
            title: `Problem ${(pageCount - 1) * 5 + i + 1}`,
            reason: 'Test recommendation',
            confidence: 0.8,
            source: 'LLM',
            difficulty: 'medium'
          })),
          meta: {
            cached: false,
            traceId: `trace-${pageCount}`,
            generatedAt: new Date().toISOString(),
            busy: pageCount === 2, // Second page is busy
            nextCursor: pageCount < 3 ? `cursor-${pageCount + 1}` : undefined,
            hasMore: pageCount < 3
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'LLM'
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Enable infinite loading
    await page.click('[data-testid="infinite-toggle"]')
    
    // Initial load - no banner
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(5)
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).not.toBeVisible()
    
    // Scroll to load second page (which has busy=true)
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })
    
    // Wait for second page to load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    
    // Banner should now be visible because second page has busy=true
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
    await expect(page.getByText('结果可能较为保守，稍后可重新获取推荐。')).toBeVisible()
    
    // Banner should have warning styling
    const banner = page.locator('text=系统繁忙，推荐已回退基础策略').locator('..')
    await expect(banner).toHaveClass(/bg-yellow-50/)
    await expect(banner).toHaveClass(/border-yellow-200/)
  })

  test('should show banner when rec-source header is DEFAULT', async ({ page }) => {
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const response = {
        data: {
          items: Array.from({ length: 10 }, (_, i) => ({
            problemId: i + 1,
            title: `Problem ${i + 1}`,
            reason: 'Fallback recommendation',
            confidence: 0.6, // Lower confidence for DEFAULT source
            source: 'DEFAULT',
            difficulty: 'easy'
          })),
          meta: {
            cached: false,
            traceId: 'default-trace',
            generatedAt: new Date().toISOString(),
            hasMore: false
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'DEFAULT', // This should trigger the banner
          'cache-hit': 'false'
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Wait for load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    
    // Banner should be visible due to DEFAULT source
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
    await expect(page.getByText('结果可能较为保守，稍后可重新获取推荐。')).toBeVisible()
    
    // Verify banner has warning icon
    const banner = page.locator('text=系统繁忙，推荐已回退基础策略').locator('..')
    await expect(banner.locator('svg')).toBeVisible() // Warning icon
  })

  test('should show banner when multiple conditions are met', async ({ page }) => {
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const response = {
        data: {
          items: Array.from({ length: 8 }, (_, i) => ({
            problemId: i + 1,
            title: `Problem ${i + 1}`,
            reason: 'Fallback recommendation with busy system',
            confidence: 0.5,
            source: 'DEFAULT',
            difficulty: 'medium'
          })),
          meta: {
            cached: false,
            traceId: 'busy-default-trace',
            generatedAt: new Date().toISOString(),
            busy: true, // Both busy AND DEFAULT source
            hasMore: false
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'DEFAULT',
          'cache-hit': 'false'
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Wait for load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(8)
    
    // Banner should be visible (both conditions trigger it)
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
    
    // Check debug info in development mode
    if (await page.locator('details:has-text("调试信息")').isVisible()) {
      await page.click('details:has-text("调试信息") summary')
      await expect(page.getByText('来源: DEFAULT')).toBeVisible()
    }
  })

  test('should not show banner when system operates normally', async ({ page }) => {
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const response = {
        data: {
          items: Array.from({ length: 10 }, (_, i) => ({
            problemId: i + 1,
            title: `Problem ${i + 1}`,
            reason: 'High-quality AI recommendation',
            confidence: 0.9,
            source: 'LLM',
            difficulty: 'medium',
            latencyMs: 120
          })),
          meta: {
            cached: false,
            traceId: 'normal-trace',
            generatedAt: new Date().toISOString(),
            busy: false, // System is not busy
            hasMore: false
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'LLM', // Normal AI source
          'cache-hit': 'false'
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Wait for load
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    
    // Banner should NOT be visible
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).not.toBeVisible()
    
    // Verify recommendations show high confidence
    await expect(page.getByText('90%')).toBeVisible() // High confidence percentage
    await expect(page.getByText('AI推荐')).toBeVisible() // LLM source badge
  })

  test('should show banner in infinite loading when any page triggers condition', async ({ page }) => {
    let pageCount = 0
    
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      pageCount++
      
      const response = {
        data: {
          items: Array.from({ length: 5 }, (_, i) => ({
            problemId: (pageCount - 1) * 5 + i + 1,
            title: `Problem ${(pageCount - 1) * 5 + i + 1}`,
            reason: `Page ${pageCount} recommendation`,
            confidence: pageCount === 3 ? 0.5 : 0.8, // Third page has low confidence
            source: pageCount === 3 ? 'DEFAULT' : 'LLM',
            difficulty: 'medium'
          })),
          meta: {
            cached: false,
            traceId: `trace-${pageCount}`,
            generatedAt: new Date().toISOString(),
            busy: false,
            nextCursor: pageCount < 3 ? `cursor-${pageCount + 1}` : undefined,
            hasMore: pageCount < 3
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': pageCount === 3 ? 'DEFAULT' : 'LLM' // Third page uses DEFAULT
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    await page.click('[data-testid="infinite-toggle"]')
    
    // Load first page - no banner
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(5)
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).not.toBeVisible()
    
    // Load second page - still no banner
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(10)
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).not.toBeVisible()
    
    // Load third page with DEFAULT source - banner should appear
    await page.evaluate(() => {
      document.querySelector('[data-testid="auto-load-trigger"]')?.scrollIntoView()
    })
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount(15)
    
    // Now banner should be visible because third page has DEFAULT source
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
    
    // Check that debug info reflects the aggregated state
    if (await page.locator('details:has-text("调试信息")').isVisible()) {
      await page.click('details:has-text("调试信息") summary')
      await expect(page.getByText('任一页Busy/默认源: 是')).toBeVisible()
    }
  })

  test('should persist banner state when refreshing', async ({ page }) => {
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const response = {
        data: {
          items: Array.from({ length: 5 }, (_, i) => ({
            problemId: i + 1,
            title: `Problem ${i + 1}`,
            reason: 'Fallback recommendation',
            confidence: 0.6,
            source: 'DEFAULT',
            difficulty: 'easy'
          })),
          meta: {
            cached: false,
            traceId: 'persistent-trace',
            generatedAt: new Date().toISOString(),
            busy: true,
            hasMore: false
          }
        }
      }

      await route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'rec-source': 'DEFAULT'
        },
        body: JSON.stringify(response)
      })
    })

    await page.goto('/dashboard/recommendations')
    
    // Banner should be visible
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
    
    // Click refresh button
    await page.click('text=刷新推荐')
    
    // Banner should still be visible after refresh
    await expect(page.getByText('系统繁忙，推荐已回退基础策略')).toBeVisible()
  })
})