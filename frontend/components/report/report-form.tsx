"use client"

import { useState, useEffect } from "react"
import { Calendar, Zap, Building2, Users, Briefcase, Search, Send, RotateCcw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { reportApi, type SubmitReportRequest } from "@/lib/report-api"
import { filterApi, type Company, type Department, type Position } from "@/lib/filter-api"

export default function ReportForm() {
  const [reportForm, setReportForm] = useState({
    company: "",
    department: "",
    position: "",
    problemSearch: "",
    date: new Date().toISOString().split('T')[0] // Today's date in YYYY-MM-DD format
  })
  
  // State for dynamic data
  const [companies, setCompanies] = useState<Company[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [positions, setPositions] = useState<Position[]>([])
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null)
  const [selectedDepartment, setSelectedDepartment] = useState<Department | null>(null)
  
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitStatus, setSubmitStatus] = useState<"idle" | "success" | "error">("idle")
  const [isLoading, setIsLoading] = useState(true)

  // Load companies on component mount
  useEffect(() => {
    const loadCompanies = async () => {
      try {
        setIsLoading(true)
        const companiesData = await filterApi.getCompanies()
        setCompanies(companiesData)
      } catch (error) {
        console.error('Failed to load companies:', error)
      } finally {
        setIsLoading(false)
      }
    }
    
    loadCompanies()
  }, [])

  // Load departments when company changes
  useEffect(() => {
    const loadDepartments = async () => {
      if (selectedCompany) {
        try {
          const departmentsData = await filterApi.getDepartmentsByCompany(selectedCompany.id)
          setDepartments(departmentsData)
          // Reset department and position when company changes
          setSelectedDepartment(null)
          setPositions([])
          handleReportFormChange('department', '')
          handleReportFormChange('position', '')
        } catch (error) {
          console.error('Failed to load departments:', error)
        }
      } else {
        setDepartments([])
        setPositions([])
      }
    }
    
    loadDepartments()
  }, [selectedCompany])

  // Load positions when department changes
  useEffect(() => {
    const loadPositions = async () => {
      if (selectedDepartment && selectedCompany) {
        try {
          const positionsData = await filterApi.getPositionsByCompanyAndDepartment(
            selectedCompany.id, 
            selectedDepartment.id
          )
          setPositions(positionsData)
          // Reset position when department changes
          handleReportFormChange('position', '')
        } catch (error) {
          console.error('Failed to load positions:', error)
        }
      } else {
        setPositions([])
      }
    }
    
    loadPositions()
  }, [selectedDepartment])

  const handleReportSubmit = async () => {
    // Basic validation
    if (!reportForm.company || !reportForm.department || !reportForm.position || !reportForm.problemSearch) {
      setSubmitStatus("error")
      return
    }

    setIsSubmitting(true)
    setSubmitStatus("idle")
    
    try {
      const selectedPosition = positions.find(p => p.id.toString() === reportForm.position)
      
      const submitRequest: SubmitReportRequest = {
        company: selectedCompany?.displayName || selectedCompany?.name || reportForm.company,
        department: selectedDepartment?.displayName || selectedDepartment?.name || reportForm.department,
        position: selectedPosition?.displayName || selectedPosition?.name || reportForm.position,
        problemSearch: reportForm.problemSearch,
        date: reportForm.date,
        interviewRound: 'OTHER', // Default value, could be made configurable
      }
      
      console.log('Submitting report:', submitRequest)
      
      const response = await reportApi.submitReport(submitRequest)
      
      if (response.success) {
        setSubmitStatus("success")
        
        // Reset form after successful submission
        setTimeout(() => {
          handleReset()
          setSubmitStatus("idle")
        }, 2000)
      } else {
        console.error('Submit failed:', response.message)
        setSubmitStatus("error")
      }
      
    } catch (error: any) {
      console.error('Submit error:', error)
      setSubmitStatus("error")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReportFormChange = (field: string, value: string) => {
    setReportForm(prev => ({
      ...prev,
      [field]: value
    }))
    
    // Handle company selection
    if (field === 'company') {
      const company = companies.find(c => c.id.toString() === value)
      setSelectedCompany(company || null)
    }
    
    // Handle department selection
    if (field === 'department') {
      const department = departments.find(d => d.id.toString() === value)
      setSelectedDepartment(department || null)
    }
    
    // Clear error status when user starts typing
    if (submitStatus === "error") {
      setSubmitStatus("idle")
    }
  }

  const handleReset = () => {
    setReportForm({
      company: "",
      department: "",
      position: "",
      problemSearch: "",
      date: new Date().toISOString().split('T')[0]
    })
    setSelectedCompany(null)
    setSelectedDepartment(null)
    setDepartments([])
    setPositions([])
    setSubmitStatus("idle")
  }

  const isFormValid = reportForm.company && reportForm.department && reportForm.position && reportForm.problemSearch

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-blue-500 p-2 rounded-lg">
            <Zap className="h-6 w-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">我要爆料</h1>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          分享您的面试经历，帮助更多求职者准备面试
        </p>
      </div>

      {/* Main Form */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-6">
        <div className="space-y-6">
          {/* Form Fields in Grid Layout */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Company Select */}
            <div className="space-y-2">
              <Label htmlFor="report-company" className="flex items-center gap-2 text-sm font-medium">
                <Building2 className="h-4 w-4 text-blue-500" />
                公司 <span className="text-red-500">*</span>
              </Label>
              <Select 
                value={reportForm.company} 
                onValueChange={(value) => handleReportFormChange('company', value)}
                disabled={isLoading}
              >
                <SelectTrigger>
                  <SelectValue placeholder={isLoading ? "加载中..." : "请选择面试公司"} />
                </SelectTrigger>
                <SelectContent>
                  {companies.map((company) => (
                    <SelectItem key={company.id} value={company.id.toString()}>
                      {company.displayName || company.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Department Select */}
            <div className="space-y-2">
              <Label htmlFor="report-department" className="flex items-center gap-2 text-sm font-medium">
                <Users className="h-4 w-4 text-green-500" />
                部门 <span className="text-red-500">*</span>
              </Label>
              <Select 
                value={reportForm.department} 
                onValueChange={(value) => handleReportFormChange('department', value)}
                disabled={!selectedCompany || departments.length === 0}
              >
                <SelectTrigger>
                  <SelectValue placeholder={
                    !selectedCompany ? "请先选择公司" : 
                    departments.length === 0 ? "该公司暂无部门数据" : 
                    "请选择面试部门"
                  } />
                </SelectTrigger>
                <SelectContent>
                  {departments.map((department) => (
                    <SelectItem key={department.id} value={department.id.toString()}>
                      {department.displayName || department.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Position Select */}
            <div className="space-y-2">
              <Label htmlFor="report-position" className="flex items-center gap-2 text-sm font-medium">
                <Briefcase className="h-4 w-4 text-purple-500" />
                岗位 <span className="text-red-500">*</span>
              </Label>
              <Select 
                value={reportForm.position} 
                onValueChange={(value) => handleReportFormChange('position', value)}
                disabled={!selectedDepartment || positions.length === 0}
              >
                <SelectTrigger>
                  <SelectValue placeholder={
                    !selectedDepartment ? "请先选择部门" : 
                    positions.length === 0 ? "该部门暂无岗位数据" : 
                    "请选择应聘岗位"
                  } />
                </SelectTrigger>
                <SelectContent>
                  {positions.map((position) => (
                    <SelectItem key={position.id} value={position.id.toString()}>
                      {position.displayName || position.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Date Input */}
            <div className="space-y-2">
              <Label htmlFor="report-date" className="flex items-center gap-2 text-sm font-medium">
                <Calendar className="h-4 w-4 text-blue-500" />
                面试日期
              </Label>
              <Input
                id="report-date"
                type="date"
                value={reportForm.date}
                onChange={(e) => handleReportFormChange('date', e.target.value)}
                className="cursor-pointer"
                max={new Date().toISOString().split('T')[0]} // Prevent future dates
              />
            </div>
          </div>

          {/* Problem Search Input - Full Width */}
          <div className="space-y-2">
            <Label htmlFor="report-problem" className="flex items-center gap-2 text-sm font-medium">
              <Search className="h-4 w-4 text-orange-500" />
              面试题目 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="report-problem"
              placeholder="请输入面试中遇到的算法题目名称或编号"
              value={reportForm.problemSearch}
              onChange={(e) => handleReportFormChange('problemSearch', e.target.value)}
            />
            <p className="text-xs text-gray-500 dark:text-gray-400">
              例如：两数之和、最长公共子序列、LeetCode 146 等
            </p>
          </div>

          {/* Submit Status Messages */}
          {submitStatus === "success" && (
            <div className="bg-green-50 dark:bg-green-900/50 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-400 px-4 py-3 rounded-lg flex items-center gap-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              爆料提交成功！感谢您的贡献，帮助更多求职者准备面试。
            </div>
          )}

          {submitStatus === "error" && (
            <div className="bg-red-50 dark:bg-red-900/50 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg">
              提交失败，请检查所有必填字段是否已填写完整。
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-3 pt-4">
            <Button 
              onClick={handleReportSubmit}
              disabled={!isFormValid || isSubmitting}
              className="bg-blue-500 hover:bg-blue-600 text-white disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  提交中...
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Send className="h-4 w-4" />
                  提交爆料
                </div>
              )}
            </Button>
            
            <Button 
              variant="outline" 
              onClick={handleReset}
              disabled={isSubmitting}
            >
              <RotateCcw className="h-4 w-4 mr-2" />
              重置
            </Button>
          </div>
        </div>
      </div>

      {/* Info Section */}
      <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
        <h3 className="text-base font-semibold text-blue-900 dark:text-blue-100 mb-2">
          💡 为什么要爆料？
        </h3>
        <p className="text-blue-700 dark:text-blue-300 text-sm leading-relaxed">
          您的面试经历对其他求职者非常宝贵。通过分享真实的面试题目和经历，
          可以帮助更多人更好地准备面试，提高求职成功率。我们会保护您的隐私，
          所有信息仅用于数据分析和题库优化。
        </p>
      </div>
    </div>
  )
}