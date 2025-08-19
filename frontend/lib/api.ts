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

// Helper function to get auth token from localStorage
const getAuthToken = (): string | null => {
  if (typeof window === 'undefined') return null
  return localStorage.getItem('accessToken')
}

// Helper function to set auth token
export const setAuthToken = (token: string): void => {
  if (typeof window !== 'undefined') {
    localStorage.setItem('accessToken', token)
  }
}

// Helper function to clear auth token
export const clearAuthToken = (): void => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }
}

// Base fetch wrapper with authentication and error handling
async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`
  const token = getAuthToken()

  const config: RequestInit = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
    ...options,
  }

  try {
    const response = await fetch(url, config)
    
    if (!response.ok) {
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`
      
      // Handle specific HTTP status codes
      if (response.status === 401) {
        errorMessage = "认证失败，请重新登录"
        // Clear invalid token
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