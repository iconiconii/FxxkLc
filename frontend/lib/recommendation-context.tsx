'use client'

import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react'

interface RecommendationContextType {
  recommendedProblemIds: Set<number>
  addRecommendedProblems: (ids: number[]) => void
  removeRecommendedProblem: (id: number) => void
  clearRecommendedProblems: () => void
  isRecommended: (id: number) => boolean
}

const RecommendationContext = createContext<RecommendationContextType | undefined>(undefined)

interface RecommendationProviderProps {
  children: ReactNode
}

export function RecommendationProvider({ children }: RecommendationProviderProps) {
  const [recommendedProblemIds, setRecommendedProblemIds] = useState<Set<number>>(new Set())

  const addRecommendedProblems = useCallback((ids: number[]) => {
    setRecommendedProblemIds(prev => new Set([...prev, ...ids]))
  }, [])

  const removeRecommendedProblem = useCallback((id: number) => {
    setRecommendedProblemIds(prev => {
      const newSet = new Set(prev)
      newSet.delete(id)
      return newSet
    })
  }, [])

  const clearRecommendedProblems = useCallback(() => {
    setRecommendedProblemIds(new Set())
  }, [])

  const isRecommended = useCallback((id: number) => {
    return recommendedProblemIds.has(id)
  }, [recommendedProblemIds])

  return (
    <RecommendationContext.Provider
      value={{
        recommendedProblemIds,
        addRecommendedProblems,
        removeRecommendedProblem,
        clearRecommendedProblems,
        isRecommended,
      }}
    >
      {children}
    </RecommendationContext.Provider>
  )
}

export function useRecommendationContext() {
  const context = useContext(RecommendationContext)
  if (context === undefined) {
    throw new Error('useRecommendationContext must be used within a RecommendationProvider')
  }
  return context
}

// Hook for optional recommendation context (returns null if not available)
export function useOptionalRecommendationContext() {
  return useContext(RecommendationContext)
}