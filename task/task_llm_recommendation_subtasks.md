# LLM智能推荐题目功能开发

LLM Service Integration
[x] AI Recommendation Service
  - [x] Create `AIRecommendationService` class in `service/` package with LLM client integration
  - [x] Implement OpenAI GPT-4 or alternative LLM API client with async support（已实现 OpenAI 兼容 provider，同步/异步、严格 JSON 解析）
  - [x] Design prompt engineering templates for recommendation generation（已实现 `PromptTemplateService`，默认 v2 模板）
  - [x] Add retry mechanism, timeout control, and graceful error handling（Resilience4j Retry；节点/Provider 超时；异常分类与兜底）
  - [x] Configure rate limiting to prevent API quota exhaustion（全局/每用户/节点级限流，异步并发保护）

[x] Provider Chain (多模型责任链)
  - [x] 引入 `ProviderChain` 责任链，按配置顺序尝试多个模型/提供商，失败则切到下一个
  - [x] 实现 `DefaultProvider` 作为末节点兜底，返回“系统繁忙”信息（策略可配）
  - [x] 支持在 `application.yml` 中以声明式方式组装责任链（按 profile 可差异化）
  - [x] 每个节点可配置超时、重试、限流与“哪些错误转下游”条件（如 TIMEOUT/HTTP_429/CIRCUIT_OPEN）（已支持 timeout/retry/rateLimit/onErrorsToNext，异步亦支持）
  - [x] 输出链路指标与日志（命中节点、跳数、失败原因、最终策略）（已输出 chainHops/strategy/fallbackReason 至 meta/headers，日志基础版）
  - [x] 根据用户消费/会员等级、AB组、路由等选择不同责任链（策略解析 + 多链注册表）（已实现 ChainSelector + 路由规则配置）

[x] Configuration Management
  - [x] Add LLM configuration section to `application.yml` (API keys, endpoints, models)
  - [x] Support multi-environment configs (dev/test/prod) with different LLM providers（dev/test/prod 示例与启停策略已补齐）
  - [x] Implement secure API key management via environment variables
  - [x] Add feature toggles for LLM recommendation on/off per user segment（已实现 LlmToggleService + 分段特性开关配置）
  - [x] 增加 `llm.chain` 配置块：定义节点顺序、启用条件、错误下钻策略与 `defaultProvider` 的兜底策略

YAML 配置（示例）
```yaml
llm:
  enabled: true
  provider: chain
  chain:
    nodes:
      - name: openai
        enabled: true
        conditions:
          abGroups: [A, default]
          routes: [ai-recommendations]
        timeoutMs: 1800
        rateLimit:
          rps: 5
        retry:
          attempts: 1
        onErrorsToNext: [TIMEOUT, HTTP_429, CIRCUIT_OPEN, PARSING_ERROR]
      - name: azure
        enabled: true
        conditions:
          abGroups: [B, default]
        timeoutMs: 1800
        onErrorsToNext: [TIMEOUT, HTTP_5XX, PARSING_ERROR]
      - name: mock
        enabled: false
    defaultProvider:
      strategy: busy_message   # busy_message | fsrs_fallback | empty
      message: "系统繁忙，请稍后重试"
      httpStatus: 503
  openai:
    baseUrl: https://api.openai.com/v1
    model: gpt-4o-mini
    apiKeyEnv: OPENAI_API_KEY
  azure:
    endpoint: https://xxx.openai.azure.com
    deployment: gpt-4o-mini
    apiKeyEnv: AZURE_OPENAI_KEY
```

Assumptions / Constraints / Non-goals
- Assumptions: LLM API access available; existing FSRS data sufficient for user profiling
- Constraints: LLM API cost budget; response time < 2s; backward compatibility with existing recommendation
- Non-goals: Real-time chat interface; content generation; replacing FSRS algorithm entirely

Open Questions
- Which LLM provider offers best cost/performance for recommendation tasks?
- Should we support multiple LLM models simultaneously for A/B testing?
- How to handle LLM API outages without degrading user experience?

Acceptance Criteria
- [x] `AIRecommendationService` successfully calls LLM API and parses responses（已实现 OpenAI 兼容调用与严格 JSON 解析，含同步/异步）
- [x] Configuration supports multiple environments and secure credential management（按 dev/test/prod 配置与环境变量管理凭据）
- [x] Service gracefully handles API failures with fallback to FSRS recommendations（责任链默认化 + 服务层 FSRS 回退）
- [x] Rate limiting prevents API quota overruns
- [x] 责任链顺序由 `application.yml` 决定，节点启停/顺序变更可通过配置切换
- [x] 上游全部失败时，由 `DefaultProvider` 返回“系统繁忙”或按策略回退（支持 busy_message / fsrs_fallback）

