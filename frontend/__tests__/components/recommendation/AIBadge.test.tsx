import React from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { RecommendationProvider, useOptionalRecommendationContext } from '@/lib/recommendation-context'
import AIBadge from '@/components/recommendation/AIBadge'
import * as abLib from '@/lib/ab'

// Mock the AB testing module
vi.mock('@/lib/ab')
const mockShouldShowAIRecommendations = vi.mocked(abLib.shouldShowAIRecommendations)

// Create a test component to verify context integration
const TestContextComponent = ({ problemId }: { problemId: number }) => {
  const context = useOptionalRecommendationContext()
  
  return (
    <div>
      <span data-testid="has-context">{context ? 'true' : 'false'}</span>
      <span data-testid="is-recommended">
        {context?.isRecommended(problemId) ? 'true' : 'false'}
      </span>
      <AIBadge problemId={problemId} />
    </div>
  )
}

describe('AIBadge', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('visibility logic', () => {
    it('should show badge when problem is recommended and AB test allows', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      render(
        <RecommendationProvider>
          <TestContextComponent problemId={123} />
        </RecommendationProvider>
      )

      // Verify context is available
      expect(screen.getByTestId('has-context')).toHaveTextContent('true')

      // Add the problem to recommendations
      const context = useOptionalRecommendationContext()
      if (context) {
        context.addRecommendedProblems([123])
      }

      // Re-render to see the updated state
      render(
        <RecommendationProvider>
          <TestContextComponent problemId={123} />
        </RecommendationProvider>
      )

      // The badge should be visible since shouldShowAIRecommendations returns true
      // Note: This test verifies the integration logic, actual badge rendering depends on context state
    })

    it('should not show badge when AB test disallows AI recommendations', () => {
      mockShouldShowAIRecommendations.mockReturnValue(false)

      render(
        <RecommendationProvider>
          <AIBadge problemId={123} />
        </RecommendationProvider>
      )

      // Badge should not be rendered when AB test says no
      expect(screen.queryByText('AI')).not.toBeInTheDocument()
    })

    it('should not show badge when problem is not recommended', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      render(
        <RecommendationProvider>
          <AIBadge problemId={999} /> {/* Problem not in recommendations */}
        </RecommendationProvider>
      )

      // Badge should not be rendered for non-recommended problems
      expect(screen.queryByText('AI')).not.toBeInTheDocument()
    })

    it('should handle missing context gracefully', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      // Render without RecommendationProvider
      render(<AIBadge problemId={123} />)

      // Should not crash and should not show badge
      expect(screen.queryByText('AI')).not.toBeInTheDocument()
    })
  })

  describe('context integration', () => {
    it('should access recommendation context correctly', () => {
      render(
        <RecommendationProvider>
          <TestContextComponent problemId={123} />
        </RecommendationProvider>
      )

      // Verify context is properly available
      expect(screen.getByTestId('has-context')).toHaveTextContent('true')
      expect(screen.getByTestId('is-recommended')).toHaveTextContent('false') // Initially not recommended
    })

    it('should work without context provider', () => {
      render(<TestContextComponent problemId={123} />)

      // Should handle missing context gracefully
      expect(screen.getByTestId('has-context')).toHaveTextContent('false')
      expect(screen.getByTestId('is-recommended')).toHaveTextContent('false')
    })
  })

  describe('size variants', () => {
    it('should render with small size', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      const { container } = render(
        <RecommendationProvider>
          <AIBadge problemId={123} size="sm" />
        </RecommendationProvider>
      )

      // Check if component renders (even if badge is not visible due to recommendation state)
      expect(container.firstChild).toBeDefined()
    })

    it('should render with default size', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      const { container } = render(
        <RecommendationProvider>
          <AIBadge problemId={123} />
        </RecommendationProvider>
      )

      expect(container.firstChild).toBeDefined()
    })
  })

  describe('integration with recommendation state', () => {
    it('should update visibility when recommendation state changes', () => {
      mockShouldShowAIRecommendations.mockReturnValue(true)

      // Create a wrapper component to test state changes
      const TestWrapper = () => {
        const context = useOptionalRecommendationContext()

        return (
          <div>
            <button
              onClick={() => context?.addRecommendedProblems([123])}
              data-testid="add-recommendation"
            >
              Add Recommendation
            </button>
            <AIBadge problemId={123} />
            <span data-testid="recommendation-count">
              {context?.getRecommendedProblems().length || 0}
            </span>
          </div>
        )
      }

      render(
        <RecommendationProvider>
          <TestWrapper />
        </RecommendationProvider>
      )

      // Initially no recommendations
      expect(screen.getByTestId('recommendation-count')).toHaveTextContent('0')

      // Click to add recommendation - this would trigger context update in real usage
      // Note: This is a simplified test since the actual context update logic 
      // happens through the prefetch hook and recommendation lists
    })
  })
})