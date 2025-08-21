"use client"

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Brain, Clock, Target, TrendingUp, Calendar, Zap, RotateCcw, AlertCircle } from "lucide-react"
import { useFSRSData, useCardStateDistribution, useMemoryStrengthData, useReviewUrgencyData, useDifficultyProgressionData, useLearningVelocity } from "@/hooks/use-fsrs-data"

// Custom tooltip component for better styling
const CustomTooltip = ({ active, payload, label }: {
  active?: boolean
  payload?: Array<{ name: string; value: number; color: string }>
  label?: string
}) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-3">
        <p className="text-sm font-medium text-gray-900 dark:text-white">{`${label}`}</p>
        {payload.map((entry: any, index: number) => (
          <p key={index} className="text-sm" style={{ color: entry.color }}>
            {`${entry.name}: ${entry.value}`}
          </p>
        ))}
      </div>
    )
  }
  return null
}

// Loading skeleton component
const LoadingSkeleton = () => (
  <div className="animate-pulse">
    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
    <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/2"></div>
  </div>
)

// Enhanced donut chart component
const DonutChart = ({ data, centerValue, centerLabel }: { 
  data: Array<{ name: string; value: number; color: string }>, 
  centerValue: string | number, 
  centerLabel: string 
}) => (
  <div className="relative">
    <ResponsiveContainer width="100%" height={250}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={100}
          paddingAngle={2}
          dataKey="value"
        >
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
      </PieChart>
    </ResponsiveContainer>
    <div className="absolute inset-0 flex items-center justify-center">
      <div className="text-center">
        <div className="text-2xl font-bold text-gray-900 dark:text-white">{centerValue}</div>
        <div className="text-sm text-gray-500 dark:text-gray-400">{centerLabel}</div>
      </div>
    </div>
  </div>
)

