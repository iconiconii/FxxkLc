"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { User, TrendingUp, Award, Calendar, LogOut } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { useAuth } from "@/lib/auth-context"
import { userApi, type UserProblemStatus } from "@/lib/user-api"

interface UserStats {
  totalSolved: number
  easyCount: number
  mediumCount: number
  hardCount: number
  streak: number
}

export default function UserPanel() {
  const router = useRouter()
  const { user, logout } = useAuth()
  const [stats, setStats] = useState<UserStats>({
    totalSolved: 0,
    easyCount: 0,
    mediumCount: 0,
    hardCount: 0,
    streak: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadUserStats = async () => {
      // Only load stats if user is available (authenticated)
      if (!user?.id) {
        setLoading(false)
        return
      }

      try {
        const userStatuses = await userApi.getUserProblemProgress()
        
        const solvedProblems = userStatuses.filter(status => status.status === "done")
        const easyCount = solvedProblems.filter(p => p.difficulty?.toLowerCase() === "easy").length
        const mediumCount = solvedProblems.filter(p => p.difficulty?.toLowerCase() === "medium").length
        const hardCount = solvedProblems.filter(p => p.difficulty?.toLowerCase() === "hard").length
        
        setStats({
          totalSolved: solvedProblems.length,
          easyCount,
          mediumCount,
          hardCount,
          streak: 7, // 这里可以从API获取连续学习天数
        })
      } catch (error) {
        console.error('Failed to load user stats:', error)
        // Set default stats on error
        setStats({
          totalSolved: 0,
          easyCount: 0,
          mediumCount: 0,
          hardCount: 0,
          streak: 0,
        })
      } finally {
        setLoading(false)
      }
    }

    loadUserStats()
  }, [user?.id])

  const handleLogout = () => {
    logout()
  }

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
        <div className="text-gray-500 dark:text-gray-400">加载中...</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* User Info Card */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl text-center">
        <div className="w-20 h-20 bg-gradient-to-br from-emerald-400 to-blue-500 rounded-3xl flex items-center justify-center mx-auto mb-4">
          <User className="w-10 h-10 text-white" />
        </div>
        <div>
          <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-1">
            {user?.email?.split('@')[0] || '用户'}
          </h3>
          <p className="text-sm text-gray-600 dark:text-gray-400">{user?.email}</p>
          <div className="inline-flex items-center gap-1 mt-2 px-3 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded-full text-xs font-medium">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span>已登录</span>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="space-y-4">
        {/* Total Solved */}
        <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-gradient-to-br from-emerald-400 to-green-500 rounded-2xl flex items-center justify-center">
              <Award className="w-7 h-7 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">已解决题目</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white">{stats.totalSolved}</p>
            </div>
            <div className="text-right">
              <span className="text-2xl">🏆</span>
            </div>
          </div>
        </div>

        {/* Difficulty Breakdown */}
        <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
          <div className="flex items-center gap-2 mb-4">
            <span className="text-lg">📊</span>
            <h4 className="font-semibold text-gray-900 dark:text-white">难度分布</h4>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div className="text-center p-3 bg-gradient-to-br from-emerald-50 to-green-100 dark:from-emerald-900/20 dark:to-green-800/20 rounded-2xl border border-emerald-200 dark:border-emerald-700">
              <p className="text-2xl font-bold text-emerald-700 dark:text-emerald-300">{stats.easyCount}</p>
              <p className="text-xs text-emerald-600 dark:text-emerald-400 font-medium">简单</p>
            </div>
            <div className="text-center p-3 bg-gradient-to-br from-amber-50 to-orange-100 dark:from-amber-900/20 dark:to-orange-800/20 rounded-2xl border border-amber-200 dark:border-amber-700">
              <p className="text-2xl font-bold text-amber-700 dark:text-amber-300">{stats.mediumCount}</p>
              <p className="text-xs text-amber-600 dark:text-amber-400 font-medium">中等</p>
            </div>
            <div className="text-center p-3 bg-gradient-to-br from-rose-50 to-red-100 dark:from-rose-900/20 dark:to-red-800/20 rounded-2xl border border-rose-200 dark:border-rose-700">
              <p className="text-2xl font-bold text-rose-700 dark:text-rose-300">{stats.hardCount}</p>
              <p className="text-xs text-rose-600 dark:text-rose-400 font-medium">困难</p>
            </div>
          </div>
        </div>

        {/* Streak */}
        <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-gradient-to-br from-violet-400 to-purple-500 rounded-2xl flex items-center justify-center">
              <TrendingUp className="w-7 h-7 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">连续学习</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white">{stats.streak} <span className="text-lg text-gray-500">天</span></p>
            </div>
            <div className="text-right">
              <span className="text-2xl">🔥</span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-3xl p-6 border border-white/20 dark:border-gray-700/50 shadow-xl space-y-4">
        <div className="flex items-center gap-2 mb-4">
          <span className="text-lg">⚡</span>
          <h4 className="font-semibold text-gray-900 dark:text-white">快速操作</h4>
        </div>
        
        <Button
          onClick={() => router.push('/dashboard')}
          className="w-full rounded-2xl py-3 bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 transition-all duration-200 shadow-lg hover:shadow-xl"
        >
          <Calendar className="w-4 h-4 mr-2" />
          📈 查看详细进度
        </Button>
        
        <Button
          onClick={() => router.push('/review')}
          className="w-full rounded-2xl py-3 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 transition-all duration-200 shadow-lg hover:shadow-xl"
        >
          <TrendingUp className="w-4 h-4 mr-2" />
          🎯 今日复习
        </Button>

        <Button
          onClick={handleLogout}
          variant="outline"
          className="w-full rounded-2xl py-3 border-2 border-gray-200 hover:border-red-300 hover:bg-red-50 dark:border-gray-600 dark:hover:border-red-500 dark:hover:bg-red-900/20 transition-all duration-200"
        >
          <LogOut className="w-4 h-4 mr-2" />
          👋 退出登录
        </Button>
      </div>
    </div>
  )
}