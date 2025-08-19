"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Eye, EyeOff, Github, Mail, User, Check, X } from "lucide-react"
import { FcGoogle } from "react-icons/fc"
import { useAuth } from "@/lib/auth-context"

interface PasswordStrength {
  score: number
  feedback: string[]
}

export default function RegisterForm() {
  const router = useRouter()
  const { register: registerUser } = useAuth()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
    agreeToTerms: false,
  })

  const checkPasswordStrength = (password: string): PasswordStrength => {
    let score = 0
    const feedback: string[] = []

    if (password.length >= 8) score++
    else feedback.push("至少8个字符")

    if (/[A-Z]/.test(password)) score++
    else feedback.push("包含大写字母")

    if (/[a-z]/.test(password)) score++
    else feedback.push("包含小写字母")

    if (/\d/.test(password)) score++
    else feedback.push("包含数字")

    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score++
    else feedback.push("包含特殊字符")

    return { score, feedback }
  }

  const passwordStrength = checkPasswordStrength(formData.password)
  const passwordsMatch = formData.password === formData.confirmPassword && formData.confirmPassword !== ""

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!formData.agreeToTerms) {
      setError("请同意服务条款和隐私政策")
      return
    }
    if (!passwordsMatch) {
      setError("密码不匹配")
      return
    }
    if (passwordStrength.score < 3) {
      setError("密码强度太低，请选择更强的密码")
      return
    }

    setIsLoading(true)

    try {
      // Use AuthContext's register method to properly update user state
      await registerUser({
        username: formData.username,
        email: formData.email,
        password: formData.password,
      })

      // Registration successful, redirect to dashboard
      router.push('/dashboard')
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '注册失败，请检查输入信息'
      setError(errorMessage)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSocialLogin = (provider: "google" | "github") => {
    console.log(`Register with ${provider}`)
    // Implement social login logic here
  }

  const getPasswordStrengthColor = (score: number) => {
    if (score <= 2) return "bg-red-500"
    if (score <= 3) return "bg-orange-500"
    if (score <= 4) return "bg-yellow-500"
    return "bg-green-500"
  }

  const getPasswordStrengthText = (score: number) => {
    if (score <= 2) return "弱"
    if (score <= 3) return "中等"
    if (score <= 4) return "强"
    return "很强"
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">创建账户</h2>
        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">加入 OLIVER，开始您的算法学习之旅</p>
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
          <span className="text-gray-700 dark:text-gray-300">使用 Google 注册</span>
        </Button>

        <Button
          type="button"
          variant="outline"
          className="w-full flex items-center justify-center gap-3 h-11 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 bg-transparent"
          onClick={() => handleSocialLogin("github")}
        >
          <Github className="h-5 w-5 text-gray-700 dark:text-gray-300" />
          <span className="text-gray-700 dark:text-gray-300">使用 GitHub 注册</span>
        </Button>
      </div>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <Separator className="w-full" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="px-2 bg-white dark:bg-[#1F1F23] text-gray-500 dark:text-gray-400">或者使用邮箱注册</span>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Registration Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <Label htmlFor="username" className="text-gray-700 dark:text-gray-300">
            用户名
          </Label>
          <div className="mt-1 relative">
            <Input
              id="username"
              type="text"
              required
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              className="pl-10 h-11"
              placeholder="请输入用户名"
            />
            <User className="h-4 w-4 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
          </div>
        </div>

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
              placeholder="请输入密码"
            />
            <button
              type="button"
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>

          {/* Password Strength Indicator */}
          {formData.password && (
            <div className="mt-2 space-y-2">
              <div className="flex items-center gap-2">
                <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-300 ${getPasswordStrengthColor(passwordStrength.score)}`}
                    style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                  />
                </div>
                <span className="text-xs text-gray-600 dark:text-gray-400">
                  {getPasswordStrengthText(passwordStrength.score)}
                </span>
              </div>
              {passwordStrength.feedback.length > 0 && (
                <div className="text-xs text-gray-500 dark:text-gray-400">
                  <p>密码需要包含：</p>
                  <ul className="mt-1 space-y-1">
                    {passwordStrength.feedback.map((item, index) => (
                      <li key={index} className="flex items-center gap-1">
                        <X className="h-3 w-3 text-red-500" />
                        {item}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>

        <div>
          <Label htmlFor="confirmPassword" className="text-gray-700 dark:text-gray-300">
            确认密码
          </Label>
          <div className="mt-1 relative">
            <Input
              id="confirmPassword"
              type={showConfirmPassword ? "text" : "password"}
              required
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              className="pr-10 h-11"
              placeholder="请再次输入密码"
            />
            <button
              type="button"
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
            >
              {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {formData.confirmPassword && (
            <div className="mt-1 flex items-center gap-1 text-xs">
              {passwordsMatch ? (
                <>
                  <Check className="h-3 w-3 text-green-500" />
                  <span className="text-green-600 dark:text-green-400">密码匹配</span>
                </>
              ) : (
                <>
                  <X className="h-3 w-3 text-red-500" />
                  <span className="text-red-600 dark:text-red-400">密码不匹配</span>
                </>
              )}
            </div>
          )}
        </div>

        <div className="flex items-start">
          <div className="flex items-center h-5">
            <input
              id="agreeToTerms"
              name="agreeToTerms"
              type="checkbox"
              required
              checked={formData.agreeToTerms}
              onChange={(e) => setFormData({ ...formData, agreeToTerms: e.target.checked })}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
          </div>
          <div className="ml-3 text-sm">
            <label htmlFor="agreeToTerms" className="text-gray-700 dark:text-gray-300">
              我同意{" "}
              <Link href="/terms" className="text-blue-600 hover:text-blue-500 dark:text-blue-400">
                服务条款
              </Link>{" "}
              和{" "}
              <Link href="/privacy" className="text-blue-600 hover:text-blue-500 dark:text-blue-400">
                隐私政策
              </Link>
            </label>
          </div>
        </div>

        <Button type="submit" className="w-full h-11 bg-blue-600 hover:bg-blue-700" disabled={isLoading}>
          {isLoading ? "注册中..." : "创建账户"}
        </Button>
      </form>

      <div className="text-center">
        <span className="text-sm text-gray-600 dark:text-gray-400">已有账户？</span>
        <Link
          href="/login"
          className="ml-1 text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
        >
          立即登录
        </Link>
      </div>
    </div>
  )
}
