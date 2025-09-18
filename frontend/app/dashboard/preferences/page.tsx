'use client'

import { useState, useEffect } from 'react'
import { Card, CardHeader, CardContent, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Slider } from '@/components/ui/slider'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { 
  ArrowLeft, 
  Save, 
  RefreshCw, 
  Target, 
  Brain, 
  Clock, 
  Filter,
  Settings,
  User,
  Zap,
  History
} from 'lucide-react'

interface UserPreferences {
  // Learning Objectives
  primaryObjective: 'SKILL_BUILDING' | 'INTERVIEW_PREP' | 'CONTEST_TRAINING' | 'REVIEW_WEAK_AREAS' | 'EXPLORE_NEW_TOPICS'
  targetDomains: string[]
  difficultyPreference: 'EASY_FOCUS' | 'MEDIUM_FOCUS' | 'HARD_FOCUS' | 'ADAPTIVE' | 'PROGRESSIVE'
  
  // Time Management
  dailyPracticeMinutes: number
  sessionTimeLimit: number
  breakDuration: number
  
  // AI Recommendation Settings
  aiRecommendationEnabled: boolean
  confidenceThreshold: number
  explanationLength: 'SHORT' | 'MEDIUM' | 'DETAILED'
  fallbackToFSRS: boolean
  
  // Topic Preferences
  preferredTopics: string[]
  avoidedTopics: string[]
  explorationRate: number // 0-100, how much to explore new topics
  
  // Study Schedule
  studySchedule: {
    monday: boolean
    tuesday: boolean
    wednesday: boolean
    thursday: boolean
    friday: boolean
    saturday: boolean
    sunday: boolean
  }
  preferredStudyTimes: string[] // e.g., ['morning', 'evening']
  
  // Notification Settings
  dailyReminder: boolean
  reminderTime: string
  weeklyProgress: boolean
  achievementNotifications: boolean
  
  // Advanced Settings
  maxRecommendationsPerDay: number
  adaptiveDifficulty: boolean
  personalizedFeedback: boolean
  dataSharing: boolean
}

const TOPIC_OPTIONS = [
  '数组', '字符串', '链表', '栈', '队列', '哈希表', '树', '图',
  '动态规划', '贪心算法', '回溯', '分治', '滑动窗口', '双指针',
  '排序', '搜索', '位运算', '数学', '设计', '并发'
]

const DIFFICULTY_OPTIONS = [
  { value: 'EASY_FOCUS', label: '专注简单题', description: '主要推荐简单题目，稳固基础' },
  { value: 'MEDIUM_FOCUS', label: '专注中等题', description: '主要推荐中等题目，提升技能' },
  { value: 'HARD_FOCUS', label: '专注困难题', description: '主要推荐困难题目，挑战自我' },
  { value: 'ADAPTIVE', label: '自适应', description: '根据表现自动调整难度' },
  { value: 'PROGRESSIVE', label: '渐进式', description: '循序渐进，逐步提高难度' }
]

const OBJECTIVE_OPTIONS = [
  { value: 'SKILL_BUILDING', label: '技能建设', description: '系统性地建立和强化编程技能' },
  { value: 'INTERVIEW_PREP', label: '面试准备', description: '针对技术面试的专项练习' },
  { value: 'CONTEST_TRAINING', label: '竞赛训练', description: '算法竞赛和编程竞赛训练' },
  { value: 'REVIEW_WEAK_AREAS', label: '查漏补缺', description: '重点练习薄弱领域' },
  { value: 'EXPLORE_NEW_TOPICS', label: '探索新领域', description: '学习新的算法和数据结构' }
]

