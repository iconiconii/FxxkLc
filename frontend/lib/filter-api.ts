/**
 * Progressive filtering API client functions for cascade filtering
 */

import { apiRequest } from './api'

export interface Company {
  id: number
  name: string
  displayName: string
  description?: string
  website?: string
  location?: string
  logoUrl?: string
  industry?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface Department {
  id: number
  name: string
  displayName: string
  description?: string
  type: string
  parentDepartmentId?: number
  level: number
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface Position {
  id: number
  name: string
  displayName: string
  description?: string
  level: string
  type: string
  skillsRequired?: string
  experienceYearsMin?: number
  experienceYearsMax?: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface FilterSelection {
  companyId?: number
  departmentId?: number
  positionId?: number
}

export const filterApi = {
  /**
   * Get all active companies for dropdown selection
   */
  async getCompanies(): Promise<Company[]> {
    return await apiRequest<Company[]>('/filter/companies')
  },

  /**
   * Get departments for a specific company
   */
  async getDepartmentsByCompany(companyId: number): Promise<Department[]> {
    return await apiRequest<Department[]>(`/filter/companies/${companyId}/departments`)
  },

  /**
   * Get positions for a specific department
   */
  async getPositionsByDepartment(departmentId: number): Promise<Position[]> {
    return await apiRequest<Position[]>(`/filter/departments/${departmentId}/positions`)
  },

  /**
   * Get positions for a specific company and department combination
   */
  async getPositionsByCompanyAndDepartment(companyId: number, departmentId: number): Promise<Position[]> {
    return await apiRequest<Position[]>(`/filter/companies/${companyId}/departments/${departmentId}/positions`)
  },

  /**
   * Health check for filtering system
   */
  async healthCheck(): Promise<string> {
    return await apiRequest<string>('/filter/health')
  }
}