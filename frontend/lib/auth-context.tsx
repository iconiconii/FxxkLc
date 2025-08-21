"use client"

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { useRouter } from 'next/navigation'
import { authApi, type UserInfo } from './auth-api'

interface AuthContextType {
  user: UserInfo | null
  loading: boolean
  login: (credentials: { identifier: string; password: string }) => Promise<void>
  register: (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => Promise<void>
  logout: () => Promise<void>
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const router = useRouter()
  const [user, setUser] = useState<UserInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)
  const [initialCheckDone, setInitialCheckDone] = useState(false)
  
  // Check for existing authentication on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        console.log('AuthContext: Starting authentication check')
        
        // Check if we have tokens in localStorage
        const hasAccessToken = typeof window !== 'undefined' && !!localStorage.getItem('accessToken')
        const hasRefreshToken = typeof window !== 'undefined' && !!localStorage.getItem('refreshToken')
        
        console.log('AuthContext: Token status - access:', hasAccessToken, 'refresh:', hasRefreshToken)
        
        if (hasAccessToken) {
          // Try to get user info from stored data first
          const currentUser = authApi.getCurrentUser()
          if (currentUser) {
            // Valid token and user data found
            setUser(currentUser)
            setIsAuthenticated(true)
            console.log('AuthContext: Authentication restored from storage:', currentUser.username)
          } else {
            console.log('AuthContext: Token exists but user data invalid, clearing auth')
            await authApi.logout()
            setUser(null)
            setIsAuthenticated(false)
          }
        } else if (hasRefreshToken) {
          // No access token but have refresh token, try to refresh
          try {
            console.log('AuthContext: Attempting token refresh')
            const response = await authApi.refreshToken()
            setUser(response.user)
            setIsAuthenticated(true)
            console.log('AuthContext: Token refresh successful:', response.user.username)
          } catch (refreshError) {
            console.error('AuthContext: Token refresh failed:', refreshError)
            // Refresh failed, clear all tokens
            await authApi.logout()
            setUser(null)
            setIsAuthenticated(false)
          }
        } else {
          console.log('AuthContext: No tokens found, user not authenticated')
          setUser(null)
          setIsAuthenticated(false)
        }
      } catch (error) {
        console.error('AuthContext: Auth check failed:', error)
        // On any error, clear authentication state
        await authApi.logout()
        setUser(null)
        setIsAuthenticated(false)
      } finally {
        setLoading(false)
        setInitialCheckDone(true)
        console.log('AuthContext: Authentication check completed')
      }
    }

    checkAuth()
  }, [])

  const login = async (credentials: { identifier: string; password: string }) => {
    // Prevent race condition: don't override if already authenticated with same user
    if (isAuthenticated && user?.id) {
      console.log('AuthContext: User already authenticated, skipping redundant login')
      return
    }

    console.log('AuthContext: Attempting login for:', credentials.identifier)
    
    const response = await authApi.login(credentials)
    
    // Update state immediately
    setUser(response.user)
    setIsAuthenticated(true)
    setLoading(false)
    
    console.log('AuthContext: Login successful for:', response.user.username)
  }

  const register = async (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => {
    // Prevent race condition: don't override if already authenticated
    if (isAuthenticated && user?.id) {
      console.log('AuthContext: User already authenticated, skipping redundant register')
      return
    }

    console.log('AuthContext: Attempting registration for:', userData.username)
    
    const response = await authApi.register(userData)
    
    // Update state immediately
    setUser(response.user)
    setIsAuthenticated(true)
    setLoading(false)
    
    console.log('AuthContext: Registration successful for:', response.user.username)
  }

  const logout = async () => {
    await authApi.logout()
    setUser(null)
    setIsAuthenticated(false)
    // Redirect to home page after logout
    router.push('/')
  }

  const value: AuthContextType = {
    user,
    loading: loading && !initialCheckDone, // Only show loading during initial check
    login,
    register,
    logout,
    isAuthenticated,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}