export default function PreferencesPage() {
  const [preferences, setPreferences] = useState<UserPreferences>({
    primaryObjective: 'SKILL_BUILDING',
    targetDomains: [],
    difficultyPreference: 'ADAPTIVE',
    dailyPracticeMinutes: 60,
    sessionTimeLimit: 30,
    breakDuration: 10,
    aiRecommendationEnabled: true,
    confidenceThreshold: 70,
    explanationLength: 'MEDIUM',
    fallbackToFSRS: true,
    preferredTopics: [],
    avoidedTopics: [],
    explorationRate: 20,
    studySchedule: {
      monday: true,
      tuesday: true,
      wednesday: true,
      thursday: true,
      friday: true,
      saturday: false,
      sunday: false
    },
    preferredStudyTimes: ['evening'],
    dailyReminder: true,
    reminderTime: '20:00',
    weeklyProgress: true,
    achievementNotifications: true,
    maxRecommendationsPerDay: 10,
    adaptiveDifficulty: true,
    personalizedFeedback: true,
    dataSharing: false
  })

  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadUserPreferences()
  }, [])

  const loadUserPreferences = async () => {
    setLoading(true)
    try {
      // Mock API call - replace with actual implementation
      // const response = await fetch('/api/user/preferences')
      // const data = await response.json()
      // setPreferences(data)
      
      // For now, load from localStorage as a demo
      const saved = localStorage.getItem('userPreferences')
      if (saved) {
        setPreferences(JSON.parse(saved))
      }
    } catch (error) {
      console.error('Failed to load user preferences:', error)
    } finally {
      setLoading(false)
    }
  }

  const savePreferences = async () => {
    setSaving(true)
    try {
      // Mock API call - replace with actual implementation
      // await fetch('/api/user/preferences', {
      //   method: 'PUT',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(preferences)
      // })
      
      // For now, save to localStorage as a demo
      localStorage.setItem('userPreferences', JSON.stringify(preferences))
      
      // Show success message
      alert('偏好设置已保存！')
    } catch (error) {
      console.error('Failed to save preferences:', error)
      alert('保存失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  const resetToDefaults = () => {
    if (confirm('确定要重置所有设置到默认值吗？')) {
      setPreferences({
        primaryObjective: 'SKILL_BUILDING',
        targetDomains: [],
        difficultyPreference: 'ADAPTIVE',
        dailyPracticeMinutes: 60,
        sessionTimeLimit: 30,
        breakDuration: 10,
        aiRecommendationEnabled: true,
        confidenceThreshold: 70,
        explanationLength: 'MEDIUM',
        fallbackToFSRS: true,
        preferredTopics: [],
        avoidedTopics: [],
        explorationRate: 20,
        studySchedule: {
          monday: true,
          tuesday: true,
          wednesday: true,
          thursday: true,
          friday: true,
          saturday: false,
          sunday: false
        },
        preferredStudyTimes: ['evening'],
        dailyReminder: true,
        reminderTime: '20:00',
        weeklyProgress: true,
        achievementNotifications: true,
        maxRecommendationsPerDay: 10,
        adaptiveDifficulty: true,
        personalizedFeedback: true,
        dataSharing: false
      })
    }
  }

  const toggleTopic = (topic: string, type: 'preferred' | 'avoided') => {
    const field = type === 'preferred' ? 'preferredTopics' : 'avoidedTopics'
    const otherField = type === 'preferred' ? 'avoidedTopics' : 'preferredTopics'
    
    setPreferences(prev => {
      const current = prev[field]
      const other = prev[otherField]
      
      if (current.includes(topic)) {
        // Remove from current
        return {
          ...prev,
          [field]: current.filter(t => t !== topic)
        }
      } else {
        // Add to current, remove from other if exists
        return {
          ...prev,
          [field]: [...current, topic],
          [otherField]: other.filter(t => t !== topic)
        }
      }
    })
  }

  const toggleScheduleDay = (day: keyof UserPreferences['studySchedule']) => {
    setPreferences(prev => ({
      ...prev,
      studySchedule: {
        ...prev.studySchedule,
        [day]: !prev.studySchedule[day]
      }
    }))
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">加载偏好设置...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-4xl mx-auto p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button
              onClick={() => window.history.back()}
              variant="outline"
              size="sm"
              className="flex items-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              返回
            </Button>
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                学习偏好设置
              </h1>
              <p className="text-gray-600 dark:text-gray-400 mt-1">
                个性化您的学习体验和推荐算法
              </p>
            </div>
          </div>
          
          <div className="flex gap-2">
            <Button
              onClick={resetToDefaults}
              variant="outline"
              className="flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              重置默认
            </Button>
            <Button
              onClick={savePreferences}
              disabled={saving}
              className="flex items-center gap-2"
            >
              {saving ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              ) : (
                <Save className="w-4 h-4" />
              )}
              保存设置
            </Button>
          </div>
        </div>

        <Tabs defaultValue="learning" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="learning" className="flex items-center gap-2">
              <Target className="w-4 h-4" />
              学习目标
            </TabsTrigger>
            <TabsTrigger value="ai" className="flex items-center gap-2">
              <Brain className="w-4 h-4" />
              AI 推荐
            </TabsTrigger>
            <TabsTrigger value="schedule" className="flex items-center gap-2">
              <Clock className="w-4 h-4" />
              时间安排
            </TabsTrigger>
            <TabsTrigger value="advanced" className="flex items-center gap-2">
              <Settings className="w-4 h-4" />
              高级设置
            </TabsTrigger>
          </TabsList>

          {/* Learning Objectives Tab */}
          <TabsContent value="learning" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Target className="w-5 h-5" />
                  学习目标与偏好
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <Label className="text-base font-medium mb-3 block">主要学习目标</Label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {OBJECTIVE_OPTIONS.map((option) => (
                      <div
                        key={option.value}
                        className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                          preferences.primaryObjective === option.value
                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-950'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                        }`}
                        onClick={() => setPreferences(prev => ({ ...prev, primaryObjective: option.value as any }))}
                      >
                        <h3 className="font-medium text-gray-900 dark:text-white">{option.label}</h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{option.description}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-3 block">难度偏好</Label>
                  <div className="grid grid-cols-1 gap-2">
                    {DIFFICULTY_OPTIONS.map((option) => (
                      <div
                        key={option.value}
                        className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                          preferences.difficultyPreference === option.value
                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-950'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                        }`}
                        onClick={() => setPreferences(prev => ({ ...prev, difficultyPreference: option.value as any }))}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <h4 className="font-medium text-gray-900 dark:text-white">{option.label}</h4>
                            <p className="text-sm text-gray-500 dark:text-gray-400">{option.description}</p>
                          </div>
                          <div className={`w-4 h-4 rounded-full border-2 ${
                            preferences.difficultyPreference === option.value
                              ? 'border-blue-500 bg-blue-500'
                              : 'border-gray-300'
                          }`} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-3 block">主题偏好</Label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">偏爱主题</h4>
                      <div className="flex flex-wrap gap-2">
                        {TOPIC_OPTIONS.map(topic => (
                          <Badge
                            key={topic}
                            variant={preferences.preferredTopics.includes(topic) ? "default" : "outline"}
                            className="cursor-pointer hover:bg-green-100 hover:text-green-800 dark:hover:bg-green-900 dark:hover:text-green-200"
                            onClick={() => toggleTopic(topic, 'preferred')}
                          >
                            {topic}
                            {preferences.preferredTopics.includes(topic) && ' ✓'}
                          </Badge>
                        ))}
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">避免主题</h4>
                      <div className="flex flex-wrap gap-2">
                        {TOPIC_OPTIONS.map(topic => (
                          <Badge
                            key={topic}
                            variant={preferences.avoidedTopics.includes(topic) ? "destructive" : "outline"}
                            className="cursor-pointer hover:bg-red-100 hover:text-red-800 dark:hover:bg-red-900 dark:hover:text-red-200"
                            onClick={() => toggleTopic(topic, 'avoided')}
                          >
                            {topic}
                            {preferences.avoidedTopics.includes(topic) && ' ✗'}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-3 block">
                    探索新主题比例: {preferences.explorationRate}%
                  </Label>
                  <Slider
                    value={[preferences.explorationRate]}
                    onValueChange={([value]) => setPreferences(prev => ({ ...prev, explorationRate: value }))}
                    max={100}
                    step={10}
                    className="w-full"
                  />
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
                    高比例将更多推荐未练习过的新主题，低比例将专注于已知薄弱领域
                  </p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* AI Recommendations Tab */}
          <TabsContent value="ai" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Brain className="w-5 h-5" />
                  AI 推荐设置
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="flex items-center justify-between">
                  <div>
                    <Label className="text-base font-medium">启用 AI 推荐</Label>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                      使用 AI 模型生成个性化推荐
                    </p>
                  </div>
                  <Switch
                    checked={preferences.aiRecommendationEnabled}
                    onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, aiRecommendationEnabled: checked }))}
                  />
                </div>

                {preferences.aiRecommendationEnabled && (
                  <>
                    <Separator />

                    <div>
                      <Label className="text-base font-medium mb-3 block">
                        推荐置信度阈值: {preferences.confidenceThreshold}%
                      </Label>
                      <Slider
                        value={[preferences.confidenceThreshold]}
                        onValueChange={([value]) => setPreferences(prev => ({ ...prev, confidenceThreshold: value }))}
                        min={50}
                        max={95}
                        step={5}
                        className="w-full"
                      />
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
                        只显示置信度高于此阈值的推荐，低置信度推荐将被过滤
                      </p>
                    </div>

                    <Separator />

                    <div>
                      <Label className="text-base font-medium mb-3 block">推荐解释详细程度</Label>
                      <Select
                        value={preferences.explanationLength}
                        onValueChange={(value: any) => setPreferences(prev => ({ ...prev, explanationLength: value }))}
                      >
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="SHORT">简短 - 一句话说明</SelectItem>
                          <SelectItem value="MEDIUM">中等 - 详细解释推荐理由</SelectItem>
                          <SelectItem value="DETAILED">详细 - 包含学习建议和解题思路</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <Separator />

                    <div className="flex items-center justify-between">
                      <div>
                        <Label className="text-base font-medium">AI 不可用时回退到 FSRS</Label>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                          当 AI 推荐服务不可用时，自动使用 FSRS 算法
                        </p>
                      </div>
                      <Switch
                        checked={preferences.fallbackToFSRS}
                        onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, fallbackToFSRS: checked }))}
                      />
                    </div>
                  </>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* Schedule Tab */}
          <TabsContent value="schedule" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Clock className="w-5 h-5" />
                  时间管理
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <Label htmlFor="dailyPractice" className="text-base font-medium">
                      每日练习目标 (分钟)
                    </Label>
                    <Input
                      id="dailyPractice"
                      type="number"
                      value={preferences.dailyPracticeMinutes}
                      onChange={(e) => setPreferences(prev => ({ 
                        ...prev, 
                        dailyPracticeMinutes: parseInt(e.target.value) || 0 
                      }))}
                      min="15"
                      max="480"
                      className="mt-1"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="sessionLimit" className="text-base font-medium">
                      单次会话时间限制 (分钟)
                    </Label>
                    <Input
                      id="sessionLimit"
                      type="number"
                      value={preferences.sessionTimeLimit}
                      onChange={(e) => setPreferences(prev => ({ 
                        ...prev, 
                        sessionTimeLimit: parseInt(e.target.value) || 0 
                      }))}
                      min="10"
                      max="120"
                      className="mt-1"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="breakDuration" className="text-base font-medium">
                      休息提醒间隔 (分钟)
                    </Label>
                    <Input
                      id="breakDuration"
                      type="number"
                      value={preferences.breakDuration}
                      onChange={(e) => setPreferences(prev => ({ 
                        ...prev, 
                        breakDuration: parseInt(e.target.value) || 0 
                      }))}
                      min="5"
                      max="60"
                      className="mt-1"
                    />
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-3 block">学习日程安排</Label>
                  <div className="grid grid-cols-7 gap-2">
                    {[
                      { key: 'monday' as const, label: '周一' },
                      { key: 'tuesday' as const, label: '周二' },
                      { key: 'wednesday' as const, label: '周三' },
                      { key: 'thursday' as const, label: '周四' },
                      { key: 'friday' as const, label: '周五' },
                      { key: 'saturday' as const, label: '周六' },
                      { key: 'sunday' as const, label: '周日' }
                    ].map(day => (
                      <div
                        key={day.key}
                        className={`p-3 text-center border rounded-lg cursor-pointer transition-colors ${
                          preferences.studySchedule[day.key]
                            ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                        }`}
                        onClick={() => toggleScheduleDay(day.key)}
                      >
                        <div className="text-sm font-medium">{day.label}</div>
                        {preferences.studySchedule[day.key] && (
                          <div className="text-xs mt-1">✓</div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-3 block">偏好学习时段</Label>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                    {[
                      { value: 'morning', label: '早上 (6-12点)' },
                      { value: 'afternoon', label: '下午 (12-18点)' },
                      { value: 'evening', label: '晚上 (18-22点)' },
                      { value: 'night', label: '深夜 (22-6点)' }
                    ].map(time => (
                      <div
                        key={time.value}
                        className={`p-3 text-center border rounded-lg cursor-pointer transition-colors ${
                          preferences.preferredStudyTimes.includes(time.value)
                            ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300'
                            : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                        }`}
                        onClick={() => {
                          const times = preferences.preferredStudyTimes
                          if (times.includes(time.value)) {
                            setPreferences(prev => ({
                              ...prev,
                              preferredStudyTimes: times.filter(t => t !== time.value)
                            }))
                          } else {
                            setPreferences(prev => ({
                              ...prev,
                              preferredStudyTimes: [...times, time.value]
                            }))
                          }
                        }}
                      >
                        <div className="text-sm font-medium">{time.label}</div>
                      </div>
                    ))}
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-4 block">提醒设置</Label>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <Label className="font-medium">每日学习提醒</Label>
                        <p className="text-sm text-gray-500 dark:text-gray-400">每天定时提醒开始学习</p>
                      </div>
                      <Switch
                        checked={preferences.dailyReminder}
                        onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, dailyReminder: checked }))}
                      />
                    </div>
                    
                    {preferences.dailyReminder && (
                      <div className="ml-6">
                        <Label htmlFor="reminderTime" className="font-medium">提醒时间</Label>
                        <Input
                          id="reminderTime"
                          type="time"
                          value={preferences.reminderTime}
                          onChange={(e) => setPreferences(prev => ({ ...prev, reminderTime: e.target.value }))}
                          className="mt-1 w-32"
                        />
                      </div>
                    )}
                    
                    <div className="flex items-center justify-between">
                      <div>
                        <Label className="font-medium">周度进度报告</Label>
                        <p className="text-sm text-gray-500 dark:text-gray-400">每周发送学习进度总结</p>
                      </div>
                      <Switch
                        checked={preferences.weeklyProgress}
                        onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, weeklyProgress: checked }))}
                      />
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <div>
                        <Label className="font-medium">成就通知</Label>
                        <p className="text-sm text-gray-500 dark:text-gray-400">达成学习里程碑时通知</p>
                      </div>
                      <Switch
                        checked={preferences.achievementNotifications}
                        onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, achievementNotifications: checked }))}
                      />
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Advanced Settings Tab */}
          <TabsContent value="advanced" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="w-5 h-5" />
                  高级设置
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <Label htmlFor="maxRecs" className="text-base font-medium">
                    每日最大推荐数量
                  </Label>
                  <Input
                    id="maxRecs"
                    type="number"
                    value={preferences.maxRecommendationsPerDay}
                    onChange={(e) => setPreferences(prev => ({ 
                      ...prev, 
                      maxRecommendationsPerDay: parseInt(e.target.value) || 1 
                    }))}
                    min="1"
                    max="50"
                    className="mt-1 w-32"
                  />
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                    避免推荐过载，提高学习专注度
                  </p>
                </div>

                <Separator />

                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <Label className="text-base font-medium">自适应难度调整</Label>
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        根据近期表现自动调整推荐难度
                      </p>
                    </div>
                    <Switch
                      checked={preferences.adaptiveDifficulty}
                      onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, adaptiveDifficulty: checked }))}
                    />
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div>
                      <Label className="text-base font-medium">个性化反馈</Label>
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        基于学习历史提供个性化的反馈和建议
                      </p>
                    </div>
                    <Switch
                      checked={preferences.personalizedFeedback}
                      onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, personalizedFeedback: checked }))}
                    />
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div>
                      <Label className="text-base font-medium">数据共享</Label>
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        匿名分享学习数据以改进推荐算法（不包含个人信息）
                      </p>
                    </div>
                    <Switch
                      checked={preferences.dataSharing}
                      onCheckedChange={(checked) => setPreferences(prev => ({ ...prev, dataSharing: checked }))}
                    />
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-base font-medium mb-2 block">备注与自定义需求</Label>
                  <Textarea
                    placeholder="描述您的特殊学习需求或偏好..."
                    className="min-h-[100px]"
                  />
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
                    这些信息将帮助我们更好地个性化您的学习体验
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Zap className="w-5 h-5" />
                  实验功能
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4 text-center py-8">
                  <Zap className="w-12 h-12 mx-auto text-gray-400" />
                  <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                    敬请期待
                  </h3>
                  <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto">
                    我们正在开发更多实验性功能，包括 AI 对话式学习助手、智能学习路径规划等
                  </p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}