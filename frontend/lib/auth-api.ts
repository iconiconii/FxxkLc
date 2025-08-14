/**
 * Authentication API client functions
 */

import { apiRequest, setAuthToken, clearAuthToken } from './api'

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
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
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
    
    // Store tokens and user info after successful login
    if (response.accessToken) {
      setAuthToken(response.accessToken)
      if (typeof window !== 'undefined') {
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        // Store user info for persistence across page refreshes
        localStorage.setItem('userInfo', JSON.stringify(response.user))
      }
    }
    
    return response
  },

  /**
   * User registration
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    const response = await apiRequest<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(request),
    })
    
    // Store tokens and user info after successful registration
    if (response.accessToken) {
      setAuthToken(response.accessToken)
      if (typeof window !== 'undefined') {
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        // Store user info for persistence across page refreshes
        localStorage.setItem('userInfo', JSON.stringify(response.user))
      }
    }
    
    return response
  },

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<AuthResponse> {
    const refreshToken = typeof window !== 'undefined' 
      ? localStorage.getItem('refreshToken') 
      : null
    
    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    const response = await apiRequest<AuthResponse>('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    })
    
    // Update stored tokens and user info
    if (response.accessToken) {
      setAuthToken(response.accessToken)
      if (typeof window !== 'undefined') {
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        // Update user info if provided in refresh response
        if (response.user) {
          localStorage.setItem('userInfo', JSON.stringify(response.user))
        }
      }
    }
    
    return response
  },

  /**
   * User logout
   */
  async logout(): Promise<void> {
    try {
      await apiRequest('/auth/logout', {
        method: 'POST',
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
    if (typeof window === 'undefined') return false
    return !!localStorage.getItem('accessToken')
  },

  /**
   * Get current user info from stored token
   */
  getCurrentUser(): UserInfo | null {
    if (typeof window === 'undefined') return null
    
    // First try to get user info from localStorage
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      try {
        return JSON.parse(storedUserInfo)
      } catch (error) {
        console.error('Error parsing stored user info:', error)
        // Remove invalid stored user info
        localStorage.removeItem('userInfo')
      }
    }
    
    // Fallback to JWT token decoding if no stored user info
    const token = localStorage.getItem('accessToken')
    if (!token) return null
    
    try {
      // Decode JWT token to get user info
      const payload = JSON.parse(atob(token.split('.')[1]))
      
      // Construct UserInfo from JWT payload with more flexible field matching
      if (payload.userId || payload.sub) {
        const userInfo = {
          id: payload.userId || payload.sub,
          username: payload.username || payload.preferred_username || payload.sub,
          email: payload.email || payload.email_address,
          firstName: payload.firstName || payload.given_name || payload.name?.split(' ')[0],
          lastName: payload.lastName || payload.family_name || payload.name?.split(' ')[1],
          avatarUrl: payload.avatarUrl || payload.picture,
          timezone: payload.timezone || payload.zoneinfo,
          authProvider: payload.authProvider || payload.provider || 'local',
          emailVerified: payload.emailVerified || payload.email_verified || false,
        }
        
        // Store the decoded user info for future use
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        return userInfo
      }
      
      return null
    } catch (error) {
      console.error('Error decoding token:', error)
      return null
    }
  },
}