Commit Message
feat(ai): add LLM service integration for intelligent recommendations
- Implement AIRecommendationService with OpenAI GPT-4 client
- Add multi-environment configuration and secure API key management
- Include retry logic, rate limiting, and fallback mechanisms

---

Progress Update (当前进展概览)
- 后端服务：完成 `AIRecommendationService` 骨架并接入责任链（ProviderChain + DefaultProvider），提供固定 tier 上下文；OpenAI Provider 为占位实现（待完善 Prompt/解析）。
- 弹性保障：引入 Resilience4j Retry（1 次受控重试）与全局/每用户 RateLimiter；基础超时/错误处理就绪。
- 配置：`application.yml` 增加 `llm` 与 `resilience4j` 配置；支持通过 YAML 声明责任链与兜底策略。
- 接口：新增 `GET /api/v1/problems/ai-recommendations`，返回包含链路 hops、traceId 的响应与相应头部。
- 测试：补充 ProviderChain 与 AIRecommendationService 单元测试（重试、限流、兜底、服务正常/忙碌路径）。
- 未完成：Prompt 模板与 JSON 解析、FSRS 回退实现、节点级限流与错误下钻、缓存与指标、反馈 API、按用户分层策略解析、多环境差异配置与前端集成。

User Profiling & Algorithm
[ ] User Profile Analysis
  - [ ] Analyze `FSRSCard` and `ReviewLog` data to extract learning patterns
  - [ ] Calculate skill levels per knowledge domain (arrays, graphs, dynamic programming, etc.)
  - [ ] Identify weak areas based on review performance and retention rates
  - [ ] Build user preference vectors from problem interaction history

[ ] Problem Feature Enhancement  
  - [ ] Add `content` TEXT field to `Problem` entity for detailed descriptions
  - [ ] Extract knowledge points, algorithm types, and complexity features from problem content
  - [ ] Implement problem similarity scoring based on tags, difficulty, and content analysis
  - [ ] Create problem categorization mapping (topic → subtopic → specific algorithms)

[ ] Intelligent Recommendation Engine
  - [ ] Design prompt templates combining user profile + problem features + learning objectives
  - [ ] Implement hybrid algorithm merging FSRS scheduling with LLM content-based recommendations
  - [ ] Support multi-dimensional recommendation strategies (progressive difficulty, knowledge coverage, personalization)
  - [ ] Add confidence scoring for recommendation quality assessment

Assumptions / Constraints / Non-goals
- Assumptions: Problem content data available or can be enriched; user has sufficient learning history
- Constraints: Algorithm complexity manageable within response time limits; recommendation diversity maintained
- Non-goals: Automatic problem content generation; real-time collaborative filtering

Open Questions
- How many historical reviews needed for reliable user profiling?
- Should recommendation confidence scores be exposed to users?
- How to balance exploration vs exploitation in recommendation diversity?

Acceptance Criteria
- [ ] User profiles accurately reflect learning strengths and weaknesses
- [ ] Problem similarity scoring produces meaningful content-based groupings
- [ ] LLM recommendations complement rather than conflict with FSRS scheduling
- [ ] Recommendation quality measurably improves over baseline methods

Commit Message  
feat(algorithm): implement intelligent user profiling and hybrid recommendation engine
- Add user skill analysis based on FSRS review history
- Enhance Problem entity with content and feature extraction
- Merge LLM content recommendations with FSRS temporal scheduling

---

API & Caching Layer
[ ] Recommendation APIs
  - [x] Add `GET /api/v1/problems/ai-recommendations` endpoint in `ProblemController`
  - [ ] Implement `POST /api/v1/problems/{id}/recommendation-feedback` for user feedback collection
  - [ ] Support request parameters (limit, difficulty_preference, topic_filter, recommendation_type)（已支持 limit）
  - [x] Create response DTOs with recommendation reasons and confidence scores
  - [ ] Integrate AI recommendation toggle into existing `getRecommendedProblems` method

[ ] Caching Strategy
  - [ ] Implement Redis caching for user profiles with configurable TTL (1 hour default)
  - [ ] Cache LLM recommendation results with composite keys (user_id + preferences_hash)
  - [ ] Add cache invalidation triggers on user learning behavior changes
  - [ ] Optimize cache hit rates through intelligent pre-loading for active users

Assumptions / Constraints / Non-goals
- Assumptions: Redis cluster available; API response formats backward compatible
- Constraints: Cache memory usage reasonable; API latency targets maintained
- Non-goals: Real-time recommendation updates; complex cache warming strategies

