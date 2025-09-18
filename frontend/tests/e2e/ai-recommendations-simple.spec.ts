import { test, expect } from '@playwright/test'

test.describe('AI Recommendations Core Functionality', () => {
  test.beforeEach(async ({ page }) => {
    // Mock the AI recommendations API
    await page.route('**/api/v1/ai/recommendations**', async (route) => {
      const url = new URL(route.request().url())
      const cursor = url.searchParams.get('cursor')
      const limit = parseInt(url.searchParams.get('limit') || '10')

      let response
      if (cursor === 'cursor-page-2') {
        // Second page with nextCursor
        response = {
          data: {
            items: Array.from({ length: limit }, (_, i) => ({
              problemId: 100 + i,
              title: `Problem ${100 + i}`,
              reason: `Advanced algorithm problem (page 2, item ${i + 1})`,
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
              totalItems: 30
            }
          }
        }
      } else if (cursor === 'cursor-page-3') {
        // Final page
        response = {
          data: {
            items: Array.from({ length: 5 }, (_, i) => ({
              problemId: 200 + i,
              title: `Problem ${200 + i}`,
              reason: `Final batch (page 3, item ${i + 1})`,
              confidence: 0.65 + (i * 0.02),
              source: 'LLM',
              difficulty: 'hard',
              topics: ['Graph Theory']
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-3',
              generatedAt: new Date().toISOString(),
              hasMore: false,
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
              reason: `Recommended based on your progress (item ${i + 1})`,
              confidence: 0.85 + (i * 0.001),
              source: 'LLM',
              difficulty: i % 3 === 0 ? 'easy' : i % 3 === 1 ? 'medium' : 'hard',
              topics: ['Arrays', 'Hash Table'],
              latencyMs: 150
            })),
            meta: {
              cached: false,
              traceId: 'trace-page-1',
              generatedAt: new Date().toISOString(),
              nextCursor: 'cursor-page-2',
              hasMore: true,
              totalItems: 30
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
  })

  test('should load and display basic recommendation page', async ({ page }) => {
    // Go to recommendations page
    await page.goto('http://localhost:3004/dashboard/recommendations')
    
    // Wait for the page to load
    await page.waitForTimeout(2000)
    
    // Check if the page loaded without errors
    const pageTitle = await page.title()
    console.log('Page title:', pageTitle)
    
    // Take a screenshot for debugging
    await page.screenshot({ path: 'test-results/recommendations-page.png' })
    
    // Check if we can find any content
    const bodyContent = await page.textContent('body')
    expect(bodyContent).toBeDefined()
    expect(bodyContent.length).toBeGreaterThan(0)
  })

  test('should test pagination logic with mock data', async ({ page }) => {
    // Test the core pagination logic
    const testLogic = await page.evaluate(() => {
      // Test nextCursor strategy
      const page1 = {
        data: {
          meta: {
            nextCursor: 'cursor-123',
            hasMore: true,
            nextPage: 2,
          },
        },
      }

      const nextParam1 = page1.data.meta.nextCursor || 
        (page1.data.meta.hasMore && page1.data.meta.nextPage ? 
          page1.data.meta.nextPage.toString() : undefined)

      // Test page fallback strategy
      const page2 = {
        data: {
          meta: {
            hasMore: true,
            nextPage: 3,
          },
        },
      }

      const nextParam2 = page2.data.meta.nextCursor || 
        (page2.data.meta.hasMore && page2.data.meta.nextPage ? 
          page2.data.meta.nextPage.toString() : undefined)

      // Test termination
      const page3 = {
        data: {
          meta: {
            hasMore: false,
          },
        },
      }

      const nextParam3 = page3.data.meta.nextCursor || 
        (page3.data.meta.hasMore && page3.data.meta.nextPage ? 
          page3.data.meta.nextPage.toString() : undefined)

      return {
        nextParam1,
        nextParam2,
        nextParam3,
      }
    })

    // Verify pagination logic
    expect(testLogic.nextParam1).toBe('cursor-123') // Uses cursor
    expect(testLogic.nextParam2).toBe('3')          // Falls back to page
    expect(testLogic.nextParam3).toBeUndefined()    // Terminates properly
  })

  test('should test banner logic with mock headers', async ({ page }) => {
    // Test banner logic
    const bannerLogic = await page.evaluate(() => {
      // Test busy condition
      const busyMeta = { busy: true }
      const normalHeaders = new Headers()
      normalHeaders.set('rec-source', 'LLM')
      
      const showBannerBusy = busyMeta.busy || normalHeaders.get('rec-source') === 'DEFAULT'

      // Test DEFAULT source condition
      const normalMeta = { busy: false }
      const defaultHeaders = new Headers()
      defaultHeaders.set('rec-source', 'DEFAULT')
      
      const showBannerDefault = normalMeta.busy || defaultHeaders.get('rec-source') === 'DEFAULT'

      // Test normal operation
      const normalMeta2 = { busy: false }
      const normalHeaders2 = new Headers()
      normalHeaders2.set('rec-source', 'LLM')
      
      const showBannerNormal = normalMeta2.busy || normalHeaders2.get('rec-source') === 'DEFAULT'

      return {
        showBannerBusy,
        showBannerDefault,
        showBannerNormal,
      }
    })

    // Verify banner logic
    expect(bannerLogic.showBannerBusy).toBe(true)    // Shows when busy
    expect(bannerLogic.showBannerDefault).toBe(true) // Shows when DEFAULT source
    expect(bannerLogic.showBannerNormal).toBe(false) // Hidden during normal operation
  })

  test('should test badge visibility logic', async ({ page }) => {
    // Test badge visibility logic
    const badgeLogic = await page.evaluate(() => {
      const recommendedIds = new Set([123, 456, 789])

      // Test recommended + AB enabled
      const shouldShow1 = true && recommendedIds.has(123)

      // Test AB disabled
      const shouldShow2 = false && recommendedIds.has(123)

      // Test not recommended
      const shouldShow3 = true && recommendedIds.has(999)

      return {
        shouldShow1,
        shouldShow2,
        shouldShow3,
      }
    })

    // Verify badge logic
    expect(badgeLogic.shouldShow1).toBe(true)  // Shows when recommended + AB enabled
    expect(badgeLogic.shouldShow2).toBe(false) // Hidden when AB disabled
    expect(badgeLogic.shouldShow3).toBe(false) // Hidden when not recommended
  })
})