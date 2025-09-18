/**
 * A/B Testing Utility
 * Handles user group assignment for recommendation experiments
 */

/**
 * Simple hash function for consistent user assignment
 */
function hashString(str: string): number {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // Convert to 32-bit integer
  }
  return Math.abs(hash)
}

/**
 * Get or assign A/B group for recommendations
 * Groups: 'A' (AI recommendations), 'B' (FSRS only), 'control' (default)
 */
export function getABGroup(userId?: string | number): string {
  // If userId is available, use deterministic assignment
  if (userId) {
    const hash = hashString(String(userId))
    const group = hash % 3
    switch (group) {
      case 0: return 'A' // AI recommendations
      case 1: return 'B' // FSRS only
      default: return 'control' // Default behavior
    }
  }

  // Fallback: check localStorage for sticky assignment
  if (typeof window !== 'undefined') {
    try {
      let storedGroup = localStorage.getItem('ab_group_reco')
      if (!storedGroup) {
        // Generate random assignment and store it
        const groups = ['A', 'B', 'control']
        storedGroup = groups[Math.floor(Math.random() * groups.length)]
        localStorage.setItem('ab_group_reco', storedGroup)
      }
      return storedGroup
    } catch {
      // localStorage not available
    }
  }

  // Ultimate fallback
  return 'control'
}

/**
 * Check if user should see AI recommendations
 * Note: This should only be used for authenticated users to avoid unnecessary API calls
 */
export function shouldShowAIRecommendations(userId?: string | number): boolean {
  const group = getABGroup(userId)
  return group === 'A'
}

/**
 * Get user preference for recommendation source
 */
export function getRecommendationPreference(): 'fsrs' | 'ai' {
  if (typeof window !== 'undefined') {
    try {
      const pref = localStorage.getItem('pref_recommendation_source')
      if (pref === 'ai' || pref === 'fsrs') {
        return pref
      }
    } catch {
      // localStorage not available
    }
  }
  
  // Default to AI for A group users, FSRS for others
  return shouldShowAIRecommendations() ? 'ai' : 'fsrs'
}

/**
 * Set user preference for recommendation source
 */
export function setRecommendationPreference(source: 'fsrs' | 'ai'): void {
  if (typeof window !== 'undefined') {
    try {
      localStorage.setItem('pref_recommendation_source', source)
    } catch {
      // localStorage not available
    }
  }
}