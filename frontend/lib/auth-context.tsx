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
  
  // Check for existing authentication on mount via cookie session
  useEffect(() => {
    const checkAuth = async () => {
      // First check if we have cached user info (for better UX and reduced API calls)
      const cachedUser = authApi.getCurrentUser()
      if (cachedUser) {
        setUser(cachedUser)
        setIsAuthenticated(true)
        setLoading(false)
        setInitialCheckDone(true)
        console.log('AuthContext: Using cached user info for:', cachedUser.username)
        
        // Optionally validate with server in the background
        try {
          const currentUser = await authApi.getCurrentUserFromServer()
          setUser(currentUser)
          console.log('AuthContext: Session validated for:', currentUser.username)
        } catch (error) {
          console.log('AuthContext: Cached session invalid, clearing auth state')
          setUser(null)
          setIsAuthenticated(false)
          // Clear invalid cached data
          if (typeof window !== 'undefined') {
            try { localStorage.removeItem('userInfo') } catch {}
          }
        }
        return
      }

      // Check if there are any auth cookies before making server call
      const hasAuthCookies = typeof window !== 'undefined' && 
        (document.cookie.includes('ACCESS_TOKEN') || document.cookie.includes('REFRESH_TOKEN'))
      
      if (!hasAuthCookies) {
        console.log('AuthContext: No auth cookies found, skipping server check')
        setUser(null)
        setIsAuthenticated(false)
        setLoading(false)
        setInitialCheckDone(true)
        return
      }

      // No cached user but has cookies, try server validation
      try {
        console.log('AuthContext: Checking session with /auth/me')
        const currentUser = await authApi.getCurrentUserFromServer()
        setUser(currentUser)
        setIsAuthenticated(true)
        console.log('AuthContext: Session valid for:', currentUser.username)
      } catch (error) {
        console.log('AuthContext: No valid session')
        setUser(null)
        setIsAuthenticated(false)
      } finally {
        setLoading(false)
        setInitialCheckDone(true)
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
    try {
      await authApi.logout()
    } catch (error) {
      console.log('AuthContext: Logout API call failed, but clearing state anyway:', error)
    } finally {
      setUser(null)
      setIsAuthenticated(false)
      // Clear cached user info
      if (typeof window !== 'undefined') {
        try { localStorage.removeItem('userInfo') } catch {}
      }
      // Redirect to home page after logout
      router.push('/')
    }
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
