# User Profile Analysis 实施建议（面向 Claude Code）

- 目标：基于 FSRS 学习数据（FSRSCard、ReviewLog）构建高质量、可缓存的用户画像，为后续 LLM 排序与混合推荐提供稳定、可解释的信号。
- 产出：UserProfile DTO + UserProfilingService 服务 + 计算公式与阈值 + 缓存与失效策略 + 对接位点。

## 数据源与关键字段
- FSRSCard（表 fsrs_cards）
  - 关注：`difficulty`、`stability`、`state`、`review_count`、`lapses`、`last_review_at`、`next_review_at`、`interval_days`、`grade`
  - 已有便捷方法：`calculateRetentionProbability()`、优先级/过期天数计算
- ReviewLog（表 review_logs）
  - 关注：`rating(1..4)`、`response_time_ms`、`old/new_state`、`old/new_difficulty`、`old/new_stability`、`reviewed_at`
  - 可聚合：成功率、耗时分布、月度趋势、难度表现
- Problem（表 problems）
  - 关注：`difficulty`、`tags(JSON)`、`title`
  - 用于从问题到“知识域/主题”的映射（tags → domain）

## 知识域/主题映射（tags → domain）
- 基本策略：解析 `Problem.tags`（JSON 数组），映射到标准域名；未命中映射记为 `OTHER`。
- 建议域名：数组、链表、哈希表、栈/队列、字符串、双指针、滑动窗口、排序/二分、搜索/回溯、分治、贪心、动态规划、图论、树/二叉树、堆/优先队列、位运算、数学/组合、前缀和/差分、单调栈/队列。
- 实施细节：
  - 在 `UserProfilingService` 内维护 `Map<String, String> tag2Domain`（可配置化），多标签问题可多域计数（可选上限1-2个域防止权重稀释）。

## 画像指标与计算（带平滑与衰减）
- 时间窗口：`recent30d`、`recent90d`、`lifetime`（默认用 recent90d 作为主画像；lifetime 作回退）。
- 成功率（Accuracy）
  - 来自 ReviewLog：`rating>=3` 视为成功。
  - Beta 平滑：`acc = (succ + α) / (total + α + β)`，建议 α=1, β=1（拉普拉斯平滑）。
- 保持/遗忘（Retention）
  - 卡片层：基于 FSRSCard 的 `calculateRetentionProbability()` 取域内均值（支持按最近一次 `last_review_at` 做权重）。
  - 回退：用 `new_stability` 与 `elapsed_days` 近似估计（数据不足时给 0.5 中性值）。
- 难度适配（Difficulty Fit）
  - 度量问题难度与表现的匹配度：`fit = acc - penalty(difficulty)`，或以 E/M/H 分箱统计平均成功率与平均响应时间。
- 复习负载/尝试（Attempts）
  - 域内 `review_count` 均值/总和、`lapses` 率（lapses / review_count）。
- 响应时长（RT）
  - 使用 ReviewLog `response_time_ms`，域内取均值或 P75；归一化到 [0,1]（短更好）。
- 衰减权重（Recency Weight）
  - `w(t) = exp(-Δdays / halfLife)`，建议 halfLife=30 天，对近数据加权更高。
- 综合技能分（SkillScore）
  - 归一化后的加权和（示例）：
    - `skill = 0.45*acc + 0.25*retention + 0.15*(1 - lapseRate) + 0.15*(1 - rtNorm)`，裁剪到 [0,1]。
  - 抽样数不足（<N）时降权或回退到 lifetime/系统均值。
- 弱项判定与样本门限
  - 弱项：`skill < 0.45` 且样本量 `>= 10`；强项：`skill > 0.75` 且样本量 `>= 10`。
- 偏好向量（Preference Vector）
  - 难度偏好：E/M/H 分布（近90天占比）+ 近期趋势（hard↑/easy↑）。
  - 主题偏好：域占比（按衰减权重累加的交互量）。

## 模块设计（包名按规范 com.codetop…）
- DTO：`com.codetop.recommendation.dto.UserProfile`
  - 字段建议：
    - `Long userId`, `Instant generatedAt`, `String window`（如 recent90d）
    - `Map<String, DomainSkill> domainSkills`
    - `DifficultyPref difficultyPref`（easy/medium/hard ∈ [0,1]）
    - `Map<String, Double> tagAffinity`（可与 domainSkills 协同）
    - `double overallMastery`（全局技能概览）
  - `DomainSkill`：`samples`, `accuracy`, `retention`, `lapseRate`, `avgRtMs`, `attempts`, `skillScore`，`strength`（WEAK/NORMAL/STRONG）
- Service：`com.codetop.recommendation.service.UserProfilingService`
  - `UserProfile getUserProfile(Long userId, boolean useCache)`
  - `UserProfile computeUserProfile(Long userId)`
  - 私有方法：`loadData(...)`、`buildDomainStats(...)`、`calcDifficultyPref(...)`、`summarize(...)`
- Caching：
  - 键：`CacheKeyBuilder.userProfile(userId)`；TTL：1 小时（与任务文档一致）。
  - 失效：新增 ReviewLog、提交推荐反馈、FSRS 参数变更 → 删除 `userProfile` 键。
    - 可在 `RecommendationFeedbackService` 同步加入：`cacheService.delete(CacheKeyBuilder.userProfile(userId))`。

