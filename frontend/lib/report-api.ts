/**
 * Report API client functions
 */

import { apiRequest } from './api'

export interface SubmitReportRequest {
  company: string
  department: string
  position: string
  problemSearch: string
  date: string
  additionalNotes?: string
  difficultyRating?: number
  interviewRound?: 'PHONE' | 'TECHNICAL' | 'ONSITE' | 'FINAL' | 'OTHER'
}

export interface SubmissionResponse {
  success: boolean
  reportId?: number
  message: string
  errorCode?: string
}

export interface InterviewReport {
  id: number
  companyName: string
  department: string
  position: string
  problemTitle: string
  interviewDate: string
  difficultyRating?: number
  additionalNotes?: string
  isVerified: boolean
  credibilityScore: number
  upvoteCount: number
  downvoteCount: number
  createdAt: string
}

export const reportApi = {
  /**
   * Submit a new interview report
   */
  async submitReport(request: SubmitReportRequest): Promise<SubmissionResponse> {
    return await apiRequest<SubmissionResponse>('/reports', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  /**
   * Get interview reports by company
   */
  async getReportsByCompany(companyName: string): Promise<InterviewReport[]> {
    return await apiRequest<InterviewReport[]>(`/reports/company/${encodeURIComponent(companyName)}`)
  },

  /**
   * Get recent interview reports
   */
  async getRecentReports(limit: number = 20): Promise<InterviewReport[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    })

    return await apiRequest<InterviewReport[]>(`/reports/recent?${params}`)
  },
}