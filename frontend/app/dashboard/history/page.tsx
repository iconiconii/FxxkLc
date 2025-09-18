'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { Card, CardHeader, CardContent, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Calendar, Clock, TrendingUp, TrendingDown, BarChart3, ArrowLeft, Brain, Settings } from 'lucide-react'

interface RecommendationHistoryItem {
  id: string
  problemId: number
  problemTitle: string
  recommendedAt: string
  source: 'LLM' | 'FSRS' | 'CACHE'
  provider?: string
  model?: string
  reason: string
  confidence: number
  score: number
  action?: 'ATTEMPTED' | 'SOLVED' | 'SKIPPED' | 'BOOKMARKED'
  rating?: number
  timeSpentMinutes?: number
  solved: boolean
  feedback?: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  topics: string[]
}

interface HistoryStats {
  total: number
  attempted: number
  solved: number
  skipped: number
  avgRating: number
  avgConfidence: number
  avgTimeSpent: number
  sourceBreakdown: Record<string, number>
  difficultyBreakdown: Record<string, number>
}

export default function RecommendationHistoryPage() {
  const [historyData, setHistoryData] = useState<RecommendationHistoryItem[]>([])
  const [stats, setStats] = useState<HistoryStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [timeRange, setTimeRange] = useState('7') // days
  const [filterSource, setFilterSource] = useState<string>('all')
  const [filterAction, setFilterAction] = useState<string>('all')

  useEffect(() => {
    fetchRecommendationHistory()
  }, [timeRange, filterSource, filterAction])

  const fetchRecommendationHistory = async () => {
    setLoading(true)
    try {
      // Mock data for demonstration - replace with actual API call
      const mockHistory: RecommendationHistoryItem[] = [
        {
          id: '1',
          problemId: 1,
          problemTitle: '两数之和',
          recommendedAt: '2025-01-14T10:30:00Z',
          source: 'LLM',
          provider: 'deepseek',
          model: 'deepseek-chat',
          reason: '基于您的数组基础薄弱，推荐从简单的哈希表应用开始',
          confidence: 0.85,
          score: 0.82,
          action: 'SOLVED',
          rating: 5,
          timeSpentMinutes: 25,
          solved: true,
          feedback: '很好的推荐，难度合适',
          difficulty: 'EASY',
          topics: ['数组', '哈希表']
        },
        {
          id: '2',
          problemId: 15,
          problemTitle: '三数之和',
          recommendedAt: '2025-01-14T09:15:00Z',
          source: 'LLM',
          provider: 'deepseek',
          model: 'deepseek-chat',
          reason: '在掌握两数之和后，进阶到三数之和可以巩固双指针技巧',
          confidence: 0.78,
          score: 0.75,
          action: 'ATTEMPTED',
          rating: 4,
          timeSpentMinutes: 45,
          solved: false,
          difficulty: 'MEDIUM',
          topics: ['数组', '双指针', '排序']
        },
        {
          id: '3',
          problemId: 42,
          problemTitle: '接雨水',
          recommendedAt: '2025-01-13T16:20:00Z',
          source: 'FSRS',
          reason: 'FSRS fallback: urgency 0.85, 65% accuracy, 2.1 days overdue',
          confidence: 0.85,
          score: 0.85,
          action: 'SKIPPED',
          rating: 2,
          timeSpentMinutes: 5,
          solved: false,
          feedback: '对当前水平来说太难了',
          difficulty: 'HARD',
          topics: ['动态规划', '栈', '双指针']
        },
        {
          id: '4',
          problemId: 206,
          problemTitle: '反转链表',
          recommendedAt: '2025-01-13T14:10:00Z',
          source: 'LLM',
          provider: 'openai',
          model: 'gpt-3.5-turbo',
          reason: '链表基础操作，适合当前学习阶段',
          confidence: 0.90,
          score: 0.88,
          action: 'SOLVED',
          rating: 5,
          timeSpentMinutes: 20,
          solved: true,
          feedback: '经典题目，很有帮助',
          difficulty: 'EASY',
          topics: ['链表', '递归']
        },
        {
          id: '5',
          problemId: 3,
          problemTitle: '无重复字符的最长子串',
          recommendedAt: '2025-01-12T11:30:00Z',
          source: 'LLM',
          provider: 'deepseek',
          model: 'deepseek-chat',
          reason: '滑动窗口技巧的经典应用，适合提升字符串处理能力',
          confidence: 0.72,
          score: 0.70,
          action: 'BOOKMARKED',
          rating: 3,
          timeSpentMinutes: 0,
          solved: false,
          feedback: '先收藏，稍后学习',
          difficulty: 'MEDIUM',
          topics: ['哈希表', '字符串', '滑动窗口']
        }
      ]

      // Filter data based on time range
      const now = new Date()
      const cutoffTime = new Date(now.getTime() - parseInt(timeRange) * 24 * 60 * 60 * 1000)
      
      let filteredData = mockHistory.filter(item => 
        new Date(item.recommendedAt) >= cutoffTime
      )

      // Apply source filter
      if (filterSource !== 'all') {
        filteredData = filteredData.filter(item => item.source === filterSource)
      }

      // Apply action filter
      if (filterAction !== 'all') {
        filteredData = filteredData.filter(item => item.action === filterAction)
      }

      // Calculate stats
      const calculatedStats: HistoryStats = {
        total: filteredData.length,
        attempted: filteredData.filter(item => item.action === 'ATTEMPTED' || item.action === 'SOLVED').length,
        solved: filteredData.filter(item => item.solved).length,
        skipped: filteredData.filter(item => item.action === 'SKIPPED').length,
        avgRating: filteredData.filter(item => item.rating).reduce((sum, item) => sum + (item.rating || 0), 0) / filteredData.filter(item => item.rating).length || 0,
        avgConfidence: filteredData.reduce((sum, item) => sum + item.confidence, 0) / filteredData.length || 0,
        avgTimeSpent: filteredData.filter(item => item.timeSpentMinutes).reduce((sum, item) => sum + (item.timeSpentMinutes || 0), 0) / filteredData.filter(item => item.timeSpentMinutes).length || 0,
        sourceBreakdown: filteredData.reduce((acc, item) => {
          acc[item.source] = (acc[item.source] || 0) + 1
          return acc
        }, {} as Record<string, number>),
        difficultyBreakdown: filteredData.reduce((acc, item) => {
          acc[item.difficulty] = (acc[item.difficulty] || 0) + 1
          return acc
        }, {} as Record<string, number>)
      }

      setHistoryData(filteredData)
      setStats(calculatedStats)
    } catch (error) {
      console.error('Failed to fetch recommendation history:', error)
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
    
    if (diffDays === 0) {
      return '今天'
    } else if (diffDays === 1) {
      return '昨天'
    } else if (diffDays < 7) {
      return `${diffDays}天前`
    } else {
      return date.toLocaleDateString('zh-CN')
    }
  }

  const getActionColor = (action: string | undefined) => {
    switch (action) {
      case 'SOLVED':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
      case 'ATTEMPTED':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
      case 'SKIPPED':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200'
      case 'BOOKMARKED':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200'
    }
  }

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY':
        return 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300'
      case 'MEDIUM':
        return 'bg-orange-100 text-orange-700 dark:bg-orange-900 dark:text-orange-300'
      case 'HARD':
        return 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300'
      default:
        return 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">加载推荐历史...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-6xl mx-auto p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button
              onClick={() => window.history.back()}
              variant="outline"
              size="sm"
              className="flex items-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              返回
            </Button>
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                推荐历史
              </h1>
              <p className="text-gray-600 dark:text-gray-400 mt-1">
                查看您的推荐记录和学习统计
              </p>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            <Link href="/dashboard/recommendations">
              <Button variant="outline" size="sm" className="flex items-center gap-2">
                <Brain className="w-4 h-4" />
                获取推荐
              </Button>
            </Link>
            
            <Link href="/dashboard/preferences">
              <Button variant="outline" size="sm" className="flex items-center gap-2">
                <Settings className="w-4 h-4" />
                推荐设置
              </Button>
            </Link>
          </div>
        </div>

        {/* Filters */}
        <Card className="p-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                时间范围
              </label>
              <Select value={timeRange} onValueChange={setTimeRange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">最近 1 天</SelectItem>
                  <SelectItem value="7">最近 7 天</SelectItem>
                  <SelectItem value="30">最近 30 天</SelectItem>
                  <SelectItem value="90">最近 90 天</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                推荐来源
              </label>
              <Select value={filterSource} onValueChange={setFilterSource}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部来源</SelectItem>
                  <SelectItem value="LLM">AI 推荐</SelectItem>
                  <SelectItem value="FSRS">FSRS 推荐</SelectItem>
                  <SelectItem value="CACHE">缓存推荐</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                处理状态
              </label>
              <Select value={filterAction} onValueChange={setFilterAction}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部状态</SelectItem>
                  <SelectItem value="SOLVED">已解决</SelectItem>
                  <SelectItem value="ATTEMPTED">已尝试</SelectItem>
                  <SelectItem value="SKIPPED">已跳过</SelectItem>
                  <SelectItem value="BOOKMARKED">已收藏</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </Card>

        {/* Statistics Cards */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">总推荐数</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">{stats.total}</p>
                </div>
                <BarChart3 className="w-8 h-8 text-blue-500" />
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">解决率</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {stats.attempted > 0 ? Math.round((stats.solved / stats.attempted) * 100) : 0}%
                  </p>
                </div>
                <TrendingUp className="w-8 h-8 text-green-500" />
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">平均评分</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {stats.avgRating > 0 ? stats.avgRating.toFixed(1) : '-'}
                  </p>
                </div>
                <TrendingUp className="w-8 h-8 text-yellow-500" />
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">平均用时</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {stats.avgTimeSpent > 0 ? `${Math.round(stats.avgTimeSpent)}分` : '-'}
                  </p>
                </div>
                <Clock className="w-8 h-8 text-purple-500" />
              </div>
            </Card>
          </div>
        )}

        {/* History List */}
        <div className="space-y-4">
          {historyData.length === 0 ? (
            <Card className="p-12 text-center">
              <Calendar className="w-16 h-16 mx-auto text-gray-400 mb-4" />
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                暂无推荐历史
              </h3>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                在所选时间范围内没有找到推荐记录
              </p>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Link href="/dashboard/recommendations">
                  <Button className="flex items-center gap-2">
                    <Brain className="w-4 h-4" />
                    获取AI推荐
                  </Button>
                </Link>
                <Link href="/dashboard/preferences">
                  <Button variant="outline" className="flex items-center gap-2">
                    <Settings className="w-4 h-4" />
                    配置偏好设置
                  </Button>
                </Link>
              </div>
            </Card>
          ) : (
            historyData.map((item) => (
              <Card key={item.id} className="p-6 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                        {item.problemTitle}
                      </h3>
                      <Badge variant="secondary" className={getDifficultyColor(item.difficulty)}>
                        {item.difficulty === 'EASY' ? '简单' : item.difficulty === 'MEDIUM' ? '中等' : '困难'}
                      </Badge>
                      {item.action && (
                        <Badge className={getActionColor(item.action)}>
                          {item.action === 'SOLVED' ? '已解决' :
                           item.action === 'ATTEMPTED' ? '已尝试' :
                           item.action === 'SKIPPED' ? '已跳过' :
                           item.action === 'BOOKMARKED' ? '已收藏' : item.action}
                        </Badge>
                      )}
                    </div>
                    <div className="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400 mb-2">
                      <span>{formatDate(item.recommendedAt)}</span>
                      <span>来源: {item.source}</span>
                      {item.provider && <span>模型: {item.provider}</span>}
                      <span>置信度: {Math.round(item.confidence * 100)}%</span>
                    </div>
                    <div className="flex flex-wrap gap-1 mb-3">
                      {item.topics.map((topic, index) => (
                        <Badge key={index} variant="outline" className="text-xs">
                          {topic}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  {item.rating && (
                    <div className="text-right">
                      <div className="flex items-center gap-1">
                        {Array.from({ length: 5 }, (_, i) => (
                          <span
                            key={i}
                            className={i < item.rating! ? 'text-yellow-400' : 'text-gray-300'}
                          >
                            ★
                          </span>
                        ))}
                      </div>
                      {item.timeSpentMinutes !== undefined && item.timeSpentMinutes > 0 && (
                        <p className="text-sm text-gray-500 mt-1">
                          用时: {item.timeSpentMinutes}分钟
                        </p>
                      )}
                    </div>
                  )}
                </div>
                
                <Separator className="my-3" />
                
                <div className="space-y-2">
                  <div>
                    <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      推荐理由:
                    </p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      {item.reason}
                    </p>
                  </div>
                  
                  {item.feedback && (
                    <div>
                      <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        用户反馈:
                      </p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        {item.feedback}
                      </p>
                    </div>
                  )}
                </div>
              </Card>
            ))
          )}
        </div>
      </div>
    </div>
  )
}