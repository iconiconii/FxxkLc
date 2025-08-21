"use client"

import { useState, useEffect } from 'react'
import { reviewApi, UserLearningStats, FSRSCard, ReviewQueueCard } from '@/lib/review-api'

export interface FSRSAnalyticsData {
  stats: UserLearningStats | null
  cards: FSRSCard[]
  dueCards: ReviewQueueCard[]
  overdueCards: ReviewQueueCard[]
  isLoading: boolean
  error: string | null
}

export interface CardStateDistribution {
  name: string
  value: number
  color: string
  percentage: number
}

export interface StabilityTrend {
  date: string
  avgStability: number
  avgDifficulty: number
  totalCards: number
}

export interface ReviewConsistency {
  date: string
  reviewsCount: number
  onTime: number
  late: number
}

/**
 * Hook for fetching comprehensive FSRS analytics data
 */
export function useFSRSData() {
  const [data, setData] = useState<FSRSAnalyticsData>({
    stats: null,
    cards: [],
    dueCards: [],
    overdueCards: [],
    isLoading: true,
    error: null
  })

  const fetchData = async () => {
    try {
      setData(prev => ({ ...prev, isLoading: true, error: null }))

      // Fetch all FSRS data in parallel
      const [stats, cards, dueCards, overdueCards] = await Promise.all([
        reviewApi.getLearningStats(),
        reviewApi.getUserCards(),
        reviewApi.getDueCards(50),
        reviewApi.getOverdueCards(50)
      ])

      setData({
        stats,
        cards,
        dueCards,
        overdueCards,
        isLoading: false,
        error: null
      })
    } catch (error) {
      console.error('Error fetching FSRS data:', error)
      setData(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Failed to fetch FSRS data'
      }))
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  return { ...data, refetch: fetchData }
}

/**
 * Hook for card state distribution analysis
 */
export function useCardStateDistribution(stats: UserLearningStats | null): CardStateDistribution[] {
  if (!stats) return []

  const distribution = [
    {
      name: '新卡片',
      value: stats.newCards,
      color: '#3B82F6', // blue
      percentage: 0
    },
    {
      name: '学习中',
      value: stats.learningCards,
      color: '#F59E0B', // amber
      percentage: 0
    },
    {
      name: '复习中',
      value: stats.reviewCards,
      color: '#10B981', // emerald
      percentage: 0
    },
    {
      name: '重新学习',
      value: stats.relearningCards,
      color: '#EF4444', // red
      percentage: 0
    }
  ]

  const total = stats.totalCards || 1
  return distribution.map(item => ({
    ...item,
    percentage: Math.round((item.value / total) * 100)
  }))
}

/**
 * Hook for memory strength analysis
 */
export function useMemoryStrengthData(cards: FSRSCard[]) {
  const strengthCategories = [
    { name: '很弱', min: 0, max: 1, color: '#EF4444', count: 0 },
    { name: '较弱', min: 1, max: 7, color: '#F59E0B', count: 0 },
    { name: '一般', min: 7, max: 30, color: '#6B7280', count: 0 },
    { name: '较强', min: 30, max: 90, color: '#10B981', count: 0 },
    { name: '很强', min: 90, max: Infinity, color: '#3B82F6', count: 0 }
  ]

  cards.forEach(card => {
    const stability = card.stability
    const category = strengthCategories.find(cat => 
      stability >= cat.min && stability < cat.max
    )
    if (category) {
      category.count++
    }
  })

  return strengthCategories.map(category => ({
    name: category.name,
    value: category.count,
    color: category.color,
    percentage: cards.length > 0 ? Math.round((category.count / cards.length) * 100) : 0
  }))
}

/**
 * Hook for review urgency analysis
 */
export function useReviewUrgencyData(dueCards: ReviewQueueCard[], overdueCards: ReviewQueueCard[]) {
  const now = new Date()
  const todayDue = dueCards.filter(card => {
    const dueDate = new Date(card.dueDate)
    return dueDate.toDateString() === now.toDateString()
  }).length

  const tomorrowDue = dueCards.filter(card => {
    const dueDate = new Date(card.dueDate)
    const tomorrow = new Date(now)
    tomorrow.setDate(tomorrow.getDate() + 1)
    return dueDate.toDateString() === tomorrow.toDateString()
  }).length

  const thisWeekDue = dueCards.filter(card => {
    const dueDate = new Date(card.dueDate)
    const weekFromNow = new Date(now)
    weekFromNow.setDate(weekFromNow.getDate() + 7)
    return dueDate <= weekFromNow && dueDate > new Date(now.getTime() + 24 * 60 * 60 * 1000)
  }).length

  return [
    { name: '今日到期', value: todayDue, color: '#EF4444' },
    { name: '明日到期', value: tomorrowDue, color: '#F59E0B' },
    { name: '本周到期', value: thisWeekDue, color: '#3B82F6' },
    { name: '已逾期', value: overdueCards.length, color: '#DC2626' }
  ]
}

/**
 * Hook for difficulty progression analysis
 */
export function useDifficultyProgressionData(cards: FSRSCard[]) {
  const difficultyRanges = [
    { name: '很简单', min: 0, max: 3, color: '#10B981', count: 0 },
    { name: '简单', min: 3, max: 5, color: '#6B7280', count: 0 },
    { name: '一般', min: 5, max: 7, color: '#F59E0B', count: 0 },
    { name: '困难', min: 7, max: 9, color: '#EF4444', count: 0 },
    { name: '很困难', min: 9, max: 10, color: '#DC2626', count: 0 }
  ]

  cards.forEach(card => {
    const difficulty = card.difficulty
    const range = difficultyRanges.find(r => 
      difficulty >= r.min && difficulty < r.max
    )
    if (range) {
      range.count++
    }
  })

  return difficultyRanges.map(range => ({
    name: range.name,
    value: range.count,
    color: range.color,
    percentage: cards.length > 0 ? Math.round((range.count / cards.length) * 100) : 0
  }))
}

/**
 * Hook for learning velocity calculation
 */
export function useLearningVelocity(stats: UserLearningStats | null) {
  if (!stats) return null

  const totalActiveCards = stats.learningCards + stats.reviewCards + stats.relearningCards
  const graduatedCards = stats.reviewCards // Cards that have moved to review state
  const totalCards = stats.totalCards

  return {
    learningEfficiency: totalCards > 0 ? Math.round((graduatedCards / totalCards) * 100) : 0,
    activeCardsRatio: totalCards > 0 ? Math.round((totalActiveCards / totalCards) * 100) : 0,
    retentionRate: stats.avgStability > 0 ? Math.min(100, Math.round(stats.avgStability * 2)) : 0,
    masteryProgress: graduatedCards,
    totalProgress: totalCards
  }
}