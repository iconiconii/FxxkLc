/**
 * Smart caching utilities for dashboard data
 */

interface CacheItem<T> {
  data: T
  timestamp: number
  ttl: number
}

class SmartCache {
  private cache = new Map<string, CacheItem<any>>()

  /**
   * Set data in cache with TTL
   */
  set<T>(key: string, data: T, ttlMinutes: number = 5): void {
    const ttl = ttlMinutes * 60 * 1000 // Convert to milliseconds
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl
    })
  }

  /**
   * Get data from cache if not expired
   */
  get<T>(key: string): T | null {
    const item = this.cache.get(key)
    if (!item) return null

    const now = Date.now()
    if (now - item.timestamp > item.ttl) {
      this.cache.delete(key)
      return null
    }

    return item.data
  }

  /**
   * Check if cache has valid data
   */
  has(key: string): boolean {
    return this.get(key) !== null
  }

  /**
   * Clear specific key or all cache
   */
  clear(key?: string): void {
    if (key) {
      this.cache.delete(key)
    } else {
      this.cache.clear()
    }
  }

  /**
   * Get cache stats
   */
  getStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    }
  }

  /**
   * Cleanup expired entries
   */
  cleanup(): void {
    const now = Date.now()
    for (const [key, item] of this.cache.entries()) {
      if (now - item.timestamp > item.ttl) {
        this.cache.delete(key)
      }
    }
  }
}

// Global cache instance
export const dashboardCache = new SmartCache()

// Cache keys
export const CACHE_KEYS = {
  ANALYTICS_OVERVIEW: 'analytics_overview',
  REVIEW_QUEUE: 'review_queue',
  USER_PROGRESS: 'user_progress',
  DAILY_ACTIVITY: 'daily_activity'
} as const

// Cache TTL configurations (in minutes)
export const CACHE_TTL = {
  ANALYTICS_OVERVIEW: 5,
  REVIEW_QUEUE: 2,
  USER_PROGRESS: 10,
  DAILY_ACTIVITY: 10
} as const

/**
 * Cache-aware fetch wrapper
 */
export async function cachedFetch<T>(
  key: string,
  fetchFn: () => Promise<T>,
  ttlMinutes?: number
): Promise<T> {
  // Try to get from cache first
  const cached = dashboardCache.get<T>(key)
  if (cached !== null) {
    return cached
  }

  // Fetch new data
  const data = await fetchFn()
  
  // Store in cache
  dashboardCache.set(key, data, ttlMinutes)
  
  return data
}

/**
 * Invalidate related cache entries
 */
export function invalidateCache(patterns: string[]): void {
  patterns.forEach(pattern => {
    if (pattern.includes('*')) {
      // Wildcard pattern
      const prefix = pattern.replace('*', '')
      const stats = dashboardCache.getStats()
      stats.keys
        .filter(key => key.startsWith(prefix))
        .forEach(key => dashboardCache.clear(key))
    } else {
      // Exact key
      dashboardCache.clear(pattern)
    }
  })
}

/**
 * Background cache cleanup (run periodically)
 */
let cleanupInterval: NodeJS.Timeout | null = null

export function startCacheCleanup(intervalMinutes: number = 10): void {
  if (cleanupInterval) {
    clearInterval(cleanupInterval)
  }
  
  cleanupInterval = setInterval(() => {
    dashboardCache.cleanup()
  }, intervalMinutes * 60 * 1000)
}

export function stopCacheCleanup(): void {
  if (cleanupInterval) {
    clearInterval(cleanupInterval)
    cleanupInterval = null
  }
}

// Auto-start cleanup in browser environment
if (typeof window !== 'undefined') {
  startCacheCleanup(10) // Cleanup every 10 minutes
  
  // Clear cache when user navigates away (prevents stale data)
  window.addEventListener('beforeunload', () => {
    dashboardCache.clear()
    stopCacheCleanup()
  })
}