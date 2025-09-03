"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/lib/auth-context"

interface LoginFormProps {
  showRegister: boolean
  onToggleRegister: () => void
}

export default function LoginForm({ showRegister, onToggleRegister }: LoginFormProps) {
  const { login } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: "",
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      if (showRegister) {
        // 注册功能暂时关闭
        setError("注册功能暂时关闭，请稍后再试")
        return
      } else {
        // 登录逻辑
        await login({
          identifier: formData.email,
          password: formData.password,
        })
      }
    } catch (err) {
      setError((err as Error)?.message || (showRegister ? "注册失败" : "登录失败"))
    } finally {
      setLoading(false)
    }
  }

  const handleGitHubLogin = () => {
    window.location.href = "/api/v1/auth/oauth2/github"
  }

  const handleGoogleLogin = () => {
    window.location.href = "/api/v1/auth/oauth2/google"
  }

  return (
    <div className="space-y-6">
      {/* Header Card */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl text-center">
        <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-3xl flex items-center justify-center mx-auto mb-4">
          <span className="text-2xl">{showRegister ? "🚀" : "👋"}</span>
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
          {showRegister ? "加入 OLIVER" : "欢迎回来"}
        </h2>
        <p className="text-gray-600 dark:text-gray-400">
          {showRegister ? "开始你的算法练习之旅" : "继续你的学习进度"}
        </p>
      </div>

      {/* OAuth Buttons Card */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl space-y-4">
        <div className="text-center mb-4">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">快速登录</h3>
          <div className="w-12 h-px bg-gradient-to-r from-transparent via-gray-300 dark:via-gray-600 to-transparent mx-auto"></div>
        </div>
        
        <Button
          type="button"
          variant="outline"
          className="w-full flex items-center gap-3 rounded-2xl py-3 border-2 border-gray-200 hover:border-gray-800 dark:border-gray-600 dark:hover:border-gray-400 transition-all duration-200 hover:shadow-lg"
          onClick={handleGitHubLogin}
        >
          <div className="w-8 h-8 bg-gray-900 dark:bg-white rounded-full flex items-center justify-center">
            <svg className="w-5 h-5 text-white dark:text-gray-900" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
          </div>
          <span className="font-medium">使用 GitHub 继续</span>
        </Button>

        <Button
          type="button"
          variant="outline"
          className="w-full flex items-center gap-3 rounded-2xl py-3 border-2 border-gray-200 hover:border-blue-400 dark:border-gray-600 dark:hover:border-blue-500 transition-all duration-200 hover:shadow-lg"
          onClick={handleGoogleLogin}
        >
          <div className="w-8 h-8 bg-white rounded-full flex items-center justify-center shadow-sm">
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
          </div>
          <span className="font-medium">使用 Google 继续</span>
        </Button>
      </div>

      {/* Form Card */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
        {/* Divider */}
        <div className="flex items-center mb-6">
          <div className="flex-1 h-px bg-gradient-to-r from-transparent via-gray-300 dark:via-gray-600 to-transparent"></div>
          <span className="px-4 text-sm text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 rounded-full">或者使用邮箱</span>
          <div className="flex-1 h-px bg-gradient-to-l from-transparent via-gray-300 dark:via-gray-600 to-transparent"></div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-medium text-gray-700 dark:text-gray-300">邮箱地址</Label>
            <Input
              id="email"
              type="email"
              placeholder="your@email.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
              className="rounded-2xl border-2 border-gray-200 focus:border-blue-400 dark:border-gray-600 dark:focus:border-blue-500 transition-all duration-200"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password" className="text-sm font-medium text-gray-700 dark:text-gray-300">密码</Label>
            <Input
              id="password"
              type="password"
              placeholder="••••••••"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
              className="rounded-2xl border-2 border-gray-200 focus:border-blue-400 dark:border-gray-600 dark:focus:border-blue-500 transition-all duration-200"
            />
          </div>

          {showRegister && (
            <div className="space-y-2">
              <Label htmlFor="confirmPassword" className="text-sm font-medium text-gray-700 dark:text-gray-300">确认密码</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                value={formData.confirmPassword}
                onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                required
                className="rounded-2xl border-2 border-gray-200 focus:border-blue-400 dark:border-gray-600 dark:focus:border-blue-500 transition-all duration-200"
              />
            </div>
          )}

          {error && (
            <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-4 rounded-2xl border border-red-200 dark:border-red-800">
              <div className="flex items-center gap-2">
                <span className="text-lg">⚠️</span>
                <span>{error}</span>
              </div>
            </div>
          )}

          <Button 
            type="submit" 
            className="w-full rounded-2xl py-3 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 transition-all duration-200 shadow-lg hover:shadow-xl" 
            disabled={loading}
          >
            {loading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                <span>处理中...</span>
              </div>
            ) : (
              <span>{showRegister ? "🚀 创建账户" : "🎯 立即登录"}</span>
            )}
          </Button>
        </form>

        {/* Toggle */}
        <div className="text-center text-sm mt-6 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-2xl">
          <span className="text-gray-600 dark:text-gray-400">
            {showRegister ? "已有账户？" : "注册功能暂时关闭"}
          </span>
          {showRegister ? (
            <Button
              type="button"
              variant="link"
              className="p-0 ml-1 text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
              onClick={onToggleRegister}
            >
              立即登录
            </Button>
          ) : (
            <span className="ml-1 text-gray-500 dark:text-gray-400">，仅限已有账户登录</span>
          )}
        </div>
      </div>
    </div>
  )
}