/**
 * API client for OLIVER FSRS Backend
 * Base URL and request configuration
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api/v1'

interface ApiResponse<T = unknown> {
  data?: T
  message?: string
  error?: string
  timestamp?: string
}

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public response?: unknown
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

// Token storage is no longer used (HttpOnly cookies now carry tokens).
// Keep a lightweight clear function for local user info only.
export const clearAuthToken = (): void => {
  if (typeof window !== 'undefined') {
    try { localStorage.removeItem('userInfo') } catch {}
  }
}

// Simple in-flight refresh lock to avoid multiple concurrent refreshes
let refreshPromise: Promise<void> | null = null

async function refreshSession(): Promise<void> {
  if (refreshPromise) return refreshPromise
  refreshPromise = (async () => {
    try {
      // Call refresh without explicit token; backend reads REFRESH_TOKEN cookie
      const resp = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({}),
      })
      if (!resp.ok) {
        clearAuthToken()
        throw new Error(`Refresh failed: ${resp.status}`)
      }
      // Optionally update cached userInfo if backend returns it
      try {
        const data = await resp.clone().json().catch(() => null)
        if (data?.user && typeof window !== 'undefined') {
          localStorage.setItem('userInfo', JSON.stringify(data.user))
        }
      } catch {}
    } finally {
      // Release lock regardless of outcome
      const p = refreshPromise
      refreshPromise = null
      await Promise.resolve(p)
    }
  })()
  return refreshPromise
}

// Base fetch wrapper with authentication and error handling
async function apiRequest<T = unknown>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await apiRequestWithHeaders<T>(endpoint, options)
  return response.body
}

// Enhanced fetch that returns both data and headers
async function apiRequestWithHeaders<T = unknown>(
  endpoint: string,
  options: RequestInit = {}
): Promise<{ body: T; headers: Headers }> {
  const url = `${API_BASE_URL}${endpoint}`

  const config: RequestInit = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    // Always include cookies for same-origin and proxied requests
    credentials: 'include',
    ...options,
  }

  const shouldAttemptSilentRefresh = !endpoint.startsWith('/auth/login') && 
    !endpoint.startsWith('/auth/logout') && 
    !endpoint.startsWith('/auth/refresh') &&
    !endpoint.startsWith('/codetop/problems/global') // Don't refresh for public APIs

  try {
    let response = await fetch(url, config)
    
    if (!response.ok) {
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`
      
      // Handle specific HTTP status codes
      if (response.status === 401) {
        if (shouldAttemptSilentRefresh) {
          // Try silent refresh once
          try {
            await refreshSession()
            // Retry original request once
            response = await fetch(url, config)
            if (response.ok) {
              const contentType2 = response.headers.get('Content-Type')
              if (!contentType2 || !contentType2.includes('application/json')) {
                return { body: null as T, headers: response.headers }
              }
              const body = await response.json()
              return { body, headers: response.headers }
            }
          } catch {}
        }
        errorMessage = "认证失败，请重新登录"
        clearAuthToken()
      } else if (response.status === 403) {
        errorMessage = "权限不足，无法执行此操作"
      } else if (response.status === 429) {
        errorMessage = "请求过于频繁，请稍后重试"
      } else if (response.status === 503) {
        errorMessage = "服务暂时不可用，请稍后重试"
      } else {
        try {
          const errorData = await response.json()
          errorMessage = errorData.message || errorData.error || errorMessage
        } catch {
          // Response is not JSON, use status text
          if (response.status >= 500) {
            errorMessage = "服务器内部错误，请稍后重试"
          } else if (response.status >= 400) {
            errorMessage = "请求参数错误，请检查后重试"
          }
        }
      }
      
      throw new ApiError(errorMessage, response.status, response)
    }

    // Handle empty responses
    const contentType = response.headers.get('Content-Type')
    if (!contentType || !contentType.includes('application/json')) {
      return { body: null as T, headers: response.headers }
    }

    try {
      const body = await response.json()
      return { body, headers: response.headers }
    } catch (parseError) {
      console.error('Failed to parse JSON response:', parseError)
      throw new ApiError('服务器返回了无效的响应格式', response.status, response)
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    
    if (error instanceof Error) {
      throw new ApiError(`Network error: ${error.message}`, 0)
    }
    
    throw new ApiError('Unknown error occurred', 0)
  }
}

export { apiRequest, apiRequestWithHeaders, ApiError, type ApiResponse }
