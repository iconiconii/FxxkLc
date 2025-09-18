"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Trophy, Target, Zap, Calendar, Crown, Medal, Award } from "lucide-react"
import { leaderboardApi, type LeaderboardEntry, type StreakLeaderboardEntry, type TopPerformersSummary } from "@/lib/leaderboard-api"
import Image from "next/image"

const rankIcons = {
  1: <Crown className="h-5 w-5 text-yellow-500" />,
  2: <Medal className="h-5 w-5 text-gray-400" />,
  3: <Award className="h-5 w-5 text-amber-600" />,
}

const getRankBadge = (rank: number) => {
  if (rank === 1) return "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300"
  if (rank === 2) return "bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300"
  if (rank === 3) return "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-300"
  return "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300"
}

function LeaderboardList({ 
  entries, 
  title, 
  icon, 
  loading,
  type = 'general'
}: { 
  entries: any[]
  title: string
  icon: React.ReactNode
  loading: boolean
  type?: 'general' | 'streak'
}) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {icon}
            {title}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-12">
            <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
              <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
              加载中...
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (entries.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {icon}
            {title}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-12">
            <div className="text-gray-500 dark:text-gray-400">
              暂无数据，可能是服务器连接问题
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          {icon}
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {entries.map((entry, index) => (
            <div
              key={entry.userId}
              className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
            >
              <div className="flex items-center gap-4">
                <div className={`flex items-center justify-center w-8 h-8 ${getRankBadge(entry.rank)} rounded-full font-bold text-sm`}>
                  {entry.rank <= 3 ? rankIcons[entry.rank as keyof typeof rankIcons] : entry.rank}
                </div>
                <div className="flex items-center gap-3">
                  <Image
                    src={entry.avatarUrl || "https://ferf1mheo22r9ira.public.blob.vercel-storage.com/avatar-01-n0x8HFv8EUetf9z6ht0wScJKoTHqf8.png"}
                    alt={`${entry.username} avatar`}
                    width={32}
                    height={32}
                    className="rounded-full"
                  />
                  <div>
                    <div className="font-medium text-gray-900 dark:text-gray-100">
                      {entry.username}
                    </div>
                    {entry.badge && (
                      <Badge variant="outline" className="text-xs mt-1">
                        {entry.badge}
                      </Badge>
                    )}
                  </div>
                </div>
              </div>
              <div className="text-right">
                {type === 'general' && (
                  <>
                    <div className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      {entry.totalReviews.toLocaleString()}
                    </div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">
                      掌握度: {entry.masteryScore ? entry.masteryScore.toFixed(1) + '%' : 'N/A'}
                    </div>
                    {entry.streak > 0 && (
                      <div className="text-sm text-orange-500">
                        🔥 {entry.streak} 天
                      </div>
                    )}
                  </>
                )}
                {type === 'streak' && (
                  <>
                    <div className="text-lg font-bold text-orange-600 dark:text-orange-400">
                      🔥 {entry.currentStreak} 天
                    </div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">
                      最长: {entry.longestStreak} 天
                    </div>
                    <div className="text-xs text-gray-400 dark:text-gray-500">
                      总计: {entry.totalActiveDays} 天
                    </div>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

export default function LeaderboardPage() {
  const [globalLeaderboard, setGlobalLeaderboard] = useState<LeaderboardEntry[]>([])
  const [weeklyLeaderboard, setWeeklyLeaderboard] = useState<LeaderboardEntry[]>([])
  const [monthlyLeaderboard, setMonthlyLeaderboard] = useState<LeaderboardEntry[]>([])
  const [streakLeaderboard, setStreakLeaderboard] = useState<StreakLeaderboardEntry[]>([])
  const [topPerformers, setTopPerformers] = useState<TopPerformersSummary | null>(null)
  const [loading, setLoading] = useState({
    global: true,
    weekly: true,
    monthly: true,
    streak: true,
    stats: true,
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchLeaderboards = async () => {
      try {
        // Fetch all leaderboards in parallel
        const [global, weekly, monthly, streak, stats] = await Promise.allSettled([
          leaderboardApi.getGlobalLeaderboard(50),
          leaderboardApi.getWeeklyLeaderboard(50),
          leaderboardApi.getMonthlyLeaderboard(50),
          leaderboardApi.getStreakLeaderboard(50),
          leaderboardApi.getLeaderboardStats(),
        ])

        let hasAnySuccess = false

        if (global.status === 'fulfilled') {
          setGlobalLeaderboard(global.value)
          hasAnySuccess = true
        } else {
          console.warn('Global leaderboard failed:', global.reason)
        }

        if (weekly.status === 'fulfilled') {
          setWeeklyLeaderboard(weekly.value)
          hasAnySuccess = true
        } else {
          console.warn('Weekly leaderboard failed:', weekly.reason)
        }

        if (monthly.status === 'fulfilled') {
          setMonthlyLeaderboard(monthly.value)
          hasAnySuccess = true
        } else {
          console.warn('Monthly leaderboard failed:', monthly.reason)
        }


        if (streak.status === 'fulfilled') {
          setStreakLeaderboard(streak.value)
          hasAnySuccess = true
        } else {
          console.warn('Streak leaderboard failed:', streak.reason)
        }

        if (stats.status === 'fulfilled') {
          setTopPerformers(stats.value)
          hasAnySuccess = true
        } else {
          console.warn('Stats failed:', stats.reason)
        }

        // Update loading states
        setLoading({
          global: global.status !== 'fulfilled',
          weekly: weekly.status !== 'fulfilled',
          monthly: monthly.status !== 'fulfilled',
          streak: streak.status !== 'fulfilled',
          stats: stats.status !== 'fulfilled',
        })

        // Only show error if ALL requests failed
        if (!hasAnySuccess) {
          console.error('All leaderboard requests failed')
          setError('加载排行榜失败，请稍后重试')
        }
      } catch (err: any) {
        console.error('Failed to fetch leaderboards:', err)
        setError('加载排行榜失败，请稍后重试')
        setLoading({
          global: false,
          weekly: false,
          monthly: false,
          streak: false,
          stats: false,
        })
      }
    }

    fetchLeaderboards()
  }, [])

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">排行榜</h1>
        </div>
        <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">排行榜</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">学习达人们的较量</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总排行</CardTitle>
            <Trophy className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{globalLeaderboard.length}</div>
            <p className="text-xs text-muted-foreground">活跃用户</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">本周活跃</CardTitle>
            <Calendar className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{weeklyLeaderboard.length}</div>
            <p className="text-xs text-muted-foreground">用户参与</p>
          </CardContent>
        </Card>


        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">连续学习</CardTitle>
            <Zap className="h-4 w-4 text-orange-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{streakLeaderboard.length}</div>
            <p className="text-xs text-muted-foreground">坚持者</p>
          </CardContent>
        </Card>
      </div>

      {/* Top Performers Summary */}
      {topPerformers && !loading.stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Trophy className="h-4 w-4 text-yellow-500" />
                练习之王
              </CardTitle>
            </CardHeader>
            <CardContent>
              {topPerformers.topByVolume.slice(0, 3).map((entry, index) => (
                <div key={entry.userId} className="flex items-center gap-2 mb-2">
                  <div className="text-lg">{['🥇', '🥈', '🥉'][index]}</div>
                  <div className="text-sm font-medium">{entry.username}</div>
                  <div className="text-xs text-gray-500 ml-auto">{entry.totalReviews} 题</div>
                </div>
              ))}
            </CardContent>
          </Card>


          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Zap className="h-4 w-4 text-orange-500" />
                坚持达人
              </CardTitle>
            </CardHeader>
            <CardContent>
              {topPerformers.topByStreak.slice(0, 3).map((entry, index) => (
                <div key={entry.userId} className="flex items-center gap-2 mb-2">
                  <div className="text-lg">{['🔥', '💪', '⚡'][index]}</div>
                  <div className="text-sm font-medium">{entry.username}</div>
                  <div className="text-xs text-gray-500 ml-auto">{entry.currentStreak} 天</div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      <Tabs defaultValue="global" className="space-y-6">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="global" className="flex items-center gap-2">
            <Trophy className="h-4 w-4" />
            总榜
          </TabsTrigger>
          <TabsTrigger value="weekly" className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            周榜
          </TabsTrigger>
          <TabsTrigger value="monthly" className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            月榜
          </TabsTrigger>
          <TabsTrigger value="streak" className="flex items-center gap-2">
            <Zap className="h-4 w-4" />
            连续学习
          </TabsTrigger>
        </TabsList>

        <TabsContent value="global">
          <LeaderboardList
            entries={globalLeaderboard}
            title="全球总排行榜"
            icon={<Trophy className="h-5 w-5 text-yellow-500" />}
            loading={loading.global}
            type="general"
          />
        </TabsContent>

        <TabsContent value="weekly">
          <LeaderboardList
            entries={weeklyLeaderboard}
            title="本周排行榜"
            icon={<Calendar className="h-5 w-5 text-blue-500" />}
            loading={loading.weekly}
            type="general"
          />
        </TabsContent>

        <TabsContent value="monthly">
          <LeaderboardList
            entries={monthlyLeaderboard}
            title="本月排行榜"
            icon={<Calendar className="h-5 w-5 text-purple-500" />}
            loading={loading.monthly}
            type="general"
          />
        </TabsContent>


        <TabsContent value="streak">
          <LeaderboardList
            entries={streakLeaderboard}
            title="连续学习排行榜"
            icon={<Zap className="h-5 w-5 text-orange-500" />}
            loading={loading.streak}
            type="streak"
          />
        </TabsContent>
      </Tabs>
    </div>
  )
}