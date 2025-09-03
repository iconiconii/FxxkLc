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