export default function AnalysisPage() {
  const { stats, cards, dueCards, overdueCards, isLoading, error, refetch } = useFSRSData()
  const cardStateData = useCardStateDistribution(stats)
  const memoryStrengthData = useMemoryStrengthData(cards)
  const reviewUrgencyData = useReviewUrgencyData(dueCards, overdueCards)
  const difficultyData = useDifficultyProgressionData(cards)
  const velocityData = useLearningVelocity(stats)

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">学习分析</h1>
          <button 
            onClick={refetch}
            className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
          >
            重新加载
          </button>
        </div>
        <Card>
          <CardContent className="flex items-center justify-center p-6">
            <div className="text-center">
              <AlertCircle className="h-8 w-8 text-red-500 mx-auto mb-2" />
              <p className="text-gray-500 dark:text-gray-400">加载数据失败: {error}</p>
              <button 
                onClick={refetch}
                className="mt-2 text-blue-600 dark:text-blue-400 hover:underline"
              >
                点击重试
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">学习分析</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">基于FSRS算法的学习进度分析</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card className="bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900/20 dark:to-blue-800/20 border-blue-200 dark:border-blue-800">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-blue-900 dark:text-blue-100">总卡片数</CardTitle>
            <Target className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          </CardHeader>
          <CardContent>
            {isLoading ? <LoadingSkeleton /> : (
              <>
                <div className="text-2xl font-bold text-blue-900 dark:text-blue-100">{stats?.totalCards || 0}</div>
                <p className="text-xs text-blue-600 dark:text-blue-400">学习卡片总数</p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-r from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-800/20 border-green-200 dark:border-green-800">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-green-900 dark:text-green-100">学习效率</CardTitle>
            <Brain className="h-4 w-4 text-green-600 dark:text-green-400" />
          </CardHeader>
          <CardContent>
            {isLoading ? <LoadingSkeleton /> : (
              <>
                <div className="text-2xl font-bold text-green-900 dark:text-green-100">{velocityData?.learningEfficiency || 0}%</div>
                <p className="text-xs text-green-600 dark:text-green-400">掌握程度</p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-r from-orange-50 to-orange-100 dark:from-orange-900/20 dark:to-orange-800/20 border-orange-200 dark:border-orange-800">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-orange-900 dark:text-orange-100">待复习</CardTitle>
            <Clock className="h-4 w-4 text-orange-600 dark:text-orange-400" />
          </CardHeader>
          <CardContent>
            {isLoading ? <LoadingSkeleton /> : (
              <>
                <div className="text-2xl font-bold text-orange-900 dark:text-orange-100">{stats?.dueCards || 0}</div>
                <p className="text-xs text-orange-600 dark:text-orange-400">到期卡片数</p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-r from-purple-50 to-purple-100 dark:from-purple-900/20 dark:to-purple-800/20 border-purple-200 dark:border-purple-800">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-purple-900 dark:text-purple-100">记忆强度</CardTitle>
            <Zap className="h-4 w-4 text-purple-600 dark:text-purple-400" />
          </CardHeader>
          <CardContent>
            {isLoading ? <LoadingSkeleton /> : (
              <>
                <div className="text-2xl font-bold text-purple-900 dark:text-purple-100">{stats?.avgStability?.toFixed(1) || 0}</div>
                <p className="text-xs text-purple-600 dark:text-purple-400">平均稳定性</p>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Card State Distribution */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              卡片状态分布
            </CardTitle>
            <CardDescription>当前学习阶段分布情况</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="h-[250px] flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
              </div>
            ) : (
              <DonutChart 
                data={cardStateData} 
                centerValue={stats?.totalCards || 0}
                centerLabel="总卡片"
              />
            )}
            <div className="grid grid-cols-2 gap-2 mt-4">
              {cardStateData.map((item, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: item.color }}
                  />
                  <span className="text-xs text-gray-600 dark:text-gray-400">
                    {item.name}: {item.value}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Memory Strength Distribution */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Brain className="h-5 w-5" />
              记忆强度分布
            </CardTitle>
            <CardDescription>基于稳定性的记忆强度分析</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="h-[250px] flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={memoryStrengthData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                  <defs>
                    <linearGradient id="colorBar" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.3}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                  <XAxis 
                    dataKey="name" 
                    tick={{ fontSize: 12 }}
                    stroke="#6B7280"
                  />
                  <YAxis tick={{ fontSize: 12 }} stroke="#6B7280" />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar 
                    dataKey="value" 
                    fill="url(#colorBar)"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Review Urgency */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5" />
              复习紧急程度
            </CardTitle>
            <CardDescription>按时间分组的复习任务</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                {[1,2,3,4].map(i => (
                  <div key={i} className="space-y-2">
                    <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4"></div>
                    <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded"></div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="space-y-4">
                {reviewUrgencyData.map((item, index) => (
                  <div key={index} className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium">{item.name}</span>
                      <span className="text-sm font-bold" style={{ color: item.color }}>
                        {item.value}
                      </span>
                    </div>
                    <Progress 
                      value={Math.min(100, (item.value / Math.max(1, Math.max(...reviewUrgencyData.map(d => d.value)))) * 100)} 
                      className="h-2"
                      style={{ 
                        backgroundColor: `${item.color}20`,
                      }}
                    />
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Difficulty Progression */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5" />
              难度分布
            </CardTitle>
            <CardDescription>卡片难度级别分布</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="h-[200px] flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
              </div>
            ) : (
              <DonutChart 
                data={difficultyData} 
                centerValue={`${stats?.avgDifficulty?.toFixed(1) || 0}`}
                centerLabel="平均难度"
              />
            )}
            <div className="grid grid-cols-2 gap-2 mt-4">
              {difficultyData.map((item, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: item.color }}
                  />
                  <span className="text-xs text-gray-600 dark:text-gray-400">
                    {item.name}: {item.value}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Learning Velocity */}
      {velocityData && (
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <RotateCcw className="h-5 w-5" />
              学习进度概览
            </CardTitle>
            <CardDescription>整体学习效果和进度评估</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">掌握进度</span>
                  <span className="text-sm font-bold text-green-600">
                    {velocityData.learningEfficiency}%
                  </span>
                </div>
                <Progress value={velocityData.learningEfficiency} className="h-2" />
                <p className="text-xs text-gray-500">已掌握 {velocityData.masteryProgress} / {velocityData.totalProgress} 张卡片</p>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">活跃比例</span>
                  <span className="text-sm font-bold text-blue-600">
                    {velocityData.activeCardsRatio}%
                  </span>
                </div>
                <Progress value={velocityData.activeCardsRatio} className="h-2" />
                <p className="text-xs text-gray-500">正在学习的卡片比例</p>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">记忆保持</span>
                  <span className="text-sm font-bold text-purple-600">
                    {velocityData.retentionRate}%
                  </span>
                </div>
                <Progress value={velocityData.retentionRate} className="h-2" />
                <p className="text-xs text-gray-500">基于稳定性的保持率估算</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
