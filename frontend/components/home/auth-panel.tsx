"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight, UserPlus, LogIn, Sparkles } from "lucide-react"
import { useAuth } from "@/lib/auth-context"
import UserPanel from "./user-panel"

export default function AuthPanel() {
  const { isAuthenticated, loading } = useAuth()

  // Show loading state
  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">加载中...</div>
      </div>
    )
  }

  // If user is authenticated, show UserPanel
  if (isAuthenticated) {
    return <UserPanel />
  }

  // If user is not authenticated, show auth options
  return (
    <div className="h-full space-y-6">
      {/* 欢迎卡片 */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-8 border border-white/20 dark:border-gray-700/50 shadow-xl text-center">
        <div className="w-16 h-16 bg-gradient-to-br from-blue-400/20 to-indigo-400/20 rounded-3xl flex items-center justify-center mx-auto mb-6">
          <Sparkles className="w-8 h-8 text-blue-500" />
        </div>
        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-3">
          开始你的算法之旅
        </h2>
        <p className="text-gray-600 dark:text-gray-400 text-sm leading-relaxed">
          加入 OLIVER 社区，与万千程序员一起刷题，提升编程技能，冲刺大厂面试
        </p>
      </div>

      {/* 登录卡片 */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-12 h-12 bg-gradient-to-br from-green-400/20 to-emerald-400/20 rounded-2xl flex items-center justify-center">
            <LogIn className="w-6 h-6 text-green-600" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-800 dark:text-white">已有账户？</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">继续你的学习进度</p>
          </div>
        </div>
        <Link href="/login">
          <Button 
            className="w-full bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600 text-white rounded-2xl py-3 shadow-lg hover:shadow-xl transition-all duration-200 group"
          >
            <span>立即登录</span>
            <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
          </Button>
        </Link>
      </div>

      {/* 注册卡片 */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-12 h-12 bg-gradient-to-br from-purple-400/20 to-violet-400/20 rounded-2xl flex items-center justify-center">
            <UserPlus className="w-6 h-6 text-purple-600" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-800 dark:text-white">新用户？</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">免费注册开始刷题</p>
          </div>
        </div>
        <Link href="/register">
          <Button 
            variant="outline"
            className="w-full border-2 border-purple-200 hover:border-purple-400 hover:bg-purple-50 dark:border-purple-700 dark:hover:border-purple-500 dark:hover:bg-purple-900/20 text-purple-600 dark:text-purple-400 rounded-2xl py-3 transition-all duration-200 group"
          >
            <span>免费注册</span>
            <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
          </Button>
        </Link>
      </div>

      {/* FSRS 核心功能介绍 */}
      <div className="bg-white/60 dark:bg-gray-800/60 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-lg">
        <div className="text-center mb-4">
          <div className="w-12 h-12 bg-gradient-to-br from-teal-400/20 to-emerald-400/20 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <span className="text-xl">🧠</span>
          </div>
          <h3 className="font-semibold text-gray-800 dark:text-white text-sm">智能复习系统</h3>
        </div>
        <div className="space-y-3">
          <div className="flex items-start gap-3 text-sm">
            <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full mt-2 flex-shrink-0"></div>
            <div>
              <div className="font-medium text-gray-700 dark:text-gray-300">间隔重复算法</div>
              <div className="text-xs text-gray-500 dark:text-gray-400">科学安排复习时间，提高记忆效率</div>
            </div>
          </div>
          <div className="flex items-start gap-3 text-sm">
            <div className="w-1.5 h-1.5 bg-blue-500 rounded-full mt-2 flex-shrink-0"></div>
            <div>
              <div className="font-medium text-gray-700 dark:text-gray-300">个性化参数</div>
              <div className="text-xs text-gray-500 dark:text-gray-400">根据你的表现调整学习难度</div>
            </div>
          </div>
          <div className="flex items-start gap-3 text-sm">
            <div className="w-1.5 h-1.5 bg-purple-500 rounded-full mt-2 flex-shrink-0"></div>
            <div>
              <div className="font-medium text-gray-700 dark:text-gray-300">进度追踪</div>
              <div className="text-xs text-gray-500 dark:text-gray-400">可视化学习成果和掌握程度</div>
            </div>
          </div>
        </div>
        <div className="mt-4 pt-4 border-t border-gray-200/50 dark:border-gray-600/50">
          <div className="text-center text-xs text-gray-500 dark:text-gray-400">
            注册后立即体验 FSRS 算法
          </div>
        </div>
      </div>
    </div>
  )
}