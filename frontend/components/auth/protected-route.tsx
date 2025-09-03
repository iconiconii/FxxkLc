"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { Spinner } from "../ui/spinner"

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredAuth?: boolean
  redirectTo?: string
}

export default function ProtectedRoute({ 
  children, 
  requiredAuth = true,
  redirectTo = "/login"
}: ProtectedRouteProps) {
  const { isAuthenticated, loading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    // Only redirect after loading is complete and initial auth check is done
    if (!loading) {
      if (requiredAuth && !isAuthenticated) {
        console.log('ProtectedRoute: Not authenticated, redirecting to login')
        router.push(redirectTo)
      } else if (!requiredAuth && isAuthenticated) {
        // If user is authenticated but doesn't need to be (like login page), redirect to dashboard
        console.log('ProtectedRoute: User already authenticated, redirecting to dashboard')
        router.push('/dashboard')
      }
    }
  }, [isAuthenticated, loading, requiredAuth, router, redirectTo])

  // Show loading spinner while checking authentication
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="flex flex-col items-center gap-4">
          <Spinner className="h-8 w-8" />
          <p className="text-gray-500 dark:text-gray-400">验证登录状态中...</p>
        </div>
      </div>
    )
  }

  // If authentication is required and user is not authenticated, don't render children
  if (requiredAuth && !isAuthenticated) {
    return null // Will redirect in useEffect
  }

  // If authentication is not required but user is authenticated, don't render children (will redirect)
  if (!requiredAuth && isAuthenticated) {
    return null // Will redirect in useEffect
  }

  return <>{children}</>
}