Open Questions
- Optimal cache TTL balancing freshness vs performance?
- Should recommendation feedback immediately invalidate user cache?
- How to handle cache consistency across multiple app instances?

Acceptance Criteria
- [ ] AI recommendation API returns personalized results within latency targets
- [ ] Caching reduces LLM API calls by >80% for repeat requests
- [ ] User feedback properly collected and stored for future algorithm improvements
- [ ] Backward compatibility maintained with existing recommendation flows

Commit Message
feat(api): add AI recommendation endpoints with intelligent caching
- Implement personalized recommendation API with user feedback collection
- Add Redis-based caching for user profiles and LLM responses
- Maintain backward compatibility with existing recommendation system

---

Frontend Integration
[ ] UI Components
  - [ ] Create `AIRecommendationCard` React component for displaying recommended problems
  - [ ] Implement recommendation explanation UI showing reasoning and confidence
  - [ ] Add user feedback buttons (helpful/not helpful/already mastered) with analytics tracking
  - [ ] Design loading states and error handling for LLM recommendation delays

[ ] Dashboard Integration
  - [ ] Add "AI Recommendations" section to user dashboard with personalized suggestions
  - [ ] Implement toggle between traditional FSRS and AI-powered recommendations
  - [ ] Create recommendation history view showing past suggestions and outcomes
  - [ ] Add recommendation settings page for user preferences (topics, difficulty, frequency)

[ ] Problem List Enhancement
  - [ ] Integrate AI recommendation indicator in problem listing pages
  - [ ] Implement lazy loading and pagination for recommendation results
  - [ ] Add recommendation filters and sorting options
  - [ ] Create recommendation quality indicators (match score, confidence level)

Assumptions / Constraints / Non-goals
- Assumptions: Next.js frontend structure allows component integration; design system available
- Constraints: Page load performance maintained; mobile responsiveness required
- Non-goals: Complete UI redesign; complex recommendation visualization

Open Questions
- How detailed should recommendation explanations be for users?
- Should recommendation preferences be per-user or per-session?
- What's the ideal recommendation refresh frequency for active users?

Acceptance Criteria
- [ ] AI recommendation components render properly across devices and browsers
- [ ] User feedback collection works and data flows to backend analytics
- [ ] Dashboard integration enhances rather than clutters existing user experience
- [ ] Recommendation UI clearly communicates value and reasoning to users

Commit Message
feat(frontend): add AI recommendation UI components and dashboard integration
- Create AIRecommendationCard with explanation and feedback functionality
- Integrate AI recommendations into dashboard and problem listings
- Add user preference controls and recommendation history views

---

Testing & Monitoring
[ ] Automated Testing
  - [x] Unit tests for `AIRecommendationService` including LLM API mocking（已添加服务与责任链单测，使用测试 Provider 模拟）
  - [ ] Integration tests for recommendation algorithms with test user data
  - [ ] API contract tests for all new recommendation endpoints
  - [ ] Frontend component tests for AI recommendation UI elements

[ ] Performance & Quality Monitoring
  - [ ] Add metrics collection for LLM API response times and success rates
  - [ ] Implement recommendation quality scoring based on user engagement
  - [ ] Set up alerting for LLM service degradation or high error rates
  - [ ] Create recommendation analytics dashboard for system administrators

[ ] Load Testing & Optimization
  - [ ] Performance tests for recommendation endpoints under realistic load
  - [ ] Cache hit rate optimization and memory usage monitoring
  - [ ] LLM API quota usage tracking and cost optimization
  - [ ] Recommendation accuracy evaluation with A/B testing framework

Assumptions / Constraints / Non-goals
- Assumptions: Testing infrastructure supports API mocking; monitoring tools available
- Constraints: Test execution time reasonable; monitoring overhead minimal
- Non-goals: Manual testing procedures; complex ML model evaluation frameworks

Open Questions
- What metrics best indicate recommendation quality beyond user feedback?
- Should we implement automated rollback for poor-performing recommendation models?
- How to balance comprehensive testing with development velocity?

Acceptance Criteria
- [ ] All automated tests pass with >90% code coverage for new functionality
- [ ] Performance monitoring shows recommendation endpoints meet latency targets
- [ ] LLM API usage stays within budget constraints with alerting for overruns
- [ ] A/B testing framework validates recommendation quality improvements

Commit Message
feat(testing): add comprehensive testing and monitoring for AI recommendations
- Implement unit, integration, and performance tests for recommendation system
- Add metrics collection and alerting for LLM service health
- Create recommendation quality evaluation and A/B testing framework

