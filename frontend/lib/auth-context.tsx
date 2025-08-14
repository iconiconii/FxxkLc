"use client"

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
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
  const [user, setUser] = useState<UserInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)
  
  // Check for existing authentication on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        if (authApi.isAuthenticated()) {
          const currentUser = authApi.getCurrentUser()
          if (currentUser) {
            // Valid token and user data found
            setUser(currentUser)
            setIsAuthenticated(true)
            console.log('Auth check successful:', currentUser)
          } else {
            // Token exists but user data invalid, try to refresh
            try {
              const response = await authApi.refreshToken()
              setUser(response.user)
              setIsAuthenticated(true)
              console.log('Token refresh successful:', response.user)
            } catch (refreshError) {
              console.error('Token refresh failed:', refreshError)
              // Refresh failed, clear tokens
              await authApi.logout()
              setUser(null)
              setIsAuthenticated(false)
            }
          }
        } else {
          console.log('No valid token found')
          setUser(null)
          setIsAuthenticated(false)
        }
      } catch (error) {
        console.error('Auth check failed:', error)
        setUser(null)
        setIsAuthenticated(false)
      } finally {
        setLoading(false)
      }
    }

    checkAuth()
  }, [])

  const login = async (credentials: { identifier: string; password: string }) => {
    const response = await authApi.login(credentials)
    setUser(response.user)
    setIsAuthenticated(true)
    // Force re-render to update isAuthenticated state
    setLoading(false)
  }

  const register = async (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => {
    const response = await authApi.register(userData)
    setUser(response.user)
    setIsAuthenticated(true)
    // Force re-render to update isAuthenticated state
    setLoading(false)
  }

  const logout = async () => {
    await authApi.logout()
    setUser(null)
    setIsAuthenticated(false)
  }

  const value: AuthContextType = {
    user,
    loading,
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