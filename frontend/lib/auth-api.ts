/**
 * Authentication API client functions
 */

import { apiRequest, clearAuthToken } from './api'
import { withIdempotency } from './idempotency-utils'

export interface UserInfo {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
  timezone?: string
  authProvider: string
  emailVerified: boolean
}

export interface AuthResponse {
  // Tokens may be omitted when using cookie-based auth (kept for compatibility)
  accessToken?: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
  user: UserInfo
}

export interface LoginRequest {
  identifier: string // email or username
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
  timezone?: string
  requestId?: string // 幂等性请求ID
}

export interface RefreshTokenRequest {
  refreshToken: string
}

// Authentication API functions
export const authApi = {
  /**
   * User login with email/username and password
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(request),
    })
    
    // Cookies are set by server. Persist basic user info for UX only.
    if (typeof window !== 'undefined' && response.user) {
      try { localStorage.setItem('userInfo', JSON.stringify(response.user)) } catch {}
    }
    
    return response
  },

  /**
   * User registration
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    // 为注册请求添加幂等性requestId
    const requestWithId = withIdempotency(request)
    
    const response = await apiRequest<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(requestWithId),
    })
    
    // Cookies are set by server. Persist basic user info for UX only.
    if (typeof window !== 'undefined' && response.user) {
      try { localStorage.setItem('userInfo', JSON.stringify(response.user)) } catch {}
    }
    
    return response
  },

  /**
   * Refresh access token
   */
  // Refresh is handled server-side via cookies; the frontend does not manage tokens.
  // For session validation, call getCurrentUserFromServer instead.
  async refreshToken(): Promise<AuthResponse> {
    throw new Error('Token refresh is not supported on the client when using cookies')
  },

  /**
   * User logout
   */
  async logout(): Promise<void> {
    try {
      const requestWithId = withIdempotency({})
      await apiRequest('/auth/logout', {
        method: 'POST',
        body: JSON.stringify(requestWithId),
      })
    } finally {
      // Always clear tokens, even if logout request fails
      clearAuthToken()
    }
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    // With cookie auth, do not rely on local storage tokens
    // This method is kept for compatibility; prefer server check via getCurrentUserFromServer
    if (typeof window === 'undefined') return false
    try {
      return !!localStorage.getItem('userInfo')
    } catch { return false }
  },

  /**
   * Get current user info from stored token
   */
  getCurrentUser(): UserInfo | null {
    if (typeof window === 'undefined') return null
    const storedUserInfo = (() => { try { return localStorage.getItem('userInfo') } catch { return null } })()
    if (storedUserInfo) {
      try { return JSON.parse(storedUserInfo) } catch { return null }
    }
    return null
  },

  async getCurrentUserFromServer(): Promise<UserInfo> {
    const user = await apiRequest<UserInfo>('/auth/me')
    // Cache for UX improvements
    if (typeof window !== 'undefined') {
      try { localStorage.setItem('userInfo', JSON.stringify(user)) } catch {}
    }
    return user
  },
}