---

架构与范围细化
- 端到端数据流：客户端请求 → `ProblemController` → `RecommendationFacade`（聚合FSRS与LLM）→ `AIRecommendationService`（LLM客户端 + Prompt/解析 + 限流/熔断/重试）→ 缓存（Redis）→ 数据层（Problem/FSRS/ReviewLog）→ 响应DTO → 前端渲染与反馈收集。
- 组件划分：
  - 后端：`com.codetop.recommendation`（controller, dto, service, provider, alg, cache, config, metrics）。
  - 前端：`frontend/app/(recommendations)/`, 组件在 `frontend/components/recommendation/`。
  - 配置：`application.yml` + 环境变量（`.env.example`）。
  - 存储：Postgres/MySQL（现有）、Redis（缓存）。
  - 监控：Micrometer + Prometheus（如已有）/日志。

重难点分析（Risks & Mitigations）
- LLM稳定性与延迟：外部API超时/波动导致用户等待。
  - 对策：`WebClient` + `Resilience4j` 超时2s、重试退避、熔断；并行预计算与缓存；超时回退FSRS。
- 成本控制与配额：高QPS/大prompt导致超预算或429。
  - 对策：请求去重（同用户同偏好hash）、结果缓存（1h）、速率限制（全局+每用户）、prompt精简、服务级配额监控告警。
- 幻觉与错误推荐：LLM输出不规范/理由不可信。
  - 对策：强制JSON模式/Schema校验、候选集限制（先筛Top-N候选再让LLM重排）、置信度阈值与FSRS融合。
- 数据一致性与缓存失效：学习行为变化后旧推荐误导。
  - 对策：基于事件的失效（复习/提交/打分后失效相关Key）、短TTL、用户粒度Key。
- 多模型/多提供商差异：切换模型影响效果与调用方式。
  - 对策：Provider接口抽象 + 工厂 + 配置可热切换；A/B分流记录模型与Prompt版本。
- 前后端解释一致性：展示原因与分数的口径不一致。
  - 对策：后端统一生成解释字段与评分，前端只渲染；版本号随响应返回。

后端设计细化
[ ] 包结构
  - `com.codetop.recommendation.controller.ProblemRecommendationController`
  - `com.codetop.recommendation.dto.{AIRecommendationRequest, AIRecommendationResponse, RecommendationItemDTO, FeedbackRequest}`
  - `com.codetop.recommendation.service.{RecommendationFacade, AIRecommendationService, FsrsService}`
  - `com.codetop.recommendation.provider.{LlmProvider, OpenAiProvider, AzureOpenAiProvider, MockProvider}`
  - `com.codetop.recommendation.alg.{ProblemRanker, SimilarityScorer, HybridScheduler}`
  - `com.codetop.recommendation.cache.{Keys, RedisCaches}`
  - `com.codetop.recommendation.config.{LlmProperties, ResilienceConfig, FeatureFlags}`
  - `com.codetop.recommendation.metrics.{RecommendationMetrics}`

[ ] DTO 定义（示意）
  - `AIRecommendationRequest`
    - `limit:int`(默认10), `difficultyPreference:optional`, `topicFilter:list<string>`, `recommendationType:enum{hybrid,llm_only,fsrs_fallback}`
    - `abGroup:string`(实验分组), `clientVersion:string`。
  - `RecommendationItemDTO`
    - `problemId:long`, `reason:string`, `confidence:double[0..1]`, `strategy:string`(如 "progressive", "coverage"), `source:string`("LLM"|"FSRS"|"HYBRID"), `explanations:string[]`, `model:string`, `promptVersion:string`, `latencyMs:int`, `score:double`。
  - `AIRecommendationResponse`
    - `items:List<RecommendationItemDTO>`, `meta:{cached:boolean, traceId:string, generatedAt:instant}`。
  - `FeedbackRequest`
    - `recommendationId:string`, `problemId:long`, `action:enum{accepted,skipped,solved,hidden}`, `helpful:boolean`, `rating:int(1..5)`, `comment?:string`。

[ ] Provider 抽象
  - `LlmProvider#rankCandidates(UserProfile, List<ProblemCandidate>, PromptOptions, RequestContext): LlmResult`
  - `OpenAiProvider`: 使用 `chat.completions`，`response_format: json_schema`（可用时），超时2s；`WebClient`配置连接池与超时。
  - `AzureOpenAiProvider`: endpoint/key 从配置读取；兼容相同接口。
  - `MockProvider`: 测试环境注入固定输出。
  - `DefaultProvider`: 责任链最终节点；根据策略返回“系统繁忙”响应或触发 FSRS 回退标记。

