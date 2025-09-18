'use client'

import { useState, useCallback } from 'react'
import Link from 'next/link'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import RecommendationToggle, { RecommendationSource } from '@/components/recommendation/RecommendationToggle'
import AIRecommendationsList from '@/components/recommendation/AIRecommendationsList'
import AIRecommendationsInfiniteList from '@/components/recommendation/AIRecommendationsInfiniteList'
import { FeedbackAction, AIRecommendationRequest } from '@/types/recommendation'
import { History } from 'lucide-react'

// Available difficulty options
const difficultyOptions = [
  { value: 'all', label: '全部难度' },
  { value: 'easy', label: '简单' },
  { value: 'medium', label: '中等' },
  { value: 'hard', label: '困难' },
]

// Available topic options (this could come from an API)
const topicOptions = [
  { value: 'all', label: '全部主题' },
  { value: 'array', label: '数组' },
  { value: 'string', label: '字符串' },
  { value: 'linked-list', label: '链表' },
  { value: 'tree', label: '树' },
  { value: 'graph', label: '图' },
  { value: 'dynamic-programming', label: '动态规划' },
  { value: 'greedy', label: '贪心算法' },
  { value: 'backtracking', label: '回溯' },
]

export default function RecommendationsPage() {
  const [recommendationSource, setRecommendationSource] = useState<RecommendationSource>('ai')
  const [limit, setLimit] = useState(20)
  const [difficulty, setDifficulty] = useState('')
  const [topicFilter, setTopicFilter] = useState<string[]>([])
  const [useInfiniteLoading, setUseInfiniteLoading] = useState(false)
  const [feedbackStats, setFeedbackStats] = useState({
    accepted: 0,
    helpful: 0,
    skipped: 0,
    solved: 0,
    hidden: 0,
  })

  const handleSourceChange = useCallback((source: RecommendationSource) => {
    setRecommendationSource(source)
  }, [])

  const handleFeedback = useCallback((action: FeedbackAction, success: boolean, problemId: number) => {
    if (success) {
      setFeedbackStats(prev => ({
        ...prev,
        [action]: prev[action] + 1,
      }))
    }
  }, [])

  const handleTopicChange = useCallback((topic: string) => {
    if (topic === 'all') {
      setTopicFilter([])
    } else {
      setTopicFilter([topic])
    }
  }, [])

  // Build query parameters
  const queryParams: Omit<AIRecommendationRequest, 'limit' | 'ab_group'> = {
    ...(difficulty && { difficulty: difficulty }),
    ...(topicFilter.length > 0 && { domains: topicFilter }),
    recommendation_type: 'hybrid',
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-4xl mx-auto p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              推荐题目
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              基于您的学习进度和偏好的个性化推荐
            </p>
          </div>
          
          <div className="flex items-center gap-3">
            <Link href="/dashboard/history">
              <Button
                variant="outline"
                className="flex items-center gap-2"
              >
                <History className="w-4 h-4" />
                查看历史
              </Button>
            </Link>
            
            <Button
              onClick={() => window.history.back()}
              variant="outline"
              className="flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              返回仪表板
            </Button>
          </div>
        </div>

        {/* Controls */}
        <Card className="p-6">
          <div className="space-y-4">
            {/* Source toggle */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                推荐方式
              </label>
              <RecommendationToggle
                value={recommendationSource}
                onValueChange={handleSourceChange}
              />
            </div>

            {/* Filters (only show for AI recommendations) */}
            {recommendationSource === 'ai' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    难度偏好
                  </label>
                  <Select value={difficulty || 'all'} onValueChange={(value) => setDifficulty(value === 'all' ? '' : value)}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="选择难度" />
                    </SelectTrigger>
                    <SelectContent>
                      {difficultyOptions.map(option => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    主题筛选
                  </label>
                  <Select value={topicFilter[0] || 'all'} onValueChange={handleTopicChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="选择主题" />
                    </SelectTrigger>
                    <SelectContent>
                      {topicOptions.map(option => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    加载模式
                  </label>
                  <Select value={useInfiniteLoading ? 'infinite' : 'paginated'} onValueChange={(value) => setUseInfiniteLoading(value === 'infinite')}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="选择加载模式" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="paginated">分页加载</SelectItem>
                      <SelectItem value="infinite">无限滚动 (实验)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}
          </div>
        </Card>

        {/* Feedback Stats */}
        {(feedbackStats.helpful + feedbackStats.skipped + feedbackStats.solved + feedbackStats.hidden) > 0 && (
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                反馈统计
              </span>
              <div className="flex gap-4 text-xs text-gray-500 dark:text-gray-400">
                {feedbackStats.helpful > 0 && <span>有用: {feedbackStats.helpful}</span>}
                {feedbackStats.skipped > 0 && <span>跳过: {feedbackStats.skipped}</span>}
                {feedbackStats.solved > 0 && <span>已解决: {feedbackStats.solved}</span>}
                {feedbackStats.hidden > 0 && <span>隐藏: {feedbackStats.hidden}</span>}
              </div>
            </div>
          </Card>
        )}

        {/* Recommendations List */}
        {recommendationSource === 'ai' ? (
          useInfiniteLoading ? (
            <AIRecommendationsInfiniteList
              pageSize={20}
              params={queryParams}
              onFeedback={handleFeedback}
              enableAutoLoad={true}
            />
          ) : (
            <AIRecommendationsList
              limit={limit}
              params={queryParams}
              onFeedback={handleFeedback}
            />
          )
        ) : (
          <Card className="p-12 text-center">
            <div className="text-gray-400 dark:text-gray-500 mb-4">
              <svg className="w-16 h-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              FSRS 推荐模式
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              您当前使用的是传统的 FSRS 间隔重复算法推荐。
              切换到 AI 推荐以获得更智能的个性化建议。
            </p>
            <Button
              onClick={() => setRecommendationSource('ai')}
              className="mx-auto"
            >
              尝试 AI 推荐
            </Button>
          </Card>
        )}
      </div>
    </div>
  )
}
