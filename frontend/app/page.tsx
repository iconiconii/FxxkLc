import ProblemList from "@/components/home/problem-list"
import AuthPanel from "@/components/home/auth-panel"

export default function Home() {
  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* 拼色背景 */}
      <div className="absolute inset-0 bg-gradient-to-br from-indigo-50 via-white to-cyan-50 dark:from-slate-900 dark:via-blue-900/20 dark:to-violet-900/20"></div>
      
      {/* 装饰性几何图形 */}
      <div className="absolute top-0 left-0 w-72 h-72 bg-gradient-to-br from-purple-400/20 to-pink-400/20 rounded-full blur-3xl"></div>
      <div className="absolute top-1/4 right-0 w-96 h-96 bg-gradient-to-bl from-blue-400/20 to-cyan-400/20 rounded-full blur-3xl"></div>
      <div className="absolute bottom-0 left-1/3 w-80 h-80 bg-gradient-to-tr from-green-400/20 to-yellow-400/20 rounded-full blur-3xl"></div>
      
      {/* 内容区域 */}
      <div className="relative z-10">
        <div className="container mx-auto px-6 py-8">
          {/* 顶部标题区域 */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center gap-3 mb-4">
              <img 
                src="/logo.svg" 
                alt="OLIVER Logo" 
                className="w-16 h-16 rounded-2xl"
              />
              <h1 className="text-4xl font-black text-slate-800 dark:text-slate-100 tracking-tight">
                OLIVER
              </h1>
            </div>
            <p className="text-lg text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
              精选高频算法题目，助你在技术面试中脱颖而出
            </p>
          </div>

          {/* FSRS 功能介绍区域 */}
          <div className="mb-8">
            <div className="bg-white/70 dark:bg-gray-800/70 backdrop-blur-md rounded-2xl p-6 border border-gray-200/40 dark:border-gray-700/40 shadow-sm">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* FSRS 核心介绍 */}
                <div className="md:col-span-2">
                  <div className="flex items-start gap-4">
                    <div className="w-10 h-10 bg-gradient-to-br from-emerald-400/20 to-teal-400/20 rounded-xl flex items-center justify-center flex-shrink-0">
                      <span className="text-lg">🧠</span>
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-gray-800 dark:text-white mb-2">
                        FSRS 智能复习算法
                      </h3>
                      <p className="text-sm text-gray-600 dark:text-gray-400 leading-relaxed mb-3">
                        基于认知科学的间隔重复算法，个性化调整复习时间，提高记忆效率。科学优化你的学习路径，让每次复习都更加高效。
                      </p>
                      <div className="flex flex-wrap gap-2">
                        <span className="inline-flex items-center gap-1 text-xs bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 px-2 py-1 rounded">
                          <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full"></span>
                          个性化参数
                        </span>
                        <span className="inline-flex items-center gap-1 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 px-2 py-1 rounded">
                          <span className="w-1.5 h-1.5 bg-blue-500 rounded-full"></span>
                          智能调度
                        </span>
                        <span className="inline-flex items-center gap-1 text-xs bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 px-2 py-1 rounded">
                          <span className="w-1.5 h-1.5 bg-purple-500 rounded-full"></span>
                          记忆优化
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 数据统计 */}
                <div className="space-y-4">
                  <div className="text-center p-4 bg-gray-50/80 dark:bg-gray-700/50 rounded-xl">
                    <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">90%</div>
                    <div className="text-xs text-gray-600 dark:text-gray-400">目标记忆保持率</div>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div className="text-center p-3 bg-gray-50/80 dark:bg-gray-700/50 rounded-lg">
                      <div className="text-lg font-bold text-emerald-600 dark:text-emerald-400">17</div>
                      <div className="text-xs text-gray-600 dark:text-gray-400">算法参数</div>
                    </div>
                    <div className="text-center p-3 bg-gray-50/80 dark:bg-gray-700/50 rounded-lg">
                      <div className="text-lg font-bold text-purple-600 dark:text-purple-400">4.5+</div>
                      <div className="text-xs text-gray-600 dark:text-gray-400">FSRS版本</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          {/* 7:3 Grid Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-10 gap-8">
            {/* Left Side - Problem List (70%) */}
            <div className="lg:col-span-7">
              <ProblemList />
            </div>
            
            {/* Right Side - Auth Panel (30%) */}
            <div className="lg:col-span-3">
              <AuthPanel />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