[ ] 责任链组件
  - `ProviderChain`（服务）：从 `LlmChainProperties` 加载节点列表，逐个检查 `enabled/conditions`，并在错误命中 `onErrorsToNext` 时下钻到下一个节点；若全部失败则委派 `DefaultProvider`。
  - `LlmChainProperties`（配置）：`nodes[{name,enabled,conditions,timeoutMs,rateLimit,retry,onErrorsToNext}]`，`defaultProvider{strategy,message,httpStatus}`。
  - `ChainNodeConditions`：AB 分组、路由、用户段等匹配。
  - 错误分类常量：`TIMEOUT, HTTP_429, HTTP_5XX, PARSING_ERROR, CIRCUIT_OPEN`。

[ ] 混合推荐流程（Hybrid）
  - 步骤：
    1) FSRS/历史构建 `UserProfile`（强弱项、进度、目标）。
    2) 候选集生成：基于标签/难度/相似度筛Top-50。
    3) Prompt组装：压缩为结构化JSON上下文（用户画像要点、候选摘要）。
    4) LLM重排/补充理由与置信度；返回Top-K。
    5) 打分融合：`finalScore = α*LLM + β*FSRS + γ*similarity`，并限制同Topic密度实现多样性。
    6) 缓存与回传元信息（model、promptVersion、abGroup）。
  - 超时/失败：跳过步骤4，使用FSRS+相似度本地排序作为回退；`source=FSRS`。

[ ] 错误处理与可恢复性
  - 错误分类：`CLIENT_4XX`（参数/配额）、`SERVER_5XX`、`TIMEOUT`、`PARSING_ERROR`、`CIRCUIT_OPEN`。
  - 策略：4xx（非限流）降级并记录；429 触发更严格限速与回退；5xx/超时重试最多1次指数退避；解析失败二次尝试“JSON提取器”。
  - 责任链：命中 `onErrorsToNext` 列表的错误时切换至下一个 Provider；全部失败时交由 `DefaultProvider` 输出“系统繁忙”。

[ ] 限流与熔断
  - 依赖 `resilience4j`：`RateLimiter`（全局与每用户），`CircuitBreaker`（按Provider），`Bulkhead`（并发限制），`TimeLimiter`（2s）。
  - 限流参数：全局5 RPS、每用户1 RPS（示例，可调），熔断失败率阈值50%，滑窗20。

[ ] 缓存策略（Redis）
  - Key 约定：
    - 用户画像：`user_profile:{userId}:v{ver}` TTL=1h
    - 推荐结果：`rec:{userId}:{prefsHash}:{model}:{promptV}` TTL=1h
  - 失效触发：提交解题、复习日志新增、反馈提交 → 删除用户画像与推荐结果Key。
  - 去重：对同key短期重复请求直接命中缓存；后台异步刷新。
  - 责任链配合：缓存命中时直接绕过责任链；缓存未命中时按责任链执行。

[ ] 指标与日志
  - 指标：`llm.requests_total{provider,model,outcome}`、`llm.latency_ms`、`llm.timeouts_total`、`rec.cache_hit_ratio`、`rec.hybrid_fallback_ratio`、`rec.ab_group_share`、`rec.cost_estimate_tokens`、`llm.chain.hops`、`llm.chain.default_hits_total`。
  - 追踪：生成 `traceId`，串联前端/后端日志；记录`promptVersion/model/abGroup`。
  - 责任链日志：记录链顺序、命中节点、错误及下钻原因、最终策略（busy_message/fsrs_fallback）。

配置与安全
[ ] `application.yml` 片段（示例）
  - `llm:`
    - `enabled: true`
    - `provider: openai|azure|mock`
    - `openai: { baseUrl, apiKeyEnv: OPENAI_API_KEY, model: gpt-4o-mini, timeoutMs: 1800 }`
    - `azure: { endpoint, apiKeyEnv: AZURE_OPENAI_KEY, deployment, apiVersion }`
    - `prompt: { version: v1, maxCandidates: 50, maxTokens: 800 }`
    - `rolloutPercentage: 20`（A/B门控）
  - `resilience4j`：rateLimiter/circuitBreaker 配置。
[ ] 环境变量管理
  - 更新 `.env.example`：`OPENAI_API_KEY=`, `AZURE_OPENAI_KEY=`；在 CI/容器以密文注入；本地用 `.env`，严禁提交。
[ ] 特性开关
  - `@ConditionalOnProperty("llm.enabled")`；用户级灰度通过 `rolloutPercentage` + hash(userId) 或服务端A/B分组。

