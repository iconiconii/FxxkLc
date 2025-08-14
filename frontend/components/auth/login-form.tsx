"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Eye, EyeOff, Github, Mail } from "lucide-react"
import { FcGoogle } from "react-icons/fc"
import { useAuth } from "@/lib/auth-context"

export default function LoginForm() {
  const router = useRouter()
  const { login } = useAuth()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError(null)

    try {
      // Use AuthContext's login method to properly update user state
      await login({
        identifier: formData.email,
        password: formData.password,
      })

      // Login successful, redirect to dashboard
      router.push('/dashboard')
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '登录失败，请检查邮箱和密码'
      setError(errorMessage)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSocialLogin = (provider: "google" | "github") => {
    console.log(`Login with ${provider}`)
    // Implement social login logic here
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">欢迎回来</h2>
        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">登录您的 CodeTop 账户</p>
      </div>

      {/* Social Login Buttons */}
      <div className="space-y-3">
        <Button
          type="button"
          variant="outline"
          className="w-full flex items-center justify-center gap-3 h-11 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 bg-transparent"
          onClick={() => handleSocialLogin("google")}
        >
          <FcGoogle className="h-5 w-5" />
          <span className="text-gray-700 dark:text-gray-300">使用 Google 登录</span>
        </Button>

        <Button
          type="button"
          variant="outline"
          className="w-full flex items-center justify-center gap-3 h-11 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 bg-transparent"
          onClick={() => handleSocialLogin("github")}
        >
          <Github className="h-5 w-5 text-gray-700 dark:text-gray-300" />
          <span className="text-gray-700 dark:text-gray-300">使用 GitHub 登录</span>
        </Button>
      </div>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <Separator className="w-full" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="px-2 bg-white dark:bg-[#1F1F23] text-gray-500 dark:text-gray-400">或者使用邮箱登录</span>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Email/Password Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <Label htmlFor="email" className="text-gray-700 dark:text-gray-300">
            邮箱地址
          </Label>
          <div className="mt-1 relative">
            <Input
              id="email"
              type="email"
              required
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="pl-10 h-11"
              placeholder="请输入您的邮箱"
            />
            <Mail className="h-4 w-4 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
          </div>
        </div>

        <div>
          <Label htmlFor="password" className="text-gray-700 dark:text-gray-300">
            密码
          </Label>
          <div className="mt-1 relative">
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              required
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              className="pr-10 h-11"
              placeholder="请输入您的密码"
            />
            <button
              type="button"
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <input
              id="remember-me"
              name="remember-me"
              type="checkbox"
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700 dark:text-gray-300">
              记住我
            </label>
          </div>

          <div className="text-sm">
            <Link
              href="/forgot-password"
              className="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
            >
              忘记密码？
            </Link>
          </div>
        </div>

        <Button type="submit" className="w-full h-11 bg-blue-600 hover:bg-blue-700" disabled={isLoading}>
          {isLoading ? "登录中..." : "登录"}
        </Button>
      </form>

      <div className="text-center">
        <span className="text-sm text-gray-600 dark:text-gray-400">还没有账户？</span>
        <Link
          href="/register"
          className="ml-1 text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
        >
          立即注册
        </Link>
      </div>
    </div>
  )
}
