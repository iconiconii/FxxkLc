'use client'

import { useState, useCallback } from 'react'
import { Card, CardHeader, CardContent, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Slider } from '@/components/ui/slider'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { Separator } from '@/components/ui/separator'
import { 
  Filter, 
  X, 
  ChevronDown, 
  ChevronUp, 
  RotateCcw,
  SortAsc,
  SortDesc,
  Search,
  Target,
  Clock,
  BarChart3
} from 'lucide-react'

export interface FilterOptions {
  // Sorting
  sortBy: 'score' | 'confidence' | 'difficulty' | 'topic' | 'recent' | 'default'
  sortOrder: 'asc' | 'desc'
  
  // Basic filters
  difficulties: string[]
  topics: string[]
  sources: string[]
  
  // Advanced filters
  confidenceRange: [number, number]
  scoreRange: [number, number]
  timeboxMinutes?: number
  
  // Search and text filters
  searchQuery: string
  reasonFilter: string
  
  // Learning objective filters
  objective?: 'SKILL_BUILDING' | 'INTERVIEW_PREP' | 'CONTEST_TRAINING' | 'REVIEW_WEAK_AREAS' | 'EXPLORE_NEW_TOPICS'
  adaptiveDifficulty: boolean
  
  // Display preferences
  showOnlyHighConfidence: boolean
  hideAttempted: boolean
  showDebugInfo: boolean
}

export interface AdvancedFiltersProps {
  filters: FilterOptions
  onFiltersChange: (filters: FilterOptions) => void
  availableTopics?: string[]
  availableSources?: string[]
  className?: string
}

const DEFAULT_FILTERS: FilterOptions = {
  sortBy: 'default',
  sortOrder: 'desc',
  difficulties: [],
  topics: [],
  sources: [],
  confidenceRange: [0, 100],
  scoreRange: [0, 100],
  searchQuery: '',
  reasonFilter: '',
  adaptiveDifficulty: false,
  showOnlyHighConfidence: false,
  hideAttempted: false,
  showDebugInfo: false
}