Prompt 设计与解析
[ ] Prompt 模板（系统/用户消息）
  - 系统：明确角色与边界，输出必须为JSON，禁止编造问题ID。
  - 用户：提供用户画像摘要、候选问题的关键信息（id、topic、difficulty、tags、历史正确率等），以及目标（巩固薄弱、渐进难度、覆盖多样性）。
[ ] JSON Schema（约束输出）
  - 字段：`items[{problemId, reason, confidence, strategy, score}]`，长度≤`limit`。
  - 解析：优先采用`response_format: json_schema`；否则使用正则截取`{...}`并 `ObjectMapper` 严格映射，失败则降级一次“纠错提示”重试。
[ ] Prompt 版本化
  - `promptVersion=v1` 初始；每次变更同步在响应 `meta.promptVersion`，并记录在日志与监控。

数据模型与迁移
[ ] `Problem` 实体
  - 新增字段：`content TEXT`（可为空，后续回填）；`topics TEXT[]` 或标签表关联（按现有设计选型）。
[ ] 索引与查询
  - 若数据库支持全文/向量：创建 `GIN`/全文索引加速候选筛选；否则靠现有标签与难度索引。
[ ] 迁移脚本
  - 在根目录新增 `Vxxx__add_problem_content.sql`（或 Flyway/Liquibase 方案）；测试资源 `src/test/resources` 提供样例数据。

API 规格（后端）
[ ] `GET /api/v1/problems/ai-recommendations`
  - 请求参数：`limit`(1..50, 默认10), `difficulty_preference`, `topic_filter[]`, `recommendation_type`, `ab_group?`
  - 响应：`AIRecommendationResponse`
  - 头部：`X-Trace-Id`, `X-Rec-Source`（LLM|FSRS|HYBRID|DEFAULT）, `X-Cache-Hit`（true/false）, `X-Provider-Chain`（如 `openai>azure>default`）
[ ] `POST /api/v1/problems/{id}/recommendation-feedback`
  - 请求体：`FeedbackRequest`
  - 响应：`{ status: "ok", recordedAt }`
  - 侧写：异步写入分析事件表/队列（若无队列，先落库）。
[ ] 校验与错误码
  - 400（参数不合法）、429（限流）、503（熔断/上游不可用或 DefaultProvider busy_message 策略）。
  - 当 `defaultProvider.strategy=fsrs_fallback` 时，HTTP 200 返回 FSRS 结果并在 `meta.busy=true` 与 `meta.strategy=fsrs_fallback` 标记。

前端集成细化（Next.js + TS）
[ ] 组件与页面
  - `components/recommendation/AIRecommendationCard.tsx`：展示题目、解释、置信度、来源、反馈按钮。
  - `components/recommendation/RecommendationToggle.tsx`：FSRS/AI切换；保存用户偏好。
  - 页面：`app/dashboard/recommendations/page.tsx`；列表页标注AI标记与过滤。
[ ] 数据与类型
  - `types/recommendation.ts`：与后端DTO对齐；`RecommendationItem`、`AIRecommendationResponse`。
[ ] 状态与请求
  - 使用 `SWR`/`React Query` 带缓存与重试；错误态骨架屏；`traceId` 透传至请求头。
[ ] 反馈收集
  - 交互：点赞/点踩/跳过/已解决；调用反馈API；埋点。
  - 忙碌态处理：当 `meta.busy=true` 或 `X-Rec-Source=DEFAULT` 时，展示“系统繁忙，请稍后重试”提示；若后端采用 `fsrs_fallback` 策略，则正常渲染 FSRS 结果并弱提示。
[ ] E2E 场景
  - 首屏加载、切换FSRS/AI、分页/懒加载、异常回退显示、反馈上报。

测试计划（分层）
[ ] 单元测试（Java）
  - `OpenAiProviderTest`：超时/429/解析失败/JSON校验；MockWebServer。
  - `HybridSchedulerTest`：融合打分、去重、多样性约束。
  - `RecommendationFacadeTest`：缓存命中、回退路径。
  - `ProviderChainTest`：
    - 节点顺序与启停匹配；`onErrorsToNext` 下钻逻辑；
    - 全部失败命中 `DefaultProvider`；不同 `default.strategy`（busy_message / fsrs_fallback / empty）行为验证；
    - 指标与日志包含链路细节；
    - profile 切换（dev/test/prod）影响链配置的集成校验。
[ ] 集成测试
  - 使用 Testcontainers（Redis + DB）；端到端调 `GET /ai-recommendations` 在 `llm.enabled=false` 与 `mockProvider` 两种模式下。