## 数据加载与聚合建议
- ReviewLog：`findRecentByUserId(userId, 2000)`（限制上限，控制内存与时延）
- FSRSCard：`findByUserId(userId)`（或仅取涉及的 problemId 集合对应的卡片）
- Problem：批量取 `selectBatchIds(problemIds)` 解析 `tags` JSON 为 `List<String>`
- 聚合思路：
  - 以 problemId 为键合并 ReviewLog + FSRSCard + Problem.tags，映射到域，按域累计各项指标。
  - 所有均值型指标使用样本加权与时间衰减：`weightedMean = sum(v_i * w_i) / sum(w_i)`。

## 伪代码（关键路径）
- 结构：
  - `UserProfile getUserProfile(uid, useCache)`
    - if useCache 且命中 → 返回
    - else 计算 → 写入缓存（TTL 1h）→ 返回
- 计算：
  - 加载 recent90d ReviewLog（<=2000）+ 对应 FSRSCard + Problem(tags)
  - 遍历日志，按 problem → tags → domain 聚合：
    - `succ += (rating>=3?1:0)`、`total += 1`、`rtSum += response_time_ms`、`lapses += (new_state==RELEARNING?1:0)`、`attempts += 1`
    - `retentionSum += fsrsCard.calculateRetentionProbability()`（若有）
    - `weight = exp(-Δdays/30)`，各项均乘权累加
  - 计算域内指标：`accuracy`（beta 平滑 + 加权）、`retention`、`lapseRate`、`avgRtMs`、`skillScore`，并打标强/弱项
  - 统计 E/M/H 占比与趋势，生成 `difficultyPref`
  - 汇总 `overallMastery = 加权 domain.skillScore 的均值`

## 与现有链路对接位点
- Prompt 注入：在 `PromptTemplateService` v2 中加入真实画像摘要（而非仅基于 candidates 的简单洞察）：
  - 扩展 `RequestContext`（或新增参数）传入 `UserProfile` 概要：`weakTopics/strongTopics`、`difficultyPref`、`averageAccuracy/overallMastery`。
  - `buildAdvancedUserMessage(...)` 中“User Profile”部分直接引用 `UserProfile` 的统计结果。
- 构建候选：`CandidateBuilder` 保持轻量，不必加载 profile；在 `AIRecommendationService` 调用链中先读取 `UserProfile`，再传给 Prompt 组装。
- 缓存隔离：画像键无需链路后缀；推荐结果缓存已按链/AB/Tier 隔离即可。

## 阈值与默认值（可配置）
- 窗口：90d 主窗口；lifetime 回退
- 衰减半衰期：30d
- Beta 平滑：α=1, β=1
- 弱项阈值：skill<0.45 且 samples≥10；强项：skill>0.75 且 samples≥10
- 冷启动：samples<10 → 中性画像：`accuracy=0.6, retention=0.6, lapseRate=0.2, rtNorm=0.5, skill≈0.55`

## 冷启动与降级策略
- 无日志/样本不足：
  - 使用全局均值或系统基线（Problem 难度总体分布、常见域优先级）
  - 提升探索比例：topic_coverage / progressive_difficulty
- LLM 关闭/降级：画像仍用于 FSRS fallback 的排序解释（例如弱项优先）。

## 性能与正确性
- 目标：计算耗时 < 50ms（2k 条日志，单用户），避免 DB N+1（批量加载 Problem 和 FSRSCard）。
- 单测：
  - 构造小规模 ReviewLog 集合验证 Beta 平滑、时间衰减、skillScore 阈值。
  - 验证缓存 TTL 与失效触发；无数据时的中性画像。
- 集成测：Testcontainers 注入 review_logs / fsrs_cards 基础数据，校验端到端画像生成。

## 建议的类与方法签名（草图）
- `com.codetop.recommendation.dto.UserProfile`
  - `Map<String, DomainSkill> getDomainSkills()`
  - `DifficultyPref getDifficultyPref()`
  - `double getOverallMastery()`
- `com.codetop.recommendation.service.UserProfilingService`
  - `public UserProfile getUserProfile(Long userId, boolean useCache)`
  - `protected UserProfile computeUserProfile(Long userId)`
  - 依赖：`ReviewLogMapper`、`FSRSCardMapper`、`ProblemMapper`、`CacheService`
- 缓存键：`CacheKeyBuilder.userProfile(userId)`，TTL `Duration.ofHours(1)`
- 失效：
  - Review 完成后插入 ReviewLog 的地方（或 `FSRSService` 评审流程结束处）尝试清理：`cacheService.delete(CacheKeyBuilder.userProfile(userId))`
  - 已有 `RecommendationFeedbackService` 中可顺带失效用户画像。

## Prompt 侧摘要建议（可直接用于模板）
- Learning Pattern：`overallMastery` 映射为 `struggling / steady_progress / advanced`
- Weak Topics：按 `skillScore` 升序取前 2-3 个域
- Strong Topics：按 `skillScore` 降序取前 2-3 个域
- Difficulty Preference：输出 `E/M/H` 分布与趋势（如 prefers_challenge/building_confidence/balanced_approach）

## 最小可行交付（Suggested MVP）
- v1：只做 recent90d、域内 `accuracy/retention/lapseRate/avgRtMs/skillScore` + `difficultyPref`，缓存 1h，失效在 Review/反馈时触发。
- v2：补齐 lifetime 回退、阈值配置化、域映射可配置、更多质量监控指标（样本数、覆盖度等）。

---

以上建议尽量复用现有 Mapper 与 Cache 基础设施，偏向可快速落地与可运维（可配置、可观测、可缓存）。如需，我可以补充 UserProfile/Service 的接口草稿代码与测试样例数据结构。