const DIFFICULTY_OPTIONS = [
  { value: 'easy', label: '简单', color: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' },
  { value: 'medium', label: '中等', color: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200' },
  { value: 'hard', label: '困难', color: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200' }
]

const SORT_OPTIONS = [
  { value: 'default', label: '默认排序', icon: BarChart3 },
  { value: 'score', label: '按评分', icon: SortDesc },
  { value: 'confidence', label: '按置信度', icon: Target },
  { value: 'difficulty', label: '按难度', icon: BarChart3 },
  { value: 'topic', label: '按主题', icon: Filter },
  { value: 'recent', label: '最新推荐', icon: Clock }
]

const OBJECTIVE_OPTIONS = [
  { value: 'SKILL_BUILDING', label: '技能建设' },
  { value: 'INTERVIEW_PREP', label: '面试准备' },
  { value: 'CONTEST_TRAINING', label: '竞赛训练' },
  { value: 'REVIEW_WEAK_AREAS', label: '查漏补缺' },
  { value: 'EXPLORE_NEW_TOPICS', label: '探索新领域' }
]

const DEFAULT_TOPICS = [
  '数组', '字符串', '链表', '栈', '队列', '哈希表', '树', '图',
  '动态规划', '贪心算法', '回溯', '分治', '滑动窗口', '双指针',
  '排序', '搜索', '位运算', '数学', '设计', '并发'
]

export default function AdvancedFilters({
  filters,
  onFiltersChange,
  availableTopics = DEFAULT_TOPICS,
  availableSources = ['LLM', 'FSRS', 'CACHE'],
  className = ''
}: AdvancedFiltersProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [activeFiltersCount, setActiveFiltersCount] = useState(0)

  // Calculate active filters count
  const calculateActiveFilters = useCallback((currentFilters: FilterOptions) => {
    let count = 0
    
    if (currentFilters.sortBy !== 'default') count++
    if (currentFilters.difficulties.length > 0) count++
    if (currentFilters.topics.length > 0) count++
    if (currentFilters.sources.length > 0) count++
    if (currentFilters.confidenceRange[0] > 0 || currentFilters.confidenceRange[1] < 100) count++
    if (currentFilters.scoreRange[0] > 0 || currentFilters.scoreRange[1] < 100) count++
    if (currentFilters.searchQuery.trim().length > 0) count++
    if (currentFilters.reasonFilter.trim().length > 0) count++
    if (currentFilters.objective) count++
    if (currentFilters.timeboxMinutes) count++
    if (currentFilters.adaptiveDifficulty) count++
    if (currentFilters.showOnlyHighConfidence) count++
    if (currentFilters.hideAttempted) count++
    
    setActiveFiltersCount(count)
  }, [])

  const updateFilters = useCallback((updates: Partial<FilterOptions>) => {
    const newFilters = { ...filters, ...updates }
    onFiltersChange(newFilters)
    calculateActiveFilters(newFilters)
  }, [filters, onFiltersChange, calculateActiveFilters])

  const resetFilters = useCallback(() => {
    onFiltersChange(DEFAULT_FILTERS)
    setActiveFiltersCount(0)
  }, [onFiltersChange])

  const toggleDifficulty = useCallback((difficulty: string) => {
    const newDifficulties = filters.difficulties.includes(difficulty)
      ? filters.difficulties.filter(d => d !== difficulty)
      : [...filters.difficulties, difficulty]
    updateFilters({ difficulties: newDifficulties })
  }, [filters.difficulties, updateFilters])

  const toggleTopic = useCallback((topic: string) => {
    const newTopics = filters.topics.includes(topic)
      ? filters.topics.filter(t => t !== topic)
      : [...filters.topics, topic]
    updateFilters({ topics: newTopics })
  }, [filters.topics, updateFilters])

  const toggleSource = useCallback((source: string) => {
    const newSources = filters.sources.includes(source)
      ? filters.sources.filter(s => s !== source)
      : [...filters.sources, source]
    updateFilters({ sources: newSources })
  }, [filters.sources, updateFilters])

  return (
    <Card className={`${className}`}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5" />
            <CardTitle className="text-lg">高级筛选</CardTitle>
            {activeFiltersCount > 0 && (
              <Badge variant="secondary" className="ml-2">
                {activeFiltersCount} 个筛选条件
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            {activeFiltersCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={resetFilters}
                className="flex items-center gap-1 text-gray-500 hover:text-gray-700"
              >
                <RotateCcw className="w-4 h-4" />
                重置
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsExpanded(!isExpanded)}
              className="flex items-center gap-1"
            >
              {isExpanded ? (
                <>
                  <ChevronUp className="w-4 h-4" />
                  收起
                </>
              ) : (
                <>
                  <ChevronDown className="w-4 h-4" />
                  展开
                </>
              )}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Always visible: Sort and Search */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <Label className="text-sm font-medium mb-2 block">排序方式</Label>
            <div className="flex gap-2">
              <Select
                value={filters.sortBy}
                onValueChange={(value: any) => updateFilters({ sortBy: value })}
              >
                <SelectTrigger className="flex-1">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SORT_OPTIONS.map(option => {
                    const IconComponent = option.icon
                    return (
                      <SelectItem key={option.value} value={option.value}>
                        <div className="flex items-center gap-2">
                          <IconComponent className="w-4 h-4" />
                          {option.label}
                        </div>
                      </SelectItem>
                    )
                  })}
                </SelectContent>
              </Select>
              <Button
                variant="outline"
                size="sm"
                onClick={() => updateFilters({ 
                  sortOrder: filters.sortOrder === 'asc' ? 'desc' : 'asc' 
                })}
                className="px-3"
              >
                {filters.sortOrder === 'asc' ? (
                  <SortAsc className="w-4 h-4" />
                ) : (
                  <SortDesc className="w-4 h-4" />
                )}
              </Button>
            </div>
          </div>

          <div>
            <Label className="text-sm font-medium mb-2 block">搜索</Label>
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <Input
                placeholder="搜索推荐理由..."
                value={filters.searchQuery}
                onChange={(e) => updateFilters({ searchQuery: e.target.value })}
                className="pl-10"
              />
              {filters.searchQuery && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => updateFilters({ searchQuery: '' })}
                  className="absolute right-1 top-1/2 transform -translate-y-1/2 px-2"
                >
                  <X className="w-4 h-4" />
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Quick filters - always visible */}
        <div className="space-y-2">
          <Label className="text-sm font-medium">快速筛选</Label>
          <div className="flex flex-wrap gap-2">
            {/* Difficulty badges */}
            {DIFFICULTY_OPTIONS.map(difficulty => (
              <Badge
                key={difficulty.value}
                variant={filters.difficulties.includes(difficulty.value) ? "default" : "outline"}
                className={`cursor-pointer transition-colors ${
                  filters.difficulties.includes(difficulty.value) 
                    ? difficulty.color 
                    : 'hover:bg-gray-100 dark:hover:bg-gray-800'
                }`}
                onClick={() => toggleDifficulty(difficulty.value)}
              >
                {difficulty.label}
                {filters.difficulties.includes(difficulty.value) && (
                  <X className="w-3 h-3 ml-1" />
                )}
              </Badge>
            ))}
            
            {/* Source badges */}
            {availableSources.map(source => (
              <Badge
                key={source}
                variant={filters.sources.includes(source) ? "default" : "outline"}
                className="cursor-pointer"
                onClick={() => toggleSource(source)}
              >
                {source}
                {filters.sources.includes(source) && (
                  <X className="w-3 h-3 ml-1" />
                )}
              </Badge>
            ))}
          </div>
        </div>

        {/* Expanded filters */}
        {isExpanded && (
          <div className="space-y-6 pt-4">
            <Separator />

            {/* Topic Selection */}
            <div>
              <Label className="text-sm font-medium mb-3 block">主题筛选</Label>
              <div className="flex flex-wrap gap-2 max-h-32 overflow-y-auto">
                {availableTopics.map(topic => (
                  <Badge
                    key={topic}
                    variant={filters.topics.includes(topic) ? "default" : "outline"}
                    className="cursor-pointer"
                    onClick={() => toggleTopic(topic)}
                  >
                    {topic}
                    {filters.topics.includes(topic) && (
                      <X className="w-3 h-3 ml-1" />
                    )}
                  </Badge>
                ))}
              </div>
              {filters.topics.length > 0 && (
                <p className="text-xs text-gray-500 mt-2">
                  已选择 {filters.topics.length} 个主题
                </p>
              )}
            </div>

            <Separator />

            {/* Range Filters */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Label className="text-sm font-medium mb-3 block">
                  置信度范围: {filters.confidenceRange[0]}% - {filters.confidenceRange[1]}%
                </Label>
                <Slider
                  value={filters.confidenceRange}
                  onValueChange={(value) => updateFilters({ confidenceRange: value as [number, number] })}
                  min={0}
                  max={100}
                  step={5}
                  className="w-full"
                />
              </div>

              <div>
                <Label className="text-sm font-medium mb-3 block">
                  评分范围: {filters.scoreRange[0]}% - {filters.scoreRange[1]}%
                </Label>
                <Slider
                  value={filters.scoreRange}
                  onValueChange={(value) => updateFilters({ scoreRange: value as [number, number] })}
                  min={0}
                  max={100}
                  step={5}
                  className="w-full"
                />
              </div>
            </div>

            <Separator />

            {/* Learning Objectives */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Label className="text-sm font-medium mb-3 block">学习目标</Label>
                <Select
                  value={filters.objective || ''}
                  onValueChange={(value) => updateFilters({ 
                    objective: value || undefined as any 
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择学习目标" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">不限定</SelectItem>
                    {OBJECTIVE_OPTIONS.map(option => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="timeboxMinutes" className="text-sm font-medium mb-3 block">
                  时间限制 (分钟)
                </Label>
                <Input
                  id="timeboxMinutes"
                  type="number"
                  placeholder="例如: 30"
                  value={filters.timeboxMinutes || ''}
                  onChange={(e) => updateFilters({ 
                    timeboxMinutes: e.target.value ? parseInt(e.target.value) : undefined 
                  })}
                  min="5"
                  max="480"
                />
              </div>
            </div>

            <Separator />

            {/* Advanced Options */}
            <div>
              <Label className="text-sm font-medium mb-3 block">高级选项</Label>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <Label className="font-medium">只显示高置信度推荐</Label>
                    <p className="text-sm text-gray-500">只显示置信度 &gt; 80% 的推荐</p>
                  </div>
                  <Switch
                    checked={filters.showOnlyHighConfidence}
                    onCheckedChange={(checked) => updateFilters({ showOnlyHighConfidence: checked })}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Label className="font-medium">隐藏已尝试题目</Label>
                    <p className="text-sm text-gray-500">不显示已经练习过的题目</p>
                  </div>
                  <Switch
                    checked={filters.hideAttempted}
                    onCheckedChange={(checked) => updateFilters({ hideAttempted: checked })}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Label className="font-medium">自适应难度</Label>
                    <p className="text-sm text-gray-500">根据表现自动调整推荐难度</p>
                  </div>
                  <Switch
                    checked={filters.adaptiveDifficulty}
                    onCheckedChange={(checked) => updateFilters({ adaptiveDifficulty: checked })}
                  />
                </div>

                {process.env.NODE_ENV === 'development' && (
                  <div className="flex items-center justify-between">
                    <div>
                      <Label className="font-medium">显示调试信息</Label>
                      <p className="text-sm text-gray-500">显示推荐元数据和调试信息</p>
                    </div>
                    <Switch
                      checked={filters.showDebugInfo}
                      onCheckedChange={(checked) => updateFilters({ showDebugInfo: checked })}
                    />
                  </div>
                )}
              </div>
            </div>

            <Separator />

            {/* Reason Filter */}
            <div>
              <Label htmlFor="reasonFilter" className="text-sm font-medium mb-2 block">
                推荐理由包含关键词
              </Label>
              <Input
                id="reasonFilter"
                placeholder="例如: 基础、进阶、面试等"
                value={filters.reasonFilter}
                onChange={(e) => updateFilters({ reasonFilter: e.target.value })}
              />
              <p className="text-xs text-gray-500 mt-1">
                筛选推荐理由中包含特定关键词的推荐
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}