[ ] 合同测试
  - 使用 Spring REST Docs 或 OpenAPI 校验前后端契约。
[ ] 前端测试
  - 组件快照、交互单测；Playwright E2E（加载/切换/反馈/错误）。
[ ] 性能与压测
  - 示例目标：P95 < 800ms（缓存命中），P95 < 2s（含LLM）；并发100时错误率<1%。
  - Gatling/JMeter 脚本放置 `scripts/perf/`。

监控与告警
[ ] 指标阈值与SLO
  - 成功率≥99%，P95延迟阈值报警，熔断开启报警，缓存命中率低报警（<50%）。
[ ] 日志与追踪
  - 统一`traceId`贯穿；敏感字段脱敏；失败包含`provider/model/promptVersion`。
[ ] 成本监控
  - 每日token使用量、错误率、429次数；超预算提醒。

成本与预算控制
[ ] Prompt压缩
  - 候选摘要化（不传全文）；限制候选数量；分批分段排名（分治）。
[ ] 缓存与去重
  - 相同偏好1小时复用；冷启动预热热门用户画像。
[ ] 退级策略
  - 高负载/高成本时自动提高FSRS权重、降低LLM调用频率。

安全与隐私
[ ] 密钥与配置
  - 从环境变量读取；不落日志；支持密钥轮转（多key尝试，主备切换）。
[ ] 数据最小化
  - Prompt中不包含可识别个人信息；仅传画像摘要与候选信息。
[ ] 数据留存
  - LLM响应存留7天用于排障（可配置）；超过即清理；敏感内容脱敏。

灰度与A/B
[ ] 分流策略
  - `rolloutPercentage` 基于 `hash(userId)`；或请求头 `X-Exp-Group` 控制；将分组、模型、Prompt版本回传与埋点。
[ ] KPI 与评估
  - 采纳率、完成率、下一日留存、学习时长、反馈评分；统计置信区间与显著性。

时间线与里程碑（建议）
1. 架构与接口确定（0.5d）
2. Provider与AIRecommendationService（1.5d）
3. 混合算法与缓存/限流（1.5d）
4. API与DTO/Controller/测试（1d）
5. 前端基础集成与UI（1d）
6. 监控/告警/成本（0.5d）
7. 灰度上线与评估（持续）

验收标准（扩展）
- 对`llm.enabled=false`：系统完全可用（FSRS-only），行为与历史一致。
- 对`llm.enabled=true`：
  - 正常路径成功率≥99%，P95延迟符合目标；缓存命中率≥60%。
  - 报文包含`traceId/model/promptVersion`；日志无敏感信息泄漏。
  - 429/超时发生时自动回退，无用户可见错误（UI优雅降级）。
  - A/B实验指标显示AI组相较基线提升（采纳率、完成率≥+5%）。

需细致完成的关键点 Checklist
[ ] Provider抽象与错误分类完备，任何异常均有明确降级路径。
[ ] Prompt模板与JSON Schema固定，解析稳健（双重校验）。
[ ] 缓存键设计与失效策略经过审计，避免“脏推荐”。
[ ] 限流/熔断参数可配置并有仪表盘可见性。
[ ] DTO与前端类型对齐，字段含义清晰且有注释。
[ ] 指标、日志字段标准化，便于排障与A/B统计。
[ ] 成本测算（tokens估算）与预算阈值预警已落地。
[ ] 安全合规（密钥、脱敏、留存周期）通过审查。

附录：Prompt 示例（v1 草案）
- 系统消息
  - 你是编程刷题推荐专家。仅输出JSON，遵循给定Schema；不要杜撰问题ID；侧重覆盖薄弱知识点并保持难度渐进。
- 用户消息（示意字段）
  - userProfile: { weakTopics: ["graph", "dp"], strengths:["array"], target:"cover_weakness_first" }
  - candidates: [{ id, topic, difficulty, tags:["graph","bfs"], recentAccuracy:0.4, attempts:2, estTimeMin:15 }, ...]
  - outputLimit: 10
  - 输出Schema: { items: [{ problemId:number, reason:string, confidence:number(0..1), strategy:string, score:number }] }

附录：OpenAPI 概要（YAML 片段）
- GET /api/v1/problems/ai-recommendations
  - parameters: limit, difficulty_preference, topic_filter, recommendation_type
  - responses: 200 { AIRecommendationResponse }
- POST /api/v1/problems/{id}/recommendation-feedback
  - requestBody: FeedbackRequest
  - responses: 200 { status }

---

