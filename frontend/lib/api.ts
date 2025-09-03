/**
 * API client for OLIVER FSRS Backend
 * Base URL and request configuration
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api/v1'

interface ApiResponse<T = any> {
  data?: T
  message?: string
  error?: string
  timestamp?: string
}

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public response?: any
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
async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
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

  const shouldAttemptSilentRefresh = !endpoint.startsWith('/auth/login') && !endpoint.startsWith('/auth/logout') && !endpoint.startsWith('/auth/refresh')

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
                return null as T
              }
              return await response.json()
            }
          } catch {}
        }
        errorMessage = "认证失败，请重新登录"
        clearAuthToken()
      } else if (response.status === 403) {
        errorMessage = "权限不足，无法执行此操作"
      } else {
        try {
          const errorData = await response.json()
          errorMessage = errorData.message || errorData.error || errorMessage
        } catch (e) {
          // Response is not JSON, use status text
        }
      }
      
      throw new ApiError(errorMessage, response.status, response)
    }

    // Handle empty responses
    const contentType = response.headers.get('Content-Type')
    if (!contentType || !contentType.includes('application/json')) {
      return null as T
    }

    try {
      return await response.json()
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

export { apiRequest, ApiError, type ApiResponse }
