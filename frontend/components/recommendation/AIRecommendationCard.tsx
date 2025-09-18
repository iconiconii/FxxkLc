'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Card, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { 
  RecommendationItem, 
  FeedbackAction
} from '@/types/recommendation'
import { postRecommendationFeedback, generateRecommendationId } from '@/lib/recommendation-api'

interface AIRecommendationCardProps {
  item: RecommendationItem
  traceId: string
  cacheHit?: boolean
  onFeedback?: (action: FeedbackAction, success: boolean) => void
  className?: string
}

const sourceVariants = {
  LLM: { variant: 'default' as const, label: 'AI推荐', color: 'text-blue-600' },
  FSRS: { variant: 'secondary' as const, label: 'FSRS', color: 'text-green-600' },
  HYBRID: { variant: 'outline' as const, label: '混合', color: 'text-purple-600' },
  DEFAULT: { variant: 'outline' as const, label: '默认', color: 'text-gray-600' },
}

export default function AIRecommendationCard({ 
  item, 
  traceId, 
  cacheHit = false,
  onFeedback,
  className 
}: AIRecommendationCardProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [feedbackLoading, setFeedbackLoading] = useState<FeedbackAction | null>(null)
  const [feedbackGiven, setFeedbackGiven] = useState<FeedbackAction | null>(null)

  const sourceConfig = sourceVariants[item.source] || sourceVariants.DEFAULT
  
  // Defensive data handling
  const confidencePercent = Math.min(100, Math.max(0, Math.round((item.confidence ?? 0) * 100)))
  const reason = item.reason || '暂无解释'

  // Persist expand state per-problem in sessionStorage
  useEffect(() => {
    try {
      const storageKey = `reco_expand:${item.problemId}`
      const saved = typeof window !== 'undefined' ? sessionStorage.getItem(storageKey) : null
      if (saved === '1') setIsExpanded(true)
    } catch {}
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [item.problemId])

  const toggleExpanded = () => {
    const next = !isExpanded
    setIsExpanded(next)
    try {
      const storageKey = `reco_expand:${item.problemId}`
      if (typeof window !== 'undefined') {
        if (next) sessionStorage.setItem(storageKey, '1')
        else sessionStorage.removeItem(storageKey)
      }
    } catch {}
  }

  const handleFeedback = async (action: FeedbackAction, helpful?: boolean) => {
    if (feedbackLoading || feedbackGiven) return

    setFeedbackLoading(action)
    
    try {
      const recommendationId = generateRecommendationId(traceId, item.problemId)
      
      await postRecommendationFeedback(item.problemId, {
        recommendationId,
        action,
        helpful,
      })
      
      setFeedbackGiven(action)
      onFeedback?.(action, true)
    } catch (error) {
      console.error('Failed to submit feedback:', error)
      onFeedback?.(action, false)
    } finally {
      setFeedbackLoading(null)
    }
  }

  return (
    <Card className={`p-4 transition-all duration-200 hover:shadow-md hover:border-gray-300 dark:hover:border-gray-600 ${className} ${feedbackGiven ? 'opacity-75' : ''}`}>
      <CardHeader className="p-0 pb-3">
        {/* Title and badges row */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <Link 
              href={`/codetop/problem/${item.problemId}`}
              className="text-base font-medium text-gray-900 dark:text-white hover:text-blue-600 dark:hover:text-blue-400 transition-colors line-clamp-2"
            >
              {item.title || `问题 ${item.problemId}`}
            </Link>
            
            {item.topics && item.topics.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-1">
                {item.topics.slice(0, 3).map((topic, index) => (
                  <span 
                    key={index}
                    className="text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded"
                  >
                    {topic}
                  </span>
                ))}
                {item.topics.length > 3 && (
                  <span className="text-xs text-gray-400 dark:text-gray-500">
                    +{item.topics.length - 3}
                  </span>
                )}
              </div>
            )}
          </div>
          
          <div className="flex flex-col gap-1 items-end">
            <div className="flex items-center gap-2">
              <Badge variant={sourceConfig.variant} className="shrink-0">
                {sourceConfig.label}
              </Badge>
              {cacheHit && (
                <div 
                  className="w-2 h-2 bg-green-500 dark:bg-green-400 rounded-full"
                  title="命中缓存，加速响应"
                  aria-label="缓存命中"
                />
              )}
            </div>
            {item.difficulty && (
              <span className="text-xs text-gray-500 dark:text-gray-400">
                {item.difficulty}
              </span>
            )}
          </div>
        </div>
      </CardHeader>

      {/* Reason section */}
      <div className="space-y-3">
        <div>
          <p className={`text-sm text-gray-600 dark:text-gray-300 ${!isExpanded ? 'line-clamp-2' : ''}`}>
            {reason}
          </p>
          
          {item.explanations && item.explanations.length > 0 && (
            <button
              onClick={toggleExpanded}
              className="text-xs text-blue-600 dark:text-blue-400 hover:underline mt-1"
              aria-expanded={isExpanded}
              aria-controls={`explanations-${item.problemId}`}
              aria-label={isExpanded ? '收起详细解释' : '展开详细解释'}
            >
              {isExpanded ? '收起解释' : '展开解释'}
            </button>
          )}
          
          {isExpanded && item.explanations && (
            <ul 
              id={`explanations-${item.problemId}`}
              className="mt-2 space-y-1"
              role="region"
              aria-label="推荐解释详情"
            >
              {item.explanations.map((explanation, index) => (
                <li 
                  key={index}
                  className="text-xs text-gray-500 dark:text-gray-400 pl-3 border-l-2 border-gray-200 dark:border-gray-700"
                >
                  {explanation}
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Confidence meter */}
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500 dark:text-gray-400">
              置信度
            </span>
            <div className="flex items-center gap-1">
              <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                {confidencePercent}%
              </span>
              {/* Performance indicator */}
              {item.latencyMs && item.latencyMs < 1000 && (
                <div 
                  className="w-1.5 h-1.5 bg-blue-500 dark:bg-blue-400 rounded-full"
                  title={`响应快速: ${item.latencyMs}ms`}
                  aria-label="快速响应"
                />
              )}
            </div>
          </div>
          <Progress 
            value={confidencePercent} 
            className="h-1.5"
          />
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-2 pt-2 border-t border-gray-100 dark:border-gray-800">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => handleFeedback('accepted', true)}
            disabled={!!feedbackLoading || !!feedbackGiven}
            className="flex items-center gap-1 h-8"
            aria-label="标记为有用"
            title="标记此推荐对您有帮助"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L9 7v13m-3-4h-.01" />
            </svg>
            <span className="sr-only">标记为有用</span>
            有用
          </Button>
          
          <Button
            size="sm"
            variant="ghost"
            onClick={() => handleFeedback('skipped', false)}
            disabled={!!feedbackLoading || !!feedbackGiven}
            className="flex items-center gap-1 h-8"
            aria-label="跳过此推荐"
            title="跳过此推荐，不感兴趣"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14H5.236a2 2 0 01-1.789-2.894l3.5-7A2 2 0 018.736 3h4.018c.163 0 .326.02.485.06L17 4m-7 10v2a2 2 0 002 2h.095c.5 0 .905-.405.905-.905 0-.714.211-1.412.608-2.006L15 17V4m-3 4h.01" />
            </svg>
            <span className="sr-only">跳过此推荐</span>
            跳过
          </Button>
          
          <Button
            size="sm"
            variant="ghost"
            onClick={() => handleFeedback('solved')}
            disabled={!!feedbackLoading || !!feedbackGiven}
            className="flex items-center gap-1 h-8"
            aria-label="标记为已解决"
            title="标记此题目已经解决"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            <span className="sr-only">标记为已解决</span>
            已解决
          </Button>

          {/* Overflow menu for additional actions */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                size="sm"
                variant="ghost"
                disabled={!!feedbackLoading || !!feedbackGiven}
                className="h-8 w-8 p-0"
                aria-label="更多操作"
                title="更多操作"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zM12 13a1 1 0 110-2 1 1 0 010 2zM12 20a1 1 0 110-2 1 1 0 010 2z" />
                </svg>
                <span className="sr-only">更多操作</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onClick={() => handleFeedback('hidden')}
                disabled={!!feedbackLoading || !!feedbackGiven}
                className="text-red-600 dark:text-red-400 focus:text-red-600 dark:focus:text-red-400"
              >
                <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L12 12M9.878 9.878l-3-3m3 3l3 3m0 0l4.242-4.242M21 12c0 1-1 3-1 3m-2.992-.993l-2.014-.007M3.51 7.17l1.414 1.415" />
                </svg>
                隐藏该推荐
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Loading indicator */}
          {feedbackLoading && (
            <div className="ml-auto">
              <svg className="w-4 h-4 animate-spin text-gray-400" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </div>
          )}

          {/* Feedback confirmation */}
          {feedbackGiven && (
            <div className="ml-auto text-xs text-green-600 dark:text-green-400">
              已记录反馈
            </div>
          )}
        </div>
      </div>

      {/* Debug info in development */}
      {process.env.NODE_ENV === 'development' && (
        <details className="mt-3 text-xs text-gray-400">
          <summary className="cursor-pointer">调试信息</summary>
          <div className="mt-1 space-y-1">
            <div>TraceId: {traceId.slice(-6)}</div>
            <div>策略: {item.strategy}</div>
            {item.model && <div>模型: {item.model}</div>}
            {item.promptVersion && <div>Prompt: {item.promptVersion}</div>}
            {item.latencyMs && <div>延迟: {item.latencyMs}ms</div>}
          </div>
        </details>
      )}
    </Card>
  )
}