按消费等级/用户分层的责任链设计（高级）
- 目标：
  - 按用户消费/会员等级（FREE/BRONZE/SILVER/GOLD/PLATINUM）、AB 组、路由、地域，选择不同责任链，控制成本与体验。
  - 新增/调整链无需改码，仅改配置；同用户在一段时间内命中同一条链（粘性）。
- 组件：
  - `ProviderCatalog`：可用 Provider 模板库（openai/azure/mock/default）。
  - `LlmChainRegistry`：注册多条链（`chainId/version/nodes/defaultProvider`）。
  - `ChainPolicyResolver`：依据 `RequestContext{ userId,tier,abGroup,route,region,time }` 选择 `chainId`，支持优先级、权重分流、粘性。
  - `RequestContext`：从 `RecommendationFacade` 注入（含 `traceId`）。
- 缓存：
  - 结果 Key 纳入链维度：`rec:{userId}:{prefsHash}:{chainId}:{promptV}`，不同链互不污染；链可定义不同 TTL。
- 响应头与指标：
  - 头部：`X-Chain-Id`, `X-Chain-Version`, `X-Policy-Id`, `X-Provider-Chain`。
  - 指标：`llm.chain.requests_total{chainId,policyId,outcome}`, `llm.chain.hops`, `llm.chain.default_hits_total`；成本指标 `llm.cost.tokens{chainId,model}`。

YAML 高级配置（多链 + 等级 + 策略）
```yaml
llm:
  enabled: true
  provider: chain
  chains:
    - id: std_chain
      version: v1
      nodes:
        - providerRef: openai.gpt4o-mini
          timeoutMs: 1800
          rateLimit: { rps: 5, perUserRps: 1 }
          retry: { attempts: 1, backoffMs: 200 }
          onErrorsToNext: [TIMEOUT, HTTP_429, HTTP_5XX, PARSING_ERROR, CIRCUIT_OPEN]
          costCap: { tokensPerReq: 4000 }
        - providerRef: azure.gpt4o-mini
          onErrorsToNext: [TIMEOUT, HTTP_5XX, PARSING_ERROR]
      defaultProvider: { strategy: fsrs_fallback, message: "系统繁忙，请稍后重试", httpStatus: 503 }
    - id: gold_chain
      version: v2
      nodes:
        - providerRef: openai.gpt4o
          timeoutMs: 2000
          rateLimit: { rps: 20, perUserRps: 3 }
          retry: { attempts: 1 }
          costCap: { tokensPerReq: 12000 }
        - providerRef: openai.gpt4o-mini
          onErrorsToNext: [TIMEOUT, HTTP_429, COST_EXCEEDED]
      defaultProvider: { strategy: fsrs_fallback }
    - id: bronze_chain
      version: v1
      nodes:
        - providerRef: openai.gpt4o-mini
          costCap: { tokensPerReq: 2000 }
      defaultProvider: { strategy: busy_message }
  providers:
    openai:
      gpt4o: { model: gpt-4o, baseUrl: https://api.openai.com/v1, apiKeyEnv: OPENAI_API_KEY }
      gpt4o-mini: { model: gpt-4o-mini, baseUrl: https://api.openai.com/v1, apiKeyEnv: OPENAI_API_KEY }
    azure:
      gpt4o-mini: { endpoint: https://xxx.openai.azure.com, deployment: gpt4o-mini, apiKeyEnv: AZURE_OPENAI_KEY }
  policies:
    - id: gold-users
      priority: 10
      match: { tier: [GOLD, PLATINUM], route: [ai-recommendations] }
      chainId: gold_chain
      sticky: { key: userId, ttlHours: 72 }
    - id: ab-test
      priority: 20
      match: { tier: [SILVER], abGroup: [A] }
      weighted:
        - { chainId: std_chain, weight: 70 }
        - { chainId: gold_chain, weight: 30 }
      sticky: { key: userId, ttlHours: 72 }
    - id: bronze-default
      priority: 100
      match: { tier: [BRONZE, FREE] }
      chainId: bronze_chain
  defaultChainId: std_chain
```

执行与测试要点
- 策略匹配：按 `priority` 自顶向下；权重分流用一致性哈希基于 `sticky.key` 保证同用户粘性。
- 链执行：覆盖 TIMEOUT/429/5XX/PARSING_ERROR/COST_EXCEEDED 的下钻；全部失败由 `DefaultProvider` 兜底（busy_message/fsrs_fallback/empty）。
- 验证观测：响应头含 `X-Chain-Id/X-Chain-Version/X-Policy-Id`；指标含 `chainId/policyId` 维度；缓存命中按 `chainId` 隔离。
